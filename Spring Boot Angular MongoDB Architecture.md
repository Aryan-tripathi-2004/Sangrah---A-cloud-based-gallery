# **Technical Architecture Specification: Distributed Event Management and Media Gallery Platform**

## **1\. Executive Overview and Architectural Strategy**

### **1.1 Introduction**

This document serves as the comprehensive technical specification for the development of a scalable, high-performance Event Management and Media Gallery platform. The system is designed to facilitate complex user interactions centered around media sharing, event organization, and granular social collaboration. Unlike static content repositories, this platform demands dynamic authorization, real-time feedback mechanisms, and sophisticated media handling capabilities. The architecture adopts a modern full-stack approach, integrating **Spring Boot** for a robust, secure backend API, **Angular** for a responsive, component-driven frontend, and **MongoDB** as the persistence layer to handle unstructured media metadata and variable event structures.

The selection of this technology stack is driven by specific non-functional requirements: high availability, horizontal scalability, and developer productivity. Spring Boot provides an opinionated yet flexible framework for building enterprise-grade Java applications, offering mature security modules and seamless database integration. Angular, with its strict typing via TypeScript and modular architecture, ensures maintainability for the complex client-side logic required for timeline visualizations and media manipulation. MongoDB is chosen over traditional relational databases (RDBMS) to accommodate the polymorphic nature of media metadata, the hierarchical structure of event permissions, and the high-write volume of event analytics.1

### **1.2 System Scope and Core Capabilities**

The platform functions as a centralized hub where users can manage their digital lifecycles through events. The core capabilities are categorized into three distinct domains: Identity and Access Management (IAM), Event Lifecycle Management, and Media Content Delivery.

* **Identity and Access Management:** The system enforces a rigorous Role-Based Access Control (RBAC) model, distinguishing between ADMIN, CREATOR, and VIEWER roles. Beyond these static roles, the architecture supports dynamic, document-level permissions, allowing an event creator to assign specific privileges (e.g., "upload capability") to collaborators for a single event context.3  
* **Event Lifecycle Management:** Users can create public, private, or protected events. These entities act as containers for media, metadata, and social interactions. Unique shareable links and QR codes bridge the physical and digital worlds, allowing rapid access for attendees.  
* **Media Content Delivery:** The platform handles the ingestion, processing, and distribution of images and videos. A critical architectural distinction is the "Reference vs. Copy" logic, which dictates how media ownership transfers or propagates between personal galleries and shared events, impacting storage quotas and data integrity.

### **1.3 Architectural Pattern: The Modular Monolith**

While microservices architectures offer strict isolation, they introduce significant operational complexity regarding distributed transactions and network latency. For this platform, a **Modular Monolith** architecture is mandated. This approach structures the Spring Boot backend into distinct, loosely coupled feature modules (Authentication, User Management, Media Service, Event Service, Analytics) deployed as a single artifact.

This architectural choice simplifies the deployment pipeline and allows for shared in-memory transactions where necessary, yet enforces strict boundary contexts. If specific modules (e.g., Image Processing) require independent scaling in the future, the modular boundaries allow them to be extracted into microservices with minimal refactoring. The communication between these modules is managed via defined Service Interfaces, preventing spaghetti code dependencies and ensuring that the Data Transfer Objects (DTOs) act as the strict contract for data exchange.

### **1.4 Technology Stack Justification**

| Component | Technology Selection | Architectural Justification |
| :---- | :---- | :---- |
| **Backend Framework** | Spring Boot 3.x (Java 17+) | Provides the "Service Layer" pattern for business logic encapsulation.4 Integrates natively with Spring Data MongoDB and Spring Security for robust protection. |
| **Frontend Framework** | Angular (Latest) | Offers a structured "Folder-by-Feature" organization 5 and powerful RxJS streams for handling asynchronous event updates and infinite scrolling.6 |
| **Database** | MongoDB 7.0+ | The document model eliminates the need for complex joins for hierarchical data (e.g., embedded collaborators) and supports Time Series collections for efficient analytics.7 |
| **Security** | Spring Security \+ JWT | Facilitates stateless authentication suitable for Single Page Applications (SPAs). Enables method-level security (@PreAuthorize) for granular control.8 |
| **File Handling** | Spring Multipart (Multer equiv.) | Replaces Node.js "Multer" with Java's native Servlet 3.0 Multipart API for streaming file uploads. Apache Tika is added for deep packet inspection of MIME types.9 |
| **Analytics Engine** | MongoDB Aggregation Framework | Offloads heavy computation (grouping views by day/month) to the database engine, minimizing data transfer overhead.10 |

## ---

**2\. Comprehensive Data Modeling and MongoDB Schema Design**

### **2.1 Design Philosophy: Access Patterns First**

In Relational Database Management Systems (RDBMS), schema design typically follows normalization rules (3NF) to reduce redundancy. In MongoDB, the primary driver for schema design is the **Application Access Pattern**.1 We analyze how data is read and written to determine when to embed data and when to reference it.

For this platform, the read-to-write ratio is expected to be high (many viewers, fewer uploaders). Therefore, the schema optimizes for read performance by pre-joining frequently accessed data (embedding) while keeping write-heavy or unbounded lists in separate collections (referencing) to avoid the 16MB document size limit.11

