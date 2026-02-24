# Container Deployment (`demo` profile)

This document describes how to run `conveyor-service` as a Docker container with:

- `demo` authentication profile enabled by default
- exposed HTTP port `8080`
- bind-mounted upload directory for extension JAR drop-in
- bundled loader scripts and sample PSV files at `/opt/conveyor/scripts`
- optional pre-seeded extension JARs copied on first container start when missing in mounted upload directory

## 1. Why `No ConveyorInitiatingService providers ...` appears

You will see this only when there are upload JARs present but none expose `ConveyorInitiatingService`.

If upload directory has no JAR files at all, this is not treated as an error.

## 2. Runtime defaults in the image

The container image is configured with:

- `SPRING_PROFILES_ACTIVE=demo`
- `SERVER_PORT=8080`
- `CONVEYOR_SERVICE_UPLOAD_DIR=/opt/conveyor/upload`
- `CONVEYOR_LOG_DIR=/opt/conveyor/logs`

The image declares:

- `EXPOSE 8080`
- `VOLUME /opt/conveyor/upload`
- `VOLUME /opt/conveyor/logs`
- test helper files in `/opt/conveyor/scripts` (`test-part-loader.sh`, `*.psv`, docs)

Seed behavior:

- During image build, any files matching `conveyor-service/upload/*.jar` are bundled into image directory `/opt/conveyor/upload-seed`.
- Image build script can also seed from any external directory via `SEED_UPLOAD_DIR`.
- On container start, entrypoint checks `/opt/conveyor/upload-seed/*.jar`.
- For each seeded JAR:
  - if `/opt/conveyor/upload/<jar-name>` does not exist, it is copied there
  - if it already exists in mounted upload directory, it is left untouched

Important:

- A plain `docker build` can only read files from Docker build context.
- If `conveyor-service/upload` is a symlink to a directory outside the repo, direct `docker build` usually cannot include those jars.
- Use `conveyor-service/scripts/build-container-image.sh` for reliable seeding from external/symlinked directories.

## 3. Build image locally

From repository root:

```bash
docker build -f conveyor-service/Dockerfile -t ghcr.io/aegisql/conveyor-service:local .
```

If you want default extension JARs embedded in the image, place them in:

- `conveyor-service/upload/`

before running the image build command.

Optional hard fail when no seed jars were found:

```bash
docker build \
  -f conveyor-service/Dockerfile \
  --build-arg REQUIRE_SEED_JARS=true \
  -t ghcr.io/aegisql/conveyor-service:local .
```

Alternative (recommended) for sharing a ready-to-demo image from your current host upload directory:

```bash
SEED_UPLOAD_DIR=~/work/tmp/upload \
bash conveyor-service/scripts/build-container-image.sh
```

You can also force failure if no seed jars are discovered:

```bash
SEED_UPLOAD_DIR=~/work/tmp/upload \
DOCKER_BUILD_ARGS="--build-arg REQUIRE_SEED_JARS=true" \
bash conveyor-service/scripts/build-container-image.sh
```

Alternative integrated Maven build (optional profile):

```bash
mvn -pl conveyor-service -Pcontainer -DskipTests package
```

Notes:

- Maven `container` profile builds the JAR and then executes `conveyor-service/scripts/build-container-image.sh`.
- Docker must be available on the host for the `container` profile.

## 4. Run container with host upload directory

From repository root:

```bash
mkdir -p conveyor-service/upload
mkdir -p conveyor-service/logs

docker run --rm \
  --name conveyor-service-demo \
  -p 8080:8080 \
  -v "$(pwd)/conveyor-service/upload:/opt/conveyor/upload" \
  -v "$(pwd)/conveyor-service/logs:/opt/conveyor/logs" \
  ghcr.io/aegisql/conveyor-service:local
```

Then open:

- Dashboard: [http://localhost:8080/dashboard](http://localhost:8080/dashboard)
- Swagger UI: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

Demo users:

- `admin/admin`
- `viewer/viewer`
- `rest/rest`

Drop extension JAR files into `conveyor-service/upload` on the host.
The service reads that mapped directory as `conveyor.service.upload-dir`.
Runtime conveyor and REST audit logs are written to `conveyor-service/logs` on the host.

If you want a different in-container log path, set:

- `CONVEYOR_LOG_DIR=/any/container/path`

Optional explicit file overrides:

- `CONVEYOR_AUDIT_LOG_FILE=/path/to/conveyor-rest-audit.log`
- `CONVEYOR_CONVEYOR_LOG_FILE=/path/to/conveyor.log`

When the upload directory is mounted, startup seed-copy is non-destructive:

- missing seeded JARs are copied in
- existing files are preserved

## 5. Run Loader Script Inside Container

Start service container:

```bash
docker run --rm \
  --name conveyor-service-demo \
  -p 8080:8080 \
  -v "$(pwd)/conveyor-service/upload:/opt/conveyor/upload" \
  -v "$(pwd)/conveyor-service/logs:/opt/conveyor/logs" \
  ghcr.io/aegisql/conveyor-service:local
```

In a second terminal, open shell in running container:

```bash
docker exec -it conveyor-service-demo bash
```

Run bundled loader script against local service inside container:

```bash
cd /opt/conveyor/scripts
./test-part-loader.sh --file collector_part_loader_100_ids.psv
```

Useful examples:

```bash
# Single ID flow
./test-part-loader.sh --conveyor collector --id 12345

# Static parts sample
./test-part-loader.sh --file collector_static_part_loader.psv
```

The script uses default demo REST credentials (`rest/rest`) and `BASE_URL=http://localhost:8080`.
Override with env vars if needed.

## 6. Docker Compose quick start

From `conveyor-service` directory:

```bash
mkdir -p upload
mkdir -p logs
docker compose -f docker-compose.demo.yml up --build -d
```

Stop:

```bash
docker compose -f docker-compose.demo.yml down
```

## 7. Image tagging and publish flow

### 7.1 Local scripted build/push

```bash
# Build tags:
# - <repo>:<project.version>
# - <repo>:<project.version>-<git-sha>
# - <repo>:latest
IMAGE_REPO=ghcr.io/<org-or-user>/conveyor-service \
IMAGE_TAG=1.7.3-SNAPSHOT \
SEED_UPLOAD_DIR=~/work/tmp/upload \
PUSH=true \
bash conveyor-service/scripts/build-container-image.sh
```

### 7.2 CI workflow for propagation

Workflow file:

- `.github/workflows/conveyor-service-container.yml`

Behavior:

- `push` to `main`/`master`: build-only validation (no push)
- `release.published`: build and push to GHCR
- `workflow_dispatch`: manual run with `push_image=true|false`

This gives a controlled mechanism to propagate the next pool of changes into container images.

## 8. Operational notes

- `demo` profile is for local/testing usage, not production IAM.
- Keep host upload directory writable by container user (`uid 10001` in image).
- For production-like deployments, switch to `prod` profile and externalize OAuth/JWT settings.
