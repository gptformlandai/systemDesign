# 3. End-to-End System Design Practice

> Modular problem bank for end-to-end system design practice.

Use this folder when you want to practice full system design problems by category. Each document keeps the same learning structure: requirements, scale, HLD, APIs, data model, reliability, trade-offs, LLD, machine-coding layer, spike handling, and interview playbook.

---

## Study Protocol

Use each problem in this order:

1. Read the goal and "How To Use This File".
2. Walk through the starter learning path.
3. Build the HLD from memory before reading the full architecture.
4. Trace one happy path and one failure path.
5. Study the data model and identify the source of truth.
6. Practice the machine-coding section without copying.
7. End with the final interview playbook and fast recall rules.

Gold bar for every problem:

- You can explain the MVP first.
- You can draw the scalable design second.
- You can defend the main trade-off.
- You can discuss retries, idempotency, backpressure, observability, and data growth.
- You can clearly say what not to build yet.

---

## 01. Core Infrastructure

- [URL Shortener](01-Core-Infrastructure/URL-Shortener-End-to-End-System-Design.md)
- [API Gateway](01-Core-Infrastructure/API-Gateway-End-to-End-System-Design.md)
- [Notification System](01-Core-Infrastructure/Notification-System-End-to-End-System-Design.md)

## 02. Caching Systems

- [LRU Cache](02-Caching-Systems/LRU-Cache-End-to-End-System-Design.md)
- [LFU Cache](02-Caching-Systems/LFU-Cache-End-to-End-System-Design.md)
- [CDN Cache](02-Caching-Systems/CDN-Cache-End-to-End-System-Design.md)
- [Distributed Cache](02-Caching-Systems/Distributed-Cache-End-to-End-System-Design.md)

## 03. Feeds / Social Systems

- [News Feed](03-Feeds-Social-Systems/News-Feed-End-to-End-System-Design.md)
- [Instagram / Facebook Feed](03-Feeds-Social-Systems/Instagram-Facebook-Feed-End-to-End-System-Design.md)
- [Twitter (X) Feed](03-Feeds-Social-Systems/Twitter-X-Feed-End-to-End-System-Design.md)
- [TikTok Video Feed and Recommendations](03-Feeds-Social-Systems/TikTok-Video-Feed-Recommendations-End-to-End-System-Design.md)
- [LinkedIn Feed](03-Feeds-Social-Systems/LinkedIn-Feed-End-to-End-System-Design.md)
- [Reddit / Discussion Forum](03-Feeds-Social-Systems/Reddit-Discussion-Forum-End-to-End-System-Design.md)

## 04. Media Streaming Systems

- [YouTube](04-Media-Streaming-Systems/YouTube-End-to-End-System-Design.md)
- [Netflix](04-Media-Streaming-Systems/Netflix-End-to-End-System-Design.md)
- [Spotify](04-Media-Streaming-Systems/Spotify-End-to-End-System-Design.md)

## 05. Messaging / Realtime Systems

- [Chat System](05-Messaging-Realtime-Systems/Chat-System-End-to-End-System-Design.md)
- [WhatsApp](05-Messaging-Realtime-Systems/WhatsApp-End-to-End-System-Design.md)
- [Messenger](05-Messaging-Realtime-Systems/Messenger-End-to-End-System-Design.md)
- [Slack](05-Messaging-Realtime-Systems/Slack-End-to-End-System-Design.md)
- [Discord](05-Messaging-Realtime-Systems/Discord-End-to-End-System-Design.md)

## 06. Transaction / Booking Systems

- [Uber / Ola](06-Transaction-Booking-Systems/Uber-Ola-End-to-End-System-Design.md)
- [BookMyShow](06-Transaction-Booking-Systems/BookMyShow-End-to-End-System-Design.md)
- [Airline Booking System](06-Transaction-Booking-Systems/Airline-Booking-System-End-to-End-System-Design.md)
- [Food Delivery (Swiggy / Zomato)](06-Transaction-Booking-Systems/Food-Delivery-Swiggy-Zomato-End-to-End-System-Design.md)

## 07. Storage / Database Systems

- [Dropbox / Google Drive](07-Storage-Database-Systems/Dropbox-Google-Drive-End-to-End-System-Design.md)
- [File Storage System](07-Storage-Database-Systems/File-Storage-System-End-to-End-System-Design.md)
- [Key-Value Store (Redis-like)](07-Storage-Database-Systems/Key-Value-Store-Redis-Like-End-to-End-System-Design.md)
- [Log Storage System](07-Storage-Database-Systems/Log-Storage-System-End-to-End-System-Design.md)

## 08. Concurrency / Machine Coding Classics

- [Rate Limiter](08-Concurrency-Machine-Coding-Classics/Rate-Limiter-End-to-End-System-Design.md)
- [Producer-Consumer](08-Concurrency-Machine-Coding-Classics/Producer-Consumer-End-to-End-System-Design.md)
- [Thread Pool](08-Concurrency-Machine-Coding-Classics/Thread-Pool-End-to-End-System-Design.md)
- [Blocking Queue](08-Concurrency-Machine-Coding-Classics/Blocking-Queue-End-to-End-System-Design.md)
- [Logger System](08-Concurrency-Machine-Coding-Classics/Logger-System-End-to-End-System-Design.md)

## 09. E-Commerce / Product Systems

- [Amazon E-Commerce System](09-Ecommerce-Product-Systems/Amazon-E-Commerce-System-End-to-End-System-Design.md)
- [Shopping Cart](09-Ecommerce-Product-Systems/Shopping-Cart-End-to-End-System-Design.md)
- [Payment System](09-Ecommerce-Product-Systems/Payment-System-End-to-End-System-Design.md)
- [Coupon / Discount Engine](09-Ecommerce-Product-Systems/Coupon-Discount-Engine-End-to-End-System-Design.md)
- [Payment Workflow](09-Ecommerce-Product-Systems/Payment-Workflow-End-to-End-System-Design.md)

## 10. Graph / Search / Recommendation Systems

- [Search Autocomplete](10-Graph-Search-Recommendation-Systems/Search-Autocomplete-End-to-End-System-Design.md)
- [Google Search](10-Graph-Search-Recommendation-Systems/Google-Search-End-to-End-System-Design.md)
- [Search Engine ElasticSearch-Like](10-Graph-Search-Recommendation-Systems/Search-Engine-ElasticSearch-Like-End-to-End-System-Design.md)
- [Recommendation System](10-Graph-Search-Recommendation-Systems/Recommendation-System-End-to-End-System-Design.md)
