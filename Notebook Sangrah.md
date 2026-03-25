# **Sangrah Platform: Technical Specification and Implementation Blueprint**

## **Introduction and System Vision**

### **Introduction**

The Sangrah platform is conceived as a flexible, pay-as-you-use cloud storage and event-sharing service. It provides users with a personal media gallery and collaborative event spaces, moving away from rigid, fixed-cost storage plans. By charging users only for the storage they actually consume beyond a complimentary free tier, Sangrah offers a more equitable and scalable solution for personal and shared media management.

### **Core Problem Analysis**

The platform directly addresses the common frustrations associated with collaborative photo and video sharing. Following shared experiences like trips or events, media is often scattered across multiple individuals' devices. The typical solution—creating a group chat for uploads—leads to significant user pain points: massive, uncurated downloads consuming phone storage, the lack of organization within a single conversational thread, and the time-consuming manual effort required to find specific photos and delete irrelevant ones. Sangrah aims to eliminate this disorganized and inefficient process by providing a centralized, structured, and intelligent environment for media aggregation and sharing.

### **Document Purpose**

This document serves as the formal technical blueprint for the Sangrah platform. It provides a comprehensive overview of the system architecture, technology stack, functional specifications, microservice design, database schema, and frontend implementation guidelines. It is intended to be the definitive guide for the development team, ensuring alignment and a shared understanding of the project's technical goals and requirements from inception to deployment.

### **Architectural Overview**

To realize this vision, the platform will be built upon a modern, scalable architecture. The following sections will deconstruct this architecture, starting with a high-level overview of the system's core components and design philosophy before delving into the specific implementation details of its features and services.

## **High-Level System Architecture**

### **Architectural Strategy**

The Sangrah platform **shall be implemented** using a **microservices architecture**. This strategic choice is driven by the platform's diverse functional domains, such as user authentication, media processing, event management, and billing. A microservices approach provides critical advantages, including enhanced scalability, improved fault isolation, and development agility. By decoupling core services, teams can develop, deploy, and scale features independently. For instance, the resource-intensive "Find My Face" feature can be scaled independently within the `Event Service` without impacting the performance of the `Billing Service`'s scheduled tasks, ensuring system resilience and facilitating a continuous development lifecycle.

### **Core Technology Stack**

The technology stack has been selected to provide a robust, modern, and scalable foundation for the platform.

| Component | Technology/Rationale |
| :---- | :---- |
| **Backend** | **Spring Boot** (Java): Chosen for its enterprise-grade capabilities, rapid development features, and robust ecosystem for building secure and high-performance microservices. |
| **Frontend** | **Angular**: A powerful framework for building structured, maintainable, and modern single-page applications (SPAs) that deliver a seamless user experience. |
| **Database** | **MongoDB** (NoSQL): Selected for its flexible schema, which is ideal for managing user-generated content with varied metadata. Its scalability aligns perfectly with the platform's data growth projections. |
| **Authentication** | **JSON Web Tokens (JWT)**: Provides a stateless, secure, and standardized method for authenticating API requests between the Angular frontend and the distributed backend microservices. |

### **Microservice Ecosystem**

The backend logic will be distributed across a suite of specialized microservices, each with a distinct responsibility:

* `User & Auth Service`: Manages all aspects of user identity, including registration, authentication, profile data, and role management.  
* `Gallery & Media Service`: Handles the storage, retrieval, and metadata management of all media within users' personal galleries and media transfers from events.  
* `Event Service`: Governs the creation, management, and collaborative aspects of shared event spaces.  
* `Billing Service`: Responsible for calculating storage usage, generating monthly invoices, and tracking payment status based on the pay-as-you-use model.  
* `Notification Service`: Manages and dispatches all user-facing communications, such as event invitations and upload alerts.

### **From Architecture to Functionality**

This high-level architecture provides the scaffolding upon which the platform's rich feature set will be built. The next section will detail these user-facing features and translate them into specific functional requirements for the development team.

## **Functional Specification and Core Features**

### **Introduction**

This section translates the platform's user-facing vision into concrete technical functionalities. It defines the core features of the Sangrah platform and outlines the specific requirements that the underlying microservices and database architecture must support to deliver a complete and intuitive user experience.

### **"Gallery" Feature**

The Gallery is the user's personal, private cloud storage space.

