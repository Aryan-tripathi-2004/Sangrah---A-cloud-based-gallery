# **Architecting Sangrah: A Scalable Pay-as-You-Use Cloud Storage Platform**

## **1\. Executive Summary**

The "Sangrah" project represents a shift from static, subscription-based storage to a dynamic "Pay-as-You-Use" utility model. By aligning infrastructure costs directly with user consumption, Sangrah offers a flexible alternative to fixed-tier cloud storage. This report provides a comprehensive technical blueprint for building Sangrah using a Spring Boot microservices ecosystem, a MongoDB persistence layer, and an Angular frontend.

The architecture prioritizes **decoupling** and **scalability**. High-latency tasks (AI face recognition) are isolated from high-throughput tasks (media streaming) using asynchronous event-driven communication via RabbitMQ. The system leverages AWS S3 for object storage with a "Direct-to-Cloud" upload pattern to minimize server load.

## **2\. Microservices Architecture & API Specification**

The backend is composed of six autonomous microservices, orchestrated by Spring Cloud Gateway and Eureka Service Discovery.

### **2.1 Service Decomposition Strategy**

| Microservice | Port | Tech Stack | Responsibility |
| :---- | :---- | :---- | :---- |
| **API Gateway** | 8080 | Spring Cloud Gateway | Entry point, Routing, Rate Limiting, SSL Termination. |
| **Auth Service** | 8081 | Spring Security, JWT | Identity Management, Token Issuance, Role Management. |
| **Media Service** | 8082 | Spring Boot, AWS SDK | File Metadata, S3 Presigned URLs, Folder Management. |
| **Event Service** | 8083 | Spring Boot | Event Logic, Collaboration Permissions, Shared Galleries. |
| **Billing Service** | 8084 | Spring Batch | Metering, Daily Usage Calculation, Invoicing. |
| **AI Service** | 5000 | Python (FastAPI), PyTorch | Face Detection (RetinaFace), Embedding Generation. |

### **2.2 API Specifications & Controller Design**

#### **A. Auth Service (AuthController)**

Responsible for issuing JWTs and managing user lifecycle.

* **POST** /api/v1/auth/register  
  * **Input:** RegisterRequestDTO { email, password, fullName }  
  * **Output:** AuthResponseDTO { userId, accessToken, refreshToken }  
  * **Reason:** Creates user identity and initializes billing wallet.  
* **POST** /api/v1/auth/login  
  * **Input:** LoginRequestDTO { email, password }  
  * **Output:** AuthResponseDTO { token, expiresIn, roles }  
  * **Reason:** Authenticates user and returns JWT for stateless session management.

#### **B. Media Service (MediaController, UploadController)**

Handles the metadata and S3 interactions.

* **POST** /api/v1/media/presigned-upload  
  * **Input:** UploadRequestDTO { fileName, fileType, sizeBytes, folderId }  
  * **Output:** PresignedUrlDTO { uploadUrl, s3Key, expiresAt }  
  * **Reason:** Generates a secure, temporary S3 URL so the frontend can upload directly to AWS, bypassing the backend server.1  
* **POST** /api/v1/media/confirm  
  * **Input:** MediaConfirmationDTO { s3Key, mimeType, metadata }  
  * **Output:** MediaResponseDTO { mediaId, status }  
  * **Reason:** Confirms the file reached S3 and saves metadata to MongoDB. Triggers media.uploaded event.  
* **GET** /api/v1/media/gallery  
  * **Input:** Pageable (page, size), filter (date, folderId)  
  * **Output:** PagedResponse\<MediaDTO\>  
  * **Reason:** Fetches paginated lists of images for the timeline view.

#### **C. Event Service (EventController)**

Manages shared spaces and permissions.

* **POST** /api/v1/events  
  * **Input:** EventCreateDTO { name, type (PUBLIC/PRIVATE), description }  
  * **Output:** EventDTO { eventId, inviteCode, qrCodeUrl }  
  * **Reason:** Creates a new event container.  
* **POST** /api/v1/events/{eventId}/join  
  * **Input:** JoinRequestDTO { userId, accessCode }  
  * **Output:** MembershipStatusDTO { role (VIEWER/CONTRIBUTOR) }  
  * **Reason:** Adds a user to an event's access control list (ACL).

