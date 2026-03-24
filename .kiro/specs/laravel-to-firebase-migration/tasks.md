# Implementation Plan: Laravel to Firebase Migration

## Overview

This implementation plan migrates an Android service ticket management application from Laravel + MySQL backend to Firebase serverless architecture. The migration eliminates Retrofit HTTP client dependencies, replaces Laravel Sanctum with Firebase Authentication, migrates MySQL data to Firestore, and adds real-time capabilities. Implementation uses Java for Android components and JavaScript for Cloud Functions.

## Tasks

- [x] 1. Set up Firebase project and Android SDK integration
  - Add Firebase SDK dependencies to app/build.gradle.kts
  - Add google-services.json configuration file
  - Initialize Firebase in Application class
  - Enable Firestore offline persistence
  - Configure Firebase Authentication providers (email/password, Google, phone)
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.9, 1.10, 13.1_

- [x] 2. Implement Firebase Authentication Module
  - [x] 2.1 Create FirebaseAuthManager class with authentication methods
    - Implement signInWithEmail, createUserWithEmail, sendPasswordResetEmail
    - Implement signInWithGoogle integration
    - Implement phone number authentication with SMS verification
    - Implement session management (getCurrentUser, getIdToken, signOut)
    - Implement custom claims retrieval (getUserRole, getUserBranchId)
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.6, 1.7, 1.8, 1.9, 12.6_

  - [x] 2.2 Write unit tests for FirebaseAuthManager
    - Test email/password authentication flows
    - Test Google Sign-In integration
    - Test phone authentication
    - Test session management
    - Test custom claims parsing
    - _Requirements: 1.1, 1.2, 1.3, 19.4_

- [ ] 3. Create Firestore data model classes
  - [x] 3.1 Implement User model class
    - Create User.java with all fields (uid, email, name, phone, role, branchId, fcmTokens, profilePhotoUrl, timestamps)
    - Implement toMap() method for Firestore writes
    - Implement fromSnapshot() static method for Firestore reads
    - _Requirements: 3.1, 3.8_

  - [x] 3.2 Implement Ticket model class
    - Create Ticket.java with all fields including denormalized customer/branch/employee data
    - Implement toMap() and fromSnapshot() methods
    - Include support for GeoPoint location field
    - _Requirements: 3.2, 3.6, 3.8_

  - [~] 3.3 Implement Branch model class
    - Create Branch.java with location, coverage area, and manager fields
    - Implement toMap() and fromSnapshot() methods
    - _Requirements: 3.3, 3.7, 3.8_

  - [~] 3.4 Implement Payment model class
    - Create Payment.java for payments subcollection
    - Implement toMap() and fromSnapshot() methods
    - _Requirements: 3.5, 3.8, 9.2_

  - [x] 3.5 Write unit tests for data model classes
    - Test toMap() serialization for all models
    - Test fromSnapshot() deserialization for all models
    - Test handling of null/missing fields
    - _Requirements: 19.1_

- [ ] 4. Implement Firestore repository layer
  - [~] 4.1 Create TicketRepository class
    - Implement CRUD operations (createTicket, getTicket, updateTicket, deleteTicket)
    - Implement real-time queries (getUserTickets, getEmployeeTickets, getBranchTickets)
    - Implement filtered queries (getTicketsByStatus, getTicketsByDateRange)
    - Implement pagination with startAfter cursor
    - _Requirements: 3.2, 6.1, 6.2, 6.10, 16.1, 16.2, 16.3, 18.2, 18.3_

  - [~] 4.2 Create UserRepository class
    - Implement createUserDocument, getUserDocument, updateUserProfile
    - Implement updateFCMToken method
    - Implement getUserLiveData for real-time user updates
    - _Requirements: 3.1, 10.2_

  - [~] 4.3 Create BranchRepository class
    - Implement getAllBranches, getBranch, getBranchesLiveData
    - Implement employee management methods (addEmployeeToBranch, getBranchEmployees, removeEmployeeFromBranch)
    - _Requirements: 3.3, 3.4, 16.1_

  - [~] 4.4 Create PaymentRepository class
    - Implement createPayment, confirmPayment, getTicketPayments
    - Implement getUserPaymentHistory with real-time updates
    - _Requirements: 3.5, 9.1, 9.4, 9.6_

  - [x] 4.5 Write integration tests for repositories using Firebase Emulator
    - Test CRUD operations for all repositories
    - Test real-time listener behavior
    - Test pagination functionality
    - Test query filters
    - _Requirements: 19.3_