* **Personal Media Storage:** Every user is allocated a personal gallery for uploading and managing their images and videos.  
* **Timeline Views:** The gallery interface must support dynamic content organization, allowing users to view their media in a timeline format filtered by day, month, or year.  
* **Media Addition to Events:** When a user adds a gallery item to an event, they must be presented with two distinct options:  
  1. **Add by Reference:** A pointer to the original gallery media is added to the event. If the original media is deleted from the user's gallery, it is automatically removed from the event as well. This option does not incur additional storage costs for the event owner.  
  2. **Add as a Distinct Copy:** A new, independent copy of the media is created and associated with the event. This copy persists even if the original is deleted from the user's gallery. This action will be metered and contribute to the event owner's monthly storage bill.

### **"Events" Feature**

Events are collaborative spaces designed for sharing media related to a specific occasion.

* **Shared Space Concept:** An event allows multiple participants to contribute and view a collective repository of photos and videos.  
* **Event Types:** The platform must support three distinct event types, each with specific access control rules:  
  1. `Public`: Accessible to everyone. The event creator can configure it to either allow direct media uploads or require that all submissions be reviewed and approved by a collaborator before they become visible.  
  2. `Protected`: Functions like a public event but requires payment or an approved access request to view content. If a price is set, users must pay to gain access. If no price is set, it becomes a request-based event where users must be granted permission by the creator. Users **shall be able** to leave ratings and reviews to help others decide whether to purchase access.  
  3. `Private`: Strictly invitation-only. Content is visible only to the event creator and designated collaborators, with no public visibility.  
* **Face Recognition ("Find My Face"):** The platform will integrate a machine learning-based feature that automatically scans all photos within an event to detect and group human faces. The UI will display thumbnails of detected individuals, allowing a user to click their own face to instantly filter the event gallery and view only the photos in which they appear.  
* **Collaboration:** Event creators can assign roles to other users, granting them specific permissions:  
  1. Edit event details (name, description, etc.)  
  2. Delete the entire event  
  3. Review and approve pending media uploads  
  4. Manage participants and their access  
* **Adding Event Media to Personal Gallery:** A user viewing an event shall have the ability to add media from that event directly to their own gallery. They must be presented with two options:  
  1. **Add by Reference:** A reference to the event media is added to the user's gallery. The media will be removed from their gallery if the event or the original media item is deleted. This option does not incur storage costs for the user.  
  2. **Add as a Distinct Copy:** A new, independent copy of the media is created in the user's personal gallery. This action is metered, and the user will be billed for the storage of this new file.

### **"User Profile" Feature**

The user profile page serves as a public-facing and personal dashboard.

* **Profile Layout:** The profile page will be designed with a layout similar to Instagram, displaying essential user details, a count of created events, and a total count of uploaded media.  
* **Personal Content Filtering:** When viewing their own profile, a user can filter their displayed media and events based on privacy levels: `Public`, `Protected`, and `Private`.  
* **Visitor Access Rules:** When a visitor views another user's profile, access is restricted. They will only see `Public` and `Protected` media and events. Any `Protected` content must be rendered in a blurred or locked state, indicating that access requires payment or permission. `Private` content is never visible to unauthorized visitors.

### **"Billing and Pricing" System**

The platform operates on a transparent, pay-as-you-use billing model.

* **Payment Model:** Users receive a monthly bill for their storage consumption. They have a 5-day window to complete the payment. Failure to pay within this period will result in the deletion of all their data.  
* **Billing Logic:** Billing is calculated on a pro-rated daily basis to ensure fairness.  
  * **Full Month Usage:** A file that exists in storage for a full 30-day billing cycle is charged based on its size and the per-MB price for 30 days.  
  * **Partial Month Usage:** A file uploaded partway through a billing cycle is charged only for the number of days it physically occupied storage. For example, a file uploaded on the 20th of a 30-day month will only be billed for 10 days of storage.  
  * This pro-rated logic applies independently to files in the personal `Gallery` and files within each `Event`.  
* **Bill Summary:** The final monthly bill presented to the user must be clearly itemized with the following structure:  
  * `Total Gallery Cost`: The sum of all pro-rated storage costs for files in the user's personal gallery.  
  * `Total Event Cost`: The sum of all pro-rated storage costs for files across all events owned by the user.  
  * `Grand Total`: The final, payable amount, calculated as `Total Gallery Cost + Total Event Cost`.

