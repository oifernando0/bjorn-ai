# bjorn-ai
Bjorn-AI Electrical agent

## API endpoints

Use the paths below with the app running on `http://localhost:8080` (or the port you mapped in Docker):

### Conversas
- `POST /api/conversations` – cria uma nova conversa. Corpo JSON: `{ "title": "string", "knowledgeBaseId": 123? }` (o `knowledgeBaseId` é opcional; quando omitido a base padrão é usada).
- `POST /api/conversations/{conversationId}/messages` – envia mensagem usando JSON `{ "content": "texto" }`.
- `POST /api/conversations/{conversationId}/messages` (multipart) – alternativa com `content` ou `message` como partes de formulário.
- `GET /api/conversations/{conversationId}/messages` – lista as mensagens da conversa em ordem cronológica.

### Documentos
- `POST /api/conversations/{conversationId}/documents` – faz upload de um arquivo relacionado à conversa. Multipart: parte `file` obrigatória e parte `sourceType` opcional (`OUTRO`, `MANUAL`, etc.).

### Base de conhecimento (PDF)
- `POST /api/knowledge/{specialist}/docs` – faz upload de um ou mais PDFs para indexação. Multipart com `files` (lista de arquivos). O parâmetro de caminho `{specialist}` será normalizado para maiúsculas.

## Docker

To run with Docker Compose, you can override the host port for PostgreSQL to avoid clashes with an existing local database:

```sh
POSTGRES_PORT=55432 docker-compose up --build
```

By default the database is published on `5433` and the application on `8080`.
