# RAG-MVP2 Architecture Diagrams

## System Flow Diagram

```mermaid
graph TD
    A["User/Admin"] -->|Login| B["Login Controller"]
    B -->|Auth| C["Security Config"]
    C -->|Grant Role| D{User Role?}
    
    D -->|ROLE_USER| E["User Portal"]
    D -->|ROLE_ADMIN| F["Admin Portal"]
    
    E -->|Upload Doc| G["Document Controller"]
    E -->|Search| H["Retrieval Controller"]
    E -->|Web Crawl| I["Crawler Controller"]
    
    F -->|Manage Docs| J["Admin Controller"]
    F -->|View Stats| K["Monitoring Controller"]
    
    G -->|Store| L["Document Service"]
    L -->|Extract| M["Ingestion Processor"]
    M -->|Chunk| N["Document Storage Service"]
    N -->|Embed| O["Embedding Indexer"]
    
    O -->|Convert to Vectors| P["EmbeddingModel<br/>Strategy"]
    P -->|OpenAI/Gemini/<br/>Ollama/Hash| Q["Vector Store<br/>PgVector"]
    
    H -->|Query| R["Retrieval Service"]
    R -->|Semantic Search| Q
    Q -->|Ranked Results| S["Response DTO"]
    S -->|JSON| A
    
    I -->|URL Queue| T["Web Crawler Service"]
    T -->|Scrape| U["Web Scraper Service"]
    U -->|Parse HTML| V["Extract Text"]
    V -->|Enqueue Links| T
    V -->|Store Content| W["Crawled Content Repo"]
    W -->|Chunk & Embed| O
    
    K -->|Collect Stats| X["Statistics Service"]
    X -->|Query| Y["Database<br/>PostgreSQL"]
    
    J -->|CRUD| Z["Knowledge Base"]
    Z --> Y
```

## Class Diagram - Domain Model

```mermaid
classDiagram
    class AppUser {
        -Long userId
        -String username
        -String password
        -Set~RoleName~ roles
        -LocalDateTime createdAt
        +getAuthorities()
    }
    
    class RoleName {
        ROLE_ADMIN
        ROLE_USER
    }
    
    class KnowledgeDocument {
        -Long documentId
        -String title
        -String content
        -DocumentStatus status
        -String storagePath
        -Long userId
        -LocalDateTime uploadedAt
        +getEmbeddingText()
    }
    
    class DocumentStatus {
        PENDING
        INGESTING
        INDEXED
        FAILED
    }
    
    class WebCrawler {
        -Long crawlerId
        -String name
        -String seedUrl
        -CrawlerStatus status
        -LocalDateTime startedAt
        -LocalDateTime completedAt
        +isCrawling()
    }
    
    class WebCrawledContent {
        -Long contentId
        -Long crawlerId
        -String url
        -String title
        -String content
        -LocalDateTime crawledAt
        +toChunks()
    }
    
    class SearchQueryLog {
        -Long logId
        -Long userId
        -String query
        -Integer resultCount
        -LocalDateTime timestamp
    }
    
    class DocumentUploadedEvent {
        -Long documentId
        -String filename
        -Long userId
    }
    
    class IngestionStatistics {
        -Long totalDocuments
        -Long indexedDocuments
        -Long failedDocuments
        -Long totalChunks
    }
    
    class RetrievalResult {
        -String documentTitle
        -String content
        -Double similarityScore
        -String source
    }
    
    AppUser "1" --> "*" KnowledgeDocument
    AppUser "1" --> "*" SearchQueryLog
    KnowledgeDocument "1" --> "1" DocumentStatus
    AppUser "*" -- "*" RoleName
    WebCrawler "1" --> "*" WebCrawledContent
    KnowledgeDocument --> DocumentUploadedEvent
```

## Component Architecture Diagram

