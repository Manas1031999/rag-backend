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

| Thing | Why | Size |
|-------|-----|------|
| Java 21 and Maven | Build and run the app | |
| [Ollama](https://ollama.com) with two models | Creates embeddings and writes answers | about 2.3 GB |
| PostgreSQL 15 or newer with the **pgvector** extension | Stores users, workspaces and embeddings | |

Last checked end to end on Windows 11 with PostgreSQL 18, pgvector 0.8.7 and Ollama 0.40.

## Installing from scratch

### 1. Ollama

Download the installer from [ollama.com](https://ollama.com), or on Windows:

```powershell
winget install Ollama.Ollama
```

Then download the two models:

```bash
ollama pull nomic-embed-text
```

```bash
ollama pull llama3.2
```

Ollama keeps running in the background and starts again on every login. To stop it,
right-click its icon in the system tray and choose Quit.

### 2. PostgreSQL with pgvector

pgvector is an add-on for PostgreSQL. It is not included in the PostgreSQL installer.
Pick the route for your system.

**Linux / macOS.** Use the package manager, for example `sudo apt install postgresql-18-pgvector`
or `brew install pgvector`.

**Windows, into an installed PostgreSQL.** pgvector has to be built from source:

1. Install *Visual Studio Build Tools 2022* with the **Desktop development with C++**
   workload (about 6 GB). It is needed only for this build and can be uninstalled afterwards.

   ```powershell
   winget install Microsoft.VisualStudio.2022.BuildTools --override "--quiet --wait --norestart --add Microsoft.VisualStudio.Workload.VCTools --includeRecommended"
   ```

2. Open **x64 Native Tools Command Prompt for VS 2022** with *Run as administrator* and run
   these one by one (change `18` to your PostgreSQL version):

   ```bat
   set "PGROOT=C:\Program Files\PostgreSQL\18"
   ```

   ```bat
   cd /d %TEMP% && git clone --branch v0.8.7 https://github.com/pgvector/pgvector.git && cd pgvector
   ```

   ```bat
   nmake /F Makefile.win
   ```

   ```bat
   nmake /F Makefile.win install
   ```

   This copies `vector.dll` into `lib`, and `vector.control` plus the `vector--*.sql` files
   into `share\extension`, inside the PostgreSQL folder. PostgreSQL does not need a restart
   and existing databases are not changed.

**Docker.** `docker compose up -d` starts a PostgreSQL with pgvector on port 5433. Then start
the app with `DB_URL=jdbc:postgresql://localhost:5433/ragdb`, `DB_USERNAME=rag` and
`DB_PASSWORD=rag_local_dev`.

> Before installing Docker Desktop on Windows: it needs WSL 2, and its installer switches on
> the Windows features *Windows Subsystem for Linux*, *Virtual Machine Platform* and *Hyper-V*.
> They stay on after Docker Desktop is uninstalled. Inside a virtual machine Docker Desktop
> only runs if nested virtualization is available.

### 3. The database

Create an empty database named `ragdb` (pgAdmin: right-click Databases, Create, Database).
The app creates its tables and the `vector` extension on first start, so the database
user must be allowed to run `CREATE EXTENSION`.

### 4. Run

Set the database password in the terminal you start the app from (PowerShell), then start:

```powershell
$env:DB_PASSWORD = "<your postgres password>"
```

```bash
mvn spring-boot:run
```

The app is ready when the log shows `Started RagApplication`.

## Uninstalling

1. Drop the `ragdb` database (pgAdmin: right-click it, Delete). Do this first: its
   `vector_store` table stops working once pgvector is gone.
2. Remove pgvector from PostgreSQL: from the pgvector source folder run
   `nmake /F Makefile.win uninstall` in the same admin command prompt, or delete `lib\vector.dll`,
   `share\extension\vector.control`, `share\extension\vector--*.sql` and
   `include\server\extension\vector` by hand.
3. Uninstall Ollama (`winget uninstall Ollama.Ollama`) and delete the `.ollama` folder in your
   user folder, which holds the downloaded models.
4. Uninstall Visual Studio Build Tools if you installed it only for this.

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

No PDF at hand? Use `samples/library-handbook.pdf`, a four-page handbook of a made-up
library. Try "What is the yearly fee for students?" (120 rupees, page 2) or
"How many books can a student borrow?" (6, page 3).

## Limits

- Scanned PDFs (images with no text layer) are rejected: there is no OCR.
- Upload is synchronous, so a large PDF keeps the request open until its embeddings are done.
- Answer quality and speed depend on the local model and your hardware.