### **Conclusion**

These detailed functional requirements form the specification for the platform's user experience. The backend architecture, detailed in the following section, is the engine designed to power these features with reliability, security, and scale.

## **Backend Architecture: Microservices Deep Dive**

### **Introduction**

This section provides the detailed technical specifications for each microservice. These services encapsulate the core business logic of the Sangrah platform. Each service is designed to be an independent, deployable unit with a clear domain of responsibility and a well-defined API contract for inter-service communication and frontend integration.

### **Common Architectural Patterns**

To ensure consistency, maintainability, and quality across all microservices, the following patterns and principles **are mandated**.

* **Project Structure:** Every Spring Boot microservice **must** adhere to a modular MVC \+ layered architecture with the following package structure:  
  * `controller`: Exposes RESTful API endpoints.  
  * `service`: Contains the core business logic.  
  * `repository`: Handles data access and persistence.  
  * `dto`: Defines Data Transfer Objects for API requests and responses.  
  * `entity`: Defines the MongoDB document models.  
  * `config`: Houses configuration classes (e.g., security).  
  * `exception`: Contains custom exception classes and global handlers.  
  * `util`: Provides utility functions.  
  * `security`: Implements JWT and role-based security logic.  
  * `test`: Contains unit and integration tests.  
* **Core Principles:**  
  * **Data Transfer Objects (DTOs):** All API communication **must** use DTOs to decouple the internal domain models (`entity`) from the external API contract.  
  * **Service Layer Abstraction:** Business logic **must** reside exclusively in the service layer, which should be defined by interfaces to promote loose coupling.  
  * **Logging:** SLF4J **must** be used for all application logging to provide a standardized and flexible logging framework.  
  * **Error Handling:** A global exception handler using `@ControllerAdvice` **must** be implemented to manage custom and common exceptions gracefully.  
  * **Security:** API endpoints **must** be secured using JWT-based authentication.  
  * **Authorization:** Role-based access control (RBAC) **must** be enforced on protected endpoints using Spring Security's `@PreAuthorize` annotation.  
  * **Validation:** Incoming request DTOs **must** be validated using Java's standard validation annotations (`@Valid`, `@NotNull`, etc.).

### **Microservice Definitions**

#### **User & Auth Service**

This service is the gatekeeper of the platform, managing user identity, authentication, and authorization. It is responsible for the entire user lifecycle, from registration to profile management, and is the sole issuer of JWTs. This service exposes two primary resource paths: `/api/auth` for stateless authentication operations and `/api/users` for stateful user profile management.

* **Primary Functions:**  
  * Handle new user registration.  
  * Authenticate users and issue JWTs upon successful login.  
  * Fetch and update user profile information.  
  * Manage user roles (ADMIN, CREATOR, VIEWER).  
* **API Endpoints:**

| Endpoint | HTTP Method | Description | Key DTOs (Request/Response) |
| :---- | :---- | :---- | :---- |
| `/api/auth/register` | `POST` | Registers a new user. | `RegisterRequestDTO (username, email, password)`\<br\>`UserResponseDTO` |
| `/api/auth/login` | `POST` | Authenticates a user and returns a JWT. | `LoginRequestDTO (email, password)`\<br\>`JwtResponseDTO (token, username, roles)` |
| `/api/users/profile` | `GET` | Fetches the profile of the authenticated user. | `UserResponseDTO` |
| `/api/users/profile` | `PUT` | Updates the profile of the authenticated user. | `UpdateUserRequestDTO (username, etc.)`\<br\>`UserResponseDTO` |
| `/api/users/{userId}` | `GET` | Fetches the public profile of another user. | `PublicProfileResponseDTO` |

#### **Gallery & Media Service**

This service manages all media files within a user's personal gallery. It handles the complexities of file uploads, storage abstraction, metadata persistence, and the implementation of the hybrid pagination logic for efficient media browsing.

* **Primary Functions:**  
  * Process media uploads to a user's gallery.  
  * List gallery media with timeline filtering (daily, monthly, yearly).  
  * Implement hybrid pagination for media listings.  
  * Handle media deletion from the gallery.  
  * Add media from an event to a user's gallery (by reference or copy).  
  * Provide secure URLs for media retrieval.  
