from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import os


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/health":
            self._send_json({"status": "ok"})
            return

        self._send_json({"message": os.getenv("APP_MESSAGE", "hello from Docker")})

    def log_message(self, format, *args):
        print("%s - %s" % (self.address_string(), format % args), flush=True)

    def _send_json(self, payload):
        body = json.dumps(payload).encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


if __name__ == "__main__":
    server = ThreadingHTTPServer(("0.0.0.0", 8080), Handler)
    print("listening on 0.0.0.0:8080", flush=True)
    server.serve_forever()