# **Architecting Sangrah: A Scalable Pay-as-You-Use Cloud Storage Platform**

## **1\. Executive Summary: The Evolution of Cloud Storage Architectures**

The paradigm of digital data storage has shifted precipitously over the last decade, moving from static, on-premise file servers to dynamic, distributed cloud environments. In this contemporary landscape, the "Sangrah" project represents a forward-thinking response to the limitations of rigid subscription models and monolithic architectures. By implementing a "Pay-as-You-Use" billing model, Sangrah aligns infrastructure costs directly with user consumption, mirroring the utility computing models popularized by infrastructure providers like AWS and Snowflake.1 This report delineates the comprehensive technical architecture, design philosophy, and implementation strategy for building Sangrah using a robust stack of Spring Boot microservices, a MongoDB persistence layer, and an Angular frontend.

The central thesis of this architecture is **decoupling**. Traditional storage applications often suffer from performance degradation when compute-intensive tasks—such as generating video thumbnails or indexing metadata—compete for resources with latency-sensitive tasks like serving files or processing payments. By adopting a microservices architecture, Sangrah isolates these distinct domains.3 The "Media Service" can scale horizontally to handle holiday traffic spikes, while the "AI Service" processes face recognition tasks asynchronously via RabbitMQ 4, and the "Billing Service" executes high-volume batch jobs during off-peak hours.5

Furthermore, the integration of Artificial Intelligence (AI) for content discovery transforms the platform from a passive "digital locker" into an intelligent media library. The implementation of face recognition, utilizing Python-based inference engines and MongoDB’s native vector search, allows users to retrieve content based on semantic meaning rather than just file names.6 This feature, however, introduces significant architectural complexity, necessitating a hybrid technology stack that bridges the Java-centric backend with the Python-centric AI ecosystem.

This document serves as an exhaustive technical blueprint. It explores the nuances of schema design in a document-oriented database, the mathematics of byte-level metering for fair billing, the optimization of frontend rendering for massive image galleries, and the security protocols required to protect user privacy in a distributed system.

## **2\. Architectural Paradigm and System Design**

### **2.1 The Case for Microservices in Storage Systems**

In the initial phases of systems development, the monolithic architecture offers simplicity in deployment and testing. However, for a platform like Sangrah, which demands independent scalability of disparate components, the monolith becomes a liability. The transition to microservices is not merely a trend but a strategic necessity for applications requiring high availability and fault tolerance.3 In the context of Sangrah, the "Pay-as-You-Use" model introduces a specific requirement: the billing engine must remain operational and accurate even if the media upload service is under heavy load or experiencing downtime. A monolithic structure would couple these failure domains, potentially risking revenue leakage during technical outages.

The architecture leverages the **Spring Ecosystem** as its backbone. Spring Boot acts as the standard for building the individual microservices, providing a production-ready environment with embedded servers (Tomcat/Netty) that simplifies the containerization process.8 To manage the complexity inherent in distributed systems, **Spring Cloud** is employed for orchestration. This includes service discovery via Netflix Eureka, which allows services to locate each other dynamically without hardcoded IP addresses, and Spring Cloud Gateway, which serves as the unified entry point for the Angular frontend, managing routing, rate limiting, and cross-cutting security concerns.3

#### **2.1.1 Domain-Driven Decomposition**

The system is decomposed into bounded contexts, each represented by a dedicated microservice. This separation ensures that development teams can iterate on specific features—such as upgrading the face recognition model—without necessitating a redeployment of the entire platform.

| Service Name | Primary Responsibility | Technology Stack | Scaling Profile |
| :---- | :---- | :---- | :---- |
| **Auth Service** | Identity management, JWT issuance, RBAC. | Spring Boot, Spring Security | CPU-bound (hashing) |
| **Media Service** | Metadata management, file/folder logic, S3 interactions. | Spring Boot, AWS SDK | IO-bound (Database/Network) |
| **AI Inference Service** | Face detection, embedding generation. | Python (FastAPI), PyTorch | GPU/CPU-intensive |
| **Billing Service** | Usage metering, daily cost calculation, invoicing. | Spring Boot, Spring Batch | Burst (Batch windows) |
| **Notification Service** | Transactional emails, billing alerts. | Spring Boot, JavaMail | IO-bound (Async) |
| **Search Service** | Vector and full-text search queries. | Spring Boot, MongoDB Atlas | IO-bound (Read heavy) |

### **2.2 Asynchronous Event-Driven Communication**

A critical architectural decision in Sangrah is the minimization of synchronous HTTP calls between internal services. While the API Gateway communicates with backend services via REST, inter-service communication—particularly for long-running processes—utilizes the Advanced Message Queuing Protocol (AMQP) via **RabbitMQ**.9

For instance, when a user uploads a large video file, the Media Service does not synchronously invoke the AI Service. Doing so would block the thread, consume memory, and degrade the user experience, potentially leading to timeouts if the AI processing takes longer than the HTTP request limit. Instead, the Media Service publishes a media.uploaded event to a RabbitMQ exchange. The AI Service, subscribing to this queue, consumes the event at its own pace.4 This buffering capability acts as a shock absorber; even if user uploads spike to 10,000 requests per second, the AI Service can process the backlog steadily without crashing the system or rejecting uploads.

