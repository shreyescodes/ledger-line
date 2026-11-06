# Local workflow. Needs Docker (Compose v2), Java 21+ (Gradle fetches JDK 25 itself) and make.
COMPOSE := docker compose -f infra/compose/compose.yaml
GRADLEW := ./gradlew

.DEFAULT_GOAL := help
.PHONY: help up down clean build test smoke logs ps lock domain-check

help: ## Show the available targets
	@grep -E '^[a-z-]+:.*## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*## "} {printf "  %-13s %s\n", $$1, $$2}'

infra/compose/.env:
	cp infra/compose/.env.example $@

build: ## Build the service jars
	$(GRADLEW) bootJar

up: infra/compose/.env build ## Build and start everything, wait until healthy
	$(COMPOSE) up -d --build --wait --wait-timeout 300

down: ## Stop everything (keeps the database volume)
	$(COMPOSE) down

clean: ## Stop everything and delete local data
	$(COMPOSE) down -v

smoke: ## Check the running stack end to end
	infra/compose/smoke-test.sh

test: ## Format check, static analysis and all tests (integration tests need Docker)
	$(GRADLEW) build

logs: ## Follow container logs
	$(COMPOSE) logs -f

ps: ## Show container status
	$(COMPOSE) ps

lock: ## Refresh gradle.lockfile files after changing dependencies
	$(GRADLEW) resolveAndLockAll --write-locks

domain-check: ## Replay the DOMAIN.md worked examples
	java tools/domain-check/DomainCheck.java DOMAIN.md
	java tools/domain-check/DomainCheck.java tools/domain-check/testdata/broken.md
