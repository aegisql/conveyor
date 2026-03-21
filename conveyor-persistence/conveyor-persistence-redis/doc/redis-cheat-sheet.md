# Redis Troubleshooting Cheat Sheet

Quick commands for inspecting the Redis persistence backend used by Conveyor.

These commands are written for the current Redis key layout in this repository.

## Assumptions

- Redis is reachable at `redis://localhost:6379`
- the persistence name is known
- examples below use:
  - persistence name: `orders`
  - namespace: `conv:{orders}`

You can reuse the commands by changing the namespace prefix.

## Setup

```bash
export REDIS_CLI='redis-cli -u redis://localhost:6379'
export NAME='orders'
export NS="conv:{${NAME}}"
```

## List Conveyor Keys

List all keys for one Conveyor persistence namespace:

```bash
$REDIS_CLI --scan --pattern "${NS}:*"
```

If you only want the tracked keys known to `archiveAll()`:

```bash
$REDIS_CLI SMEMBERS "${NS}:tracker"
```

## Inspect Bootstrap And Basic State

Show namespace metadata:

```bash
$REDIS_CLI HGETALL "${NS}:meta"
```

Show the current part id sequence:

```bash
$REDIS_CLI GET "${NS}:seq"
```

Show total active and static part counts:

```bash
$REDIS_CLI ZCARD "${NS}:parts:active"
$REDIS_CLI ZCARD "${NS}:parts:static"
```

Show completed build key count:

```bash
$REDIS_CLI SCARD "${NS}:completed"
```

## List Part Ids

List active part ids:

```bash
$REDIS_CLI ZRANGE "${NS}:parts:active" 0 -1
```

List static part ids:

```bash
$REDIS_CLI ZRANGE "${NS}:parts:static" 0 -1
```

List expiring part ids with expiration timestamps:

```bash
$REDIS_CLI ZRANGE "${NS}:parts:expires" 0 -1 WITHSCORES
```

List completed build keys:

```bash
$REDIS_CLI SMEMBERS "${NS}:completed"
```

Note:

- completed keys are stored in encoded serialized form
- they are useful for presence checks, but not always human-readable directly

## Inspect One Part By Id

Replace `42` with a real part id.

Show value payload key:

```bash
$REDIS_CLI GET "${NS}:part:42:payload"
```

Show authoritative part metadata:

```bash
$REDIS_CLI HGETALL "${NS}:part:42:meta"
```

Show the most useful metadata fields directly:

```bash
$REDIS_CLI HMGET "${NS}:part:42:meta" \
  loadType creationTime expirationTime priority \
  keyHint labelHint valueHint propertiesHint commandFilterHint
```

Show reverse key index for the part:

```bash
$REDIS_CLI SMEMBERS "${NS}:part:42:keys"
```

What these mean:

- `:payload`
  - Base64-url encoded serialized cart value bytes
  - may also be encrypted
- `:meta`
  - authoritative cart metadata such as:
    - `id`
    - `loadType`
    - `creationTime`
    - `expirationTime`
    - `priority`
    - `keyHint` / `keyData`
    - `labelHint` / `labelData`
    - `valueHint`
    - `propertiesHint` / `propertiesData`
    - `commandFilterHint` / `commandFilterData` for filter-based commands
  - new writes do not store mirrored `valueData`; the value bytes live in `:payload`
- `:keys`
  - encoded build keys this part is linked to

Legacy note:

- older Redis records may still have a whole serialized cart in `:payload`
- older itemized Redis records may still have mirrored `valueData` inside `:meta`
- the current implementation still reads that older format, but new writes use the itemized metadata layout above

## Look Up Parts For One Build Key

The per-key index uses encoded keys.

Typical workflow:

1. inspect one known part
2. copy the encoded key from:
   - `${NS}:part:<id>:keys`
3. query the matching per-key sorted set

Example, once you have an encoded key value:

```bash
export ENCODED_KEY='paste-encoded-key-here'
$REDIS_CLI ZRANGE "${NS}:parts:key:${ENCODED_KEY}" 0 -1
```

This is the main troubleshooting path when you need to answer:

- which part ids belong to this build key?

## Inspect Expiration Behavior

Show all expiration scores:

```bash
$REDIS_CLI ZRANGE "${NS}:parts:expires" 0 -1 WITHSCORES
```

Show only already-expired ids using current local time in milliseconds:

```bash
$REDIS_CLI ZRANGEBYSCORE "${NS}:parts:expires" -inf "$(date +%s%3N)"
```

Then inspect one of those ids:

```bash
$REDIS_CLI HGETALL "${NS}:part:42:meta"
```

## Common Troubleshooting Questions

### Is this namespace initialized?

```bash
$REDIS_CLI HGETALL "${NS}:meta"
```

If empty, either:

- nothing has initialized the namespace yet
- or the namespace was archived/deleted

### Are parts being written at all?

```bash
$REDIS_CLI ZCARD "${NS}:parts:active"
$REDIS_CLI ZCARD "${NS}:parts:static"
$REDIS_CLI GET "${NS}:seq"
```

If `seq` grows but active/static sets stay empty, inspect filtering logic and archive activity.

### Which keys will `archiveAll()` delete?

```bash
$REDIS_CLI SMEMBERS "${NS}:tracker"
```

### Why can I not read a payload by eye?

Because the stored value is:

- serialized
- Base64-url encoded
- and may also be encrypted

Also note:

- in the current format, `:payload` is only the cart value bytes, not the whole cart
- the rest of the cart shape now lives in `:meta`

So `GET "${NS}:part:<id>:payload"` is mainly useful to confirm presence, size, and change over time.

### Which ids are currently considered active?

```bash
$REDIS_CLI ZRANGE "${NS}:parts:active" 0 -1
```

### Which ids are currently considered static?

```bash
$REDIS_CLI ZRANGE "${NS}:parts:static" 0 -1
```

### Which build keys are marked complete?

```bash
$REDIS_CLI SMEMBERS "${NS}:completed"
```

## Dangerous Cleanup Commands

Delete one namespace by tracked keys:

```bash
$REDIS_CLI SMEMBERS "${NS}:tracker" | xargs -n 100 $REDIS_CLI DEL
$REDIS_CLI DEL "${NS}:tracker"
```

Delete one namespace by scan pattern:

```bash
$REDIS_CLI --scan --pattern "${NS}:*" | xargs -n 100 $REDIS_CLI DEL
```

Use these only when you intentionally want to wipe the Redis persistence namespace.

## Current Key Layout Reference

For a persistence name `orders`, the current implementation uses:

- `conv:{orders}:meta`
- `conv:{orders}:seq`
- `conv:{orders}:parts:active`
- `conv:{orders}:parts:static`
- `conv:{orders}:parts:expires`
- `conv:{orders}:completed`
- `conv:{orders}:tracker`
- `conv:{orders}:part:{id}:payload`
- `conv:{orders}:part:{id}:meta`
- `conv:{orders}:part:{id}:keys`
- `conv:{orders}:parts:key:{encodedKey}`
