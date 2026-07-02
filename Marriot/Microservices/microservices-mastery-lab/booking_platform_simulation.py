#!/usr/bin/env python3
"""
Dependency-free microservices capstone simulation.

Starts three local HTTP services and one background worker:
- Gateway on 8080
- Booking Service on 8081
- Payment Service on 8082
- Notification worker polling the booking outbox

This is a learning lab, not production code. It intentionally keeps the code in one file so
the distributed flow is easy to inspect.
"""

from __future__ import annotations

import hashlib
import json
import sqlite3
import threading
import time
import uuid
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Any
from urllib import error, request


DB_PATH = "booking_lab.sqlite3"
DB_LOCK = threading.Lock()
STOP_EVENT = threading.Event()


def now_ms() -> int:
    return int(time.time() * 1000)


def new_id(prefix: str) -> str:
    return f"{prefix}_{uuid.uuid4().hex[:12]}"


def canonical_json(value: Any) -> str:
    return json.dumps(value, sort_keys=True, separators=(",", ":"))


def request_hash(payload: dict[str, Any]) -> str:
    return hashlib.sha256(canonical_json(payload).encode("utf-8")).hexdigest()


def log(service: str, request_id: str, message: str, **fields: Any) -> None:
    record = {
        "ts": now_ms(),
        "service": service,
        "requestId": request_id,
        "message": message,
        **fields,
    }
    print(json.dumps(record, sort_keys=True), flush=True)


def db() -> sqlite3.Connection:
    connection = sqlite3.connect(DB_PATH, timeout=5)
    connection.row_factory = sqlite3.Row
    return connection


def init_db() -> None:
    with DB_LOCK:
        connection = db()
        try:
            connection.executescript(
                """
                CREATE TABLE IF NOT EXISTS booking (
                    booking_id TEXT PRIMARY KEY,
                    user_id TEXT NOT NULL,
                    hotel_id TEXT NOT NULL,
                    room_type TEXT NOT NULL,
                    amount INTEGER NOT NULL,
                    status TEXT NOT NULL,
                    payment_id TEXT,
                    created_at_ms INTEGER NOT NULL,
                    updated_at_ms INTEGER NOT NULL
                );

                CREATE TABLE IF NOT EXISTS booking_idempotency (
                    user_id TEXT NOT NULL,
                    idempotency_key TEXT NOT NULL,
                    payload_hash TEXT NOT NULL,
                    booking_id TEXT NOT NULL,
                    response_json TEXT,
                    created_at_ms INTEGER NOT NULL,
                    PRIMARY KEY (user_id, idempotency_key)
                );

                CREATE TABLE IF NOT EXISTS payment_idempotency (
                    idempotency_key TEXT PRIMARY KEY,
                    payload_hash TEXT NOT NULL,
                    payment_id TEXT NOT NULL,
                    response_json TEXT NOT NULL,
                    created_at_ms INTEGER NOT NULL
                );

                CREATE TABLE IF NOT EXISTS outbox (
                    event_id TEXT PRIMARY KEY,
                    event_type TEXT NOT NULL,
                    aggregate_id TEXT NOT NULL,
                    payload_json TEXT NOT NULL,
                    status TEXT NOT NULL,
                    attempts INTEGER NOT NULL DEFAULT 0,
                    created_at_ms INTEGER NOT NULL,
                    updated_at_ms INTEGER NOT NULL
                );
                """
            )
            connection.commit()
        finally:
            connection.close()


def read_json(handler: BaseHTTPRequestHandler) -> dict[str, Any]:
    length = int(handler.headers.get("Content-Length", "0"))
    body = handler.rfile.read(length) if length else b"{}"
    if not body:
        return {}
    return json.loads(body.decode("utf-8"))


def send_json(
    handler: BaseHTTPRequestHandler,
    status: int,
    payload: dict[str, Any],
    request_id: str,
) -> None:
    encoded = json.dumps(payload, sort_keys=True).encode("utf-8")
    handler.send_response(status)
    handler.send_header("Content-Type", "application/json")
    handler.send_header("Content-Length", str(len(encoded)))
    handler.send_header("X-Request-Id", request_id)
    handler.end_headers()
    try:
        handler.wfile.write(encoded)
    except BrokenPipeError:
        pass


