#!/usr/bin/env bash
# Checks a running local stack (`make up` first, then `make smoke`):
# databases created, Kafka and Redis answering, every service ready,
# Prometheus scraping all services, Grafana wired to Prometheus,
# and Keycloak serving the ledgerline realm.
set -euo pipefail
cd "$(dirname "$0")"
set -a
# shellcheck disable=SC1091
source .env
set +a

compose() { docker compose -f compose.yaml "$@"; }
ok() { echo "ok    $*"; }
fail() {
    echo "FAIL  $*" >&2
    exit 1
}
# retry <seconds> <command...>: some checks need a scrape interval or two.
retry() {
    local deadline=$((SECONDS + $1))
    shift
    until "$@"; do
        ((SECONDS < deadline)) || return 1
        sleep 3
    done
}

databases=$(compose exec -T postgres psql -U postgres -tAc "select string_agg(datname, ',' order by datname) from pg_database")
for db in ledger notification payment reconciliation; do
    [[ ",$databases," == *",$db,"* ]] || fail "database $db missing (have: $databases)"
done
ok "postgres has one database per service: $databases"

compose exec -T kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list > /dev/null ||
    fail "kafka broker not answering"
ok "kafka broker answers"

[[ "$(compose exec -T redis redis-cli ping | tr -d '\r')" == "PONG" ]] || fail "redis not answering"
ok "redis answers PONG"

declare -A ports=(
    [ledger-service]=8081
    [payment-service]=8082
    [mock-psp]=8083
    [reconciliation-service]=8084
    [notification-service]=8085
)
for service in ledger-service payment-service mock-psp reconciliation-service notification-service; do
    body=$(curl -fsS "http://localhost:${ports[$service]}/actuator/health/readiness") ||
        fail "$service readiness endpoint not reachable"
    [[ "$body" == *'"status":"UP"'* ]] || fail "$service not ready: $body"
    ok "$service ready: $body"
done

all_services_scraped() {
    curl -fsS -G "http://localhost:9090/api/v1/query" \
        --data-urlencode 'query=sum(up{job="ledgerline-services"})' | grep -q '"5"\]'
}
retry 90 all_services_scraped || fail "prometheus is not scraping all 5 services"
ok "prometheus scrapes all 5 services"

curl -fsS -u "$GRAFANA_ADMIN_USER:$GRAFANA_ADMIN_PASSWORD" \
    "http://localhost:3000/api/datasources/uid/prometheus/health" | grep -q '"status":"OK"' ||
    fail "grafana can't reach its prometheus data source"
ok "grafana reaches prometheus"

curl -fsS "http://localhost:8180/realms/ledgerline/.well-known/openid-configuration" | grep -q '"issuer"' ||
    fail "keycloak isn't serving the ledgerline realm"
ok "keycloak serves the ledgerline realm"

echo "SMOKE TEST PASSED"
