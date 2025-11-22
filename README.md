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
- `POST /api/knowledge/{specialist}/docs` – faz upload de um ou mais PDFs para indexação. Envie como multipart **form-data** com uma parte `files` do tipo **File** para cada PDF (repita o campo se tiver vários). O parâmetro de caminho `{specialist}` será normalizado para maiúsculas.

### Importação no Postman
- Coleção pronta: `postman_collection.json` na raiz do repositório.
- Variáveis: `baseUrl` (padrão `http://localhost:8080`), `conversationId` (preencha com o ID retornado ao criar a conversa) e `specialist` (por exemplo, `GERAL`).

## Ordem recomendada para popular e usar a base
1) **Subir os PDFs da base de conhecimento**
   - Endpoint: `POST /api/knowledge/{specialist}/docs` (multipart)
   - O `{specialist}` será convertido para maiúsculas (use algo como `GERAL`).
   - Envie cada PDF em uma parte `files` do tipo **File** no form-data; se o campo tiver outro nome ou não for `File`, o backend retornará 400.
   - Exemplo cURL: `curl -X POST "http://localhost:8080/api/knowledge/GERAL/docs" -F "files=@/caminho/arquivo1.pdf" -F "files=@/caminho/arquivo2.pdf"`
2) **Criar a conversa ligada à base padrão**
   - Endpoint: `POST /api/conversations`
   - Corpo JSON mínimo: `{ "title": "Minha conversa" }` (o serviço ligará automaticamente à base padrão "Base Global").
   - Se quiser apontar para outra base criada manualmente, inclua `knowledgeBaseId` no corpo.
3) **Enviar mensagens na conversa**
   - Endpoint: `POST /api/conversations/{conversationId}/messages`
   - Corpo JSON: `{ "content": "Minha pergunta sobre os PDFs" }`.
   - O `conversationId` vem da resposta da etapa 2.
4) **(Opcional) Anexar documentos específicos da conversa**
   - Endpoint: `POST /api/conversations/{conversationId}/documents` (multipart)
   - Use a parte `file` (obrigatória) e, se quiser classificar a fonte, adicione `sourceType` (ex.: `MANUAL`).

No Postman, basta importar a coleção e executar os requests na ordem acima, preenchendo as variáveis `conversationId` e `specialist` após cada passo.

## Docker

To run with Docker Compose, you can override the host port for PostgreSQL to avoid clashes with an existing local database:

```sh
POSTGRES_PORT=55432 docker-compose up --build
```

By default the database is published on `5433` and the application on `8080`.