Furthermore, this pattern supports the "Smart Storage" requirement. Multiple consumers can subscribe to the same media.uploaded event. One consumer might trigger face recognition, while another triggers virus scanning or format transcoding. This extensibility is central to the microservices philosophy, allowing new features to be added by simply attaching new consumers to existing event streams without modifying the producer code.11

### **2.3 System Resilience and Fault Tolerance**

Distributed systems introduce the fallacy of reliable networks. Services *will* fail, and the network *will* have latency. To mitigate this, Sangrah integrates **Resilience4j** patterns within the Spring Boot application. Circuit Breakers are configured on all remote calls. If the AI Service fails repeatedly, the circuit opens, and the Media Service immediately returns a fallback response (e.g., "AI processing pending") instead of waiting for timeouts. This prevents cascading failures where one struggling service consumes all available threads in the calling service, bringing down the entire platform.3

Additionally, the **Saga Pattern** is utilized for distributed transactions. Since MongoDB supports multi-document transactions but not cross-database transactions (in a polyglot scenario), operations spanning multiple services (e.g., creating a user, provisioning a billing wallet, and initializing an S3 bucket) must be managed logically. If the billing wallet creation fails, the Saga orchestrator publishes a compensating event to roll back the user creation, ensuring eventual consistency.

## ---

**3\. Data Persistence Layer: Schema and Strategy**

### **3.1 The Strategic Choice of MongoDB**

The selection of MongoDB as the primary persistence layer is driven by the specific nature of media metadata. Unlike transactional financial ledgers which fit neatly into rows and columns, media metadata is inherently semi-structured and polymorphic. A file might be a JPEG image with EXIF data (shutter speed, ISO, GPS), or it might be a PDF with page counts and author tags. A relational database (RDBMS) would require complex Entity-Attribute-Value (EAV) tables or numerous nullable columns to accommodate this variety. MongoDB’s BSON document model allows Sangrah to store rich, nested metadata natively, adapting to new file types without expensive schema migrations.12

Moreover, the requirement for **Vector Search** to support facial recognition heavily favors MongoDB Atlas. Traditional architectures would require a separate specialized vector database (like Pinecone or Milvus) alongside the metadata store. MongoDB Atlas Vector Search unifies these, allowing developers to store face embeddings in the same document (or a linked collection) as the metadata. This consolidation simplifies the infrastructure, reduces data synchronization latency, and enables powerful "hybrid queries"—such as searching for "Faces similar to X" AND "Date is 2024" in a single query execution.14

### **3.2 Schema Design: Embedding vs. Referencing**

Effective schema design in MongoDB relies on understanding the cardinality of relationships—whether a relationship is "one-to-few," "one-to-many," or "one-to-squillions".16 Sangrah employs a hybrid approach to optimize for read performance while maintaining data integrity.

#### **3.2.1 Media and Folder Structure**

For the directory structure, the system uses a **Referencing** pattern with materialized paths. While embedding files inside a folder document seems intuitive, it fails at scale; a folder with 100,000 files would exceed the 16MB BSON document limit.17 Instead, each File document contains a reference to its parentFolderId.

To optimize the "read folder contents" query, the schema includes a compound index on { parentFolderId: 1, name: 1 }. This allows the application to retrieve files within a folder sorted by name without performing an in-memory sort.

JavaScript

// File Document Schema  
{  
  "\_id": ObjectId("..."),  
  "userId": ObjectId("..."),  // Owner  
  "parentFolderId": ObjectId("..."),  
  "metadata": {               // Embedded polymorphic metadata  
    "type": "IMAGE",  
    "width": 1920,  
    "height": 1080,  
    "camera": "Sony A7III"  
  },  
  "s3Key": "users/123/vacation.jpg",  
  "sharedWith": \[             // Array of references for sharing  
    ObjectId("..."),   
    ObjectId("...")  
  \]   
}

The sharedWith array utilizes the **Extended Reference Pattern**. Instead of just storing the User ID, we might store a small sub-document { userId, permissions, addedDate }. This avoids a secondary lookup (JOIN) on the Users collection just to check permission levels during file access.16

#### **3.2.2 User and Usage History (Bucket Pattern)**

The "Pay-as-You-Use" billing requirement necessitates storing granular usage data. Storing one document per upload/delete event would result in billions of documents. Conversely, storing all history in a single User document would hit the size limit. Sangrah adopts the **Bucket Pattern**.

A UsageBucket document is created for each user per billing cycle (e.g., monthly). This document contains an array of daily summaries or hourly snapshots.

JavaScript

// Usage Bucket Document  
{  
  "\_id": ObjectId("..."),  
  "userId": ObjectId("..."),  
  "billingPeriod": "2023-10",  
  "dailyUsage":  
}

This pattern optimizes read performance for the Billing Service. When generating an invoice, the service fetches a single document per user rather than aggregating thousands of individual event logs, significantly reducing disk I/O and CPU overhead.18

### **3.3 Database Sharding Strategy**

As the platform grows, a single replica set will eventually hit write capacity or storage limits. MongoDB’s **Sharding** capability allows for horizontal scaling. Sangrah utilizes **Hashed Sharding** on the userId field for the Media collection. This ensures that data is evenly distributed across multiple shards based on the hash of the user ID, preventing "hot chunks" where sequential writes (e.g., a single user uploading thousands of files) overwhelm a single shard.20