* **API Endpoints:**

| Endpoint | HTTP Method | Description | Key DTOs (Request/Response) |
| :---- | :---- | :---- | :---- |
| `/api/gallery/media` | `POST` | Uploads one or more media files to the user's gallery. | `MultipartFile[]`\<br\>`List<MediaItemResponseDTO>` |
| `/api/gallery/media` | `GET` | Lists media items. Implements hybrid pagination by accepting `batchSize` and `page`. Response includes the batch for client caching. | `PagedResponse<MediaItemResponseDTO>` |
| `/api/gallery/media/from-event` | `POST` | Adds an image from an event to the user's gallery. | `AddFromEventDTO (event_media_id, as_copy: boolean)`\<br\>`MediaItemResponseDTO` |
| `/api/gallery/media/{id}` | `DELETE` | Deletes a specific media item from the gallery. | `Void` |
| `/api/gallery/media/{id}` | `GET` | Retrieves metadata for a single media item. | `MediaItemResponseDTO` |

#### **Event Service**

This service is the core of the collaborative experience, managing the lifecycle of events. It handles event creation, participant management, access control logic, media aggregation, ratings/reviews, and face recognition features.

* **Primary Functions:**  
  * Create, update, and delete events.  
  * Manage event participants and assign collaborator roles/permissions.  
  * Add media to an event, handling both "by reference" and "by copy" logic.  
  * List events with search and filtering.  
  * Manage access requests for `Protected` events.  
  * Manage ratings and reviews for events.  
  * Initiate face recognition scans and provide endpoints for filtering.  
  * Generate sharable links and QR codes.  
* **API Endpoints:**

| Endpoint | HTTP Method | Description | Key DTOs (Request/Response) |
| :---- | :---- | :---- | :---- |
| `/api/events` | `POST` | Creates a new event. | `CreateEventDTO (name, type, etc.)`\<br\>`EventResponseDTO` |
| `/api/events/{eventId}` | `GET` | Retrieves details of a specific event. | `EventDetailResponseDTO` |
| `/api/events/{eventId}` | `PUT` | Updates an existing event. | `UpdateEventDTO`\<br\>`EventResponseDTO` |
| `/api/events/{eventId}` | `DELETE` | Deletes an event. | `Void` |
| `/api/events/{eventId}/media` | `POST` | Adds media to an event (from gallery). | `AddMediaToEventDTO (media_id, add_as_copy: boolean)`\<br\>`List<MediaItemResponseDTO>` |
| `/api/events/{eventId}/media` | `GET` | Lists media in an event, with optional face filtering. | `?faceId={faceId}`\<br\>`PagedResponse<MediaItemResponseDTO>` |
| `/api/events/{eventId}/faces` | `GET` | Retrieves a list of detected faces in the event. | `List<FaceDTO (faceId, thumbnailUrl)>` |
| `/api/events/{eventId}/collaborators` | `POST` | Adds a collaborator with specific permissions. | `AddCollaboratorDTO (user_id, permissions)`\<br\>`EventResponseDTO` |
| `/api/events/{eventId}/share` | `GET` | Generates a sharable link and QR code for the event. | `ShareableLinkDTO (url, qr_code_url)` |
| `/api/events/{eventId}/reviews` | `POST` | Submits a rating and review for a protected event. | `CreateReviewDTO (rating, review_text)`\<br\>`ReviewResponseDTO` |
| `/api/events/{eventId}/reviews` | `GET` | Lists all reviews for a protected event. | `List<ReviewResponseDTO>` |

#### **Billing Service**

This service implements the platform's unique pay-as-you-use monetization model. It operates primarily through scheduled tasks to perform daily storage calculations and culminates in the generation of a monthly bill for each user.

* **Primary Functions:**  
  * Run a scheduled daily task to calculate and log storage usage for every user.  
  * Generate a detailed monthly bill summary at the end of each billing cycle.  
  * Provide an endpoint for users to retrieve their current bill.  
  * Allow an administrator or payment gateway callback to mark a bill as paid.  
  * Trigger data deletion for users with overdue bills.  
* **API Endpoints:**

