#!/usr/bin/env bash
set +x
set -euo pipefail
umask 077

readonly region=ap-northeast-2
readonly registry=015454941458.dkr.ecr.ap-northeast-2.amazonaws.com
readonly repository="$registry/ieee-backend"
readonly compose_file=/opt/ieee/backend/compose.yaml
readonly env_file=/etc/ieee/backend.env
readonly truststore=/etc/ieee/rds-truststore.p12
readonly state_dir=/var/lib/ieee-backend-deploy
readonly last_good_file="$state_dir/last-good-sha"
readonly health_url=http://127.0.0.1:8080/actuator/health

fail() {
  printf 'ERROR: %s\n' "$1" >&2
  exit 1
}

compose() {
  local sha=$1
  shift
  BACKEND_IMAGE="$repository:$sha" docker compose --file "$compose_file" "$@"
}

wait_healthy() {
  local deadline=$((SECONDS + 120))
  while (( SECONDS < deadline )); do
    if curl -q --noproxy '*' --fail --silent --max-time 3 "$health_url" |
      python3 -c 'import json, sys; sys.exit(json.load(sys.stdin).get("status") != "UP")' 2>/dev/null; then
      return 0
    fi
    sleep 3
  done
  return 1
}

start_backend() {
  compose "$1" up --detach --no-deps --no-build --pull never backend >/dev/null 2>&1 &&
    wait_healthy
}

[[ $# -eq 1 && $1 =~ ^[0-9a-f]{40}$ ]] || fail 'Expected one full commit SHA.'
readonly target_sha=$1
(( EUID == 0 )) || fail 'Deployment must run as root.'

for tool in aws docker curl python3 flock timeout; do
  command -v "$tool" >/dev/null || fail "Required command is missing: $tool."
done
for file in "$compose_file" "$env_file" "$truststore"; do
  [[ -f $file && -r $file && -s $file ]] || fail "Required file is missing, unreadable or empty: $file."
done

install -d -m 0700 "$state_dir" || fail 'Cannot prepare deployment state directory.'
exec 9>"$state_dir/deploy.lock"
flock -n 9 || fail 'Another deployment is running on this instance.'

compose "$target_sha" config --quiet >/dev/null 2>&1 || fail 'Compose configuration validation failed.'
configured_image=$(compose "$target_sha" config --images backend 2>/dev/null) || fail 'Cannot resolve the backend image from Compose.'
[[ $configured_image == "$repository:$target_sha" ]] || fail 'Compose backend image does not match the requested commit.'

previous_sha=
if [[ -e $last_good_file ]]; then
  if ! IFS= read -r previous_sha < "$last_good_file" || [[ ! $previous_sha =~ ^[0-9a-f]{40}$ ]]; then
    fail 'Last known good image record is invalid.'
  fi
else
  existing_container=$(compose "$target_sha" ps --all --quiet backend 2>/dev/null) || fail 'Cannot check for an existing backend container.'
  [[ -z $existing_container ]] || fail 'An existing backend has no last known good record. Verify it before enrolling it for rollback.'
fi

scratch_dir=$(mktemp -d "$state_dir/deploy.XXXXXXXX") || fail 'Cannot prepare deployment files.'
trap 'rm -rf -- "$scratch_dir"' EXIT
docker_config="$scratch_dir/docker"
mkdir -m 0700 "$docker_config"
printf '%s\n' "$target_sha" > "$scratch_dir/last-good-sha" || fail 'Cannot stage the image record.'

if ! { timeout 45s aws ecr get-login-password --region "$region" --no-cli-pager |
  timeout 45s docker --config "$docker_config" login --username AWS --password-stdin "$registry"; } >/dev/null 2>&1; then
  fail 'ECR authentication failed.'
fi
if ! timeout 240s docker --config "$docker_config" pull "$repository:$target_sha" >/dev/null 2>&1; then
  fail 'Could not pull the requested image.'
fi
if [[ -n $previous_sha ]] && ! docker image inspect "$repository:$previous_sha" >/dev/null 2>&1; then
  timeout 240s docker --config "$docker_config" pull "$repository:$previous_sha" >/dev/null 2>&1 || fail 'Could not prepare the previous image for rollback.'
fi

if start_backend "$target_sha"; then
  if mv -fT -- "$scratch_dir/last-good-sha" "$last_good_file"; then
    printf 'Backend is healthy at %s.\n' "$target_sha"
    exit 0
  fi
  printf 'ERROR: Could not record the healthy image.\n' >&2
else
  printf 'ERROR: Backend startup or health check failed for %s.\n' "$target_sha" >&2
fi

if [[ -n $previous_sha ]]; then
  start_backend "$previous_sha" || fail 'Rollback failed or the restored backend is unhealthy.'
  printf 'Previous healthy image restored: %s.\n' "$previous_sha" >&2
else
  compose "$target_sha" rm --stop --force backend >/dev/null 2>&1 || fail 'First deployment failed and the backend container could not be removed.'
  printf 'First deployment failed. Removed the backend container created by this run.\n' >&2
fi
exit 1