While Range-based sharding supports efficient range queries, the primary access pattern in Sangrah is isolating data by user. Therefore, ensuring an even distribution of users across shards is prioritized over range query efficiency across different users.22

## ---

**4\. The Python-AI Bridge: Face Recognition Implementation**

### **4.1 Hybrid Technology Stack**

While Java/Spring Boot provides the stability and enterprise integration required for the core backend, the center of gravity for machine learning and computer vision lies firmly in the Python ecosystem. Libraries like PyTorch, TensorFlow, and InsightFace offer superior performance and ease of use compared to their Java counterparts.23 Consequently, Sangrah adopts a polyglot architecture where the AI processing is offloaded to Python microservices.

### **4.2 The Face Recognition Pipeline**

The implementation of face recognition is not merely comparing two images; it is a multi-stage pipeline designed to extract robust "embeddings"—numerical representations of facial features that are invariant to lighting, pose, and age.6

#### **4.2.1 Stage 1: Detection (RetinaFace)**

Upon receiving an image processing task, the AI Service first employs a face detection model. Sangrah utilizes **RetinaFace**, a component of the InsightFace library. Research and benchmarks indicate that RetinaFace offers superior performance in detecting faces at varying scales (e.g., a small face in a crowd) compared to older Haar Cascades or HOG-based detectors used in libraries like dlib.24

#### **4.2.2 Stage 2: Alignment and Normalization**

A detected face might be tilted or rotated. The system identifies facial landmarks (eyes, nose tip, mouth corners) and performs an affine transformation to align the face to a standard frontal geometry. This step is crucial for the accuracy of the subsequent embedding generation.26

#### **4.2.3 Stage 3: Feature Extraction (ArcFace)**

The aligned face crop is passed through a Deep Convolutional Neural Network (CNN), specifically **ArcFace** (ResNet-100 backbone). This model maps the face into a 512-dimensional vector space. The key property of this space is that the Euclidean distance (or angle) between vectors corresponds to facial similarity.6

Sangrah migrates away from older models like **DeepFace** or **VGG-Face** due to performance bottlenecks. DeepFace often struggles with multi-image throughput and GPU parallelism in production environments. **InsightFace**, by contrast, is optimized for batch inference, allowing the system to process multiple photos in parallel on the GPU, significantly reducing the latency per upload.28

### **4.3 Storing and Searching Vectors**

Once generated, these 512-dimensional vectors must be stored and indexed. In MongoDB Atlas, the FaceEmbeddings collection is configured with a vector search index.

**Vector Index Configuration:**

To optimize for speed and recall, an Hierarchical Navigable Small World (**HNSW**) algorithm is used for the index.

JSON

{  
  "fields":  
}

**Cosine Similarity** is chosen over Euclidean distance because ArcFace embeddings are typically normalized to lie on a hypersphere; thus, the angle (cosine) is the most semantically meaningful measure of similarity.7

When a user selects a face and asks "Who is this?", the system performs a $vectorSearch aggregation. This operation finds the ![][image1] nearest neighbors in the high-dimensional space. Unlike standard database queries which are exact, vector search is approximate (ANN \- Approximate Nearest Neighbor). The results are returned with a "similarity score." A threshold (e.g., 0.6) is applied to filter out false positives.30

## ---

**5\. Pay-as-You-Use Billing Engine**

### **5.1 The Complexity of Metered Storage**

Implementing a true "Pay-as-You-Use" model is significantly more complex than fixed-tier subscriptions. The system must account for the integral of storage used over time. If a user stores 1TB for 12 hours and then deletes it, they should be billed for 0.5 TB-Days (or 12 TB-Hours). A simple daily snapshot at midnight would miss this usage entirely, leading to revenue leakage. Conversely, continuous polling is computationally expensive.32

Sangrah employs an **Event Sourcing** approach for billing. Every state change in the storage layer—FileUploaded, FileDeleted, FileRestored—is recorded as an immutable event in the UsageEvents collection.

### **5.2 Spring Batch Processing**

To calculate the daily bill, Sangrah utilizes **Spring Batch**, the industry-standard framework for robust batch processing in Java.5 A scheduled job runs nightly (or hourly) to process these events.

#### **5.2.1 The Batch Job Architecture**

The job consists of a Job containing multiple Steps:

1. **Partitioning Step:** To handle millions of users, the job is partitioned. A Partitioner divides the user base into ranges (e.g., User IDs 0-10000, 10001-20000).  
2. **Slave Steps:** Multiple threads (or worker pods in Kubernetes) process these partitions in parallel.35  
3. **ItemReader:** Reads the previous day's closing balance (Snapshot) and the day's events for a specific user.  
4. **ItemProcessor:** Replays the events to calculate the "Byte-Seconds" or "Byte-Hours" utilized.  
   * *Algorithm:*  
     ![][image2]  
     This allows for precise, auditable billing even for short-lived files.5  
5. **ItemWriter:** Writes the calculated cost to the BillingLedger and updates the user's current storage snapshot for the next run.

