# Marketing Content Production Readiness

## Scope

This runbook covers the production path for the content marketing closed loop:

- CMS entries and immutable entry releases.
- DAM assets, folders, provider upload intents, scan/transcode callbacks, and asset readiness gates.
- Email and multichannel content templates, approval, preview, release publish, resolve, and rollback.
- Runtime delivery through `contentReleaseKey` resolving an immutable active release snapshot.

The upload model follows the same production primitives documented by AWS S3 presigned upload and CORS guidance, and by common webhook signature validation guidance: presigned PUT grants short-lived object-specific upload capability, browser uploads require a bucket CORS rule matching origin/method/headers, and callbacks must be HMAC-verified before processing.

References:

- AWS S3 presigned URLs: https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-presigned-url.html
- AWS S3 CORS: https://docs.aws.amazon.com/AmazonS3/latest/userguide/cors.html
- GitHub webhook signature validation: https://docs.github.com/en/webhooks/using-webhooks/validating-webhook-deliveries

## Required Configuration

Production must configure these values before enabling the content upload path:

```bash
CANVAS_MARKETING_ASSET_UPLOAD_WEBHOOK_SECRET=<32+ byte shared secret>
CANVAS_MARKETING_ASSET_UPLOAD_WEBHOOK_TOLERANCE_SECONDS=300

CANVAS_MARKETING_ASSET_UPLOAD_S3_ENABLED=true
CANVAS_MARKETING_ASSET_UPLOAD_S3_ENDPOINT=https://s3.example.com
CANVAS_MARKETING_ASSET_UPLOAD_S3_REGION=us-east-1
CANVAS_MARKETING_ASSET_UPLOAD_S3_BUCKET=canvas-assets
CANVAS_MARKETING_ASSET_UPLOAD_S3_ACCESS_KEY=<least-privilege access key>
CANVAS_MARKETING_ASSET_UPLOAD_S3_SECRET_KEY=<least-privilege secret>
CANVAS_MARKETING_ASSET_UPLOAD_S3_KEY_PREFIX=marketing-assets
CANVAS_MARKETING_ASSET_UPLOAD_S3_PATH_STYLE=true
CANVAS_MARKETING_ASSET_UPLOAD_S3_PUBLIC_BASE_URL=https://cdn.example.com/canvas-assets

CANVAS_MARKETING_ASSET_UPLOAD_CLEANUP_ENABLED=true
CANVAS_MARKETING_ASSET_UPLOAD_CLEANUP_TENANT_ID=1
CANVAS_MARKETING_ASSET_UPLOAD_CLEANUP_LIMIT=100
CANVAS_MARKETING_ASSET_UPLOAD_CLEANUP_FIXED_DELAY_MS=60000
```

The `prod` profile fails startup when the webhook secret is missing or shorter than 32 bytes. If S3 handoff is enabled, the guard also requires HTTPS endpoint/public URL, bucket, access key, and a non-empty secret key.

## Upload Flow

1. Authenticated content editor creates an upload intent through `POST /marketing/content/assets/upload-intents`.
2. Backend validates asset key, asset type, MIME allowlist, extension allowlist, file size limit, and tenant identity.
3. For S3 handoff, backend returns a short-lived SigV4 presigned PUT URL and signed required headers.
4. Browser uploads the file directly to the provider using the exact returned headers.
5. Scanner/transcoder verifies the object out of band and calls `POST /public/marketing/content/assets/upload-callbacks/{tenantId}/{provider}`.
6. Public callback verifies `X-Canvas-Asset-Timestamp` and `X-Canvas-Asset-Signature` before updating the intent.
7. For S3 presigned uploads, the READY callback `storageUrl` must match the storage URL produced by the original upload intent.
8. Asset becomes `READY` only when callback evidence satisfies storage binding, checksum, scan, size, MIME, and video transcode gates.

There is no tenant management endpoint for upload completion. Do not expose or call an unsigned
`/marketing/content/assets/upload-callbacks` path; production readiness changes must come from the signed public
webhook after provider, scanner, or transcoder verification.

## Callback Contract

Provider callbacks are JSON and signed over:

```text
<timestamp>
<raw request body>
```

Required headers:

```text
X-Canvas-Asset-Timestamp: <epoch seconds>
X-Canvas-Asset-Signature: sha256=<hex hmac>
```

READY callback minimum payload for direct uploads:

```json
{
  "provider": "S3",
  "uploadToken": "token-from-upload-intent",
  "assetKey": "launch_hero",
  "assetType": "VIDEO",
  "mimeType": "video/mp4",
  "storageUrl": "https://cdn.example.com/canvas-assets/tenants/8/marketing-assets/launch_hero/file.mp4",
  "status": "READY",
  "transcodeStatus": "READY",
  "sizeBytes": 123456,
  "durationMs": 95000,
  "checksumSha256": "64-character-lowercase-or-uppercase-hex",
  "scanStatus": "PASSED",
  "metadata": {
    "scanner": "clamav",
    "transcoder": "media-pipeline"
  }
}
```

For non-video assets, `durationMs` and `transcodeStatus` are not required. Direct S3 and external uploads require `checksumSha256` and cannot use `scanStatus=NOT_REQUIRED`. Trusted media providers can use `scanStatus=PROVIDER_VERIFIED`.
For S3 `PRESIGNED_PUT` intents, the `storageUrl` in this payload must exactly match the intent's returned `uploadParams.storageUrl`; mismatches are rejected before any asset row is marked `READY`.

## Storage Requirements

