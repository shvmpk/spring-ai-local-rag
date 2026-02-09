# Document Search & Insights Platform 
**(Local AI Chat + Document ETL + Model Management Platform)**

This project is a **scalable LLM backend** designed to run fully **locally** using:

* **Ollama** (local LLM runtime)
* **ChromaDB** (vector database)
* **RabbitMQ** (async ETL pipeline)
* **Spring Boot** (API + orchestration)
* **Local filesystem** for file storage (S3 replacement planned)

It supports **chat**, **conversation memory**, **document ingestion**, **chunk embedding**, and **dynamic model management** through Ollama's HTTP API — all architected in a clean, maintainable, and scalable way.

This is not a toy demo — it is built with enterprise-grade patterns, clean separation of concerns, and future cloud migration in mind.

---

## 🚀 Features

### 🧠 1. **Chat with LLMs (Local)**

* Supports any Ollama model installed locally
* Maintains **conversation memory** per session ID
* Clean abstraction through `ChatService`
* Model can be selected per request
* No business logic inside controllers — all logic lives in services

---

### 📄 2. **Document Upload & Storage**

* Upload any file type (PDF, DOCX, TXT, HTML, etc.)
* Files stored locally under `data/uploads/`
* Storage abstraction through `FileStorageService`
* Ready to be swapped for Amazon S3 in the future with zero changes to controllers

---

### 🔄 3. **Asynchronous ETL Pipeline**

When a file is uploaded:

1. File is saved locally
2. Path is pushed to RabbitMQ
3. Background `EtlWorker` picks it up
4. Tika extracts text
5. Text is chunked using a TokenTextSplitter
6. Chunks are embedded
7. Chunks are persisted into Chroma vector store

This allows uploads to be instant while large files get processed in the background.

---

### 🧬 4. **Chroma Vector Store Integration**

* Stores embeddings for all processed document chunks
* Ready for intelligent RAG retrieval
* Simple and future-proof — can be replaced by Pinecone/Weaviate later

---

### ⚙️ 5. **Dynamic Model Management (Ollama)**

Handles real model installation workflow:

* Pull models using **Ollama’s HTTP API**
* Check if a model is already installed
* Track install progress (0–100%)
* Support **parallel installs** with a configurable limit
* Cancel running installs
* Persist and query model install state
* Clean separation via `ModelManagementService`

This lets your app behave like a real AI platform — not a static toy.

---

### 💬 6. **Session History**

* Retrieve paginated conversation messages
* Stored in ChatMemory per session ID
* Lightweight and fast

---

### 🧱 7. **Clean Architecture**

The project intentionally avoids the anti-patterns that plague 99% of LLM backend code:

* No controller bloating
* No long-running processes inside controllers
* No inline shell commands
* No state stored in controllers
* No duplicated ETL logic
* No single god-class

Every concern lives in the correct layer — period.

---

## 🏗️ Architecture Overview

### Application Layers

```
controller/
    ChatController
    ModelController
    UploadController
    SessionController

service/
    ChatService
    ModelManagementService
    FileStorageService

etl/
    EtlWorker
    EtlMessagePublisher

config/
    AiConfiguration
    RabbitConfig
    ExecutorConfig
    ChromaConfig
```

### Data Flow

**Chat Flow**

```
Request → ChatController → ChatService → ChatClient/Ollama → Response
```

**Document ETL Flow**

```
UploadController → FileStorageService → RabbitMQ → EtlWorker →
Tika → Chunking → Embedding → Chroma
```

**Model Management Flow**

```
ModelController → ModelManagementService → Ollama HTTP API → Progress Registry
```

---

## ⚡ Technology Stack

### **Backend**

* **Java 21**
* **Spring Boot**
* **Spring AI**
* **Spring AMQP**
* **Spring Web**
* **Spring Validation**

### **AI Runtime**

* **Ollama** (local LLM engine)

### **Vector Store**

* **ChromaDB**

### **Message Queue**

* **RabbitMQ**

### **ETL**

* **Apache Tika**
* **TokenTextSplitter**

### **Storage**

* **Local filesystem**
  (S3 planned)

---

## 📦 Features Under Development / Future Additions

* Replace RabbitMQ with **Kafka** for distributed ETL
* Replace local FS with **S3** for scalable file storage
* Add **RAG search endpoint**
* Add **websocket streaming** for chat
* Add **fine-grained model permissions**
* Add **auto-model-download based on usage patterns**

---

## 🏁 Running the Project

### Requirements:

* Java 21
* Ollama installed
* RabbitMQ running
* Chroma running

### Example

```
./mvnw spring-boot:run
```

Upload files via:

```
POST /upload
```

Chat via:

```
POST /chat/inference
```

Manage models via:

```
POST /models/install
GET  /models/status/{model}
POST /models/cancel/{model}
```

---