#### **D. AI Service (Python \- FaceController)**

Exposes REST endpoints for the Java backend to consume, or listens to RabbitMQ.

* **POST** /internal/ai/detect-faces  
  * **Input:** FaceDetectionRequest { imageUrl (S3 URL) }  
  * **Output:** FaceDetectionResponse { faceCount, embeddings \[Array\], boundingBoxes }  
  * **Reason:** Processes an image to extract 512-dimensional vector embeddings for recognition.3

#### **E. Billing Service (BillingController)**

Provides transparency on costs.

* **GET** /api/v1/billing/current-usage  
  * **Input:** userId  
  * **Output:** UsageSummaryDTO { totalStorageBytes, currentMonthCost, projectedCost }  
  * **Reason:** Real-time dashboard display of user's "Pay-as-You-Use" status.

## ---

**3\. Data Persistence Layer: Schema and Relations**

The database is **MongoDB** (v6.0+) to support flexible metadata and native Vector Search.

### **3.1 Entity-Relationship (ER) Diagram**

The following Mermaid diagram illustrates the logical relationships between collections. Note that in MongoDB, "relationships" are often handled via references (ObjectId) or embedding.

Code snippet

erDiagram  
    User |

|--o{ Media : "owns"  
    User |

|--o{ Event : "creates"  
    User |

|--o{ UsageBucket : "has monthly"  
    User |

|--o{ BillingInvoice : "receives"  
      
    Event |

|--|{ EventMember : "contains"  
    Event |

|--o{ Media : "contains reference to"  
      
    Media |

|--o{ FaceTag : "contains detected"  
    Media }|--|

| Folder : "lives in"  
      
    User {  
        ObjectId \_id  
        String email  
        String passwordHash  
        List roles  
    }

    Media {  
        ObjectId \_id  
        ObjectId ownerId  
        String s3Key  
        String type  
        Date uploadDate  
        Double sizeBytes  
        Object metadata  
    }

    Event {  
        ObjectId \_id  
        String name  
        String type  
        List members  
        Date createdAt  
    }

    UsageBucket {  
        ObjectId \_id  
        ObjectId userId  
        String monthYear  
        List dailySnapshots  
    }

### **3.2 Collection Definitions**

#### **1\. users Collection**

Stores authentication and profile data.

JSON

{  
  "\_id": ObjectId("..."),  
  "email": "user@example.com",  
  "roles":,  
  "storageLimit": 10737418240, // 10GB in bytes  
  "faces":  
}

#### **2\. media Collection**

Stores file metadata. Polymorphic for Images and Videos.

JSON

{  
  "\_id": ObjectId("..."),  
  "ownerId": ObjectId("ref\_user"),  
  "folderId": ObjectId("ref\_folder"),  
  "s3Key": "users/123/2023/vacation.jpg",  
  "size": 5242880, // 5MB  
  "mimeType": "image/jpeg",  
  "uploadDate": ISODate("2023-10-27T10:00:00Z"),  
  "eventIds": \[ObjectId("ref\_event\_1")\], // Linked events  
  "faceEmbeddings":,   
    \[0.01, 0.99,... \-0.32\]  
}

* **Indexes:**  
  * { ownerId: 1, uploadDate: \-1 }: For timeline queries.  
  * { eventIds: 1 }: For retrieving event photos.  
  * **Vector Index**: On faceEmbeddings using Cosine Similarity.5

#### **3\. billing\_usage (Bucket Pattern)**

Optimized for the "Pay-as-You-Use" calculation. One document per user per month.

JSON

{  
  "\_id": ObjectId("..."),  
  "userId": ObjectId("ref\_user"),  
  "billingPeriod": "2023-11",  
  "dailyUsage":  
}

## ---

**4\. System Diagrams**

### **4.1 Sequence Diagram: Media Upload Flow**

This diagram details the "Direct-to-S3" pattern and asynchronous AI processing.

Code snippet

sequenceDiagram  
    participant Client as Angular App  
    participant API as Media Service  
    participant S3 as AWS S3  
    participant DB as MongoDB  
    participant MQ as RabbitMQ  
    participant AI as AI Service

    Client-\>\>API: POST /presigned-upload (filename, size)  
    API-\>\>DB: Check Storage Quota  
    API--\>\>Client: Return Presigned URL  
      
    Client-\>\>S3: PUT /upload (Binary Data)  
    S3--\>\>Client: 200 OK  
      
    Client-\>\>API: POST /confirm (s3Key)  
    API-\>\>DB: Save Metadata (State: PROCESSING)  
    API-\>\>MQ: Publish "media.uploaded" event  
    API--\>\>Client: 200 OK (Upload Complete)  
      
    MQ-\>\>AI: Consume event  
    AI-\>\>S3: Download Image  
    AI-\>\>AI: Run RetinaFace \+ ArcFace  
    AI-\>\>DB: Update Media (Add Embeddings)  
    AI-\>\>DB: Update Status (State: READY)

### **4.2 Class Diagram (Backend Core)**

A simplified view of the Spring Boot application structure showing the service layer abstraction.

Code snippet

classDiagram  
    class MediaController {  
        \+getPresignedUrl()  
        \+confirmUpload()  
        \+getGallery()  
    }  
      
    class MediaService {  
        \+generateS3Url()  
        \+saveMetadata()  
        \+linkToEvent()  
    }  
      
    class StorageProvider {  
        \<\<Interface\>\>  
        \+generatePresignedUrl()  
        \+deleteFile()  
    }  
      
    class S3StorageProvider {  
        \+generatePresignedUrl()  
    }  
      
    class MediaRepository {  
        \+findByOwnerId()  
        \+findByEventId()  
    }  
      
    MediaController \--\> MediaService  
    MediaService \--\> StorageProvider  
    StorageProvider \<|-- S3StorageProvider  
    MediaService \--\> MediaRepository

## ---

**5\. Technical Deep Dives**

### **5.1 Pay-as-You-Use Billing Algorithm**

The Billing Service uses **Spring Batch** to calculate costs precisely.

* **Formula:** Cost \= ![][image1]  
* **Execution:**  
  1. A nightly job reads the billing\_usage bucket.  
  2. It calculates the "Byte-Seconds" for the day. If a 1GB file was uploaded at noon, it counts as 0.5 GB-Days for that specific day.  
  3. This granular approach handles the "upload then delete" scenario fairly.6

### **5.2 Frontend Optimization (Angular)**

* **Virtual Scrolling:** The cdk-virtual-scroll-viewport is used for the gallery. It renders only the DOM elements currently in view (e.g., 20 items) while the user scrolls through 5,000 items.8  
* **Hybrid Pagination:** The frontend requests data in pages (e.g., page 1, page 2\) from the API. These pages are appended to the VirtualScrollDataSource. This combines the UX of infinite scroll with the network efficiency of pagination.  
* **Security:** Images are loaded using \<img\> tags. Since standard image tags cannot send Authorization headers, the backend provides **CloudFront Signed URLs** or S3 Presigned URLs in the JSON response. The frontend binds these directly to the src attribute.1

### **5.3 Face Recognition Implementation**

The AI Service uses a **Hybrid Stack**:

* **Java (Spring Boot):** Orchestrates the workflow. It does not load heavy ML models.  
* **Python (FastAPI):** Hosts the **InsightFace** library. Python is chosen for its superior support for PyTorch/TensorFlow.  
* **Vector Search:** When the Python service returns a 512-float vector, the Java service saves it to MongoDB. Searching "Find my face" triggers a $vectorSearch pipeline in MongoDB Atlas, which utilizes the HNSW algorithm to find nearest neighbors efficiently.5

## **6\. Security & Compliance**

* **JWT Authentication:** Stateless tokens carry user roles (ROLE\_USER, ROLE\_ADMIN).  
* **Presigned URLs:** Ensure the backend never handles heavy file binary streams, preventing Denial of Service (DoS) attacks via large uploads.  
* **Data Isolation:** Multi-tenant logical isolation ensures users can only query media where ownerId matches their ID or sharedWith contains their ID.

This architectural report provides the necessary "minute details," diagrams, and schema definitions for the development team to commence implementation of the Sangrah platform.

#### **Works cited**

1. Scalable, Secure and faster user file management with Spring boot, Angular and S3, accessed on February 3, 2026, [https://medium.com/@sachinrd199/scalable-secure-and-faster-user-file-management-with-spring-boot-angular-and-s3-dcccb6e9ffd3](https://medium.com/@sachinrd199/scalable-secure-and-faster-user-file-management-with-spring-boot-angular-and-s3-dcccb6e9ffd3)  
2. Uploading large files to AWS S3 : r/SpringBoot \- Reddit, accessed on February 3, 2026, [https://www.reddit.com/r/SpringBoot/comments/1f8pf6i/uploading\_large\_files\_to\_aws\_s3/](https://www.reddit.com/r/SpringBoot/comments/1f8pf6i/uploading_large_files_to_aws_s3/)  
3. 1\. Introduction to Face Recognition | by Rajneesh Gupta | Dec, 2025 | Medium, accessed on February 3, 2026, [https://medium.com/@gupta.rajneesh2010/868fb0d0476f](https://medium.com/@gupta.rajneesh2010/868fb0d0476f)  
4. Face recognition using python/tensorflow in Spring Boot app \- Stack Overflow, accessed on February 3, 2026, [https://stackoverflow.com/questions/59217719/face-recognition-using-python-tensorflow-in-spring-boot-app](https://stackoverflow.com/questions/59217719/face-recognition-using-python-tensorflow-in-spring-boot-app)  
5. Build a Local RAG Implementation with MongoDB Vector Search \- Atlas, accessed on February 3, 2026, [https://www.mongodb.com/docs/atlas/atlas-vector-search/tutorials/local-rag/](https://www.mongodb.com/docs/atlas/atlas-vector-search/tutorials/local-rag/)  
6. Understanding storage cost \- Snowflake Documentation, accessed on February 3, 2026, [https://docs.snowflake.com/en/user-guide/cost-understanding-data-storage](https://docs.snowflake.com/en/user-guide/cost-understanding-data-storage)  
7. How to Build a Flexible, Usage-Based Billing System \- WebMob Technologies, accessed on February 3, 2026, [https://webmobtech.com/blog/build-metered-usage-based-billing-system/](https://webmobtech.com/blog/build-metered-usage-based-billing-system/)  
8. Angular virtual scroll: append new items when reaching the end of scroll \- Stack Overflow, accessed on February 3, 2026, [https://stackoverflow.com/questions/54770671/angular-virtual-scroll-append-new-items-when-reaching-the-end-of-scroll](https://stackoverflow.com/questions/54770671/angular-virtual-scroll-append-new-items-when-reaching-the-end-of-scroll)  
9. Angular Components \- Virtual scroll with a custom data source \- StackBlitz, accessed on February 3, 2026, [https://stackblitz.com/edit/angular-gchlgl?file=src%2Fstyles.scss,src%2Fapp%2Fcdk-virtual-scroll-data-source-example.css,src%2Fapp%2Fcdk-virtual-scroll-data-source-example.html](https://stackblitz.com/edit/angular-gchlgl?file=src/styles.scss,src/app/cdk-virtual-scroll-data-source-example.css,src/app/cdk-virtual-scroll-data-source-example.html)  
10. Sending images to angular client in spring boot rest api? \- Stack Overflow, accessed on February 3, 2026, [https://stackoverflow.com/questions/54453672/sending-images-to-angular-client-in-spring-boot-rest-api](https://stackoverflow.com/questions/54453672/sending-images-to-angular-client-in-spring-boot-rest-api)  
11. Upgrading Face Recognition: From DeepFace to InsightFace — Performance, Quality, and Integration \- DEV Community, accessed on February 3, 2026, [https://dev.to/wintrover/upgrading-face-recognition-from-deepface-to-insightface-performance-quality-and-integration-5b7f](https://dev.to/wintrover/upgrading-face-recognition-from-deepface-to-insightface-performance-quality-and-integration-5b7f)

[image1]: <data:image/png;base64,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>