def http_post_json(
    url: str,
    payload: dict[str, Any],
    headers: dict[str, str],
    timeout_seconds: float,
) -> tuple[int, dict[str, Any]]:
    data = json.dumps(payload).encode("utf-8")
    req = request.Request(
        url,
        data=data,
        headers={"Content-Type": "application/json", **headers},
        method="POST",
    )
    with request.urlopen(req, timeout=timeout_seconds) as response:
        body = response.read().decode("utf-8")
        return response.status, json.loads(body) if body else {}


def http_get_json(url: str, headers: dict[str, str], timeout_seconds: float) -> tuple[int, dict[str, Any]]:
    req = request.Request(url, headers=headers, method="GET")
    with request.urlopen(req, timeout=timeout_seconds) as response:
        body = response.read().decode("utf-8")
        return response.status, json.loads(body) if body else {}


class GatewayHandler(BaseHTTPRequestHandler):
    service_name = "gateway"

    def do_GET(self) -> None:
        request_id = self.headers.get("X-Request-Id") or new_id("req")
        if self.path == "/health":
            send_json(self, 200, {"status": "UP", "service": self.service_name}, request_id)
            return
        send_json(self, 404, {"error": "not_found"}, request_id)

    def do_POST(self) -> None:
        request_id = self.headers.get("X-Request-Id") or new_id("req")
        if self.path != "/bookings":
            send_json(self, 404, {"error": "not_found"}, request_id)
            return

        payload = read_json(self)
        idempotency_key = self.headers.get("Idempotency-Key")
        if not idempotency_key:
            send_json(self, 400, {"error": "missing_idempotency_key"}, request_id)
            return

        log(self.service_name, request_id, "forwarding create booking")
        try:
            status, response_payload = http_post_json(
                "http://localhost:8081/bookings",
                payload,
                {
                    "X-Request-Id": request_id,
                    "Idempotency-Key": idempotency_key,
                },
                timeout_seconds=3,
            )
            send_json(self, status, response_payload, request_id)
        except error.HTTPError as exc:
            body = exc.read().decode("utf-8")
            response_payload = json.loads(body) if body else {"error": "downstream_http_error"}
            send_json(self, exc.code, response_payload, request_id)
        except Exception as exc:  # noqa: BLE001 - this is a learning lab gateway boundary
            log(self.service_name, request_id, "booking service unavailable", error=str(exc))
            send_json(self, 502, {"error": "booking_service_unavailable"}, request_id)

    def log_message(self, format: str, *args: Any) -> None:
        return


