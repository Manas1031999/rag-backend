# RAG Backend

Upload PDFs into a workspace, then ask questions about them. Answers come from a model
running on your own machine through Ollama, so there are no API keys and no costs.

Built with Spring Boot 3.5, Spring AI 1.1, PostgreSQL + pgvector, and Ollama.

## The pipeline

| # | Step | Where |
|---|------|-------|
| 1 | Login / register | `auth/AuthController` (JWT, BCrypt) |
| 2 | Create workspace | `workspace/WorkspaceController` |
| 3 | Upload PDF | `document/DocumentController` |
| 4 | Extract text page by page | `document/IngestionService` (`PagePdfDocumentReader`) |
| 5 | Chunk the text | `document/TextChunker` (220 words, 40 overlap) |
| 6 | Turn chunks into embeddings | `VectorStore.add` calls Ollama `nomic-embed-text` |
| 7 | Store embeddings in pgvector | table `vector_store` |
| 8 | User asks a question | `chat/ChatController` |
| 9 | Turn the question into an embedding | `chat/RagService` |
| 10 | Semantic search | `VectorStore.similaritySearch`, limited to the workspace |
| 11 | LLM writes the answer | Ollama `llama3.2`, with file and page of each source |

## What you need

- Java 21 and Maven
- PostgreSQL 15 or newer with the **pgvector** extension (installed, or through Docker)
- [Ollama](https://ollama.com) running locally

## Setup

1. Install Ollama. The app downloads the two models (about 2.3 GB) the first time it starts.
   To download them yourself beforehand:

   ```bash
   ollama pull nomic-embed-text
   ```

   ```bash
   ollama pull llama3.2
   ```

2. Get a PostgreSQL that has pgvector. Pick one:

   - **Your installed PostgreSQL.** Install pgvector into it (on Windows it is built from
     source, see the [pgvector installation notes](https://github.com/pgvector/pgvector#windows)),
     then create an empty database named `ragdb` (pgAdmin: right-click Databases, Create, Database).
   - **Docker.** `docker compose up -d` starts one on port 5433 with everything included.
     Then set `DB_URL=jdbc:postgresql://localhost:5433/ragdb`, `DB_USERNAME=rag` and
     `DB_PASSWORD=rag_local_dev`.

   The app creates its tables and the `vector` extension on first start, so the database
   user must be allowed to run `CREATE EXTENSION`.

3. Set the database password in the terminal you start the app from (PowerShell):

   ```powershell
   $env:DB_PASSWORD = "<your postgres password>"
   ```

4. Start the app:

   ```bash
   mvn spring-boot:run
   ```

## Settings

All settings are environment variables with defaults in `src/main/resources/application.yml`.

| Variable | Default | Meaning |
|----------|---------|---------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/ragdb` | Database |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | none | Database password |
| `JWT_SECRET` | random at startup | At least 32 characters. Set it to keep logins across restarts |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama address |
| `OLLAMA_CHAT_MODEL` | `llama3.2` | Model that writes answers |
| `OLLAMA_EMBEDDING_MODEL` | `nomic-embed-text` | Model that creates embeddings |
| `SERVER_PORT` | `8080` | Port of this app |

If you change the embedding model, change `spring.ai.vectorstore.pgvector.dimensions` to match
it and drop the `vector_store` table, because old and new vectors cannot be compared.

## API

Every request except register and login needs the header `Authorization: Bearer <token>`.

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/api/auth/register` | Create an account, returns a token |
| POST | `/api/auth/login` | Returns a token |
| GET | `/api/auth/me` | Current user |
| POST | `/api/workspaces` | Create a workspace |
| GET | `/api/workspaces` | List your workspaces |
| GET | `/api/workspaces/{id}` | One workspace |
| DELETE | `/api/workspaces/{id}` | Delete a workspace with its documents and embeddings |
| POST | `/api/workspaces/{id}/documents` | Upload a PDF (form field `file`, up to 20MB) |
| GET | `/api/workspaces/{id}/documents` | List documents |
| DELETE | `/api/workspaces/{id}/documents/{docId}` | Delete a document and its embeddings |
| POST | `/api/workspaces/{id}/chat` | Ask a question: `{"question": "..."}` |

## Testing with Bruno

Open the `bruno` folder in [Bruno](https://www.usebruno.com) as a collection, choose the
`local` environment, and run the requests in order: Register, Create Workspace, Upload PDF
(choose a file in the Body tab), Ask Question. The token, workspace id and document id are
carried from one request to the next automatically.

## Limits

- Scanned PDFs (images with no text layer) are rejected: there is no OCR.
- Upload is synchronous, so a large PDF keeps the request open until its embeddings are done.
- Answer quality and speed depend on the local model and your hardware.