| Endpoint | HTTP Method | Description | Key DTOs (Request/Response) |
| :---- | :---- | :---- | :---- |
| `/api/billing/current` | `GET` | Retrieves the current month's bill for the user. | `BillResponseDTO` |
| `/api/billing/history` | `GET` | Retrieves a list of past bills for the user. | `List<BillSummaryDTO>` |
| `/api/billing/{billId}/pay` | `POST` | Marks a specific bill as paid (admin/system). | `MarkAsPaidDTO (transaction_id)`\<br\>`BillResponseDTO` |

#### **Notification Service**

This service decouples notification logic from the other services. It is responsible for sending transactional emails and potentially other forms of notifications to users based on events occurring within the platform.

* **Primary Functions:**  
  * Send email invitations to join an event.  
  * Notify users when new media is uploaded to an event they are part of.  
  * Send billing reminders and confirmations.  
* **API Endpoints:**

| Endpoint | HTTP Method | Description | Key DTOs (Request/Response) |
| :---- | :---- | :---- | :---- |
| `/api/notifications/email` | `POST` | Sends an email notification. (Internal-facing API) | `EmailNotificationRequestDTO (to, subject, body)` |
| `/api/notifications/invite` | `POST` | Sends an event invitation email to a specified user. | `EventInviteRequestDTO (to_email, event_id)` |

### **Conclusion**

These microservices form a robust, decoupled backend system. Their persistence layer, defined by a carefully designed database schema, is critical for maintaining data integrity and enabling the complex queries required by the platform's features. The next section details this database schema.

## **Database Schema: MongoDB Collections**

### **Data Modeling Strategy**

This section defines the schemas for the MongoDB collections that will serve as the persistence layer for the Sangrah platform. While the initial requirements briefly mentioned SQL, a NoSQL model with MongoDB was selected as the definitive choice due to its superior flexibility in handling user-generated content, unstructured metadata, and the platform's need for horizontal scalability, which aligns better with the overall microservices strategy. The data model prioritizes referencing documents via `ObjectId` over embedding large arrays to prevent documents from exceeding MongoDB's size limit and to ensure data consistency across collections.

### **Database Collections**

#### **`users` Collection**

Stores information for every registered user, including credentials and platform role.

| Field Name | Data Type | Description | Example |
| :---- | :---- | :---- | :---- |
| `_id` | ObjectId | Unique identifier for the user document. | `ObjectId("63d8a...")` |
| `username` | String | User's unique public username. | `"jane_doe"` |
| `email` | String | User's unique email address, used for login. | `"jane.doe@example.com"` |
| `password_hash` | String | Hashed and salted password for secure storage. | `"$2a$10$..."` |
| `role` | String | User's role (e.g., `CREATOR`, `VIEWER`, `ADMIN`). | `"CREATOR"` |
| `created_at` | ISODate | Timestamp of when the user account was created. | `2023-01-31T10:00:00Z` |
| *Design Analysis:* This is the central identity collection. Fields like `username` and `email` will be indexed for fast lookups during authentication and profile viewing. |  |  |  |

#### **`media_items` Collection**

A master collection containing metadata for every unique media file uploaded to the platform.

| Field Name | Data Type | Description | Example |
| :---- | :---- | :---- | :---- |
| `_id` | ObjectId | Unique identifier for the media item. | `ObjectId("63d8b...")` |
| `owner_id` | ObjectId | Reference to the `_id` of the user who owns the file. | `ObjectId("63d8a...")` |
| `file_name` | String | Original name of the uploaded file. | `"vacation_photo_01.jpg"` |
| `storage_url` | String | Secure URL pointing to the file in cloud storage. | `"https://storage.sangrah.com/..."` |
| `mime_type` | String | The MIME type of the file. | `"image/jpeg"` |
| `size_mb` | Double | File size in megabytes, used for billing. | `4.75` |
| `upload_date` | ISODate | Timestamp of when the file was uploaded. | `2023-02-15T14:30:00Z` |
| `privacy_level` | String | Visibility setting (`Public`, `Protected`, `Private`). | `"Protected"` |
| *Design Analysis:* This collection acts as the single source of truth for all media files. Storing metadata here separately from galleries or events allows for efficient querying and prevents data duplication. |  |  |  |

#### **`galleries` Collection**

Represents a user's personal gallery, linking a user to their collection of media items.