class BookingHandler(BaseHTTPRequestHandler):
    service_name = "booking-service"

    def do_GET(self) -> None:
        request_id = self.headers.get("X-Request-Id") or new_id("req")
        if not self.path.startswith("/bookings/"):
            send_json(self, 404, {"error": "not_found"}, request_id)
            return

        booking_id = self.path.rsplit("/", 1)[-1]
        with DB_LOCK:
            connection = db()
            try:
                row = connection.execute(
                    "SELECT * FROM booking WHERE booking_id = ?",
                    (booking_id,),
                ).fetchone()
            finally:
                connection.close()

        if not row:
            send_json(self, 404, {"error": "booking_not_found"}, request_id)
            return

        send_json(self, 200, dict(row), request_id)

    def do_POST(self) -> None:
        request_id = self.headers.get("X-Request-Id") or new_id("req")
        if self.path != "/bookings":
            send_json(self, 404, {"error": "not_found"}, request_id)
            return

        payload = read_json(self)
        idempotency_key = self.headers.get("Idempotency-Key")
        if not idempotency_key:
            send_json(self, 400, {"error": "missing_idempotency_key"}, request_id)
            return

        required = ["userId", "hotelId", "roomType", "amount"]
        missing = [field for field in required if field not in payload]
        if missing:
            send_json(self, 400, {"error": "missing_fields", "fields": missing}, request_id)
            return

        user_id = str(payload["userId"])
        payload_hash = request_hash(payload)

        existing = self.find_idempotency(user_id, idempotency_key)
        if existing:
            if existing["payload_hash"] != payload_hash:
                send_json(
                    self,
                    409,
                    {
                        "error": "idempotency_key_reused_with_different_payload",
                        "bookingId": existing["booking_id"],
                    },
                    request_id,
                )
                return
            if existing["response_json"]:
                response_payload = json.loads(existing["response_json"])
                response_payload["idempotentReplay"] = True
                send_json(self, 200, response_payload, request_id)
                return
            send_json(
                self,
                202,
                {"status": "IN_PROGRESS", "bookingId": existing["booking_id"]},
                request_id,
            )
            return

        booking_id = new_id("booking")
        self.create_pending_booking(booking_id, user_id, idempotency_key, payload_hash, payload)

        log(self.service_name, request_id, "created pending booking", bookingId=booking_id)

        payment_status, payment_payload = self.authorize_payment(
            request_id=request_id,
            booking_id=booking_id,
            payload=payload,
        )

        if payment_status == 200:
            status = "CONFIRMED"
            payment_id = payment_payload["paymentId"]
            response_payload = {
                "bookingId": booking_id,
                "status": status,
                "paymentId": payment_id,
                "requestId": request_id,
            }
            self.confirm_booking(booking_id, payment_id, response_payload)
            log(self.service_name, request_id, "booking confirmed", bookingId=booking_id)
            send_json(self, 201, response_payload, request_id)
            return

        if payment_status == 402:
            response_payload = {
                "bookingId": booking_id,
                "status": "PAYMENT_FAILED",
                "reason": "payment_declined",
                "requestId": request_id,
            }
            self.finish_failed_booking(booking_id, response_payload)
            log(self.service_name, request_id, "booking payment failed", bookingId=booking_id)
            send_json(self, 402, response_payload, request_id)
            return

        response_payload = {
            "bookingId": booking_id,
            "status": "PAYMENT_UNKNOWN",
            "reason": "payment_timeout_or_unavailable",
            "requestId": request_id,
            "nextStep": "reconcile_payment_with_same_idempotency_key",
        }
        self.finish_unknown_booking(booking_id, response_payload)
        log(self.service_name, request_id, "booking payment unknown", bookingId=booking_id)
        send_json(self, 202, response_payload, request_id)

    @staticmethod
    def find_idempotency(user_id: str, idempotency_key: str) -> sqlite3.Row | None:
        with DB_LOCK:
            connection = db()
            try:
                return connection.execute(
                    """
                    SELECT * FROM booking_idempotency
                    WHERE user_id = ? AND idempotency_key = ?
                    """,
                    (user_id, idempotency_key),
                ).fetchone()
            finally:
                connection.close()

    @staticmethod
    def create_pending_booking(
        booking_id: str,
        user_id: str,
        idempotency_key: str,
        payload_hash: str,
        payload: dict[str, Any],
    ) -> None:
        with DB_LOCK:
            connection = db()
            try:
                timestamp = now_ms()
                connection.execute(
                    """
                    INSERT INTO booking
                    (booking_id, user_id, hotel_id, room_type, amount, status, created_at_ms, updated_at_ms)
                    VALUES (?, ?, ?, ?, ?, 'PENDING', ?, ?)
                    """,
                    (
                        booking_id,
                        user_id,
                        str(payload["hotelId"]),
                        str(payload["roomType"]),
                        int(payload["amount"]),
                        timestamp,
                        timestamp,
                    ),
                )
                connection.execute(
                    """
                    INSERT INTO booking_idempotency
                    (user_id, idempotency_key, payload_hash, booking_id, response_json, created_at_ms)
                    VALUES (?, ?, ?, ?, NULL, ?)
                    """,
                    (user_id, idempotency_key, payload_hash, booking_id, timestamp),
                )
                connection.commit()
            finally:
                connection.close()

    @staticmethod
    def authorize_payment(
        request_id: str,
        booking_id: str,
        payload: dict[str, Any],
    ) -> tuple[int, dict[str, Any]]:
        payment_payload = {
            "bookingId": booking_id,
            "amount": int(payload["amount"]),
            "simulatePayment": payload.get("simulatePayment", "success"),
        }
        payment_key = f"payment-{booking_id}"
        try:
            return http_post_json(
                "http://localhost:8082/payments/authorize",
                payment_payload,
                {
                    "X-Request-Id": request_id,
                    "Idempotency-Key": payment_key,
                },
                timeout_seconds=1,
            )
        except error.HTTPError as exc:
            body = exc.read().decode("utf-8")
            response_payload = json.loads(body) if body else {}
            return exc.code, response_payload
        except Exception as exc:  # noqa: BLE001 - timeout means unknown at this boundary
            log("booking-service", request_id, "payment call unknown", error=str(exc))
            return 504, {"error": "payment_unknown"}

    @staticmethod
    def confirm_booking(booking_id: str, payment_id: str, response_payload: dict[str, Any]) -> None:
        with DB_LOCK:
            connection = db()
            try:
                timestamp = now_ms()
                connection.execute(
                    """
                    UPDATE booking
                    SET status = 'CONFIRMED', payment_id = ?, updated_at_ms = ?
                    WHERE booking_id = ?
                    """,
                    (payment_id, timestamp, booking_id),
                )
                event_id = new_id("evt")
                connection.execute(
                    """
                    INSERT INTO outbox
                    (event_id, event_type, aggregate_id, payload_json, status, attempts, created_at_ms, updated_at_ms)
                    VALUES (?, 'BookingConfirmed', ?, ?, 'PENDING', 0, ?, ?)
                    """,
                    (
                        event_id,
                        booking_id,
                        json.dumps(
                            {
                                "eventId": event_id,
                                "bookingId": booking_id,
                                "paymentId": payment_id,
                                "status": "CONFIRMED",
                            },
                            sort_keys=True,
                        ),
                        timestamp,
                        timestamp,
                    ),
                )
                connection.execute(
                    """
                    UPDATE booking_idempotency
                    SET response_json = ?
                    WHERE booking_id = ?
                    """,
                    (json.dumps(response_payload, sort_keys=True), booking_id),
                )
                connection.commit()
            finally:
                connection.close()

    @staticmethod
    def finish_failed_booking(booking_id: str, response_payload: dict[str, Any]) -> None:
        BookingHandler.finish_terminal_booking(booking_id, "PAYMENT_FAILED", response_payload)

    @staticmethod
    def finish_unknown_booking(booking_id: str, response_payload: dict[str, Any]) -> None:
        BookingHandler.finish_terminal_booking(booking_id, "PAYMENT_UNKNOWN", response_payload)

    @staticmethod
    def finish_terminal_booking(booking_id: str, status: str, response_payload: dict[str, Any]) -> None:
        with DB_LOCK:
            connection = db()
            try:
                connection.execute(
                    "UPDATE booking SET status = ?, updated_at_ms = ? WHERE booking_id = ?",
                    (status, now_ms(), booking_id),
                )
                connection.execute(
                    "UPDATE booking_idempotency SET response_json = ? WHERE booking_id = ?",
                    (json.dumps(response_payload, sort_keys=True), booking_id),
                )
                connection.commit()
            finally:
                connection.close()

    def log_message(self, format: str, *args: Any) -> None:
        return