- [ ] 5. Implement Cloud Storage Manager
  - [~] 5.1 Create StorageManager class
    - Implement uploadTicketPhoto with progress tracking
    - Implement uploadProfilePhoto method
    - Implement getTicketPhotoUrl and getProfilePhotoUrl methods
    - Implement deleteTicketPhotos and deleteProfilePhoto methods
    - Implement image compression (compressImage method)
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.10, 18.4_

  - [x] 5.2 Write unit tests for StorageManager
    - Test upload operations with mock URIs
    - Test progress tracking callbacks
    - Test image compression logic
    - Test delete operations
    - _Requirements: 19.1_

- [ ] 6. Remove Retrofit dependencies and API layer
  - [~] 6.1 Remove Retrofit dependencies from build.gradle.kts
    - Remove Retrofit, OkHttp, and Gson converter dependencies
    - _Requirements: 2.1, 2.6, 2.7_

  - [~] 6.2 Delete API package classes
    - Delete ApiClient.java
    - Delete ApiService.java interface
    - Delete all request/response model classes in app/src/main/java/app/hub/api/
    - _Requirements: 2.2, 2.3, 2.4_

  - [~] 6.3 Verify compilation after Retrofit removal
    - Run Gradle build to ensure no Retrofit references remain
    - _Requirements: 2.8_

- [~] 7. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 8. Implement Cloud Functions for business logic
  - [~] 8.1 Set up Cloud Functions project structure
    - Initialize Firebase Functions with JavaScript/TypeScript
    - Install Firebase Admin SDK dependencies
    - Configure functions deployment settings
    - _Requirements: 7.6, 8.7_

  - [~] 8.2 Implement assignTicketToBranch callable function
    - Implement Haversine formula for distance calculation
    - Query all branches and calculate distances
    - Assign ticket to nearest branch within coverage area
    - Handle edge cases (no branches, no coverage)
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.7, 7.8, 7.9, 7.10_

  - [~] 8.3 Implement getDashboardStats callable function
    - Aggregate ticket counts by status
    - Calculate total revenue from payments
    - Calculate average completion time
    - Calculate employee workload
    - Filter by branch for managers, all branches for admins
    - Implement 5-minute caching
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 8.8, 8.9, 8.10_

  - [~] 8.4 Implement user management callable functions
    - Implement createUserAccount function (admin only)
    - Implement setUserRole function with custom claims
    - Implement deleteUserAccount function
    - Validate admin role before execution
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5, 12.9, 12.10, 15.1, 15.2, 15.3, 15.4, 15.5, 15.6, 15.7, 15.8, 15.9, 15.10_

  - [~] 8.5 Implement Firestore trigger functions
    - Implement onUserCreate trigger to create user document and set default custom claims
    - Implement onTicketStatusChange trigger to send FCM notifications
    - Implement onTicketDelete trigger to cleanup photos from Cloud Storage
    - Implement onUserNameChange trigger to update denormalized data in tickets
    - _Requirements: 1.5, 5.7, 10.3, 10.4_

  - [x] 8.6 Write integration tests for Cloud Functions using Firebase Emulator
    - Test assignTicketToBranch with various scenarios
    - Test getDashboardStats aggregation logic
    - Test user management functions with role validation
    - Test Firestore triggers
    - _Requirements: 19.3, 19.9_