### **2.2 Collection Specifications**

#### **2.2.1 Users Collection**

The Users collection is the root of identity. It utilizes the **Subset Pattern** to manage profile data. Frequently accessed fields (displayName, avatar) are kept in the main document to facilitate rapid authentication checks, while rarely accessed data (login history, audit logs) are offloaded to a separate collection or bucket if they grow too large.

**Schema Definition:**

JSON

{  
  "\_id": "ObjectId",  
  "username": "String (Unique, Indexed)",  
  "email": "String (Unique, Indexed)",  
  "password": "String (BCrypt Encoded)",  
  "roles":,  
  "status": "ACTIVE", // ACTIVE, PENDING\_VERIFICATION, SUSPENDED  
  "storage": {  
    "quotaBytes": 5368709120, // 5GB limit  
    "usedBytes": 104857600  
  },  
  "profile": {  
    "firstName": "String",  
    "lastName": "String",  
    "avatarUrl": "String"  
  },  
  "verificationToken": "String (Indexed)",  
  "verificationExpires": "ISODate"   
}

**Strategic Insight: TTL Index for User Verification** To handle the requirement of "Scheduled cleanup tasks" for unverified users, we employ a MongoDB **TTL (Time-To-Live) Index** on the verificationExpires field.12

* **Mechanism:** When a user registers, the verificationExpires is set to NOW() \+ 24 Hours.  
* **Automation:** A background thread in the mongod process scans this index. If the document is still present (meaning the user hasn't verified and the field hasn't been unset) after the time expires, MongoDB atomically deletes the document.  
* **Advantage:** This removes the need for a complex Spring Boot @Scheduled task that polls the database, reducing application CPU load and network traffic.13

#### **2.2.2 Events Collection**

The Events collection represents the core aggregation point. It uses the **Extended Reference Pattern** to handle collaborators.

**Schema Definition:**

JSON

{  
  "\_id": "ObjectId",  
  "slug": "String (Unique, Indexed)", // For shareable links e.g., platform.com/e/summer-gala  
  "title": "String",  
  "description": "String",  
  "type": "PROTECTED", // PUBLIC, PROTECTED, PRIVATE  
  "accessCode": "String (Hashed, Optional)",  
  "creator": {  
    "userId": "ObjectId",  
    "displayName": "String", // Extended Reference: Avoids JOIN on listing  
    "avatarUrl": "String"  
  },  
  "collaborators": // Document-level permissions  
    }  
  \],  
  "settings": {  
    "allowPublicUploads": true,  
    "requiresApproval": true,  
    "showTimeline": true  
  },  
  "stats": {  
    "mediaCount": 150,  
    "viewCount": 5000  
  },  
  "createdAt": "ISODate",  
  "updatedAt": "ISODate"  
}

**Design Decision: Extended Reference for Collaborators**

We embed the collaborators list directly in the Event document. This includes not just the userId, but also the displayName and permissions.

* **Justification:** When rendering the "Event Details" page, the frontend needs to show the list of collaborators. If we only stored userId, the application would need to perform a $lookup (JOIN) with the Users collection or fire ![][image1] additional queries to fetch names. Embedding optimizes this frequent read pattern.11  
* **Trade-off:** If a user changes their name, we must update all Event documents where they are a collaborator. Given that name changes are rare compared to event reads, this write amplification is acceptable.

#### **2.2.3 Media Collection**

This collection stores metadata for the files. The binary data is assumed to be stored in an object store (like AWS S3 or MinIO), not in GridFS, to keep the database lightweight. The schema uses a **Polymorphic Pattern** to handle differences between Images and Videos.

**Schema Definition:**

JSON

{  
  "\_id": "ObjectId",  
  "ownerId": "ObjectId (Indexed)",  
  "storageKey": "String (S3 Key)",  
  "url": "String (CDN URL)",  
  "mimeType": "String", // image/jpeg, video/mp4  
  "size": "Long",  
  "hash": "String (SHA-256)", // For deduplication  
  "type": "IMAGE",  
  "metadata": { // Polymorphic sub-document  
    "width": 1920,  
    "height": 1080,  
    "durationSec": 0, // Only for video  
    "exif": {  
      "capturedAt": "ISODate",  
      "device": "String"  
    }  
  },  
  "visibility": "PRIVATE",  
  "uploadDate": "ISODate (Indexed)",  
  "deletedAt": "ISODate (Optional)" // Soft delete support  
}

**Strategic Insight: Soft Delete with DBRef** To support the "Reference" feature where a user adds gallery media to an event, we must ensure that if the user "deletes" the media from their profile, it doesn't break the event. We implement a **Soft Delete** pattern.15

* **Mechanism:** Deletion sets deletedAt to the current timestamp.  
* **Querying:** Standard queries filter { deletedAt: { $exists: false } }.  
* **Event Integrity:** Events referencing this media can still resolve it via ID, but the UI can display a "Archived by Owner" tag.

#### **2.2.4 EventMedia Collection (The Join Entity)**