### **5.3 Shared Content Billing Logic**

A critical business logic decision in cloud storage is the attribution of cost for shared content.

* **Storage Cost:** Follows the "Owner Pays" model. The user who uploads the file owns the storage object and incurs the cost.  
* **Bandwidth (Egress) Cost:** This is more nuanced. If User A shares a popular video that is downloaded 1,000 times by others, User A could face a massive bill. Sangrah implements configurable policies:  
  * *Standard:* Owner pays for egress (similar to S3).  
  * *Throttled:* Owner sets a bandwidth budget; once exceeded, the file becomes unavailable or requires the downloader to pay.  
  * *Design Implication:* The billing system must track DownloadEvents and attribute the byte count to the appropriate PayerID based on the active policy.37

## ---

**6\. Storage Infrastructure Optimization**

### **6.1 Direct-to-S3 Uploads (Presigned URLs)**

A common performance bottleneck in web applications is routing file uploads through the application server (Client ![][image3] Spring Boot ![][image3] S3). This "double-hop" architecture consumes server bandwidth, blocks threads, and increases memory pressure on the JVM.39

Sangrah implements a **Direct-to-Cloud** pattern using S3 Presigned URLs.

1. **Request:** The Angular client requests an upload slot.  
2. **Authorization:** The Spring Boot backend verifies the user's quota and generates a **Presigned PUT URL**. This URL effectively delegates a temporary, limited capability to the client to write directly to a specific S3 object key.  
3. **Upload:** The client pushes the binary data directly to AWS S3.  
4. **Verification:** Upon completion, the client notifies the backend, which may perform a lightweight verification (e.g., checking object existence via headObject) before creating the metadata record.41

**Performance Comparison Table:**

| Method | Latency | Server Load | Scalability |
| :---- | :---- | :---- | :---- |
| **Server Proxy** | High (2 hops) | High (Buffering streams) | Low (Bottlenecked by server I/O) |
| **Presigned URL** | Low (1 hop) | Negligible (URL generation only) | High (Offloaded to AWS S3) |

For files larger than 100MB, the backend generates a **Multipart Upload** configuration, allowing the frontend to upload chunks in parallel. This saturates the user's available bandwidth and provides resilience; if one chunk fails, only that chunk needs retrying.41

### **6.2 Secure Content Delivery**

Serving private content requires a mechanism to validate permissions without routing every byte through the application server. While S3 Presigned URLs are useful for uploads, **CloudFront Signed URLs** are superior for content delivery (downloads/streaming). CloudFront Signed URLs allow the content to be cached at Edge Locations, significantly reducing latency for the user and egress costs for the platform.44

The Media Service acts as the signer. When the Angular application loads a gallery, it requests a batch of signed URLs for the visible images. These URLs contain a cryptographic signature and an expiration time.

**Security Consideration:** To prevent "URL sharing" where a user generates a signed URL and shares it publicly, the expiration time is kept short (e.g., 1 hour), and the signature can be bound to the user's IP address range if strict security is required.45

## ---

**7\. Frontend Engineering: Angular Implementation**

### **7.1 Virtual Scrolling for Performance**

A key requirement for a storage platform is the ability to browse massive libraries of photos. A naive implementation rendering standard DOM elements for 5,000 photos will crash the browser due to memory exhaustion and DOM reflow overhead.

Sangrah utilizes the **Angular Component Development Kit (CDK)**, specifically the VirtualScrollViewport.

* **Mechanism:** The viewport renders only the items currently visible to the user plus a small buffer. As the user scrolls, DOM nodes are recycled and repurposed for the new data.  
* **Result:** The browser maintains a constant number of DOM elements (e.g., 20\) regardless of whether the list contains 100 or 100,000 items.46

This is paired with a **Hybrid Pagination** strategy. While the UI presents an "infinite scroll" experience, the data layer utilizes standard pagination (fetching page 1, then page 2\) from the backend API. This balances the UX of infinite scrolling with the network efficiency of pagination.47

### **7.2 Image Optimization and Lazy Loading**

Bandwidth conservation is critical for a "Pay-as-You-Use" platform; users do not want to pay for data they haven't explicitly viewed. Sangrah leverages the **NgOptimizedImage** directive (ngSrc) introduced in recent Angular versions.

* **Automatic srcset:** The directive automatically generates srcset attributes, ensuring that a mobile device downloads a 400px wide thumbnail while a desktop downloads a 1200px preview.  
* **Lazy Loading:** Images are configured to load only when they approach the viewport.  
* **LCP Prioritization:** The "Largest Contentful Paint" (LCP) image—typically the first large image a user sees—is marked with priority to bypass lazy loading and improve perceived load speed.49

### **7.3 Security in the Frontend**

Traditional session-based auth (cookies) is often replaced by token-based auth (JWT) in microservices.

* **JWT Interceptor:** An Angular HttpInterceptor automatically attaches the Bearer token to all outgoing API requests.  
* **Handling \<img\> Tags:** Standard HTML image tags cannot send custom headers (like Authorization). Since the backend generates Signed URLs (which have authorization embedded in the query string), the frontend can use standard \<img\> tags safely.  
* **Token Refresh:** The frontend handles 401 Unauthorized responses by attempting to refresh the access token using a refresh token (stored in a secure, HTTP-only cookie) before redirecting the user to login.52

