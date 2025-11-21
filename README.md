# bjorn-ai
Bjorn-AI Electrical agent

## Docker

To run with Docker Compose, you can override the host port for PostgreSQL to avoid clashes with an existing local database:

```sh
POSTGRES_PORT=55432 docker-compose up --build
```

By default the database is published on `5433` and the application on `8080`.