To avoid the "unbounded array" antipattern (where an Event document grows indefinitely as thousands of photos are added), we utilize a separate collection to link Media to Events.

**Schema Definition:**

JSON

{  
  "\_id": "ObjectId",  
  "eventId": "ObjectId (Indexed)",  
  "mediaId": "ObjectId (Indexed)",  
  "uploaderId": "ObjectId",  
  "status": "APPROVED", // PENDING, APPROVED, REJECTED  
  "addedAt": "ISODate",  
  "isCopy": "Boolean" // Logic flag: Option 1 vs Option 2  
}

* **isCopy=false (Option 1 \- Reference):** The mediaId points to the original Media document owned by the user. Deleting the original affects this link (handled via soft delete logic).  
* **isCopy=true (Option 2 \- Copy):** The system clones the Media document (and potentially the underlying file) creating a new Media entry. The EventMedia points to this *new* ID. The event owner pays the storage cost.1

#### **2.2.5 Analytics Collections: Hybrid Approach**

The requirement for "EventAnalytics" with "Daily/Monthly" views requires a careful choice between MongoDB's **Time Series Collections** and the **Bucket Pattern**.

**Analysis:**

* **Time Series Collection:** Best for high-velocity insertion of raw view events. MongoDB 5.0+ optimizes storage using columnar compression.16  
* **Bucket Pattern:** Best for pre-aggregated read performance. Querying a raw time series of 10 million views to get a "daily count" is computationally expensive for a real-time dashboard.18

**Decision: Hybrid Architecture**

1. **Ingestion (Time Series):** Raw view events are written to a Time Series collection RawEventViews.  
   JSON  
   {  
     "timestamp": "ISODate",  
     "metadata": { "eventId": "ObjectId", "browser": "Chrome" },  
     "action": "VIEW"  
   }

2. **Aggregation (Bucket Pattern):** A scheduled background process (or MongoDB Materialized View trigger) aggregates these into DailyEventStats.  
   JSON  
   {  
     "\_id": { "eventId": "ObjectId", "date": "2025-10-27" },  
     "totalViews": 1500,  
     "hourlyDistribution": \[12, 45, 100,...\], // Array of 24 integers  
     "uniqueVisitors": 800  
   }

This allows the dashboard to load instantly by reading a few dozen bucket documents rather than scanning millions of raw logs.19

## ---

**3\. Backend Engineering: Spring Boot Implementation Details**

### **3.1 Project Structure: Modular Monolith**

The backend is structured as a **Modular Monolith** using Maven or Gradle. This avoids the latency and transactional complexity of microservices while maintaining clean separation of concerns. The package structure is domain-driven.

com.enterprise.eventplatform

├── common

│ ├── config // Security, Mongo, Swagger, Async

│ ├── exception // Global Exception Handler (@ControllerAdvice)

│ ├── validation // Custom validators (MimeType)

│ └── util // JwtUtil, FileUtil

├── modules

│ ├── auth // AuthController, AuthService, UserDetails impl

│ ├── user // UserProfile, QuotaManagement

│ ├── media // StorageService, MediaMetadataService

│ ├── event // EventController, CollaborationService

│ └── analytics // EventStatsService, ReportingController

└── EventPlatformApplication.java

### **3.2 Security Architecture**

#### **3.2.1 Authentication (JWT Implementation)**

The system uses a stateless JWT (JSON Web Token) authentication mechanism.8

* **Token Generation:** Upon login, the AuthService generates a signed JWT containing the sub (userId), iat, exp, and roles.  
* **Filter Chain:** A JwtRequestFilter intercepts every request. It extracts the Authorization: Bearer \<token\> header, validates the signature using the configured secret key, and populates the SecurityContextHolder with a UsernamePasswordAuthenticationToken.  
* **Refresh Strategy:** To balance security and UX, short-lived Access Tokens (15 min) are paired with long-lived Refresh Tokens (7 days, stored in HTTP-only cookies).

#### **3.2.2 Authorization: The Custom Permission Evaluator**

Standard Spring Security roles (ROLE\_ADMIN) are insufficient for the "Private: Creator \+ Collaborators only" requirement. This requires **Instance-Level Access Control** (ACL). We implement a custom PermissionEvaluator.21

**Implementation Logic:**

1. Create a class DomainPermissionEvaluator implementing PermissionEvaluator.  
2. Override hasPermission(Authentication auth, Serializable targetId, String targetType, Object permission).  
3. Inside the method, inject EventRepository.  
4. Fetch the Event by targetId.  
5. Check if auth.getPrincipal() matches event.getCreatorId(). If yes, grant access.  
6. If not, iterate through event.getCollaborators(). Check if the user ID exists and if the permissions array contains the requested action (e.g., "UPLOAD").

**Usage in Controllers:**

Java

@PreAuthorize("hasPermission(\#eventId, 'Event', 'UPLOAD\_MEDIA')")  
@PostMapping("/{eventId}/upload")  
public ResponseEntity\<?\> uploadMedia(@PathVariable String eventId, @RequestParam("file") MultipartFile file) {  
    // Logic proceeds only if evaluator returns true  
}

### **3.3 File Handling: Multipart vs. Multer**