| Field Name | Data Type | Description | Example |
| :---- | :---- | :---- | :---- |
| `_id` | ObjectId | Unique identifier for the gallery. | `ObjectId("63d8c...")` |
| `user_id` | ObjectId | Reference to the `_id` of the gallery owner. | `ObjectId("63d8a...")` |
| `media_item_ids` | Array of ObjectIds | An array of `_id`s referencing `media_items`. | `[ObjectId("..."), ObjectId("...")]` |
| *Design Analysis:* Using an array of `ObjectId` references to `media_items` keeps this document lightweight and avoids duplicating media metadata. This design is scalable and maintains data integrity. |  |  |  |

#### **`events` Collection**

Defines a collaborative event, including its metadata, participants, and associated media.

| Field Name | Data Type | Description | Example |
| :---- | :---- | :---- | :---- |
| `_id` | ObjectId | Unique identifier for the event. | `ObjectId("63d8d...")` |
| `creator_id` | ObjectId | Reference to the `_id` of the event creator. | `ObjectId("63d8a...")` |
| `name` | String | The name of the event. | `"Summer Trip 2023"` |
| `description` | String | A brief description of the event. | `"Friends' trip to the mountains."` |
| `event_type` | String | Type of event (`Public`, `Protected`, `Private`). | `"Protected"` |
| `access_price` | Double | Cost to access a `Protected` event (0 for request-based). | `9.99` |
| `collaborators` | Array of Objects | List of users with special permissions for the event. | `[{ "user_id": ObjectId("..."), "permissions": ["EDIT"] }]` |
| `media` | Array of Objects | List of media in the event, referencing `media_items`. | `[{ "media_item_id": ObjectId("..."), "is_copy": true }]` |
| *Design Analysis:* The `media` array contains objects that reference a `media_item_id` and include an `is_copy` flag. This flag is crucial for implementing the distinct billing and data persistence logic for media added by copy versus by reference. |  |  |  |

#### **`event_reviews` Collection**

Stores user-submitted ratings and reviews for `Protected` events.

| Field Name | Data Type | Description | Example |
| :---- | :---- | :---- | :---- |
| `_id` | ObjectId | Unique identifier for the review. | `ObjectId("63d8e...")` |
| `event_id` | ObjectId | Reference to the `_id` of the reviewed `events` document. | `ObjectId("63d8d...")` |
| `user_id` | ObjectId | Reference to the `_id` of the user who wrote the review. | `ObjectId("63d8a...")` |
| `rating` | Integer | The rating given by the user (e.g., 1-5). | `5` |
| `review_text` | String | The text content of the review. | `"Great collection of photos!"` |
| `created_at` | ISODate | Timestamp of when the review was submitted. | `2023-03-01T18:00:00Z` |
| *Design Analysis:* This dedicated collection manages user-generated content for events, keeping the main `events` collection clean and focused on core event metadata. |  |  |  |

#### **`event_analytics` Collection**

Tracks engagement metrics for media items within events.

| Field Name | Data Type | Description | Example |
| :---- | :---- | :---- | :---- |
| `_id` | ObjectId | Unique identifier for the analytics entry. | `ObjectId("63d8f...")` |
| `media_item_id` | ObjectId | Reference to the `_id` of the `media_items` document. | `ObjectId("63d8b...")` |
| `view_count` | Integer | Total number of times the media has been viewed. | `150` |
| `download_count` | Integer | Total number of times the media has been downloaded. | `25` |
| *Design Analysis:* This collection is kept separate to allow for high-frequency write operations (incrementing counters) without locking the main `media_items` collection, ensuring better performance. |  |  |  |

#### **`bills` Collection**

Stores historical and current billing information for each user.

| Field Name | Data Type | Description | Example |
| :---- | :---- | :---- | :---- |
| `_id` | ObjectId | Unique identifier for the bill. | `ObjectId("63d9a...")` |
| `user_id` | ObjectId | Reference to the `_id` of the billed user. | `ObjectId("63d8a...")` |
| `month` | Integer | The billing month (1-12). | `2` |
| `year` | Integer | The billing year. | `2023` |
| `gallery_cost` | Double | Total calculated cost for gallery storage. | `5.42` |
| `event_cost` | Double | Total calculated cost for event storage. | `12.89` |
| `total_amount` | Double | The grand total amount due for the month. | `18.31` |
| `status` | String | The current status of the bill (`Due`, `Paid`, `Overdue`). | `"Due"` |
| *Design Analysis:* This collection provides a clear, queryable history of a user's financial transactions with the platform, essential for customer support and financial reporting. |  |  |  |