- [ ] 9. Implement Firestore Security Rules
  - [~] 9.1 Create firestore.rules file
    - Implement helper functions (isAuthenticated, getUserRole, isAdmin, isManager, isEmployee, isUser, isOwner)
    - Implement users collection rules (read own document, prevent role modification)
    - Implement tickets collection rules (role-based read/write access, status transition validation)
    - Implement payments subcollection rules
    - Implement branches collection rules
    - Implement employees subcollection rules
    - Implement notifications collection rules
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10, 9.7, 9.8, 12.8_

  - [x] 9.2 Write unit tests for Security Rules using Firebase Emulator
    - Test user document access control
    - Test ticket access by role (admin, manager, employee, user)
    - Test status transition validation
    - Test payment access control
    - Test branch and employee access
    - _Requirements: 19.2, 19.8_

- [ ] 10. Implement Cloud Storage Security Rules
  - [~] 10.1 Create storage.rules file
    - Implement helper functions (isAuthenticated, isAdmin, isValidImage)
    - Implement ticket-images path rules with 10MB size limit
    - Implement profile-photos path rules
    - _Requirements: 5.5, 5.6, 5.8, 5.9_

  - [x] 10.2 Write tests for Storage Rules
    - Test image upload size limits
    - Test file type validation
    - Test access control for ticket images
    - Test access control for profile photos
    - _Requirements: 19.2_

- [ ] 11. Implement Firebase Cloud Messaging integration
  - [~] 11.1 Create FCMManager class
    - Implement getToken and registerToken methods
    - Implement handleNotification method
    - Implement showNotification with ticket navigation
    - Implement topic subscription methods
    - _Requirements: 10.1, 10.2, 10.5, 10.6, 10.7_

  - [~] 11.2 Create MyFirebaseMessagingService
    - Implement onMessageReceived to handle notification payload
    - Implement onNewToken to update token in Firestore
    - Handle notification tap to navigate to ticket detail
    - _Requirements: 10.1, 10.2, 10.6, 10.8, 10.9_

  - [~] 11.3 Update AndroidManifest.xml for FCM
    - Register MyFirebaseMessagingService
    - Add FCM permissions
    - _Requirements: 10.1_

  - [x] 11.4 Write tests for FCM integration
    - Test token registration
    - Test notification handling
    - Test notification navigation
    - _Requirements: 19.4_

- [ ] 12. Configure Firestore indexes
  - [~] 12.1 Create firestore.indexes.json file
    - Define compound index for (customerId, status, createdAt)
    - Define compound index for (branchId, status, createdAt)
    - Define compound index for (assignedEmployeeId, status, scheduledDate)
    - Define compound index for (assignedEmployeeId, scheduledDate)
    - _Requirements: 3.10, 14.9, 16.7, 16.8, 16.9_

  - [~] 12.2 Deploy indexes to Firebase
    - Run firebase deploy --only firestore:indexes
    - _Requirements: 3.10, 20.9_

- [~] 13. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 14. Migrate existing activities to use Firebase SDK
  - [~] 14.1 Update authentication activities
    - Refactor MainActivity to use FirebaseAuthManager instead of ApiClient
    - Refactor RegisterActivity to use FirebaseAuthManager
    - Update SplashActivity to check Firebase auth state
    - _Requirements: 1.9, 2.5_

  - [~] 14.2 Update admin dashboard activities
    - Refactor AdminDashboardActivity to use getDashboardStats Cloud Function
    - Update AdminEmployeesFragment to use BranchRepository
    - Update AdminBranchesFragment to use BranchRepository
    - Update AdminAddEmployee and AdminAddManager to use createUserAccount Cloud Function
    - _Requirements: 8.6, 15.1, 15.2, 15.3_

  - [~] 14.3 Update manager dashboard activities
    - Refactor manager dashboard to use getDashboardStats with branch filter
    - Update ticket list to use TicketRepository with real-time listeners
    - Update employee management to use BranchRepository
    - _Requirements: 6.1, 6.2, 6.10, 8.5_

  - [~] 14.4 Update employee activities
    - Refactor employee ticket list to use TicketRepository with real-time listeners
    - Update employee schedule view to query by scheduledDate
    - Update ticket status updates to use TicketRepository
    - _Requirements: 6.1, 6.2, 6.3, 14.2, 14.3, 14.6, 14.7, 14.8_

  - [~] 14.5 Update user/customer activities
    - Refactor ticket creation to use TicketRepository and assignTicketToBranch function
    - Update ticket list to use real-time listeners
    - Update payment confirmation to use PaymentRepository
    - _Requirements: 6.1, 6.2, 6.4, 7.6, 9.4, 9.5_

  - [~] 14.6 Update photo upload functionality
    - Replace existing photo upload with StorageManager
    - Implement image compression before upload
    - Show upload progress in UI
    - Store download URLs in ticket documents
    - _Requirements: 5.3, 5.4, 5.10_