```mermaid
graph LR
    subgraph "Presentation Layer"
        HC["HomeController"]
        LC["LoginController"]
        DC["DocumentController"]
        AC["AdminController"]
        CC["CrawlerController"]
        MC["MonitoringController"]
        RC["RetrievalController"]
    end
    
    subgraph "Application Layer - Services"
        subgraph "Document Service"
            DS["DocumentService"]
            ADS["AdminDocumentService"]
            DOSS["DocumentStorageService"]
        end
        
        subgraph "Retrieval Service"
            RS["RetrievalService"]
        end
        
        subgraph "Crawler Service"
            WCS["WebCrawlerService"]
            WSS["WebScraperService"]
            CQS["CrawlQueueService"]
            CAR["CrawlerAsyncRunner"]
        end
        
        subgraph "Monitoring Service"
            MS["MonitoringService"]
            SS["StatisticsService"]
        end
        
        subgraph "Ingestion Pipeline"
            IP["IngestionProcessor"]
            IEL["IngestionEventListener"]
            EI["EmbeddingIndexer"]
        end
    end
    
    subgraph "Infrastructure Layer"
        subgraph "Embedding Strategy"
            EA["EmbeddingAutoConfiguration"]
            HEM["HashEmbeddingModel"]
            OAI["OpenAI Provider"]
            GEM["Gemini Provider"]
            OLL["Ollama Provider"]
        end
        
        subgraph "LLM Strategy"
            LA["LlmAutoConfiguration"]
            OAICM["OpenAI ChatModel"]
            OLLCM["Ollama ChatModel"]
        end
        
        subgraph "Data Access"
            KDR["KnowledgeDocumentRepository"]
            AUR["AppUserRepository"]
            WCR["WebCrawlerRepository"]
            WCCR["WebCrawledContentRepository"]
            SQLR["SearchQueryLogRepository"]
        end
    end
    
    subgraph "Data & External"
        DB["PostgreSQL<br/>with pgvector"]
        VS["Vector Store<br/>PgVector"]
        ES["External APIs<br/>OpenAI/Gemini"]
        WEB["Web Pages"]
    end
    
    HC --> DS
    DC --> DS
    AC --> ADS
    CC --> WCS
    RC --> RS
    MC --> SS
    
    DS --> IP
    IP --> EI
    IEL --> EI
    EI --> EA
    
    WCS --> WSS
    WSS --> WEB
    WCS --> CQS
    CAR --> WCS
    
    EA --> HEM
    EA --> OAI
    EA --> GEM
    EA --> OLL
    
    RS --> VS
    DS --> KDR
    ADS --> AUR
    WCS --> WCR
    WCS --> WCCR
    
    KDR --> DB
    AUR --> DB
    WCR --> DB
    WCCR --> DB
    SQLR --> DB
    
    VS --> DB
    OAI --> ES
    GEM --> ES
    OAICM --> ES
```

## RBAC (Role-Based Access Control) Diagram

```mermaid
graph TD
    A["User"] -->|Login| B["Authentication<br/>Form-Based"]
    B -->|Verified| C{Authorization}
    
    C -->|ROLE_ADMIN| D["Admin Access"]
    C -->|ROLE_USER| E["User Access"]
    C -->|No Role| F["Public Access<br/>Login/Error/Static"]
    
    D -->|/admin/**| D1["Admin Dashboard"]
    D -->|@PreAuthorize<br/>hasRole ADMIN| D2["Manage Users"]
    D -->|@PreAuthorize<br/>hasRole ADMIN| D3["View Statistics"]
    D -->|@PreAuthorize<br/>hasRole ADMIN| D4["Delete Documents"]
    D -->|@PreAuthorize<br/>hasRole ADMIN| D5["System Monitoring"]
    
    D1 --> D2
    D1 --> D3
    D1 --> D4
    D1 --> D5
    
    E -->|/home| E1["Home Page"]
    E -->|/documents/**| E2["Document Mgmt"]
    E -->|/retrieve/search| E3["Search & Retrieve"]
    E -->|/crawler/**| E4["Web Crawler Control"]
    
    E2 -->|@PreAuthorize<br/>hasAnyRole USER ADMIN| E2A["Upload Document"]
    E2 -->|@PreAuthorize<br/>hasAnyRole USER ADMIN| E2B["View My Documents"]
    E2 -->|@PreAuthorize<br/>hasAnyRole USER ADMIN| E2C["Ingest Document"]
    
    E3 -->|@PreAuthorize<br/>hasAnyRole USER ADMIN| E3A["Semantic Search"]
    E3 -->|@PreAuthorize<br/>hasAnyRole USER ADMIN| E3B["Log Query"]
    
    E4 -->|@PreAuthorize<br/>hasAnyRole USER ADMIN| E4A["Start Crawler"]
    E4 -->|@PreAuthorize<br/>hasAnyRole USER ADMIN| E4B["View Crawler Status"]
    E4 -->|@PreAuthorize<br/>hasAnyRole USER ADMIN| E4C["Pause/Resume/Stop"]
    
    F -->|/login| F1["Login Page"]
    F -->|/css/**,/images/**| F2["Static Resources"]
    F -->|/error| F3["Error Page"]
```

## Data Flow - Document Ingestion Pipeline

```mermaid
sequenceDiagram
    participant User
    participant Controller
    participant Service
    participant Storage
    participant Processor
    participant Embedder
    participant VectorStore
    
    User->>Controller: Upload Document
    Controller->>Service: receiveDocument()
    Service->>Storage: storeFile()
    Storage-->>Service: filePath
    Service->>Processor: publishEvent()
    Processor->>Processor: onDocumentUploaded()
    Processor->>Processor: extractAndChunk()
    Processor->>Embedder: embedChunks()
    Embedder->>Embedder: selectEmbeddingModel()
    Note over Embedder: Strategy Pattern:<br/>OpenAI/Gemini/Ollama/Hash
    Embedder->>Embedder: generateVectors()
    Embedder->>VectorStore: storeEmbeddings()
    VectorStore->>VectorStore: pgvector INSERT
    VectorStore-->>Embedder: Success
    Embedder-->>Processor: Indexed
    Processor->>Service: updateStatus(INDEXED)
    Service-->>Controller: Complete
    Controller-->>User: Success Response
```

