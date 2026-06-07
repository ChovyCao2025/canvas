# P2-080 Product Index Hunk Boundary

Date: 2026-06-07

Purpose: define the P2-080-only product index rows without staging the mixed
product-evolution index files wholesale.

Do not use this file as approval to stage:

- `docs/product-evolution/IMPLEMENTATION_ORDER.md`
- `docs/product-evolution/specs/INDEX.md`
- `docs/product-evolution/plans/INDEX.md`

Those files currently include P2-080 plus P2-081 through P2-089 and many
P2-082 sub-slices. For P2-080 staging, include only the rows below by
hunk-level review.

## Implementation Order Row

File:

- `docs/product-evolution/IMPLEMENTATION_ORDER.md`

P2-080-only row:

```markdown
| P2-080 | Conversational Session Foundation | [Spec](specs/p2-080-conversational-session-foundation.md) | [Plan](plans/p2-080-conversational-session-foundation-plan.md) | Direction 23 accelerated foundation for durable conversation sessions and WAIT resume from inbound replies |
```

Do not include adjacent P2-081, P2-082, P2-082D, P2-082D2, P2-082K,
P2-082L, P2-083 through P2-089, or other later product rows as part of a
P2-080 core package.

## Specs Index Row

File:

- `docs/product-evolution/specs/INDEX.md`

P2-080-only row:

```markdown
| P2-080 | [Conversational Session Foundation](p2-080-conversational-session-foundation.md) |
```

Do not include adjacent P2-081, P2-082, P2-082D, P2-082D2, P2-082K,
P2-082L, P2-083 through P2-089, or other later product rows as part of a
P2-080 core package.

## Plans Index Row

File:

- `docs/product-evolution/plans/INDEX.md`

P2-080-only row:

```markdown
| P2-080 | [Conversational Session Foundation](p2-080-conversational-session-foundation-plan.md) |
```

Do not include adjacent P2-081, P2-082, P2-082D, P2-082D2, P2-082K,
P2-082L, P2-083 through P2-089, or other later product rows as part of a
P2-080 core package.

## Safe Review Commands

Use these as read-only checks before any hunk-level staging:

```bash
rg -n 'P2-080|P2-081|P2-082|P2-083|P2-084|P2-085|P2-086|P2-087|P2-088|P2-089' \
  docs/product-evolution/IMPLEMENTATION_ORDER.md \
  docs/product-evolution/specs/INDEX.md \
  docs/product-evolution/plans/INDEX.md
```

The expected P2-080 rows are the three rows listed above. Any non-P2-080 rows
from that command remain owned by their respective product slices or a separate
index-maintenance change.