The user request references "Multer," which is a Node.js middleware. In Spring Boot, the equivalent is the **Servlet 3.0 Multipart API**. We rely on the MultipartFile interface.23

#### **3.3.1 Configuration**

Large video uploads require adjusting default Tomcat limits in application.properties:

Properties

spring.servlet.multipart.max-file-size\=1GB  
spring.servlet.multipart.max-request-size\=1GB  
spring.servlet.multipart.enabled\=true

#### **3.3.2 Security: MIME Type Validation**

Trusting the file extension is a critical security vulnerability. We employ **Apache Tika** for content detection.9

* **Process:** The MediaService receives the MultipartFile.  
* **Detection:** It reads the first few bytes (magic numbers) using Tika.detect(inputStream).  
* **Validation:** The detected MIME type is compared against a strict allowlist (image/jpeg, image/png, video/mp4). If the user uploads a .jpg that is actually a shell script, Tika will identify it as application/x-sh and the upload is rejected.26

### **3.4 Service Layer Patterns and Business Logic**

#### **3.4.1 The Service Layer Pattern**

We strictly adhere to the **Service Layer Pattern**.4 Controllers are "dumb"; they only handle HTTP protocol concerns (headers, status codes). All business logic resides in @Service classes.

* **Transactional Boundaries:** The @Transactional annotation is used on service methods. While MongoDB transactions (Replica Set required) are heavier than SQL, they are necessary for atomic operations like "Create Event AND Add Creator as Collaborator".28

#### **3.4.2 DTO Mapping**

We do not expose MongoDB @Document entities to the API. This prevents "Mass Assignment" vulnerabilities. We use **MapStruct** or pure Java getters/setters to map Event entities to EventResponseDTO and EventCreateRequestDTO.4

#### **3.4.3 Feature Logic: Reference vs. Copy**

This logic resides in MediaService.addMediaToEvent(String mediaId, String eventId, boolean isCopy).

* **Option 1 (Reference):**  
  * Validate mediaId exists and belongs to the user.  
  * Create EventMedia linking mediaId and eventId.  
  * Set isCopy \= false.  
* **Option 2 (Copy):**  
  * Fetch original Media metadata.  
  * **Storage Cloning:** Invoke StorageService.copyFile(sourceKey, destinationKey). This uses the underlying storage provider's efficient copy API (e.g., S3 CopyObject) to avoid network round-trips.  
  * **Metadata Cloning:** Create a new Media document with the new storageKey and set ownerId to the current user (or Event owner).  
  * **Quota Check:** Deduct the file size from the target user's storage quota.  
  * Create EventMedia linking the *new* mediaId.

### **3.5 Real-Time Notifications via Change Streams**

To implement notifications (e.g., "New photo added to your event"), we leverage **MongoDB Change Streams**.30

* **Mechanism:** A Spring bean ChangeStreamListener opens a cursor on the EventMedia collection.  
* **Filtering:** It listens only for insert operations.  
* **Processing:** When a new document appears:  
  1. Extract eventId.  
  2. Lookup the Event to find the creatorId and collaborators.  
  3. Generate a Notification object.  
  4. Dispatch this object via **Server-Sent Events (SSE)** endpoint /api/notifications/stream to the connected clients.32 This ensures the notification system is event-driven and decoupled from the upload controller.

## ---

**4\. Frontend Engineering: Angular Application Architecture**

### **4.1 Directory Structure and Modularity**

The Angular application follows a **Folder-by-Feature** structure to ensure scalability.5

src/app

├── core // Singletons: AuthService, Guards, Interceptors, Logger

├── shared // Components used everywhere: Loader, Alert, MasonryPipe

│ ├── components

│ ├── directives

│ └── pipes

├── features

│ ├── auth // Login, Register

│ ├── dashboard // Stats, Recent Activity

│ ├── events // Event List, Detail, Create

│ │ ├── components // Event-specific dumb components

│ │ └── pages // Routed smart components

│ ├── gallery // Personal Media Timeline

│ └── public // Public access views (no-auth required)

└── app-routing.module.ts

**Lazy Loading:** To optimize initial load time, all feature modules (EventsModule, GalleryModule) are lazy-loaded using the loadChildren syntax in the router configuration.

### **4.2 Gallery Feature: The Variable Height Timeline**

The requirement for "Daily/Monthly/Yearly" views with variable-height images (Masonry-style) presents a significant rendering performance challenge.

#### **4.2.1 Data Virtualization**

Loading 5,000 images into the DOM will crash the browser. We utilize the **Angular CDK (Component Dev Kit) Scrolling Module**.34 However, the standard cdk-virtual-scroll-viewport expects fixed item sizes.

* **Solution:** We implement a **Custom Virtual Scroll Strategy**.36 This strategy calculates the specific height of items (headers vs image rows) or uses the autosize directive from cdk-experimental/scrolling.38  
* **Data Structure flattening:** The backend returns grouped data (Year \-\> Month \-\> Images). The Angular service flattens this into a linear array of TimelineItem objects for the virtual scroller:  
  * Index 0: Type HEADER (Height 50px) \- "October 2023"  
  * Index 1: Type ROW (Height 200px) \- \[Img1, Img2, Img3, Img4\]  
  * Index 2: Type ROW (Height 200px) \- \[Img5, Img6...\]