- [ ] 15. Implement offline data persistence
  - [~] 15.1 Enable Firestore offline persistence in Application class
    - Configure cache size limit (100MB)
    - _Requirements: 13.1, 13.8_

  - [~] 15.2 Update UI to show offline/online status
    - Add network connectivity listener
    - Display visual indicator when offline
    - Show cache vs server data indicators
    - _Requirements: 13.5, 13.10_

  - [~] 15.3 Handle offline write operations
    - Ensure UI handles queued writes gracefully
    - Show pending status for offline operations
    - _Requirements: 13.3, 13.4, 13.6_

- [ ] 16. Implement search and filter functionality
  - [~] 16.1 Add ticket filtering UI components
    - Add status filter dropdown
    - Add service type filter
    - Add date range picker
    - _Requirements: 16.1, 16.2, 16.3_

  - [~] 16.2 Implement filter queries in TicketRepository
    - Implement combined filter queries using compound indexes
    - Apply role-based filtering (manager by branch, employee by assignment, user by ownership)
    - _Requirements: 16.4, 16.5, 16.6, 16.10_

- [ ] 17. Implement error handling and logging
  - [~] 17.1 Add Firebase Crashlytics integration
    - Add Crashlytics dependency to build.gradle.kts
    - Initialize Crashlytics in Application class
    - _Requirements: 17.3, 17.10_

  - [~] 17.2 Implement error handling in repositories
    - Add try-catch blocks for Firebase operations
    - Display user-friendly error messages
    - Log errors to Crashlytics
    - Implement retry logic for transient failures
    - _Requirements: 17.1, 17.2, 17.6, 17.7, 17.8_

  - [~] 17.3 Add structured logging to Cloud Functions
    - Use console.log with severity levels (INFO, WARNING, ERROR)
    - Log function invocations with parameters
    - Log errors with stack traces
    - _Requirements: 17.4, 17.5, 17.9_

- [ ] 18. Implement performance optimizations
  - [~] 18.1 Optimize list rendering
    - Ensure RecyclerView with ViewHolder pattern is used
    - Implement lazy loading for images using Glide/Picasso
    - Implement pagination for ticket lists
    - _Requirements: 18.2, 18.3, 18.7, 18.10_

  - [~] 18.2 Optimize Cloud Functions
    - Set appropriate memory allocation (256MB minimum)
    - Implement connection pooling for Firestore Admin SDK
    - _Requirements: 18.5, 18.6_

  - [~] 18.3 Implement proper listener lifecycle management
    - Detach Firestore listeners in onDestroy/onStop
    - Prevent memory leaks from listeners
    - _Requirements: 6.7, 18.9_

