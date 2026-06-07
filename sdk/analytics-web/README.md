# Canvas Analytics Web SDK

Minimal browser SDK for sending governed CDP event batches to `/cdp/events/track`.

## Install

```bash
npm install @canvas/analytics-web
```

## Immediate Load

```ts
import { CanvasAnalytics } from '@canvas/analytics-web'

const analytics = CanvasAnalytics.load({
  writeKey: 'ck_live_xxx',
  serverUrl: 'https://api.example.com/cdp/events/track',
})

analytics.track('OrderComplete', { amount: 99 })
analytics.flush()
```

## Delayed Consent Load

```ts
import { createAnalytics } from '@canvas/analytics-web'

const analytics = createAnalytics()
analytics.load({
  writeKey: 'ck_live_xxx',
  serverUrl: 'https://api.example.com/cdp/events/track',
  isComplianceEnabled: true,
})

analytics.track('PageViewed')

// Events stay in memory and are not persisted or sent until consent is granted.
analytics.optIn()
await analytics.flush()
```

## Core Calls

```ts
analytics.track('OrderComplete', { amount: 99, currency: 'USD' })
analytics.identify('user-123', { vipLevel: 'gold' })
analytics.page('ProductDetail', { sku: 'SKU-1' })
analytics.group('company-1', { tier: 'enterprise' })
analytics.alias('user-123', analytics.anonymousId())
```

## Consent And Reset

```ts
analytics.optOut({ clearPersistence: true })
analytics.hasOptedOut()

analytics.optIn()
analytics.reset()
await analytics.flush()
```

## Request Payload

The SDK sends Basic Auth as `base64(writeKey + ":")` and posts one batch:

```http
POST /cdp/events/track
Authorization: Basic <base64(writeKey:)>
Content-Type: application/json
```

```json
{
  "sentAt": "2026-06-05T09:00:00.000Z",
  "batch": [
    {
      "messageId": "msg_...",
      "type": "track",
      "event": "OrderComplete",
      "userId": "user-123",
      "anonymousId": "anon_...",
      "idempotencyKey": "msg_...",
      "properties": {
        "amount": 99
      },
      "context": {
        "library": {
          "name": "@canvas/analytics-web",
          "version": "0.1.0"
        },
        "page": {
          "url": "https://shop.example.com/products/sku-1",
          "path": "/products/sku-1",
          "title": "Product Detail"
        },
        "campaign": {
          "utm_source": "newsletter"
        },
        "sessionId": "sess_..."
      },
      "timestamp": "2026-06-05T08:59:59.000Z",
      "sentAt": "2026-06-05T09:00:00.000Z"
    }
  ]
}
```

Sent events are removed from the queue only after a successful 2xx response. Failed flushes keep the same `messageId` values for retry.
