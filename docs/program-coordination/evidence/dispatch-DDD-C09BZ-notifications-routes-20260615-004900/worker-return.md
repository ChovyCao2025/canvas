# DDD-C09BZ Worker Return

Worker: Lorentz `019ec70a-2994-7393-bd42-55c3d3593e56`

Status: shutdown before return packet

The coordinator closed Lorentz after the read-only Notification contract explorer returned and before Lorentz produced a packet, to avoid same-file write conflicts on the six reserved notification files.

No worker-authored patch was merged. The coordinator-owned implementation and verification are authoritative for this dispatch.

Accepted concern: sidecar worker did not return a packet.