- [ ] 19. Create data migration script
  - [~] 19.1 Create Node.js migration script
    - Set up script to connect to MySQL database
    - Set up Firebase Admin SDK connection
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5_

  - [~] 19.2 Implement user data migration
    - Export users from MySQL
    - Create Firebase Auth accounts using email
    - Create Firestore user documents
    - Map MySQL user IDs to Firebase UIDs
    - _Requirements: 11.1, 11.6_

  - [~] 19.3 Implement ticket data migration
    - Export tickets from MySQL
    - Convert MySQL datetime to Firestore Timestamp
    - Map foreign keys to Firebase UIDs/document IDs
    - Use batch writes for performance
    - _Requirements: 11.2, 11.7, 11.8_

  - [~] 19.4 Implement branch and employee data migration
    - Export branches from MySQL
    - Export employees and create in branch subcollections
    - _Requirements: 11.3, 11.4_

  - [~] 19.5 Implement payment data migration
    - Export payments from MySQL
    - Create payment documents in ticket subcollections
    - _Requirements: 11.5, 11.7_

  - [~] 19.6 Add error handling and reporting to migration script
    - Log errors and continue with remaining records
    - Generate migration report with success/failure counts
    - _Requirements: 11.9, 11.10_

  - [x] 19.7 Test migration script with sample data
    - Run migration on test dataset
    - Verify data integrity in Firestore
    - Validate all relationships are preserved
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5_

- [~] 20. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 21. Create documentation
  - [~] 21.1 Create Firestore data model diagram
    - Document all collections, subcollections, and fields
    - Show relationships and denormalized data
    - _Requirements: 20.1_

  - [~] 21.2 Document Security Rules
    - Add inline comments to firestore.rules explaining each rule
    - Add inline comments to storage.rules
    - _Requirements: 20.2_

  - [~] 21.3 Create deployment guide
    - Document Cloud Functions deployment steps
    - Document Firebase project setup
    - Document index deployment
    - _Requirements: 20.3, 20.4, 20.9_

  - [~] 21.4 Create migration guide
    - Document data migration process
    - Create Laravel to Firebase endpoint comparison table
    - Document rollback procedures
    - _Requirements: 20.5, 20.6, 20.10_

  - [~] 21.5 Create troubleshooting guide
    - Document common Firebase errors and solutions
    - Document cost estimation for Firebase services
    - _Requirements: 20.7, 20.8_

- [ ] 22. Final integration testing
  - [-] 22.1 Test complete user flows
    - Test user registration and authentication
    - Test ticket creation with photo upload
    - Test ticket assignment and routing
    - Test employee schedule management
    - Test payment processing
    - Test push notifications
    - _Requirements: 19.4, 19.5_

  - [~] 22.2 Test role-based access control
    - Verify admin can access all data
    - Verify manager can only access their branch
    - Verify employee can only access assigned tickets
    - Verify user can only access their own tickets
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 12.7, 12.8_

  - [~] 22.3 Test offline functionality
    - Test app behavior when offline
    - Test data sync when connectivity returns
    - Test queued writes
    - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.6_

  - [~] 22.4 Test real-time updates
    - Verify ticket status updates appear in real-time
    - Verify new tickets appear automatically
    - Verify deleted tickets are removed from lists
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ] 23. Deploy to production
  - [~] 23.1 Deploy Cloud Functions
    - Run firebase deploy --only functions
    - Verify all functions are deployed successfully
    - _Requirements: 20.3_

  - [~] 23.2 Deploy Security Rules
    - Run firebase deploy --only firestore:rules
    - Run firebase deploy --only storage:rules
    - _Requirements: 20.3_

  - [~] 23.3 Deploy Firestore indexes
    - Run firebase deploy --only firestore:indexes
    - Wait for index creation to complete
    - _Requirements: 20.9_

  - [~] 23.4 Run data migration
    - Execute migration script against production MySQL
    - Monitor migration progress
    - Verify migration report
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.9, 11.10_

  - [~] 23.5 Build and release Android app
    - Build release APK/AAB with Firebase SDK
    - Test on production Firebase project
    - Deploy to Google Play Store
    - _Requirements: 2.8_

- [~] 24. Final checkpoint - Verify production deployment
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional testing tasks and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation at key milestones
- The migration maintains all existing functionality while adding real-time capabilities
- Firebase SDK replaces all Retrofit HTTP client code
- Cloud Functions handle server-side business logic previously in Laravel controllers
- Security Rules enforce access control at the database level
- Data migration preserves all historical data from MySQL
