# test-part-loader.sh

This script supports two modes:

1. Fixed 3-step Part Loader sequence:
   - `USER` with `{"name":"John D"}`
   - `ADDRESS` with `{"zip_code":"11111"}`
   - `DONE` with `{}`
2. File playback mode (`ID|LABEL|BODY`), with optional random shuffle.

In fixed mode, all three requests use the same conveyor name and numeric ID.

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
- `--id <number>`: fixed-mode numeric ID
- `--file <path>`: input file with rows `ID|LABEL|BODY`
- `--shuffle`: randomize file rows before sending
- `-h`, `--help`: print usage

## Positional Parameters (Backward Compatible)

- `conveyor` (optional): conveyor name. Default: `collector`
- `id` (optional): fixed-mode build ID, must be numeric. Default: current epoch seconds

## Environment Parameters

- `BASE_URL`: service base URL. Default: `http://localhost:8080`
- `TTL`: sent as `ttl` query param. Default: `1 SECONDS`
- `REQUEST_TTL`: sent as `requestTTL` query param. Default: `100`
- `REST_USER`: username for auth. Default: `rest`
- `REST_PASSWORD`: password for auth. Default: `rest`
- `AUTH_MODE`: `session` (default, login at `/login` and reuse cookie session) or `basic` (send HTTP basic auth on each request)

## File Playback Example

Use generated dataset in scripts directory:

```bash
AUTH_MODE=session REST_USER=rest REST_PASSWORD=rest \
/Users/mike/work/conveyor/conveyor-service/scripts/test-part-loader.sh \
  --conveyor collector \
  --file /Users/mike/work/conveyor/conveyor-service/scripts/part_loader_100_ids.psv
```

Shuffle playback order:

```bash
AUTH_MODE=session REST_USER=rest REST_PASSWORD=rest \
/Users/mike/work/conveyor/conveyor-service/scripts/test-part-loader.sh \
  --conveyor collector \
  --file /Users/mike/work/conveyor/conveyor-service/scripts/part_loader_100_ids.psv \
  --shuffle
```

Recommended command (longer TTL + shuffle):

```bash
TTL="30 SECONDS" AUTH_MODE=session REST_USER=rest REST_PASSWORD=rest \
/Users/mike/work/conveyor/conveyor-service/scripts/test-part-loader.sh \
  --conveyor collector \
  --file /Users/mike/work/conveyor/conveyor-service/scripts/part_loader_100_ids.psv \
  --shuffle
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
- In file mode, header row `ID|LABEL|BODY` is accepted and skipped.
- In the demo profile, use `AUTH_MODE=session` for `/part/**` endpoints.