## ---

**8\. Security, Compliance, and Privacy**

### **8.1 Authentication and Authorization Framework**

Security is paramount. The system delegates identity management to an OAuth2/OIDC provider (like Keycloak or a custom Spring Authorization Server).

* **Resource Server:** Each microservice (Media, Billing) is configured as an OAuth2 Resource Server. They validate the JWT signature locally (using the public key) without calling the Auth Service for every request, reducing latency.  
* **RBAC:** Role-Based Access Control is enforced using Spring Security's Method Security (@PreAuthorize("hasRole('PREMIUM')")).  
* **Scope-Based Access:** Scopes (e.g., media:read, media:write) restrict what third-party applications can do on behalf of a user.52

### **8.2 Encryption Strategies**

* **At Rest:** Data in MongoDB Atlas is encrypted at the volume level. Sensitive fields (like PII in the User collection) can be encrypted at the field level using MongoDB's Client-Side Field Level Encryption (CSFLE), ensuring that even a database administrator cannot view user emails or names. S3 buckets are configured with Server-Side Encryption (SSE-S3 or SSE-KMS).  
* **In Transit:** All internal traffic between microservices is encrypted via mTLS (Mutual TLS), and all external traffic is forced over HTTPS/TLS 1.3.30

### **8.3 Biometric Data Privacy**

The storage of face embeddings constitutes the processing of biometric data, which is subject to strict regulation (GDPR, CCPA).

* **Anonymization:** The FaceEmbeddings collection does not store names. It stores a personId. The mapping between personId and a human-readable name ("Dad") is stored in a separate, user-specific collection.  
* **Right to be Forgotten:** If a user requests deletion, the system performs a "hard delete" of all embeddings associated with their User ID. Since embeddings are just mathematical representations and not the actual photos, this process is cleaner, but compliance requires that the source photos (if used for training) are also managed according to policy.30

## ---

**9\. Conclusion**

The Sangrah Pay-as-You-Use Cloud Storage Platform represents a sophisticated convergence of modern software engineering disciplines. It addresses the "subscription fatigue" of the modern market by engineering a true utility-based billing model, supported by a rigorous **Spring Batch** metering engine. The architecture prioritizes resilience and scale through **Spring Boot Microservices**, ensuring that the high-throughput demands of media streaming do not compromise the transactional integrity of billing.

The integration of **MongoDB** proves pivotal, offering the schema flexibility to handle diverse file metadata while simultaneously providing the native vector search capabilities required to power next-generation **AI features**. By bridging the Java backend with **Python-based InsightFace** inference, the platform delivers state-of-the-art facial recognition without sacrificing the stability of the core application.

Finally, the **Angular frontend** demonstrates that handling massive datasets in the browser need not come at the cost of performance. Through virtualization, lazy loading, and intelligent caching, Sangrah delivers a fluid user experience that rivals native desktop applications. This report provides a comprehensive roadmap for architects and developers to realize this vision, building a storage platform that is not only robust and secure but also commercially aligned with the evolving expectations of the cloud economy.

#### **Works cited**

