.PHONY: help build run docker-build up down logs db-shell test clean docker-build-local

help:
	@echo "Targets: build run docker-build up down logs db-shell test clean"

# Build local com Gradle wrapper
build:
	./gradlew clean build -x test

# Run em modo dev (Spring Boot)
run:
	./gradlew bootRun

# Build da imagem Docker da aplicação
docker-build:
	docker build -t api-app:latest .

# Build image using local gradle cache (faster on dev machines)
docker-build-local:
	docker build --build-arg CACHEBUST=$(date +%s) -t api-app:latest .

# Up via docker compose (app + db)
up:
	docker compose up --build -d

down:
	docker compose down

logs:
	docker compose logs -f app

# Entrar no shell do Postgres
db-shell:
	@# Load variables from .env (if present) into the shell environment, then run psql
	@if [ -f .env ]; then \
		export $$(grep -v '^#' .env | xargs); \
	fi; \
	docker compose exec db psql -U "$${DB_USER:-postgres}" -d "$${DB_NAME:-api}"

test:
	./gradlew test

clean:
	./gradlew clean
	-@docker image rm api-app:latest 2>/dev/null || true
