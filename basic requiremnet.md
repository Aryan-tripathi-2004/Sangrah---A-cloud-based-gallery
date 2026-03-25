**Sangrah Pay-as-You-Use Cloud Storage Platform**

**1\. Core Concept**

A cloud storage platform where users can upload and manage images and videos. Unlike traditional storage services that charge fixed monthly packs (e.g., 130 for 100GB), this platform follows a pay-as-you-use model:

* Users pay only for the exact storage they consume beyond the free limit.  
  * *Example:* If the free limit is 10GB and a user consumes 15GB, they pay only for 5GB, not for a fixed 100GB plan.

**2\. The Problem We All Know Too Well**

We've all faced this situation: you go on a trip with friends, everyone takes amazing photos that deserve a frame. Later, instead of chasing each friend individually for their pictures, you create a WhatsApp group and ask everyone to upload them there.

Sounds convenient until it isn't. Suddenly, you're staring at 500+ photos and videos in one endless thread. To even view them properly, you have to download the entire batch—a nightmare for your phone's storage. And if you're only interested in the pictures you're actually in? Well, now you're scrolling through hundreds of shots, manually deleting the ones you don't need. It's time-consuming, messy, and frustrating.

**TechStack Used:**  
	**Microservice based project**  
**Frontend-** Angular  
	**Backend-  java SpringBoot**  
	**DataBase-**MongoDB  
	**Cloud Storage \-  Amazon S3 bucket**

**3\. Key Features**

**A. Gallery**

* Every user has their own gallery in which they can upload their content (images and videos).  
* Displays all uploaded images and videos in a daily timeline view. Users can choose in which way they want to see the content: daily, monthly, or yearly timeline.  
* Allows users to upload additional media anytime.  
* Supports both images and videos.

**B. Events**

A shared event space where multiple users can upload and view photos and videos for a specific occasion. Users can add their own media to the event, and everyone invited can see the collected memories.

* **Event Types:**  
  * **Public Event:**  
    * Accessible to everyone.  
    * Creator can allow direct uploads or require approval before media is visible.  
    * Pending uploads are reviewed by the creator before publishing.  
  * **Protected Event:**  
    * Similar to Public Event but with requested access.  
    * it is a **Requested Event** where users must request to access the content, and if approved, they are eligible to see it.  
  * **Private Event:**  
    * Accessible only to the creator.  
    * No public visibility.

**C. Profile Page**

* A Profile Section similar to Instagram, where the user can see their own profile details, how many events they have created, and the total number of images and videos they have uploaded.   
* The profile should also include filters so the user can view their media based on privacy level:  
  * Public images  
  * Protected images  
  * Private images  
* The same filtering should be available for events as well.  
* **When other users visit someone else's profile:**  
  * They should only see Public and Protected events and media.  
  * Protected items should appear blurred or locked unless the viewer has proper access or has paid for access.  
  * This ensures that unauthorized users cannot view protected content.  
  * Private content should be visible only to the owner or authorized collaborators.

**D. Billing and Pricing**

I want to create a separate Billing Section where users can clearly see:

* How much storage they have used for Gallery and Events  
* How much they need to pay for that usage

**Payment Schedule & Policy:**

* The billing amount should automatically update every night at 12 AM.  
* Users should receive a monthly bill.  
* If they pay within 5 days, everything is fine.  
* If they do not pay within 5 days, all of their data should be deleted.

**Billing Logic**

**D.1. Full Month Images & Videos (Gallery)**

* In the billing section, there will be a Gallery price section. Below that, the user can see a list of all images and videos uploaded during the month.  
* **For each file uploaded at the start or during the month:**  
  * If it existed for the entire 30-day month, its price is calculated using the (30 days \* per-MB price \* file size).  
  * If a file (image or video) is uploaded later in the month, such as on the 20th, the user only pays for the days it existed: (Number of days it existed \* per-MB price \* file size).  
* This ensures the user pays only for the days the file actually occupied storage.

**D.2. Events Billing**

* Each event will show:  
  * How many photos and videos it contains  
  * The total size of the event  
  * The upload dates for those items  
  * The total storage cost of the event  
* Just like the gallery:  
  * Items present for the full month are charged for 30 days.  
  * Items uploaded later in the month are charged only for the remaining days.  
* This calculation happens separately for every event.

**D.3. Monthly Bill Summary**

* At the bottom of the bill, the user will see:  
  * Total Gallery Cost  
  * Total Event Cost  
  * Grand Total Amount the user must pay at the end of the month  
* **Final Bill Formula:**  
  * Gallery (full-month files \+ partial-month files) \+ Events (full-month event files \+ partial month event files) \= **User's Total Monthly Bill**

    

    

    

    