#### **4.2.2 Masonry Layout Implementation**

To achieve the "Masonry" look (images packing tightly without vertical gaps) without heavy JavaScript libraries that cause layout thrashing, we use a **CSS Column** or **Flexbox Order** technique.40

* **The Pipe Approach:** We create a MasonryPipe 42 that transforms a list of images into ![][image1] columns (arrays).  
  TypeScript  
  // Input: \[Img1, Img2, Img3, Img4, Img5\]  
  // Output (3 Cols): \[\[Img1, Img4\], \[Img2, Img5\], \[Img3\]\]

* **Template:** We render 3 div columns side-by-side. Inside each column, we iterate over the distributed images. This allows natural vertical stacking purely via HTML structure, solving the variable height issue.

### **4.3 Event Management Logic**

#### **4.3.1 QR Code and Link Generation**

In the EventDetailComponent, we use a library like angularx-qrcode to generate QR codes on the fly.

* **Data:** The QR code encodes the URL: https://myapp.com/e/{eventSlug}.  
* **Deep Linking:** The router is configured to handle /e/:slug.  
  * **Public Event:** The backend API allows GET access. The component renders immediately.  
  * **Protected Event:** The backend returns 403\. The component redirects to a "Request Access" or "Enter Password" screen.

#### **4.3.2 Media Upload Manager**

The upload UI handles multiple files.

* **Logic:** It uses the HttpClient with reportProgress: true to show a progress bar.  
* **Authorization:** Before upload, the component checks canUpload() which inspects the local User object against the Event's collaborator list. This is a UI-only check; the real enforcement happens on the server.

## ---

**5\. Security Architecture and Access Control**

### **5.1 Authorization Layers**

Security is applied in layers ("Defense in Depth").

1. **Network Layer:** HTTPS is mandatory to protect JWTs during transit.  
2. **Application Layer (Route Guards):** Angular AuthGuard prevents navigation to /admin or /dashboard if no token is present.  
3. **Method Layer (Spring Security):** The primary enforcement point.  
   * **Endpoint:** @PreAuthorize("hasRole('ADMIN')") protects admin APIs.  
   * **Resource:** @PreAuthorize("hasPermission(\#id, 'Event', 'READ')") protects specific event access.

### **5.2 Secure File Uploads**

We implement "Spoof-Proof" validation.25

* **Double Extension Attack:** We reject files with double extensions (e.g., image.php.jpg).  
* **Content Type Sniffing:** We instruct the browser not to sniff content types by setting the X-Content-Type-Options: nosniff header on served media.  
* **Filename Sanitization:** All uploaded filenames are sanitized (replacing special chars) and prefixed with a UUID to prevent path traversal attacks (e.g., ../../etc/passwd).

## ---

**6\. Advanced Feature Implementation: Logic and Algorithms**

### **6.1 Analytics and Statistics (Profile Page)**

The Profile Page requires statistics: "Total Views", "Most Popular Event".

* **Implementation:** We rely on the **MongoDB Aggregation Pipeline**.10  
  JavaScript  
  // Aggregation to get total views across all user events  
  db.events.aggregate(\[  
      { $match: { "creator.userId": currentUserId } },  
      { $group: {  
          \_id: null,  
          totalViews: { $sum: "$stats.viewCount" },  
          totalMedia: { $sum: "$stats.mediaCount" }  
      }}  
  \])

  This query runs efficiently because stats are embedded in the Event document. If we had to count the EventViews collection every time, the profile page would be slow. This validates the decision to use the **Bucket Pattern** (pre-aggregating counts into the Event document) for read-heavy views.

### **6.2 Collaborative Permissions System**

When a Creator adds a Collaborator:

1. **Frontend:** User searches by email. Selects permissions (CheckBoxes: \[x\] Upload, \[ \] Delete).  
2. **Backend:** EventService validates the target user exists. It updates the Event document using $addToSet to add the new collaborator sub-document.  
3. **Consistency:** The system is "eventually consistent" regarding the collaborator's own dashboard. The next time the collaborator loads their "Shared with Me" list, a query { "collaborators.userId": myId } retrieves the event.

## ---

**7\. Operational Excellence and Infrastructure**

### **7.1 Logging and Auditing**

We implement **SLF4J** with **Logback**. Crucially, we use **MDC (Mapped Diagnostic Context)** to trace requests across the modular monolith.

* **Filter:** A LogFilter intercepts requests, extracts the requestId and userId (from JWT), and puts them into MDC.  
* **Output:** Every log message (log.info(...)) automatically includes \[User: 123\], enabling rapid debugging of concurrency issues in event uploads.

### **7.2 Testing Strategy**

* **Unit Tests (JUnit 5 \+ Mockito):** Focus on the complex PermissionEvaluator logic. We must mock the Authentication object and EventRepository to assert that users with specific permissions are granted access, while others are denied.  
* **Integration Tests:** Use @DataMongoTest to spin up an embedded MongoDB. Test the Aggregation Pipelines for Analytics to ensure they correctly group by Date/Month.43  
* **API Tests:** Use MockMvc to test the Controllers, ensuring that invalid JWTs return 401 and missing permissions return 403\.