The S3-compatible bucket must enforce:

- PUT allowed only through presigned requests for the object prefix generated by the backend.
- CORS allowing the production frontend origin to PUT with returned signed headers.
- Server-side encryption matching the signed `x-amz-server-side-encryption` header.
- Lifecycle cleanup for stale uploaded objects that never receive a valid callback.
- Scanner/transcoder read access and callback delivery credentials separated from browser upload credentials.

The application also exposes `POST /marketing/content/assets/upload-intents/expire-stale` and a default-off cleanup scheduler
under `canvas.marketing.content.asset-upload.cleanup.*`. The cleanup path marks expired `PENDING` upload intents as `FAILED`
and writes `ASSET_UPLOAD_EXPIRED` audit evidence; bucket lifecycle rules remain responsible for deleting orphaned binary objects.

## Release Gate

Before publishing a content release:

1. Template source must be `APPROVED`; CMS entry source must be `PUBLISHED`.
2. Every referenced asset must exist in the same tenant and be `READY`.
3. Video assets must have `transcodeStatus=READY` or `EXTERNAL`.
4. Publishing writes an immutable release snapshot and release items.
5. Runtime consumers should use `contentReleaseKey`, not draft template/body fields.
6. Rollback marks the current active release `ROLLED_BACK`, restores the latest lower `SUPERSEDED` version to `ACTIVE` when one exists, and writes `RELEASE_ROLLED_BACK` plus `RELEASE_RESTORED` audit events.
7. If no previous superseded version exists, rollback leaves the release key with no active version; publish a new approved snapshot before routing runtime delivery back to that key.

## Verification

Backend:

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH \
mvn -pl canvas-engine test -DfailIfNoTests=false
```

Focused backend content gate:

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH \
mvn -pl canvas-engine test \
  -Dtest=MarketingAssetUploadServiceTest,MarketingAssetUploadWebhookSignatureServiceTest,MarketingAssetUploadIntentSchemaTest,MarketingAssetServiceTest,MarketingContentReleaseServiceTest,MarketingContentReleaseSchemaTest,MarketingContentHubSchemaTest,MarketingContentUploadControllerTest,PublicMarketingContentUploadWebhookControllerTest,SecurityConfigRouteTest,SecurityConfigRoleTest,ProductionConfigGuardTest,ApplicationYamlTest,FlywayMigrationPolicyTest \
  -DfailIfNoTests=false
```

Frontend:

```bash
cd frontend
npm run test -- --run src/services/marketingContentApi.test.ts src/pages/content-hub/contentHubPresentation.test.ts src/pages/content-hub/index.test.tsx
npm run build
```

Live upload verifier against a running backend and S3-compatible provider:

```bash
API_BASE=https://canvas.example.com \
JWT_TOKEN="$TOKEN" \
TENANT_ID=1 \
FILE_PATH=/tmp/proof.pdf \
ASSET_KEY=proof_pdf \
ASSET_TYPE=FILE \
MIME_TYPE=application/pdf \
CORS_ORIGIN=https://app.example.com \
CALLBACK_READY=true \
CANVAS_MARKETING_ASSET_UPLOAD_WEBHOOK_SECRET="$SECRET" \
scripts/verify-marketing-content-upload-live.sh
```

The script creates the upload intent, optionally verifies browser CORS preflight when `CORS_ORIGIN` is set, performs the presigned PUT with backend-returned signed headers, computes SHA-256, proves invalid and stale READY callbacks are rejected, signs the public READY callback, and fails unless the upload intent reaches `COMPLETED` and the DAM asset row is `READY` with the expected `storageUrl` and checksum.

Live release verifier against the same backend, using the READY asset from the upload verifier:

```bash
API_BASE=https://canvas.example.com \
JWT_TOKEN="$TOKEN" \
ASSET_KEY=proof_pdf \
TEMPLATE_KEY=release-proof-$(date +%s) \
scripts/verify-marketing-content-release-live.sh
```

The release verifier fails unless the existing asset is `READY`, the template approval gate passes, release validation includes the asset, publish writes version 1, runtime resolve renders the immutable snapshot and READY asset item, a second publish leaves exactly one `ACTIVE` release, rollback restores version 1, and release audit events include `RELEASE_PUBLISHED`, `RELEASE_SUPERSEDED`, `RELEASE_ROLLED_BACK`, and `RELEASE_RESTORED`.

Run both live verifiers before claiming production readiness: the upload verifier proves provider handoff and signed callback evidence; the release verifier proves the CMS/template release and runtime consumption loop.

## Pre-Release Checklist

- Migration directory has unique numeric Flyway versions.
- Migration history repair has been verified with `scripts/verify-flyway-history.sh`; see `docs/runbooks/flyway-migration-history-repair-2026-06-06.md`.
- No applied migration is deleted or rewritten without an explicit migration-history decision.
- `prod` profile starts only with real secrets and HTTPS storage endpoints.
- Browser direct upload succeeds against the production bucket CORS policy.
- Scanner/transcoder callback is signed and rejected when stale, unsigned, mismatched, or incomplete.
- S3 READY callback is rejected when `storageUrl` does not match the original presigned upload intent.
- READY assets cannot be created or manually transitioned without scan/transcode evidence.
- Release publish, resolve, rollback restore, and delivery `contentReleaseKey` paths are covered by tests.
- Exactly one active release per tenant/release key is enforced by the database.
- Expired `PENDING` upload intents are cleaned by the admin endpoint or enabled scheduler and leave audit evidence.
