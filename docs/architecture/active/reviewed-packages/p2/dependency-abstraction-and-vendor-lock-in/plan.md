# Plan: Dependency Abstraction And Vendor Lock-In

1. Inventory direct usages of Redis, RocketMQ, Groovy, WebClient, and React Flow APIs.
2. Identify high-value abstractions: message bus, distributed lock, rate limiter, expression engine, external client.
3. Add interfaces only where there are multiple call sites or clear migration/testing value.
4. Migrate one dependency slice at a time with tests.
5. Document alternatives and non-goals.