### **7.3 Scheduled Maintenance**

We configure Spring's @EnableScheduling.

* **Task:** CleanupTask runs nightly (@Scheduled(cron \= "0 0 3 \* \* \*")).  
* **Job:**  
  1. Find Users where status: PENDING\_VERIFICATION AND verificationExpires \< NOW. Delete them.  
  2. Find Media where deletedAt \< (NOW \- 30 Days). Permanently delete the S3 object and the MongoDB metadata. This implements a "Trash" feature with auto-expulsion.

## **8\. Conclusion**

This architecture specification delivers a robust blueprint for the Distributed Event Management Platform. By carefully selecting **MongoDB** for its schema flexibility and **Spring Boot** for its secure, structured backend environment, we address the complex requirements of dynamic media handling and granular permissions. The **Angular** frontend, with its sophisticated virtual scrolling and masonry layouts, ensures a high-quality user experience. The integration of specific patterns—such as the Extended Reference pattern for events, the Bucket pattern for analytics, and the Service Layer pattern for business logic—ensures the system is not only functional but scalable and maintainable for the long term.

#### **Works cited**

1. Apply Design Patterns \- Database Manual \- MongoDB Docs, accessed on February 1, 2026, [https://www.mongodb.com/docs/manual/data-modeling/schema-design-process/apply-patterns/](https://www.mongodb.com/docs/manual/data-modeling/schema-design-process/apply-patterns/)  
2. MongoDB Design Reviews: how applying schema design best practices resulted in a 60x performance improvement \- DEV Community, accessed on February 1, 2026, [https://dev.to/mongodb/mongodb-design-reviews-how-applying-schema-design-best-practices-resulted-in-a-60x-performance-improvement-56m5](https://dev.to/mongodb/mongodb-design-reviews-how-applying-schema-design-best-practices-resulted-in-a-60x-performance-improvement-56m5)  
3. Creating Role-Based Access Control in MongoDB \- ScaleGrid, accessed on February 1, 2026, [https://scalegrid.io/blog/creating-role-based-access-control-in-mongodb/](https://scalegrid.io/blog/creating-role-based-access-control-in-mongodb/)  
4. Service Layer Pattern in Java With Spring Boot \- foojay, accessed on February 1, 2026, [https://foojay.io/today/service-layer-pattern-in-java-with-spring-boot/](https://foojay.io/today/service-layer-pattern-in-java-with-spring-boot/)  
5. Angular v20+ Folder Structure Guide: Best Practices for Scalable Apps, accessed on February 1, 2026, [https://www.angular.courses/blog/angular-folder-structure-guide](https://www.angular.courses/blog/angular-folder-structure-guide)  
6. RxVirtualView | RxAngular, accessed on February 1, 2026, [https://rx-angular.io/docs/template/virtual-view-directive](https://rx-angular.io/docs/template/virtual-view-directive)  
7. Time Series Data Introduction \- MongoDB, accessed on February 1, 2026, [https://www.mongodb.com/resources/basics/time-series-data-analysis](https://www.mongodb.com/resources/basics/time-series-data-analysis)  
8. Spring Boot, MongoDB: JWT Authentication with Spring Security \- BezKoder, accessed on February 1, 2026, [https://www.bezkoder.com/spring-boot-jwt-auth-mongodb/](https://www.bezkoder.com/spring-boot-jwt-auth-mongodb/)  
9. MIME Type and Uploaded File Type Detection Problem \- Nguyễn Tuấn's Blog, accessed on February 1, 2026, [https://chidokun.github.io/2021/10/mime-type-and-upload-file-problem/en/](https://chidokun.github.io/2021/10/mime-type-and-upload-file-problem/en/)  
10. $group (aggregation stage) \- Database Manual \- MongoDB Docs, accessed on February 1, 2026, [https://www.mongodb.com/docs/manual/reference/operator/aggregation/group/](https://www.mongodb.com/docs/manual/reference/operator/aggregation/group/)  
11. Building with Patterns: The Extended Reference Pattern \- MongoDB, accessed on February 1, 2026, [https://www.mongodb.com/company/blog/building-with-patterns-the-extended-reference-pattern](https://www.mongodb.com/company/blog/building-with-patterns-the-extended-reference-pattern)  
12. TTL Indexes \- Database Manual \- MongoDB Docs, accessed on February 1, 2026, [https://www.mongodb.com/docs/manual/core/index-ttl/](https://www.mongodb.com/docs/manual/core/index-ttl/)  
13. Automatic Data Expiration in MongoDB Using TTL Indexes | by Navidbarsalari \- Medium, accessed on February 1, 2026, [https://medium.com/@navidbarsalari/automatic-data-expiration-in-mongodb-using-ttl-indexes-306351bfee6d](https://medium.com/@navidbarsalari/automatic-data-expiration-in-mongodb-using-ttl-indexes-306351bfee6d)  
14. Hello , I want to remove new users from mongoDB atlas within 2 minutes if they do not verify their phone number , I think TTL is the way \- Stack Overflow, accessed on February 1, 2026, [https://stackoverflow.com/questions/74616125/hello-i-want-to-remove-new-users-from-mongodb-atlas-within-2-minutes-if-they-d](https://stackoverflow.com/questions/74616125/hello-i-want-to-remove-new-users-from-mongodb-atlas-within-2-minutes-if-they-d)  
15. Soft Delete Pattern in MongoDB \- GeeksforGeeks, accessed on February 1, 2026, [https://www.geeksforgeeks.org/mongodb/soft-delete-pattern-in-mongodb/](https://www.geeksforgeeks.org/mongodb/soft-delete-pattern-in-mongodb/)  
16. Time Series \- Database Manual \- MongoDB Docs, accessed on February 1, 2026, [https://www.mongodb.com/docs/manual/core/timeseries-collections/](https://www.mongodb.com/docs/manual/core/timeseries-collections/)  
17. 3 Reasons (and 2 Ways) to Use MongoDB's Improved Time Series Collections, accessed on February 1, 2026, [https://www.mongodb.com/company/blog/technical/three-reasons-two-ways-use-improved-time-series-collections](https://www.mongodb.com/company/blog/technical/three-reasons-two-ways-use-improved-time-series-collections)  
18. Building with Patterns: The Bucket Pattern \- MongoDB, accessed on February 1, 2026, [https://www.mongodb.com/company/blog/building-with-patterns-the-bucket-pattern](https://www.mongodb.com/company/blog/building-with-patterns-the-bucket-pattern)  
19. Bucket Pattern — Time Series Data | by kelvinBz \- Medium, accessed on February 1, 2026, [https://kelvinbz.medium.com/bucket-pattern-time-series-data-525b8257363b](https://kelvinbz.medium.com/bucket-pattern-time-series-data-525b8257363b)  
20. Implementing JWT Authentication in Spring Boot with MongoDB \- Hungry Coders, accessed on February 1, 2026, [https://www.hungrycoders.com/blog/jwt-authentication-in-spring-boot-with-mongodb](https://www.hungrycoders.com/blog/jwt-authentication-in-spring-boot-with-mongodb)  
21. Spring Security: Custom Permission Evaluator \- JDriven Blog, accessed on February 1, 2026, [https://jdriven.com/blog/2019/10/Spring-Security-Custom-Permission-Evaluator](https://jdriven.com/blog/2019/10/Spring-Security-Custom-Permission-Evaluator)  
22. Spring Security custom PermissionEvaluator \- Stack Overflow, accessed on February 1, 2026, [https://stackoverflow.com/questions/62217326/spring-security-custom-permissionevaluator](https://stackoverflow.com/questions/62217326/spring-security-custom-permissionevaluator)  
23. Getting Started | Uploading Files \- Spring, accessed on February 1, 2026, [https://spring.io/guides/gs/uploading-files/](https://spring.io/guides/gs/uploading-files/)  
24. Which one is better for Creating an JAVA Springboot Rest Endpoint to upload file \- Multipart file vs base 64 encoded byte array in the POST body \- Stack Overflow, accessed on February 1, 2026, [https://stackoverflow.com/questions/68400688/which-one-is-better-for-creating-an-java-springboot-rest-endpoint-to-upload-file](https://stackoverflow.com/questions/68400688/which-one-is-better-for-creating-an-java-springboot-rest-endpoint-to-upload-file)  
25. Spoof-proof file type validation in a Spring Boot app \- Stack Overflow, accessed on February 1, 2026, [https://stackoverflow.com/questions/79394502/spoof-proof-file-type-validation-in-a-spring-boot-app](https://stackoverflow.com/questions/79394502/spoof-proof-file-type-validation-in-a-spring-boot-app)  
26. Secure File Upload API with SpringBoot | by Jens@Fivesec | Medium, accessed on February 1, 2026, [https://medium.com/@js\_9757/secure-file-upload-api-with-springboot-1d1f415b80a6](https://medium.com/@js_9757/secure-file-upload-api-with-springboot-1d1f415b80a6)  
27. Controller-Service-Repository \- Tom Collings \- Medium, accessed on February 1, 2026, [https://tom-collings.medium.com/controller-service-repository-16e29a4684e5](https://tom-collings.medium.com/controller-service-repository-16e29a4684e5)  
28. Atomicity and Transactions \- Database Manual \- MongoDB Docs, accessed on February 1, 2026, [https://www.mongodb.com/docs/manual/core/write-operations-atomicity/](https://www.mongodb.com/docs/manual/core/write-operations-atomicity/)  
29. Atomicity and Transactions \- Database Manual v7.0 \- MongoDB Docs, accessed on February 1, 2026, [https://www.mongodb.com/docs/v7.0/core/write-operations-atomicity/](https://www.mongodb.com/docs/v7.0/core/write-operations-atomicity/)  
30. MongoDB Change Streams with Java Spring Data | by sparshneel chanchlani \- Medium, accessed on February 1, 2026, [https://medium.com/the-quick-learners/mongodb-change-streams-with-java-spring-data-0a1016fe1120](https://medium.com/the-quick-learners/mongodb-change-streams-with-java-spring-data-0a1016fe1120)  
31. How to use MongoDB Change Streams as a Powerful Event-Driven Engine, accessed on February 1, 2026, [https://www.geeksforgeeks.org/dbms/how-to-use-mongodb-change-streams-as-a-powerful-event-driven-engine/](https://www.geeksforgeeks.org/dbms/how-to-use-mongodb-change-streams-as-a-powerful-event-driven-engine/)  
32. Mongo Change Streams with Server-Sent Events in SpringBoot and Kotlin \- tolkiana, accessed on February 1, 2026, [https://tolkiana.com/mongo-change-streams-with-server-sent-events-in-springboot-and-kotlin/](https://tolkiana.com/mongo-change-streams-with-server-sent-events-in-springboot-and-kotlin/)  
33. Angular Best Practices: Tips for Project Structure and Organization \- Thinkitive, accessed on February 1, 2026, [https://www.thinkitive.com/blog/angular-best-practices-tips-for-project-structure-and-organization/](https://www.thinkitive.com/blog/angular-best-practices-tips-for-project-structure-and-organization/)  
34. Scrolling | Angular Material, accessed on February 1, 2026, [https://v9.material.angular.dev/cdk/scrolling](https://v9.material.angular.dev/cdk/scrolling)  
35. How to use virtual scrolling to load images in Angular \- Cloudinary, accessed on February 1, 2026, [https://cloudinary.com/blog/guest\_post/how-to-use-virtual-scrolling-to-load-images-in-angular](https://cloudinary.com/blog/guest_post/how-to-use-virtual-scrolling-to-load-images-in-angular)  
36. Writing custom virtual scroll strategy in Angular apps, accessed on February 1, 2026, [https://angular.love/writing-custom-virtual-scroll-strategy-in-angular-apps/](https://angular.love/writing-custom-virtual-scroll-strategy-in-angular-apps/)  
37. Writing custom virtual scroll strategy | by Alexander Inkin | Angular In Depth \- Medium, accessed on February 1, 2026, [https://medium.com/angular-in-depth/writing-custom-virtual-scroll-strategy-e6b1b2d2e1ac](https://medium.com/angular-in-depth/writing-custom-virtual-scroll-strategy-e6b1b2d2e1ac)  
38. cdk-virtual-scroll-viewport with variable item heights \- Stack Overflow, accessed on February 1, 2026, [https://stackoverflow.com/questions/54180280/cdk-virtual-scroll-viewport-with-variable-item-heights](https://stackoverflow.com/questions/54180280/cdk-virtual-scroll-viewport-with-variable-item-heights)  
39. Angular Virtual Scroll with dynamic variable height/width? \- Stack Overflow, accessed on February 1, 2026, [https://stackoverflow.com/questions/55092697/angular-virtual-scroll-with-dynamic-variable-height-width](https://stackoverflow.com/questions/55092697/angular-virtual-scroll-with-dynamic-variable-height-width)  
40. Masonry style layout with CSS Grid | by Andy Barefoot \- Medium, accessed on February 1, 2026, [https://medium.com/@andybarefoot/a-masonry-style-layout-using-css-grid-8c663d355ebb](https://medium.com/@andybarefoot/a-masonry-style-layout-using-css-grid-8c663d355ebb)  
41. CSS masonry with flexbox, :nth-child(), and order | Tobias Ahlin, accessed on February 1, 2026, [https://tobiasahlin.com/blog/masonry-with-css/](https://tobiasahlin.com/blog/masonry-with-css/)  
42. Masonry layout \- CSS \- MDN Web Docs, accessed on February 1, 2026, [https://developer.mozilla.org/en-US/docs/Web/CSS/Guides/Grid\_layout/Masonry\_layout](https://developer.mozilla.org/en-US/docs/Web/CSS/Guides/Grid_layout/Masonry_layout)  
43. Group By year-month \- Node.js Frameworks \- MongoDB Community Hub, accessed on February 1, 2026, [https://www.mongodb.com/community/forums/t/group-by-year-month/102514](https://www.mongodb.com/community/forums/t/group-by-year-month/102514)

[image1]: <data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABIAAAAYCAYAAAD3Va0xAAABHElEQVR4Xu3Sv0tCURQH8CPZYCjVJG3ZEAhBg39DUIuDgySNrmKjTSK2OAiiS+Ae/QsN/QGBf4D/QFNL0Kbgj+/Xey4e33vyCBrfFz6D5x7ve+/cK5Lkr6mrsXqCjFk/h6HyPT04MT2bsEB38AsLuDHrh1BQH9CEMzgwPZv820Y+NWjDD7xB2qzl1AhOTX0nKcXvvhS3CTe7Mj1F1RfXGxk+gQaQhVtYQsf0VNSDqYVSUi39fQyfMIW81rqKfXvD2VDZ1BqwgnvZziZ2PpwNXZg6T+gL3sW9BWcTOx/Oxs/Hh394hpm4C8jZxM7Hf38wPDWeHu/WtYoMn/oIVRUM7xGvwkS2JxsKj/Jb3EDn6hWObJO4q/ASqCVJwqwBVtA0FLqyjDcAAAAASUVORK5CYII=>