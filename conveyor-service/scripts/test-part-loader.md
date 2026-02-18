# test-part-loader.sh

This script supports two modes:

1. Fixed 3-step Part Loader sequence:
   - `USER` with `{"name":"John D"}`
   - `ADDRESS` with `{"zip_code":"11111"}`
   - `DONE` with `{}`
2. File playback mode with header-based routing:
   - `CONVEYOR_NAME|ID|LABEL|BODY` -> Part Loader (`/part/...`)
   - `CONVEYOR_NAME|LABEL|BODY` -> Static Part Loader (`/static-part/...`)
   - Any columns after `BODY` are sent as query properties by column name.

In fixed mode, all three requests use the same conveyor name and ID.

## Script Location

`/Users/mike/work/conveyor/conveyor-service/scripts/test-part-loader.sh`

## Basic Usage

```bash
/Users/mike/work/conveyor/conveyor-service/scripts/test-part-loader.sh [conveyor] [id]
```

Example:

```bash
/Users/mike/work/conveyor/conveyor-service/scripts/test-part-loader.sh collector 123
```

## Option Usage

```bash
/Users/mike/work/conveyor/conveyor-service/scripts/test-part-loader.sh [options]
```

Options:

- `--conveyor <name>`: conveyor name. Default: `collector`
- `--id <value>`: fixed-mode ID (string or number)
- `--file <path>`: input file with header `CONVEYOR_NAME|ID|LABEL|BODY...` or `CONVEYOR_NAME|LABEL|BODY...`
- `--shuffle`: randomize by build blocks:
  - parts mode: by `CONVEYOR_NAME|ID`
  - static mode: by `CONVEYOR_NAME|LABEL`
- `-h`, `--help`: print usage

## Positional Parameters (Backward Compatible)

- `conveyor` (optional): conveyor name. Default: `collector`
- `id` (optional): fixed-mode build ID (string or number). Default: current epoch seconds

## Environment Parameters

- `BASE_URL`: service base URL. Default: `http://localhost:8080`
- `TTL`: sent as `ttl` query param in parts mode. Default: `1 SECONDS`
- `REQUEST_TTL`: sent as `requestTTL` query param. Default: `100`
- `REST_USER`: username for auth. Default: `rest`
- `REST_PASSWORD`: password for auth. Default: `rest`
- `AUTH_MODE`: `session` (default, login at `/login` and reuse cookie session) or `basic` (send HTTP basic auth on each request)

## File Playback Example

Use generated dataset in scripts directory:

```bash
AUTH_MODE=session REST_USER=rest REST_PASSWORD=rest \
/Users/mike/work/conveyor/conveyor-service/scripts/test-part-loader.sh \
  --file /Users/mike/work/conveyor/conveyor-service/scripts/collector_part_loader_100_ids.psv
```

Shuffle playback order:

```bash
AUTH_MODE=session REST_USER=rest REST_PASSWORD=rest \
/Users/mike/work/conveyor/conveyor-service/scripts/test-part-loader.sh \
  --file /Users/mike/work/conveyor/conveyor-service/scripts/collector_part_loader_100_ids.psv \
  --shuffle
```

Recommended command (longer TTL + shuffle):

```bash
TTL="30 SECONDS" AUTH_MODE=session REST_USER=rest REST_PASSWORD=rest \
/Users/mike/work/conveyor/conveyor-service/scripts/test-part-loader.sh \
  --file /Users/mike/work/conveyor/conveyor-service/scripts/collector_part_loader_100_ids.psv \
  --shuffle
```

Static parts file example (`CONVEYOR_NAME|LABEL|BODY`):

```bash
AUTH_MODE=session REST_USER=rest REST_PASSWORD=rest \
/Users/mike/work/conveyor/conveyor-service/scripts/test-part-loader.sh \
  --file /Users/mike/work/conveyor/conveyor-service/scripts/static_parts.psv
```

File with extra properties (parts):

```text
CONVEYOR_NAME|ID|LABEL|BODY|priority|source
collector|1001|USER|{"name":"John D"}|5|bulk
```

File with extra properties (static):

```text
CONVEYOR_NAME|LABEL|BODY|region
collector|CONFIG|{"enabled":true}|us-east
```

## Auth Examples

Demo/session mode:

```bash
REST_USER=admin REST_PASSWORD=admin \
/Users/mike/work/conveyor/conveyor-service/scripts/test-part-loader.sh collector 2001
```

Basic auth mode:

```bash
AUTH_MODE=basic REST_USER=rest REST_PASSWORD=rest \
TTL="2500" REQUEST_TTL="1 SECONDS" \
/Users/mike/work/conveyor/conveyor-service/scripts/test-part-loader.sh collector 2002
```

## Notes

- If `jq` is installed, JSON responses are pretty-printed.
- The script fails fast on HTTP `>= 400`.
- The script also fails on HTTP `302` (usually authentication redirect or wrong auth mode).
- In file mode, header must be one of:
  - `CONVEYOR_NAME|ID|LABEL|BODY[|PROP_1|...]` (parts mode)
  - `CONVEYOR_NAME|LABEL|BODY[|PROP_1|...]` (static mode)
- In file mode, conveyor name is taken from each row (`CONVEYOR_NAME` column).
- In file mode, each property column value is sent as a query parameter with the same name.
- In static mode, `ttl` is not sent (static part endpoint rejects it); `requestTTL` is still sent.
- In the demo profile, use `AUTH_MODE=session` for `/part/**` endpoints.