### **Conclusion**

This well-defined database schema provides a solid foundation for data persistence. The structured data within these collections will be consumed, manipulated, and presented by the frontend application, whose architecture is detailed next.

## **Frontend Architecture: Angular Application**

### **Frontend Design Philosophy**

The Sangrah frontend **shall be** a modern, responsive single-page application (SPA) built using the latest version of Angular. The architecture **shall be** modular, maintainable, and deliver a fast, seamless user experience. By breaking the application into feature-based modules and adhering to best practices, the architecture will support scalability and ease of future development.

### **Proposed Folder Structure**

A modular folder structure **shall be enforced** to ensure logical separation of concerns and promote code reuse.

* `src/app/`  
  * `auth/`: Contains components for user authentication (Login, Register).  
  * `events/`: Manages all event-related components (`event-create`, `event-list`, `event-detail`, `event-share`).  
  * `media/`: Includes components for media handling (`media-upload`, `media-gallery`).  
  * `shared/`: A repository for reusable components, services, models, guards, and interceptors used across multiple feature modules.  
  * `core/`: Houses singleton services and components foundational to the application layout (e.g., Header, Footer).

### **Authentication and Authorization Flow**

* **JWT-Based Login:** Upon successful login, the backend will issue a JWT. This token **must** be stored securely in the browser's `localStorage` and attached to the `Authorization` header of all subsequent API requests.  
* **Route Guards:** Angular Route Guards **shall be implemented** to protect application routes. These guards will check for the presence of a valid JWT and verify the user's role before granting access to a specific route (e.g., preventing a `VIEWER` from accessing an event creation page).  
* **Conditional UI Rendering:** UI elements such as navigation links, buttons (`Create Event`, `Delete`), and menu options **must** be conditionally rendered (shown/hidden) based on the authenticated user's role, ensuring that users only see the actions they are permitted to perform.

### **Core Application Modules and Components**

* **Auth Module:**  
  * `LoginComponent`, `RegisterComponent`: These components **shall use** Angular's Reactive Forms to implement user-friendly forms with robust, real-time validation.  
* **Events Module:**  
  * `event-create`: A form-based component for creating and editing events.  
  * `event-list`: Displays a paginated list of events with capabilities for searching and filtering.  
  * `event-detail`: A comprehensive view of a single event, including its media gallery and participant list.  
  * `event-share`: A component responsible for generating a unique shareable URL and a QR code for an event by calling the appropriate backend service.  
* **Media Module:**  
  * `media-upload`: An interface for uploading media files, featuring support for multiple file selection and previews.  
  * `media-gallery`: A responsive grid component for displaying images and videos, designed to work seamlessly on all screen sizes.  
* **Shared Module:**  
  * This module is critical for code reuse. It will contain:  
    * **Services:** Centralized services wrapping `HttpClient` for all backend API communication.  
    * **Guards:** Authentication and authorization guards.  
    * **Interceptors:** An `HttpInterceptor` to automatically attach the JWT to outgoing requests and another for global error handling (e.g., catching 401 Unauthorized responses).

### **Frontend Best Practices**

Adherence to the following best practices **is mandatory** to ensure a high-quality, production-ready application:

* **Asynchronous Operations:** Use RxJS Observables to handle all asynchronous API calls and manage application state reactively.  
* **Loading Indicators:** Implement loading spinners or progress bars during all data fetching operations to provide clear visual feedback to the user.  
* **User Feedback:** Utilize a toast notification service (e.g., `ngx-toastr` or Angular Material's SnackBar) to display concise success and error messages.  
* **Environment Configuration:** Store the base URL for the backend API in Angular's environment files (`environment.ts`, `environment.prod.ts`) to manage different deployment environments.  
* **Responsive Design:** The entire application layout **must be** fully responsive, adapting gracefully to various devices from mobile phones to desktops, using a framework like Angular Material or Bootstrap.

### **Conclusion**

This technical specification provides a complete architectural blueprint for the Sangrah platform. By adhering to the principles and guidelines outlined herein—from the microservice-based backend and flexible NoSQL database to the modular Angular frontend—the development team is equipped with a clear and comprehensive plan to build a scalable, maintainable, and feature-rich cloud storage and event-sharing service.