1. MongoDB Pricing, accessed on February 3, 2026, [https://www.mongodb.com/pricing](https://www.mongodb.com/pricing)  
2. Billing for Amazon Redshift Serverless \- AWS Documentation, accessed on February 3, 2026, [https://docs.aws.amazon.com/redshift/latest/mgmt/serverless-billing.html](https://docs.aws.amazon.com/redshift/latest/mgmt/serverless-billing.html)  
3. Microservices \- Spring, accessed on February 3, 2026, [https://spring.io/microservices/](https://spring.io/microservices/)  
4. Microservices asynchronous response \- spring boot \- Stack Overflow, accessed on February 3, 2026, [https://stackoverflow.com/questions/58306667/microservices-asynchronous-response](https://stackoverflow.com/questions/58306667/microservices-asynchronous-response)  
5. How Spring Boot Configures Scheduled Batch Jobs | Medium, accessed on February 3, 2026, [https://medium.com/@AlexanderObregon/how-spring-boot-configures-scheduled-batch-jobs-e27aea943a36](https://medium.com/@AlexanderObregon/how-spring-boot-configures-scheduled-batch-jobs-e27aea943a36)  
6. 1\. Introduction to Face Recognition | by Rajneesh Gupta | Dec, 2025 | Medium, accessed on February 3, 2026, [https://medium.com/@gupta.rajneesh2010/868fb0d0476f](https://medium.com/@gupta.rajneesh2010/868fb0d0476f)  
7. How to Store and Query Embeddings in MongoDB \- DataCamp, accessed on February 3, 2026, [https://www.datacamp.com/tutorial/how-to-store-query-embeddings-mongodb](https://www.datacamp.com/tutorial/how-to-store-query-embeddings-mongodb)  
8. Essential Principles of Spring Boot Microservices Architecture \- Mobisoft Infotech, accessed on February 3, 2026, [https://mobisoftinfotech.com/resources/blog/essential-principles-spring-boot-microservices](https://mobisoftinfotech.com/resources/blog/essential-principles-spring-boot-microservices)  
9. Building a Spring Boot Application with RabbitMQ \- Elinext, accessed on February 3, 2026, [https://www.elinext.com/blog/building-spring-boot-application-with-rabbitmq/](https://www.elinext.com/blog/building-spring-boot-application-with-rabbitmq/)  
10. RabbitMQ In Event-Driven Communication Between Microservices | by Darshana Dinushal, accessed on February 3, 2026, [https://darshanadinushal.medium.com/rabbitmq-in-event-driven-communication-between-microservices-df3a9c38ece9](https://darshanadinushal.medium.com/rabbitmq-in-event-driven-communication-between-microservices-df3a9c38ece9)  
11. Microservices Communication with RabbitMQ \- GeeksforGeeks, accessed on February 3, 2026, [https://www.geeksforgeeks.org/advance-java/microservices-communication-with-rabbitmq/](https://www.geeksforgeeks.org/advance-java/microservices-communication-with-rabbitmq/)  
12. Making MongoDB on Google Cloud even more flexible with Pay-Go, accessed on February 3, 2026, [https://cloud.google.com/blog/products/databases/making-mongodb-on-google-cloud-even-more-flexible-with-pay-go](https://cloud.google.com/blog/products/databases/making-mongodb-on-google-cloud-even-more-flexible-with-pay-go)  
13. Best Practices for Data Modeling in MongoDB \- Database Manual, accessed on February 3, 2026, [https://www.mongodb.com/docs/manual/data-modeling/best-practices/](https://www.mongodb.com/docs/manual/data-modeling/best-practices/)  
14. Build a Local RAG Implementation with MongoDB Vector Search \- Atlas, accessed on February 3, 2026, [https://www.mongodb.com/docs/atlas/atlas-vector-search/tutorials/local-rag/](https://www.mongodb.com/docs/atlas/atlas-vector-search/tutorials/local-rag/)  
15. How to Use MongoDB as a Vector Store for Retrieval-Augmented Generation (RAG), accessed on February 3, 2026, [https://medium.com/@mohantaastha/how-to-use-mongodb-as-a-vector-store-for-retrieval-augmented-generation-rag-e5032324c92a](https://medium.com/@mohantaastha/how-to-use-mongodb-as-a-vector-store-for-retrieval-augmented-generation-rag-e5032324c92a)  
16. 6 Rules Of Thumb For MongoDB Schema Design, accessed on February 3, 2026, [https://www.mongodb.com/company/blog/mongodb/6-rules-of-thumb-for-mongodb-schema-design](https://www.mongodb.com/company/blog/mongodb/6-rules-of-thumb-for-mongodb-schema-design)  
17. MongoDB relationships: embed or reference? \- Stack Overflow, accessed on February 3, 2026, [https://stackoverflow.com/questions/5373198/mongodb-relationships-embed-or-reference](https://stackoverflow.com/questions/5373198/mongodb-relationships-embed-or-reference)  
18. Reference Data in Your MongoDB Schema \- Database Manual, accessed on February 3, 2026, [https://www.mongodb.com/docs/manual/data-modeling/referencing/](https://www.mongodb.com/docs/manual/data-modeling/referencing/)  
19. How to design Schema for usage history and billing \- Working with Data \- MongoDB, accessed on February 3, 2026, [https://www.mongodb.com/community/forums/t/how-to-design-schema-for-usage-history-and-billing/199994](https://www.mongodb.com/community/forums/t/how-to-design-schema-for-usage-history-and-billing/199994)  
20. MongoDB Sharding, accessed on February 3, 2026, [https://www.mongodb.com/resources/products/capabilities/sharding](https://www.mongodb.com/resources/products/capabilities/sharding)  
21. Data Partitioning with Chunks \- Database Manual \- MongoDB Docs, accessed on February 3, 2026, [https://www.mongodb.com/docs/manual/core/sharding-data-partitioning/](https://www.mongodb.com/docs/manual/core/sharding-data-partitioning/)  
22. Sharding \- Database Manual \- MongoDB Docs, accessed on February 3, 2026, [https://www.mongodb.com/docs/manual/sharding/](https://www.mongodb.com/docs/manual/sharding/)  
23. Face recognition using python/tensorflow in Spring Boot app \- Stack Overflow, accessed on February 3, 2026, [https://stackoverflow.com/questions/59217719/face-recognition-using-python-tensorflow-in-spring-boot-app](https://stackoverflow.com/questions/59217719/face-recognition-using-python-tensorflow-in-spring-boot-app)  
24. What's the Best Face Detector?. Comparing Dlib, OpenCV, MTCNN, and… | by Amos Stailey-Young | Python's Gurus | Medium, accessed on February 3, 2026, [https://medium.com/pythons-gurus/what-is-the-best-face-detector-ab650d8c1225](https://medium.com/pythons-gurus/what-is-the-best-face-detector-ab650d8c1225)  
25. Best Face Landmark Detection models : r/computervision \- Reddit, accessed on February 3, 2026, [https://www.reddit.com/r/computervision/comments/vvu653/best\_face\_landmark\_detection\_models/](https://www.reddit.com/r/computervision/comments/vvu653/best_face_landmark_detection_models/)  
26. 2\. Face Recognition Concepts & Architecture | by Rajneesh Gupta | Dec, 2025 | Medium, accessed on February 3, 2026, [https://medium.com/@gupta.rajneesh2010/2-face-recognition-concepts-architecture-5eee7ca3f70f](https://medium.com/@gupta.rajneesh2010/2-face-recognition-concepts-architecture-5eee7ca3f70f)  
27. Master Facial Recognition with DeepFace in Python \- Viso Suite, accessed on February 3, 2026, [https://viso.ai/computer-vision/deepface/](https://viso.ai/computer-vision/deepface/)  
28. Upgrading Face Recognition: From DeepFace to InsightFace — Performance, Quality, and Integration \- DEV Community, accessed on February 3, 2026, [https://dev.to/wintrover/upgrading-face-recognition-from-deepface-to-insightface-performance-quality-and-integration-5b7f](https://dev.to/wintrover/upgrading-face-recognition-from-deepface-to-insightface-performance-quality-and-integration-5b7f)  
29. Face Recognition Upgrade: DeepFace to InsightFace \- Kite Metric, accessed on February 3, 2026, [https://kitemetric.com/blogs/upgrading-face-recognition-from-deepface-to-insightface](https://kitemetric.com/blogs/upgrading-face-recognition-from-deepface-to-insightface)  
30. Facial Recognition System Development — Architecture, Features & Cost Guide (2025), accessed on February 3, 2026, [https://www.inexture.com/facial-recognition-system-development-cost-details/](https://www.inexture.com/facial-recognition-system-development-cost-details/)  
31. Run Vector Search Queries \- Atlas \- MongoDB Docs, accessed on February 3, 2026, [https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-stage/](https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-stage/)  
32. Understanding storage cost \- Snowflake Documentation, accessed on February 3, 2026, [https://docs.snowflake.com/en/user-guide/cost-understanding-data-storage](https://docs.snowflake.com/en/user-guide/cost-understanding-data-storage)  
33. How to Accurately Calculate the Cost of Cloud Storage \- Backblaze, accessed on February 3, 2026, [https://www.backblaze.com/blog/calculate-cost-cloud-storage/](https://www.backblaze.com/blog/calculate-cost-cloud-storage/)  
34. Spring Batch Tutorial for Beginners | Spring Boot | Batch Architecture \- YouTube, accessed on February 3, 2026, [https://www.youtube.com/watch?v=jilqHdnoDRM](https://www.youtube.com/watch?v=jilqHdnoDRM)  
35. Spring batch to process huge data \- Stack Overflow, accessed on February 3, 2026, [https://stackoverflow.com/questions/66150694/spring-batch-to-process-huge-data](https://stackoverflow.com/questions/66150694/spring-batch-to-process-huge-data)  
36. Spring Batch on Kubernetes: Efficient batch processing at scale, accessed on February 3, 2026, [https://spring.io/blog/2021/01/27/spring-batch-on-kubernetes-efficient-batch-processing-at-scale/](https://spring.io/blog/2021/01/27/spring-batch-on-kubernetes-efficient-batch-processing-at-scale/)  
37. Managing Shared Cloud Costs \- The FinOps Foundation, accessed on February 3, 2026, [https://www.finops.org/wg/identifying-shared-costs/](https://www.finops.org/wg/identifying-shared-costs/)  
38. Field Notes: Building a Shared Account Structure Using AWS Organizations, accessed on February 3, 2026, [https://aws.amazon.com/blogs/architecture/field-notes-building-a-shared-account-structure-using-aws-organizations/](https://aws.amazon.com/blogs/architecture/field-notes-building-a-shared-account-structure-using-aws-organizations/)  
39. How Spring Boot Implements File Streaming for Large Files \- Medium, accessed on February 3, 2026, [https://medium.com/@AlexanderObregon/how-spring-boot-implements-file-streaming-for-large-files-fcce7e2af662](https://medium.com/@AlexanderObregon/how-spring-boot-implements-file-streaming-for-large-files-fcce7e2af662)  
40. Scalable, Secure and faster user file management with Spring boot, Angular and S3, accessed on February 3, 2026, [https://medium.com/@sachinrd199/scalable-secure-and-faster-user-file-management-with-spring-boot-angular-and-s3-dcccb6e9ffd3](https://medium.com/@sachinrd199/scalable-secure-and-faster-user-file-management-with-spring-boot-angular-and-s3-dcccb6e9ffd3)  
41. Efficient S3 File Uploads: Speed & Large File Handling in Spring Boot \- DEV Community, accessed on February 3, 2026, [https://dev.to/adamthedeveloper/efficient-s3-file-uploads-speed-large-file-handling-in-spring-boot-gil](https://dev.to/adamthedeveloper/efficient-s3-file-uploads-speed-large-file-handling-in-spring-boot-gil)  
42. Work with Amazon S3 pre-signed URLs \- AWS SDK for Java 2.x, accessed on February 3, 2026, [https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-s3-presign.html](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-s3-presign.html)  
43. Uploading large files to AWS S3 : r/SpringBoot \- Reddit, accessed on February 3, 2026, [https://www.reddit.com/r/SpringBoot/comments/1f8pf6i/uploading\_large\_files\_to\_aws\_s3/](https://www.reddit.com/r/SpringBoot/comments/1f8pf6i/uploading_large_files_to_aws_s3/)  
44. Are SignedURLs the right way to serve images? \- Supabase \- Reddit, accessed on February 3, 2026, [https://www.reddit.com/r/Supabase/comments/1h4472n/are\_signedurls\_the\_right\_way\_to\_serve\_images/](https://www.reddit.com/r/Supabase/comments/1h4472n/are_signedurls_the_right_way_to_serve_images/)  
45. How to cope with the performance of generating signed URLs for accessing private content via CloudFront? \- Stack Overflow, accessed on February 3, 2026, [https://stackoverflow.com/questions/31376763/how-to-cope-with-the-performance-of-generating-signed-urls-for-accessing-private](https://stackoverflow.com/questions/31376763/how-to-cope-with-the-performance-of-generating-signed-urls-for-accessing-private)  
46. Virtual Scrolling using Angular 7 CDK \- Oodles Technologies, accessed on February 3, 2026, [https://www.oodlestechnologies.com/blogs/virtual-scrolling-using-angular-7-cdk/](https://www.oodlestechnologies.com/blogs/virtual-scrolling-using-angular-7-cdk/)  
47. Handling Large Datasets in Angular: Pagination vs Infinite Scroll \- Medium, accessed on February 3, 2026, [https://medium.com/@geekieshpixel/handling-large-datasets-in-angular-pagination-vs-infinite-scroll-dc07dadeac0b](https://medium.com/@geekieshpixel/handling-large-datasets-in-angular-pagination-vs-infinite-scroll-dc07dadeac0b)  
48. Lazy Loading vs Pagination. Fight\! \- Realnet Cambridge, accessed on February 3, 2026, [https://www.realnet.co.uk/news/lazy-loading-vs-pagination-fight/](https://www.realnet.co.uk/news/lazy-loading-vs-pagination-fight/)  
49. Optimizing images with NgOptimizedImage \- Angular, accessed on February 3, 2026, [https://angular.dev/guide/image-optimization](https://angular.dev/guide/image-optimization)  
50. How I Optimized Images in Angular with NgOptimizedImage: A Deep Dive | by Sina Nasiri, accessed on February 3, 2026, [https://medium.com/@sinasiri/how-i-optimized-images-in-angular-with-ngoptimizedimage-a-deep-dive-0f2ba2abf0a4](https://medium.com/@sinasiri/how-i-optimized-images-in-angular-with-ngoptimizedimage-a-deep-dive-0f2ba2abf0a4)  
51. Angular Image Optimization: Lazy Loading and Beyond \- DEV Community, accessed on February 3, 2026, [https://dev.to/hardik\_b2d8f0bca/angular-image-optimization-lazy-loading-and-beyond-he7](https://dev.to/hardik_b2d8f0bca/angular-image-optimization-lazy-loading-and-beyond-he7)  
52. Getting Started | Spring Security and Angular, accessed on February 3, 2026, [https://spring.io/guides/tutorials/spring-security-and-angular-js/](https://spring.io/guides/tutorials/spring-security-and-angular-js/)  
53. How to load images with JWT Authorization in the header in Angular 16? \- Stack Overflow, accessed on February 3, 2026, [https://stackoverflow.com/questions/78029089/how-to-load-images-with-jwt-authorization-in-the-header-in-angular-16](https://stackoverflow.com/questions/78029089/how-to-load-images-with-jwt-authorization-in-the-header-in-angular-16)  
54. Architecture Patterns for SaaS Platforms: Billing, RBAC, and Onboarding | by Kishan Rank, accessed on February 3, 2026, [https://medium.com/appfoster/architecture-patterns-for-saas-platforms-billing-rbac-and-onboarding-964ea071f571](https://medium.com/appfoster/architecture-patterns-for-saas-platforms-billing-rbac-and-onboarding-964ea071f571)

[image1]: <data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAsAAAAYCAYAAAAs7gcTAAAA2klEQVR4Xt3RMQtBURQH8COEspCNic2iyKAkPoDdYpZSBgODxexz2DCaDGaLyWKilPIBLBb/0z3XPe8Nr97Iv37Lvf/ePfc+ot9NCmawh4PIqX1PImQ2t7AWMU9DJVSZw+UzTEVg6vCEpgjMAK6QFzxaGcbkuyxvrMjMmhBDGMEGJq7q5p1LgVWhDW/ofZtk5n3BBTqCT+MkbcmG571BH05iB2ld4uh5+W35FHaHGpSgZcv6fTld8YAimfkbskcVMl+xCwVxhCUsSP3NKGTIXcgmDln/eqjyX+cDf2AlLaDkaK0AAAAASUVORK5CYII=>

[image2]: <data:image/png;base64,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>

[image3]: <data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABMAAAAYCAYAAAAYl8YPAAAAdElEQVR4XmNgGAWjYHACdSB2hWKKgTQQd0AxC5ocWSAHim3RJcgB/FA8H4hN0eRIBjgNE2CA+H8WGXgFEH8B4n4g5mSgEHgC8RQgFkaXIAWYQXEnELOiyZEEQN6ZCMXiaHIkA2MgLodiigHIW2xQPAoGEwAAnIQT1uH8wg8AAAAASUVORK5CYII=>