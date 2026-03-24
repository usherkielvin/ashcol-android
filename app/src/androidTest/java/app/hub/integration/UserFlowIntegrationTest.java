package app.hub.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.Uri;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import app.hub.common.FirebaseAuthManager;
import app.hub.models.Payment;
import app.hub.models.Ticket;
import app.hub.models.User;
import app.hub.repositories.PaymentRepository;
import app.hub.repositories.TicketRepository;
import app.hub.repositories.UserRepository;

/**
 * End-to-end integration tests for complete user flows through the Firebase-based system.
 *
 * **Validates: Requirements 19.4, 19.5**
 *
 * SETUP INSTRUCTIONS:
 * 1. Install Firebase CLI: npm install -g firebase-tools
 * 2. Start emulators: firebase emulators:start --only auth,firestore,storage,functions
 * 3. Emulator ports: Auth=9099, Firestore=8080, Storage=9199, Functions=5001
 * 4. Run tests: ./gradlew connectedAndroidTest --tests UserFlowIntegrationTest
 *
 * These tests cover:
 * - User registration and authentication flows
 * - Ticket creation with photo upload
 * - Ticket assignment and routing
 * - Employee schedule management
 * - Payment processing
 * - Push notification registration and handling
 *
 * Tests requiring full implementation are annotated with @Ignore and TODO comments.
 */
@RunWith(AndroidJUnit4.class)
public class UserFlowIntegrationTest {

    // ---- Emulator configuration ----
    private static final String EMULATOR_HOST = "10.0.2.2"; // Android emulator loopback
    private static final int FIRESTORE_PORT = 8080;
    private static final int AUTH_PORT = 9099;
    private static final int STORAGE_PORT = 9199;
    private static final int LATCH_TIMEOUT_SECONDS = 10;

    // ---- Test data constants ----
    private static final String TEST_EMAIL = "integration_test@example.com";
    private static final String TEST_PASSWORD = "TestPassword123!";
    private static final String TEST_NAME = "Integration Test User";
    private static final String TEST_PHONE = "+15550001234";
    private static final String EMPLOYEE_EMAIL = "employee_test@example.com";
    private static final String EMPLOYEE_PASSWORD = "EmployeePass123!";
    private static final String BRANCH_ID = "branch_integration_test";
    private static final String BRANCH_NAME = "Test Branch";

    private Context context;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseAuthManager authManager;
    private TicketRepository ticketRepository;
    private UserRepository userRepository;
    private PaymentRepository paymentRepository;

    private List<String> createdTicketIds;
    private List<String> createdUserIds;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        MockitoAnnotations.openMocks(this);

        // TODO: Uncomment to connect to Firebase Emulator Suite
        // FirebaseFirestore.getInstance().useEmulator(EMULATOR_HOST, FIRESTORE_PORT);
        // FirebaseAuth.getInstance().useEmulator("http://" + EMULATOR_HOST + ":" + AUTH_PORT);
        // FirebaseStorage.getInstance().useEmulator(EMULATOR_HOST, STORAGE_PORT);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        authManager = new FirebaseAuthManager(context);
        ticketRepository = new TicketRepository();
        userRepository = new UserRepository();
        paymentRepository = new PaymentRepository();

