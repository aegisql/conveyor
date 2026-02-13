# test-part-loader.sh

This script sends a simple 3-step Part Loader sequence to `conveyor-service`:

1. `USER` with `{"name":"John D"}`
2. `ADDRESS` with `{"zip_code":"11111"}`
3. `DONE` with `{}`

All three requests use the same conveyor name and numeric ID.

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

## Positional Parameters

- `conveyor` (optional): conveyor name. Default: `collector`
- `id` (optional): build ID, must be numeric. Default: current epoch seconds

## Environment Parameters

- `BASE_URL`: service base URL. Default: `http://localhost:8080`
- `TTL`: sent as `ttl` query param. Default: `1 SECONDS`
- `REQUEST_TTL`: sent as `requestTTL` query param. Default: `100`
- `REST_USER`: username for auth. Default: `rest`
- `REST_PASSWORD`: password for auth. Default: `rest`
- `AUTH_MODE`: `session` (default, login at `/login` and reuse cookie session) or `basic` (send HTTP basic auth on each request)

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