class PaymentHandler(BaseHTTPRequestHandler):
    service_name = "payment-service"

    def do_GET(self) -> None:
        request_id = self.headers.get("X-Request-Id") or new_id("req")
        if self.path == "/health":
            send_json(self, 200, {"status": "UP", "service": self.service_name}, request_id)
            return
        send_json(self, 404, {"error": "not_found"}, request_id)

    def do_POST(self) -> None:
        request_id = self.headers.get("X-Request-Id") or new_id("req")
        if self.path != "/payments/authorize":
            send_json(self, 404, {"error": "not_found"}, request_id)
            return

        payload = read_json(self)
        idempotency_key = self.headers.get("Idempotency-Key")
        if not idempotency_key:
            send_json(self, 400, {"error": "missing_idempotency_key"}, request_id)
            return

        payload_hash = request_hash(payload)
        existing = self.find_idempotency(idempotency_key)
        if existing:
            if existing["payload_hash"] != payload_hash:
                send_json(self, 409, {"error": "payment_key_reused_with_different_payload"}, request_id)
                return
            response_payload = json.loads(existing["response_json"])
            response_payload["idempotentReplay"] = True
            send_json(self, 200, response_payload, request_id)
            return

        simulate = payload.get("simulatePayment", "success")
        if simulate == "timeout":
            log(self.service_name, request_id, "simulating slow payment provider")
            time.sleep(2)

        if simulate == "decline":
            log(self.service_name, request_id, "payment declined", bookingId=payload.get("bookingId"))
            send_json(self, 402, {"error": "payment_declined"}, request_id)
            return

        payment_id = new_id("pay")
        response_payload = {
            "paymentId": payment_id,
            "status": "AUTHORIZED",
            "bookingId": payload["bookingId"],
        }
        self.store_idempotency(idempotency_key, payload_hash, payment_id, response_payload)
        log(self.service_name, request_id, "payment authorized", paymentId=payment_id)
        send_json(self, 200, response_payload, request_id)

    @staticmethod
    def find_idempotency(idempotency_key: str) -> sqlite3.Row | None:
        with DB_LOCK:
            connection = db()
            try:
                return connection.execute(
                    "SELECT * FROM payment_idempotency WHERE idempotency_key = ?",
                    (idempotency_key,),
                ).fetchone()
            finally:
                connection.close()

    @staticmethod
    def store_idempotency(
        idempotency_key: str,
        payload_hash: str,
        payment_id: str,
        response_payload: dict[str, Any],
    ) -> None:
        with DB_LOCK:
            connection = db()
            try:
                connection.execute(
                    """
                    INSERT INTO payment_idempotency
                    (idempotency_key, payload_hash, payment_id, response_json, created_at_ms)
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    (
                        idempotency_key,
                        payload_hash,
                        payment_id,
                        json.dumps(response_payload, sort_keys=True),
                        now_ms(),
                    ),
                )
                connection.commit()
            finally:
                connection.close()

    def log_message(self, format: str, *args: Any) -> None:
        return


def outbox_worker() -> None:
    service = "notification-worker"
    while not STOP_EVENT.is_set():
        event = None
        with DB_LOCK:
            connection = db()
            try:
                event = connection.execute(
                    """
                    SELECT * FROM outbox
                    WHERE status = 'PENDING'
                    ORDER BY created_at_ms
                    LIMIT 1
                    """
                ).fetchone()

                if event:
                    connection.execute(
                        """
                        UPDATE outbox
                        SET status = 'PROCESSING', attempts = attempts + 1, updated_at_ms = ?
                        WHERE event_id = ?
                        """,
                        (now_ms(), event["event_id"]),
                    )
                    connection.commit()
            finally:
                connection.close()

        if not event:
            time.sleep(0.25)
            continue

        request_id = new_id("async")
        payload = json.loads(event["payload_json"])
        log(
            service,
            request_id,
            "sending booking confirmation notification",
            eventId=event["event_id"],
            bookingId=payload["bookingId"],
        )
        time.sleep(0.1)

        with DB_LOCK:
            connection = db()
            try:
                connection.execute(
                    """
                    UPDATE outbox
                    SET status = 'PUBLISHED', updated_at_ms = ?
                    WHERE event_id = ?
                    """,
                    (now_ms(), event["event_id"]),
                )
                connection.commit()
            finally:
                connection.close()


def serve(name: str, port: int, handler: type[BaseHTTPRequestHandler]) -> ThreadingHTTPServer:
    server = ThreadingHTTPServer(("localhost", port), handler)
    thread = threading.Thread(target=server.serve_forever, name=name, daemon=True)
    thread.start()
    print(f"{name} listening on http://localhost:{port}", flush=True)
    return server


def print_examples() -> None:
    print(
        """
Try:

curl -s -X POST http://localhost:8080/bookings \\
  -H 'Content-Type: application/json' \\
  -H 'Idempotency-Key: demo-key-1' \\
  -d '{"userId":"guest-1","hotelId":"hotel-nyc-1","roomType":"king","amount":120}'

Replay the same command to see idempotency.

Payment decline:

curl -s -X POST http://localhost:8080/bookings \\
  -H 'Content-Type: application/json' \\
  -H 'Idempotency-Key: demo-key-decline' \\
  -d '{"userId":"guest-1","hotelId":"hotel-nyc-1","roomType":"king","amount":120,"simulatePayment":"decline"}'

Payment timeout / unknown state:

curl -s -X POST http://localhost:8080/bookings \\
  -H 'Content-Type: application/json' \\
  -H 'Idempotency-Key: demo-key-timeout' \\
  -d '{"userId":"guest-1","hotelId":"hotel-nyc-1","roomType":"king","amount":120,"simulatePayment":"timeout"}'
""",
        flush=True,
    )


def main() -> None:
    init_db()
    servers = [
        serve("gateway", 8080, GatewayHandler),
        serve("booking-service", 8081, BookingHandler),
        serve("payment-service", 8082, PaymentHandler),
    ]
    worker = threading.Thread(target=outbox_worker, name="notification-worker", daemon=True)
    worker.start()
    print_examples()

    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        STOP_EVENT.set()
        for server in servers:
            server.shutdown()
        print("Stopped microservices mastery lab.", flush=True)


if __name__ == "__main__":
    main()