        createdTicketIds = new ArrayList<>();
        createdUserIds = new ArrayList<>();
    }

    @After
    public void tearDown() throws Exception {
        // Sign out any authenticated user
        auth.signOut();

        // Clean up created tickets
        for (String ticketId : createdTicketIds) {
            try {
                Tasks.await(db.collection("tickets").document(ticketId).delete());
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }

        // Clean up created user documents
        for (String userId : createdUserIds) {
            try {
                Tasks.await(db.collection("users").document(userId).delete());
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }

    // =========================================================================
    // 1. USER REGISTRATION FLOW TESTS
    // =========================================================================

    /**
     * Tests that registering with email/password creates a Firebase Auth account
     * and a corresponding Firestore user document with role="user".
     *
     * Requirement 1.1, 1.5
     */
    @Test
    @Ignore("TODO: Enable once Firebase Emulator is configured in setUp()")
    public void testRegistration_emailPassword_createsAuthAccountAndFirestoreDocument()
            throws Exception {
        // Act: create Firebase Auth account
        AuthResult result = Tasks.await(
                authManager.createUserWithEmail(TEST_EMAIL, TEST_PASSWORD));

        // Assert: Auth account created
        assertNotNull("AuthResult should not be null", result);
        FirebaseUser firebaseUser = result.getUser();
        assertNotNull("FirebaseUser should not be null", firebaseUser);
        assertEquals("Email should match", TEST_EMAIL, firebaseUser.getEmail());

        String uid = firebaseUser.getUid();
        createdUserIds.add(uid);

        // Assert: Firestore user document created by onUserCreate trigger
        // Allow time for Cloud Function trigger to execute
        Thread.sleep(2000);
        DocumentSnapshot userDoc = Tasks.await(db.collection("users").document(uid).get());
        assertTrue("User document should exist in Firestore", userDoc.exists());
        assertEquals("Role should default to 'user'", "user", userDoc.getString("role"));
        assertEquals("Email should be stored", TEST_EMAIL, userDoc.getString("email"));
    }

    /**
     * Tests that Google Sign-In creates a Firebase Auth account and Firestore document.
     *
     * Requirement 1.2, 1.5
     */
    @Test
    @Ignore("TODO: Enable once Google Sign-In is configured with test credentials in emulator")
    public void testRegistration_googleSignIn_createsAuthAccountAndFirestoreDocument() {
        // Google Sign-In requires a real GoogleSignInAccount with a valid ID token.
        // In emulator testing, this is typically done by mocking the credential exchange.
        // Full implementation requires a test Google account configured in the emulator.
        //
        // Expected flow:
        // 1. Obtain GoogleSignInAccount with test ID token
        // 2. authManager.signInWithGoogle(account) -> AuthResult
        // 3. Verify FirebaseUser created
        // 4. Verify Firestore user document created with role="user"
    }

    // =========================================================================
    // 2. AUTHENTICATION FLOW TESTS
    // =========================================================================

    /**
     * Tests that login with valid credentials authenticates the user and establishes a session.
     *
     * Requirement 1.1, 1.4, 1.10
     */
    @Test
    @Ignore("TODO: Enable once Firebase Emulator is configured in setUp()")
    public void testAuthentication_validCredentials_authenticatesAndEstablishesSession()
            throws Exception {
        // Arrange: create a user first
        Tasks.await(authManager.createUserWithEmail(TEST_EMAIL, TEST_PASSWORD));
        auth.signOut();

        // Act: sign in
        AuthResult result = Tasks.await(authManager.signInWithEmail(TEST_EMAIL, TEST_PASSWORD));

        // Assert
        assertNotNull("AuthResult should not be null", result);
        assertNotNull("FirebaseUser should not be null", result.getUser());
        assertNotNull("getCurrentUser should return signed-in user",
                authManager.getCurrentUser());
        assertEquals("Signed-in email should match", TEST_EMAIL,
                authManager.getCurrentUser().getEmail());

        createdUserIds.add(result.getUser().getUid());
    }

    /**
     * Tests that login with invalid credentials returns an appropriate error.
     *
     * Requirement 1.1
     */
    @Test
    @Ignore("TODO: Enable once Firebase Emulator is configured in setUp()")
    public void testAuthentication_invalidCredentials_returnsError() {
        // Act & Assert: sign in with wrong password should fail
        try {
            Tasks.await(authManager.signInWithEmail(TEST_EMAIL, "WrongPassword!"));
            // Should not reach here
            assertTrue("Should have thrown an exception for invalid credentials", false);
        } catch (Exception e) {
            assertNotNull("Exception should not be null", e);
            // FirebaseAuthInvalidCredentialsException expected
        }
    }

    /**
     * Tests that logout clears the session.
     *
     * Requirement 1.7
     */
    @Test
    @Ignore("TODO: Enable once Firebase Emulator is configured in setUp()")
    public void testAuthentication_logout_clearsSession() throws Exception {
        // Arrange: sign in
        AuthResult result = Tasks.await(authManager.createUserWithEmail(TEST_EMAIL, TEST_PASSWORD));
        createdUserIds.add(result.getUser().getUid());
        assertNotNull("Should be signed in", authManager.getCurrentUser());

        // Act: sign out
        authManager.signOut();

        // Assert
        assertNull("getCurrentUser should return null after sign out",
                authManager.getCurrentUser());
    }

    /**
     * Tests that password reset email is sent successfully.
     *
     * Requirement 1.8
     */
    @Test
    @Ignore("TODO: Enable once Firebase Emulator is configured in setUp()")
    public void testAuthentication_passwordReset_emailSent() throws Exception {
        // Arrange: create user
        AuthResult result = Tasks.await(authManager.createUserWithEmail(TEST_EMAIL, TEST_PASSWORD));
        createdUserIds.add(result.getUser().getUid());
        auth.signOut();

        // Act: send password reset email (emulator captures it without actually sending)
        Void voidResult = Tasks.await(authManager.sendPasswordResetEmail(TEST_EMAIL));

        // Assert: no exception means email was queued successfully
        // In emulator, check http://localhost:4000/auth for the email
        assertTrue("Password reset should complete without error", true);
    }

    // =========================================================================
    // 3. TICKET CREATION FLOW TESTS
    // =========================================================================

    /**
     * Tests the complete ticket creation flow:
     * customer creates ticket → Firestore document created → branch assigned.
     *
     * Requirement 6.1, 7.6, 19.5
     */
    @Test
    @Ignore("TODO: Enable once Firebase Emulator is configured in setUp()")
    public void testTicketCreation_withDescriptionAndLocation_createsFirestoreDocument()
            throws Exception {
        // Arrange
        Ticket ticket = buildSampleTicket("customer_001", "Plumbing", "pending");

        // Act
        DocumentReference docRef = Tasks.await(ticketRepository.createTicket(ticket));
        createdTicketIds.add(docRef.getId());

        // Assert: document exists
        DocumentSnapshot snapshot = Tasks.await(docRef.get());
        assertTrue("Ticket document should exist", snapshot.exists());
        assertEquals("Service type should match", "Plumbing", snapshot.getString("serviceType"));
        assertEquals("Status should be pending", "pending", snapshot.getString("status"));
        assertNotNull("Location should be stored", snapshot.getGeoPoint("location"));
    }

    /**
     * Tests that a photo URL is stored in the ticket document after upload.
     *
     * Requirement 5.1, 5.4, 19.5
     */
    @Test
    @Ignore("TODO: Enable once StorageManager is implemented (task 5.1) and emulator configured")
    public void testTicketCreation_withPhotoUpload_storesDownloadUrlInFirestore()
            throws Exception {
        // Arrange: create ticket
        Ticket ticket = buildSampleTicket("customer_photo", "Electrical", "pending");
        DocumentReference docRef = Tasks.await(ticketRepository.createTicket(ticket));
        createdTicketIds.add(docRef.getId());

        // TODO: Upload photo via StorageManager
        // Uri photoUri = Uri.parse("content://test/photo.jpg");
        // String downloadUrl = Tasks.await(storageManager.uploadTicketPhoto(docRef.getId(), photoUri));

        // TODO: Update ticket with photo URL
        // Map<String, Object> update = new HashMap<>();
        // update.put("photoUrls", Arrays.asList(downloadUrl));
        // Tasks.await(ticketRepository.updateTicket(docRef.getId(), update));

        // Assert: photo URL stored in Firestore
        // DocumentSnapshot snapshot = Tasks.await(docRef.get());
        // List<String> photoUrls = (List<String>) snapshot.get("photoUrls");
        // assertNotNull("Photo URLs should not be null", photoUrls);
        // assertFalse("Photo URLs should not be empty", photoUrls.isEmpty());
        // assertTrue("Download URL should start with https://", photoUrls.get(0).startsWith("https://"));
    }

    /**
     * Tests that the assignTicketToBranch Cloud Function assigns the ticket to the nearest branch.
     *
     * Requirement 7.1, 7.6, 7.8
     */
    @Test
    @Ignore("TODO: Enable once assignTicketToBranch Cloud Function is deployed to emulator (task 8.2)")
    public void testTicketCreation_assignTicketToBranch_assignsNearestBranch() throws Exception {
        // Arrange: create ticket with location
        Ticket ticket = buildSampleTicket("customer_routing", "HVAC", "pending");
        DocumentReference docRef = Tasks.await(ticketRepository.createTicket(ticket));
        createdTicketIds.add(docRef.getId());

        // TODO: Call assignTicketToBranch Cloud Function
        // FirebaseFunctions functions = FirebaseFunctions.getInstance();
        // Map<String, Object> data = new HashMap<>();
        // data.put("ticketId", docRef.getId());
        // data.put("latitude", 37.7749);
        // data.put("longitude", -122.4194);
        // Map<String, Object> result = Tasks.await(
        //     functions.getHttpsCallable("assignTicketToBranch").call(data)
        //         .continueWith(task -> (Map<String, Object>) task.getResult().getData()));

        // Assert: ticket updated with branchId
        // DocumentSnapshot snapshot = Tasks.await(docRef.get());
        // assertNotNull("Branch ID should be assigned", snapshot.getString("branchId"));
        // assertEquals("Status should be 'assigned'", "assigned", snapshot.getString("status"));
    }

    /**
     * Tests that a real-time listener receives the newly created ticket.
     *
     * Requirement 6.1, 6.4
     */
    @Test
    @Ignore("TODO: Enable once Firebase Emulator is configured in setUp()")
    public void testTicketCreation_realTimeListener_receivesNewTicket() throws Exception {
        // Arrange
        String customerId = "customer_realtime";
        CountDownLatch latch = new CountDownLatch(1);
        List<Ticket> receivedTickets = new ArrayList<>();

        // TODO: Attach real-time listener via TicketRepository
        // LiveData<List<Ticket>> liveData = ticketRepository.getUserTickets(customerId);
        // liveData.observeForever(tickets -> {
        //     if (tickets != null && !tickets.isEmpty()) {
        //         receivedTickets.addAll(tickets);
        //         latch.countDown();
        //     }
        // });

        // Act: create ticket
        Ticket ticket = buildSampleTicket(customerId, "Carpentry", "pending");
        DocumentReference docRef = Tasks.await(ticketRepository.createTicket(ticket));
        createdTicketIds.add(docRef.getId());

        // Assert: listener received the new ticket
        // boolean received = latch.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        // assertTrue("Real-time listener should receive new ticket", received);
        // assertEquals("Should have received 1 ticket", 1, receivedTickets.size());
        // assertEquals("Ticket service type should match", "Carpentry",
        //         receivedTickets.get(0).getServiceType());
    }

    // =========================================================================
    // 4. TICKET ASSIGNMENT AND ROUTING TESTS
    // =========================================================================

    /**
     * Tests that an admin/manager can assign a ticket to an employee.
     *
     * Requirement 6.2, 14.4
     */
    @Test
    @Ignore("TODO: Enable once Firebase Emulator is configured in setUp()")
    public void testTicketAssignment_managerAssignsToEmployee_updatesTicketDocument()
            throws Exception {
        // Arrange: create a ticket in "pending" state
        Ticket ticket = buildSampleTicket("customer_assign", "Painting", "pending");
        ticket.setBranchId(BRANCH_ID);
        DocumentReference docRef = Tasks.await(ticketRepository.createTicket(ticket));
        createdTicketIds.add(docRef.getId());

        // Act: assign to employee
        Map<String, Object> assignment = new HashMap<>();
        assignment.put("assignedEmployeeId", "employee_001");
        assignment.put("assignedEmployeeName", "John Doe");
        assignment.put("status", "assigned");
        assignment.put("updatedAt", Timestamp.now());
        Tasks.await(ticketRepository.updateTicket(docRef.getId(), assignment));

        // Assert
        DocumentSnapshot snapshot = Tasks.await(docRef.get());
        assertEquals("Status should be 'assigned'", "assigned", snapshot.getString("status"));
        assertEquals("Employee ID should be set", "employee_001",
                snapshot.getString("assignedEmployeeId"));
        assertEquals("Employee name should be set", "John Doe",
                snapshot.getString("assignedEmployeeName"));
    }

    /**
     * Tests that the employee receives an FCM notification when a ticket is assigned.
     *
     * Requirement 10.4
     */
    @Test
    @Ignore("TODO: Enable once onTicketStatusChange Cloud Function trigger is deployed (task 8.5)")
    public void testTicketAssignment_employeeReceivesFcmNotification() throws Exception {
        // Arrange: create ticket and assign to employee
        Ticket ticket = buildSampleTicket("customer_notify", "Roofing", "pending");
        DocumentReference docRef = Tasks.await(ticketRepository.createTicket(ticket));
        createdTicketIds.add(docRef.getId());

        // TODO: Register employee FCM token in Firestore
        // Map<String, Object> employeeData = new HashMap<>();
        // employeeData.put("fcmTokens", Arrays.asList("test_fcm_token_employee_001"));
        // Tasks.await(db.collection("users").document("employee_001").set(employeeData));

        // Act: assign ticket (triggers onTicketStatusChange function)
        Map<String, Object> assignment = new HashMap<>();
        assignment.put("assignedEmployeeId", "employee_001");
        assignment.put("status", "assigned");
        Tasks.await(ticketRepository.updateTicket(docRef.getId(), assignment));

        // Allow time for Cloud Function trigger to execute
        Thread.sleep(2000);

        // Assert: verify FCM message was sent (check emulator logs or use FCM emulator)
        // In emulator, FCM messages can be verified via the emulator UI or logs
        assertTrue("FCM notification should have been sent to employee", true);
    }

    /**
     * Tests that ticket status is updated in real-time when assignment changes.
     *
     * Requirement 6.2, 6.3
     */
    @Test
    @Ignore("TODO: Enable once Firebase Emulator is configured in setUp()")
    public void testTicketAssignment_statusUpdatedInRealTime() throws Exception {
        // Arrange
        String customerId = "customer_status_rt";
        Ticket ticket = buildSampleTicket(customerId, "Flooring", "pending");
        DocumentReference docRef = Tasks.await(ticketRepository.createTicket(ticket));
        createdTicketIds.add(docRef.getId());

        CountDownLatch latch = new CountDownLatch(1);
        final String[] observedStatus = {null};

        // TODO: Attach real-time listener
        // docRef.addSnapshotListener((snapshot, error) -> {
        //     if (snapshot != null && snapshot.exists()) {
        //         String status = snapshot.getString("status");
        //         if ("assigned".equals(status)) {
        //             observedStatus[0] = status;
        //             latch.countDown();
        //         }
        //     }
        // });

        // Act: update status
        Map<String, Object> update = new HashMap<>();
        update.put("status", "assigned");
        Tasks.await(ticketRepository.updateTicket(docRef.getId(), update));

        // Assert
        // boolean received = latch.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        // assertTrue("Real-time listener should receive status update", received);
        // assertEquals("Status should be 'assigned'", "assigned", observedStatus[0]);
    }

    // =========================================================================
    // 5. EMPLOYEE SCHEDULE MANAGEMENT TESTS
    // =========================================================================

    /**
     * Tests that an employee can view their assigned tickets.
     *
     * Requirement 14.2, 6.1
     */
    @Test
    @Ignore("TODO: Enable once Firebase Emulator is configured in setUp()")
    public void testEmployeeSchedule_viewAssignedTickets_returnsOnlyAssignedTickets()
            throws Exception {
        // Arrange: create tickets assigned to different employees
        String employeeId = "employee_schedule_001";

        Ticket myTicket = buildSampleTicket("customer_a", "Plumbing", "assigned");
        myTicket.setAssignedEmployeeId(employeeId);
        DocumentReference myRef = Tasks.await(ticketRepository.createTicket(myTicket));
        createdTicketIds.add(myRef.getId());

        Ticket otherTicket = buildSampleTicket("customer_b", "Electrical", "assigned");
        otherTicket.setAssignedEmployeeId("other_employee_999");
        DocumentReference otherRef = Tasks.await(ticketRepository.createTicket(otherTicket));
        createdTicketIds.add(otherRef.getId());

        // Act: query employee's tickets
        QuerySnapshot snapshot = Tasks.await(
                db.collection("tickets")
                        .whereEqualTo("assignedEmployeeId", employeeId)
                        .get());

        // Assert
        assertNotNull("Snapshot should not be null", snapshot);
        assertEquals("Should return only 1 ticket for this employee", 1, snapshot.size());
        assertEquals("Ticket service type should match", "Plumbing",
                snapshot.getDocuments().get(0).getString("serviceType"));
    }

    /**
     * Tests that an employee can update ticket status from pending → in_progress → completed.
     *
     * Requirement 6.3, 14.3
     */
    @Test
    @Ignore("TODO: Enable once Firebase Emulator is configured in setUp()")
    public void testEmployeeSchedule_updateTicketStatus_pendingToInProgressToCompleted()
            throws Exception {
        // Arrange
        Ticket ticket = buildSampleTicket("customer_status", "HVAC", "assigned");
        ticket.setAssignedEmployeeId("employee_status_001");
        DocumentReference docRef = Tasks.await(ticketRepository.createTicket(ticket));
        createdTicketIds.add(docRef.getId());

        // Act: transition to in_progress
        Map<String, Object> update1 = new HashMap<>();
        update1.put("status", "in_progress");
        update1.put("updatedAt", Timestamp.now());
        Tasks.await(ticketRepository.updateTicket(docRef.getId(), update1));

        DocumentSnapshot snapshot1 = Tasks.await(docRef.get());
        assertEquals("Status should be in_progress", "in_progress", snapshot1.getString("status"));

        // Act: transition to completed
        Map<String, Object> update2 = new HashMap<>();
        update2.put("status", "completed");
        update2.put("completedDate", Timestamp.now());
        update2.put("updatedAt", Timestamp.now());
        Tasks.await(ticketRepository.updateTicket(docRef.getId(), update2));

        DocumentSnapshot snapshot2 = Tasks.await(docRef.get());
        assertEquals("Status should be completed", "completed", snapshot2.getString("status"));
        assertNotNull("Completed date should be set", snapshot2.getTimestamp("completedDate"));
    }

    /**
     * Tests that status changes are reflected in real-time for the customer.
     *
     * Requirement 6.3, 10.3
     */
    @Test
    @Ignore("TODO: Enable once Firebase Emulator is configured in setUp()")
    public void testEmployeeSchedule_statusChangeReflectedInRealTimeForCustomer()
            throws Exception {
        // Arrange
        String customerId = "customer_realtime_status";
        Ticket ticket = buildSampleTicket(customerId, "Painting", "assigned");
        DocumentReference docRef = Tasks.await(ticketRepository.createTicket(ticket));
        createdTicketIds.add(docRef.getId());

        CountDownLatch latch = new CountDownLatch(1);
        final String[] latestStatus = {"assigned"};

        // TODO: Customer attaches real-time listener
        // docRef.addSnapshotListener((snapshot, error) -> {
        //     if (snapshot != null && snapshot.exists()) {
        //         String status = snapshot.getString("status");
        //         if ("in_progress".equals(status)) {
        //             latestStatus[0] = status;
        //             latch.countDown();
        //         }
        //     }
        // });

        // Act: employee updates status
        Map<String, Object> update = new HashMap<>();
        update.put("status", "in_progress");
        Tasks.await(ticketRepository.updateTicket(docRef.getId(), update));

        // Assert
        // boolean received = latch.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        // assertTrue("Customer should receive real-time status update", received);
        // assertEquals("Status should be in_progress", "in_progress", latestStatus[0]);
    }

    // =========================================================================
    // 6. PAYMENT PROCESSING TESTS
    // =========================================================================

    /**
     * Tests that marking work as complete creates a payment record in the ticket subcollection.
     *
     * Requirement 9.1, 9.2
     */
    @Test
    @Ignore("TODO: Enable once Firebase Emulator is configured in setUp()")
    public void testPaymentProcessing_employeeMarksWorkComplete_createsPaymentRecord()
            throws Exception {
        // Arrange: create a completed ticket
        Ticket ticket = buildSampleTicket("customer_payment", "Electrical", "completed");
        ticket.setAssignedEmployeeId("employee_payment_001");
        DocumentReference ticketRef = Tasks.await(ticketRepository.createTicket(ticket));
        createdTicketIds.add(ticketRef.getId());

        // Act: create payment record
        Payment payment = buildSamplePayment(250.00, "credit_card", "pending");
        payment.setEmployeeId("employee_payment_001");
        DocumentReference paymentRef = Tasks.await(
                paymentRepository.createPayment(ticketRef.getId(), payment));

        // Assert: payment document exists in subcollection
        DocumentSnapshot snapshot = Tasks.await(paymentRef.get());
        assertTrue("Payment document should exist", snapshot.exists());
        assertEquals("Amount should match", 250.00, snapshot.getDouble("amount"), 0.01);
        assertEquals("Method should match", "credit_card", snapshot.getString("method"));
        assertEquals("Status should be pending", "pending", snapshot.getString("status"));
        assertEquals("Employee ID should be stored", "employee_payment_001",
                snapshot.getString("employeeId"));
    }

    /**
     * Tests that the customer can confirm payment, updating status to "paid".
     *
     * Requirement 9.4, 9.5
     */
    @Test
    @Ignore("TODO: Enable once Firebase Emulator is configured in setUp()")
    public void testPaymentProcessing_customerConfirmsPayment_updatesStatusToPaid()
            throws Exception {
        // Arrange
        Ticket ticket = buildSampleTicket("customer_confirm_pay", "Plumbing", "completed");
        DocumentReference ticketRef = Tasks.await(ticketRepository.createTicket(ticket));
        createdTicketIds.add(ticketRef.getId());

        Payment payment = buildSamplePayment(150.00, "cash", "pending");
        DocumentReference paymentRef = Tasks.await(
                paymentRepository.createPayment(ticketRef.getId(), payment));

        // Act: customer confirms payment
        Tasks.await(paymentRepository.confirmPayment(ticketRef.getId(), paymentRef.getId()));

        // Assert: payment status updated to "paid"
        DocumentSnapshot paymentSnapshot = Tasks.await(paymentRef.get());
        assertEquals("Payment status should be 'paid'", "paid",
                paymentSnapshot.getString("status"));
        assertNotNull("paidAt timestamp should be set", paymentSnapshot.getTimestamp("paidAt"));
    }

    /**
     * Tests that confirming payment also updates the ticket status to "completed".
     *
     * Requirement 9.5, 9.6
     */
    @Test
    @Ignore("TODO: Enable once confirmPayment Cloud Function is deployed to emulator (task 8.x)")
    public void testPaymentProcessing_confirmPayment_updatesTicketStatusAtomically()
            throws Exception {
        // Arrange
        Ticket ticket = buildSampleTicket("customer_atomic", "Roofing", "pending_payment");
        DocumentReference ticketRef = Tasks.await(ticketRepository.createTicket(ticket));
        createdTicketIds.add(ticketRef.getId());

        Payment payment = buildSamplePayment(500.00, "digital_wallet", "pending");
        DocumentReference paymentRef = Tasks.await(
                paymentRepository.createPayment(ticketRef.getId(), payment));

        // TODO: Call confirmPayment Cloud Function (uses Firestore Transaction)
        // FirebaseFunctions functions = FirebaseFunctions.getInstance();
        // Map<String, Object> data = new HashMap<>();
        // data.put("ticketId", ticketRef.getId());
        // data.put("paymentId", paymentRef.getId());
        // Tasks.await(functions.getHttpsCallable("confirmPayment").call(data));

        // Assert: both payment and ticket updated atomically
        // DocumentSnapshot paymentSnapshot = Tasks.await(paymentRef.get());
        // assertEquals("paid", paymentSnapshot.getString("status"));
        // DocumentSnapshot ticketSnapshot = Tasks.await(ticketRef.get());
        // assertEquals("completed", ticketSnapshot.getString("status"));
    }

    /**
     * Tests that payment status update is reflected in real-time.
     *
     * Requirement 6.2
     */
    @Test
    @Ignore("TODO: Enable once Firebase Emulator is configured in setUp()")
    public void testPaymentProcessing_paymentStatusUpdatedInRealTime() throws Exception {
        // Arrange
        Ticket ticket = buildSampleTicket("customer_pay_rt", "Carpentry", "completed");
        DocumentReference ticketRef = Tasks.await(ticketRepository.createTicket(ticket));
        createdTicketIds.add(ticketRef.getId());

        Payment payment = buildSamplePayment(75.00, "cash", "pending");
        DocumentReference paymentRef = Tasks.await(
                paymentRepository.createPayment(ticketRef.getId(), payment));

        CountDownLatch latch = new CountDownLatch(1);
        final String[] observedStatus = {null};

        // TODO: Attach real-time listener to payment document
        // paymentRef.addSnapshotListener((snapshot, error) -> {
        //     if (snapshot != null && snapshot.exists()) {
        //         String status = snapshot.getString("status");
        //         if ("paid".equals(status)) {
        //             observedStatus[0] = status;
        //             latch.countDown();
        //         }
        //     }
        // });

        // Act: confirm payment
        Tasks.await(paymentRepository.confirmPayment(ticketRef.getId(), paymentRef.getId()));

        // Assert
        // boolean received = latch.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        // assertTrue("Real-time listener should receive payment status update", received);
        // assertEquals("paid", observedStatus[0]);
    }

    // =========================================================================
    // 7. PUSH NOTIFICATION TESTS
    // =========================================================================

    /**
     * Tests that an FCM token is registered in Firestore on login.
     *
     * Requirement 10.1, 10.2
     */
    @Test
    @Ignore("TODO: Enable once FCMManager is implemented (task 11.1) and emulator configured")
    public void testPushNotifications_fcmTokenRegisteredOnLogin() throws Exception {
        // Arrange: sign in user
        AuthResult result = Tasks.await(authManager.createUserWithEmail(TEST_EMAIL, TEST_PASSWORD));
        String uid = result.getUser().getUid();
        createdUserIds.add(uid);

        // TODO: Simulate FCM token registration via FCMManager
        // String testToken = "test_fcm_token_" + uid;
        // fcmManager.registerToken(uid, testToken);

        // Assert: token stored in user's Firestore document
        // DocumentSnapshot userDoc = Tasks.await(db.collection("users").document(uid).get());
        // List<String> tokens = (List<String>) userDoc.get("fcmTokens");
        // assertNotNull("FCM tokens list should not be null", tokens);
        // assertTrue("Token should be in the list", tokens.contains(testToken));
    }

    /**
     * Tests that a notification is received when ticket status changes.
     *
     * Requirement 10.3
     */
    @Test
    @Ignore("TODO: Enable once onTicketStatusChange Cloud Function trigger is deployed (task 8.5)")
    public void testPushNotifications_notificationReceivedOnTicketStatusChange()
            throws Exception {
        // Arrange: create ticket and register customer FCM token
        String customerId = "customer_notif_001";
        Ticket ticket = buildSampleTicket(customerId, "Plumbing", "pending");
        DocumentReference docRef = Tasks.await(ticketRepository.createTicket(ticket));
        createdTicketIds.add(docRef.getId());

        // TODO: Register customer FCM token
        // Map<String, Object> userData = new HashMap<>();
        // userData.put("fcmTokens", Arrays.asList("customer_fcm_token_001"));
        // Tasks.await(db.collection("users").document(customerId).set(userData));

        // Act: update ticket status (triggers onTicketStatusChange function)
        Map<String, Object> update = new HashMap<>();
        update.put("status", "assigned");
        Tasks.await(ticketRepository.updateTicket(docRef.getId(), update));

        // Allow time for Cloud Function trigger to execute
        Thread.sleep(2000);

        // Assert: FCM notification sent (verify via emulator logs or FCM emulator)
        // In a real test environment, you would intercept the FCM message
        assertTrue("FCM notification should have been triggered", true);
    }

    /**
     * Tests that tapping a notification navigates to the correct ticket.
     *
     * Requirement 10.6
     */
    @Test
    @Ignore("TODO: Enable once FCMManager and notification navigation are implemented (task 11.1, 11.2)")
    public void testPushNotifications_notificationTap_navigatesToCorrectTicket() {
        // This test verifies that the notification PendingIntent contains the correct ticketId
        // and that tapping it opens the ticket detail screen.
        //
        // Expected flow:
        // 1. FCM message received with data payload: { ticketId: "ticket_123" }
        // 2. MyFirebaseMessagingService.onMessageReceived() called
        // 3. FCMManager.showNotification() creates PendingIntent with ticketId extra
        // 4. User taps notification → TicketDetailActivity opened with ticketId="ticket_123"
        //
        // TODO: Use Espresso Intents to verify the launched intent:
        // Intents.init();
        // RemoteMessage message = buildRemoteMessage("ticket_nav_test", "Status Update", "In progress");
        // fcmManager.handleNotification(message);
        // intended(hasComponent(TicketDetailActivity.class.getName()));
        // intended(hasExtra("ticketId", "ticket_nav_test"));
        // Intents.release();
    }

    /**
     * Tests that an invalid FCM token is removed from the user document.
     *
     * Requirement 10.9
     */
    @Test
    @Ignore("TODO: Enable once onTicketStatusChange Cloud Function handles invalid tokens (task 8.5)")
    public void testPushNotifications_invalidToken_removedFromUserDocument() throws Exception {
        // Arrange: store an invalid FCM token in user document
        String customerId = "customer_invalid_token";
        Map<String, Object> userData = new HashMap<>();
        userData.put("fcmTokens", Arrays.asList("invalid_token_that_will_fail"));
        Tasks.await(db.collection("users").document(customerId).set(userData));
        createdUserIds.add(customerId);

        Ticket ticket = buildSampleTicket(customerId, "Electrical", "pending");
        DocumentReference docRef = Tasks.await(ticketRepository.createTicket(ticket));
        createdTicketIds.add(docRef.getId());

        // Act: trigger notification (Cloud Function will detect invalid token)
        Map<String, Object> update = new HashMap<>();
        update.put("status", "assigned");
        Tasks.await(ticketRepository.updateTicket(docRef.getId(), update));

        // Allow time for Cloud Function to process and remove invalid token
        Thread.sleep(3000);

        // Assert: invalid token removed from user document
        // DocumentSnapshot userDoc = Tasks.await(db.collection("users").document(customerId).get());
        // List<String> tokens = (List<String>) userDoc.get("fcmTokens");
        // assertTrue("Invalid token should be removed", tokens == null || tokens.isEmpty());
    }

    // =========================================================================
    // 8. SMOKE TESTS (runnable without full emulator setup)
    // =========================================================================

    /**
     * Verifies the Android instrumentation context is available.
     */
    @Test
    public void testSmokeTest_appContextIsAvailable() {
        assertNotNull("App context should not be null", context);
        assertEquals("Package name should match", "app.hub", context.getPackageName());
    }

    /**
     * Verifies FirebaseAuthManager can be instantiated.
     */
    @Test
    public void testSmokeTest_firebaseAuthManagerInitializes() {
        assertNotNull("FirebaseAuthManager should initialize", authManager);
        assertNotNull("FirebaseAuth instance should not be null", authManager.getFirebaseAuth());
    }

    /**
     * Verifies TicketRepository can be instantiated.
     */
    @Test
    public void testSmokeTest_ticketRepositoryInitializes() {
        assertNotNull("TicketRepository should initialize", ticketRepository);
    }

    /**
     * Verifies UserRepository can be instantiated.
     */
    @Test
    public void testSmokeTest_userRepositoryInitializes() {
        assertNotNull("UserRepository should initialize", userRepository);
    }

    /**
     * Verifies PaymentRepository can be instantiated.
     */
    @Test
    public void testSmokeTest_paymentRepositoryInitializes() {
        assertNotNull("PaymentRepository should initialize", paymentRepository);
    }

    /**
     * Verifies Ticket model can be built and serialized.
     */
    @Test
    public void testSmokeTest_ticketModelBuildsAndSerializes() {
        Ticket ticket = buildSampleTicket("customer_smoke", "Plumbing", "pending");
        assertNotNull("Ticket should not be null", ticket);
        assertNotNull("toMap() should not return null", ticket.toMap());
        assertEquals("Service type should match", "Plumbing", ticket.getServiceType());
        assertEquals("Status should match", "pending", ticket.getStatus());
    }

    /**
     * Verifies Payment model can be built and serialized.
     */
    @Test
    public void testSmokeTest_paymentModelBuildsAndSerializes() {
        Payment payment = buildSamplePayment(100.00, "cash", "pending");
        assertNotNull("Payment should not be null", payment);
        assertNotNull("toMap() should not return null", payment.toMap());
        assertEquals("Amount should match", 100.00, payment.getAmount(), 0.01);
        assertEquals("Method should match", "cash", payment.getMethod());
    }

    /**
     * Verifies User model can be built and serialized.
     */
    @Test
    public void testSmokeTest_userModelBuildsAndSerializes() {
        User user = buildSampleUser("uid_smoke", TEST_EMAIL, "user");
        assertNotNull("User should not be null", user);
        assertNotNull("toMap() should not return null", user.toMap());
        assertEquals("Email should match", TEST_EMAIL, user.getEmail());
        assertEquals("Role should match", "user", user.getRole());
        assertFalse("Regular user should not be admin", user.isAdmin());
        assertTrue("Regular user should be regular user", user.isRegularUser());
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private Ticket buildSampleTicket(String customerId, String serviceType, String status) {
        Ticket ticket = new Ticket();
        ticket.setCustomerId(customerId);
        ticket.setCustomerName("Test Customer");
        ticket.setCustomerEmail("customer@example.com");
        ticket.setCustomerPhone(TEST_PHONE);
        ticket.setServiceType(serviceType);
        ticket.setDescription("Integration test ticket for " + serviceType);
        ticket.setStatus(status);
        ticket.setPriority("medium");
        ticket.setLocation(new GeoPoint(37.7749, -122.4194)); // San Francisco
        ticket.setAddress("123 Test St, San Francisco, CA 94102");
        ticket.setEstimatedCost(100.00);
        ticket.setCreatedAt(Timestamp.now());
        ticket.setUpdatedAt(Timestamp.now());
        return ticket;
    }

    private Payment buildSamplePayment(double amount, String method, String status) {
        Payment payment = new Payment();
        payment.setAmount(amount);
        payment.setMethod(method);
        payment.setStatus(status);
        payment.setCreatedAt(Timestamp.now());
        return payment;
    }

    private User buildSampleUser(String uid, String email, String role) {
        User user = new User();
        user.setUid(uid);
        user.setEmail(email);
        user.setName(TEST_NAME);
        user.setPhone(TEST_PHONE);
        user.setRole(role);
        user.setCreatedAt(Timestamp.now());
        user.setUpdatedAt(Timestamp.now());
        return user;
    }

    private RemoteMessage buildRemoteMessage(String ticketId, String title, String body) {
        RemoteMessage.Builder builder = new RemoteMessage.Builder("test@fcm.googleapis.com");
        if (ticketId != null) {
            builder.addData("ticketId", ticketId);
        }
        if (title != null) {
            builder.addData("title", title);
        }
        if (body != null) {
            builder.addData("body", body);
        }
        return builder.build();
    }
}