## Data Flow - Retrieval/Search Pipeline

```mermaid
sequenceDiagram
    participant User
    participant Controller
    participant RetrievalService
    participant EmbeddingModel
    participant VectorStore
    participant DB
    participant Response
    
    User->>Controller: Search Query
    Controller->>RetrievalService: search(query)
    RetrievalService->>EmbeddingModel: embed(query)
    Note over EmbeddingModel: Using Active Provider
    EmbeddingModel-->>RetrievalService: queryVector[]
    RetrievalService->>VectorStore: similaritySearch(queryVector)
    VectorStore->>VectorStore: pgvector<br/>COSINE_DISTANCE<br/>with HNSW index
    VectorStore-->>RetrievalService: topK Results
    RetrievalService->>DB: getDocumentMetadata()
    DB-->>RetrievalService: metadata
    RetrievalService->>Response: buildSearchResponse()
    Response-->>Controller: RetrievalResult[]
    Controller-->>User: JSON Response<br/>with similarity scores
```

## Web Crawler Pipeline

```mermaid
graph LR
    A["Start URL"] -->|Enqueue| B["URL Queue<br/>In-Memory"]
    B -->|Dequeue| C["WebScraperService"]
    C -->|HTTP Fetch<br/>ignoreHttpErrors| D["HTML Response"]
    D -->|Jsoup Parse| E["Content Extraction"]
    E -->|Text + Links| F["WebCrawlerService"]
    F -->|Persist| G["WebCrawledContent DB"]
    F -->|New Links| H["Link Extraction"]
    H -->|Check visited/pending| I["Add to Queue"]
    I -->|Non-duplicate| B
    
    G -->|Content| J["Ingestion Pipeline"]
    J -->|Chunk & Embed| K["Vector Store"]
    
    L["Async Loop<br/>CrawlerAsyncRunner"] -->|Pull URLs| B
    L -->|Consecutive Empty<br/>Retry 3x| L
    L -->|Status Check| M["WebCrawler Status"]
    M -->|Running?| L
    M -->|Paused/Stopped| N["Halt Loop"]
```

## Embedding Provider Strategy Pattern

```mermaid
graph TD
    A["@ConditionalOnProperty<br/>app.embedding.provider"] -->|Evaluates| B["EmbeddingAutoConfiguration"]
    
    B -->|provider=hash| C["HashEmbeddingModel<br/>8-dim<br/>Dev/Test/Default"]
    B -->|provider=openai| D["OpenAI<br/>text-embedding-3-small<br/>1536-dim"]
    B -->|provider=gemini| E["Gemini<br/>text-embedding-004<br/>768-dim<br/>Vertex AI"]
    B -->|provider=ollama| F["Ollama<br/>nomic-embed-text<br/>768-dim<br/>Local"]
    
    C -->|Implements| G["EmbeddingModel"]
    D -->|Implements| G
    E -->|Implements| G
    F -->|Implements| G
    
    G -->|Injected into| H["VectorStoreConfiguration"]
    H -->|dimensions| I["PgVectorStore<br/>Adaptive Dimensions"]
    
    J["application.yml<br/>app.embedding"] -->|config| C
    J -->|config| D
    J -->|config| E
    J -->|config| F
```

## Security Architecture

```mermaid
graph TD
    A["HTTP Request"] -->|Filter Chain| B["SecurityConfiguration"]
    
    B -->|/login<br/>/css/**<br/>/images/**| C["Permit All"]
    B -->|/admin/**| D["require ADMIN"]
    B -->|/documents/**<br/>/retrieve/**<br/>/crawler/**| E["require ADMIN<br/>or USER"]
    
    C -->|Public| F["Login Controller<br/>Static Resources"]
    D -->|Admin Only| G["Admin Controller"]
    E -->|Authenticated| H["Document/Retrieval<br/>Controllers"]
    
    B -->|Form-Based| I["DatabaseUserDetailsService"]
    I -->|Load User| J["AppUser Entity<br/>+ Roles"]
    J -->|Check Credentials| K["Authenticate"]
    
    K -->|Create Session| L["HTTP Session<br/>SecurityContext"]
    L -->|Include in Request| M["Method-Level<br/>@PreAuthorize"]
    
    M -->|hasRole ADMIN| N["Admin Operations"]
    M -->|hasAnyRole USER,ADMIN| O["User Operations"]
```
