# Jazzify Embedding Worker

Small FastAPI service that exposes local text embeddings for Jazzify.

It uses the same default model described in the RAG handoff:

```text
sentence-transformers/paraphrase-multilingual-mpnet-base-v2
```

The first embedding call downloads the model into the container cache unless the
model is already mounted in `/models`.

## Endpoints

### `GET /health`

Lightweight liveness check. Does not load the model.

### `GET /ready`

Loads the embedding model if needed and returns model metadata.

### `POST /embed`

Embeds one text input.

```bash
curl -X POST http://localhost:8001/embed \
  -H "Content-Type: application/json" \
  -d "{\"text\":\"ii-V-I progression in C major\"}"
```

Response shape:

```json
{
  "model": "sentence-transformers/paraphrase-multilingual-mpnet-base-v2",
  "dimension": 768,
  "count": 1,
  "normalized": false,
  "embedding": [0.0123, -0.0456],
  "embeddings": [[0.0123, -0.0456]],
  "usage": {
    "input_texts": 1,
    "input_characters": 29
  }
}
```

Batch input is also supported:

```json
{
  "texts": ["first text", "second text"]
}
```

`POST /v1/embed` is an alias for the same endpoint.

## Project Layout

```text
app/
  api/        FastAPI routers and route handlers
  core/       Configuration and app-level settings
  schemas/    Pydantic request and response models
  services/   Embedding model loading and encoding logic
  main.py     FastAPI app creation and router registration
```

## Run Locally

```bash
python -m venv .venv
.venv\Scripts\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8001
```

## Run With Docker

```bash
docker compose up --build
```

The service will be available at:

```text
http://localhost:8001
```

From the main backend running on the same server, call:

```text
http://127.0.0.1:8001/embed
```

From another container on the same Docker network, call:

```text
http://jazzify-embedding-worker:8000/embed
```

## Configuration

| Variable | Default | Notes |
| --- | --- | --- |
| `EMBEDDING_MODEL` | `sentence-transformers/paraphrase-multilingual-mpnet-base-v2` | Hugging Face / sentence-transformers model name or local path. |
| `EMBEDDING_NORMALIZE_DEFAULT` | `false` | Matches the existing RAG scripts' raw `SentenceTransformer.encode(...)` behavior. |
| `MAX_TEXTS_PER_REQUEST` | `64` | Batch limit. |
| `MAX_CHARS_PER_TEXT` | `8000` | Per-input safety limit. |
| `PRELOAD_MODEL` | `false` | Set to `true` to load the model during container startup. |
| `PORT` | `8000` | Container listen port. |
