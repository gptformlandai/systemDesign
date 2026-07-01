# Project 03: Chat Feed Storage Architecture

Goal: design storage for chat, feeds, and notifications.

Include:

- message store
- conversation index
- timeline/feed materialization
- unread counters
- notification dedupe
- message search

Discuss:

- partition key
- ordering
- hot users
- fanout-on-write vs fanout-on-read
- search lag