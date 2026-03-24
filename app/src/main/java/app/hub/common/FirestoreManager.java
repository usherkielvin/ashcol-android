package app.hub.common;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;

import app.hub.api.TicketListResponse;
import app.hub.api.UserResponse;
import app.hub.util.TokenManager;

public class FirestoreManager {
    private static final String TAG = "FirestoreManager";
    private FirebaseFirestore db;
    private TokenManager tokenManager;
    private ListenerRegistration userProfileListener;
    private ListenerRegistration addressListener;
    private ListenerRegistration paymentListener;
    private ListenerRegistration completedPaymentListener;
    private ListenerRegistration pendingPaymentsListener;
    private ListenerRegistration completedPaymentsListener;

    public interface UserProfileListener {
        void onProfileLoaded(DocumentSnapshot doc);

        void onError(Exception e);
    }

    public FirestoreManager(Context context) {
        db = FirebaseFirestore.getInstance();
        tokenManager = new TokenManager(context);
    }

    public void getUserProfile(String uid, UserProfileListener listener) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    listener.onProfileLoaded(documentSnapshot);
                } else {
                    listener.onError(new Exception("User profile not found"));
                }
            })
            .addOnFailureListener(e -> {
                listener.onError(e);
            });
    }

    public void listenToUserProfile(UserProfileListener listener) {
        String uid = tokenManager.getUid();
        if (uid == null) {
            listener.onError(new Exception("No user UID found"));
            return;
        }

        userProfileListener = db.collection("users").document(uid)
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed: " + error);
                        listener.onError(error);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        listener.onProfileLoaded(snapshot);
                    }
                });
    }

    public void stopListening() {
        if (userProfileListener != null) {
            userProfileListener.remove();
            userProfileListener = null;
        }
    }

    public static class UserAddress {
        public String id;
        public String customerEmail;
        public String name;
        public String phone;
        public String locationDetails;
        public String postalCode;
        public String streetDetails;
        public boolean isDefault;
        public java.util.Date updatedAt;

        public UserAddress() {
        }
    }

    public interface UserAddressListener {
        void onAddressesUpdated(java.util.List<UserAddress> addresses);

        void onError(Exception e);
    }

    public interface AddressSaveListener {
        void onSuccess();

        void onError(Exception e);
    }

    public void listenToUserAddresses(UserAddressListener listener) {
        String email = tokenManager.getEmail();
        if (email == null) {
            listener.onError(new Exception("No user email found"));
            return;
        }

        addressListener = db.collection("addresses")
                .whereEqualTo("customerEmail", email)
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshots, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }

                    if (snapshots != null) {
                        java.util.List<UserAddress> addresses = new java.util.ArrayList<>();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            UserAddress address = doc.toObject(UserAddress.class);
                            if (address != null) {
                                address.id = doc.getId();
                                addresses.add(address);
                            }
                        }
                        listener.onAddressesUpdated(addresses);
                    }
                });
    }

    public void stopAddressListening() {
        if (addressListener != null) {
            addressListener.remove();
            addressListener = null;
        }
    }

    public void saveUserAddress(UserAddress address, AddressSaveListener listener) {
        String email = tokenManager.getEmail();
        if (email == null) {
            listener.onError(new Exception("No user email found"));
            return;
        }

        address.customerEmail = email;
        address.updatedAt = new java.util.Date();

        com.google.firebase.firestore.WriteBatch batch = db.batch();
        com.google.firebase.firestore.CollectionReference addressesRef = db.collection("addresses");

        if (address.isDefault) {
            db.collection("addresses")
                    .whereEqualTo("customerEmail", email)
                    .get()
                    .addOnSuccessListener(query -> {
                        for (DocumentSnapshot doc : query.getDocuments()) {
                            if (address.id == null || !doc.getId().equals(address.id)) {
                                batch.update(doc.getReference(), "isDefault", false);
                            }
                        }
                        saveAddressDocument(addressesRef, batch, address, listener);
                    })
                    .addOnFailureListener(listener::onError);
            return;
        }

        saveAddressDocument(addressesRef, batch, address, listener);
    }

    private void saveAddressDocument(com.google.firebase.firestore.CollectionReference addressesRef,
            com.google.firebase.firestore.WriteBatch batch,
            UserAddress address,
            AddressSaveListener listener) {
        com.google.firebase.firestore.DocumentReference docRef =
                address.id != null ? addressesRef.document(address.id) : addressesRef.document();

        batch.set(docRef, address);
        batch.commit()
                .addOnSuccessListener(unused -> listener.onSuccess())
                .addOnFailureListener(listener::onError);
    }

    public interface TicketListListener {
        void onTicketsUpdated(java.util.List<app.hub.api.TicketListResponse.TicketItem> tickets);

        void onError(Exception e);
    }

    private ListenerRegistration ticketListener;

    public void listenToMyTickets(TicketListListener listener) {
        String email = tokenManager.getEmail();
        if (email == null) {
            listener.onError(new Exception("No user found"));
            return;
        }

        // We need to query tickets where customerId matches.
        // Note: The backend syncs ticket with 'customerId' field.
        // We need to match this with what we have.
        // Backend 'customerId' is likely an int ID.
        // TokenManager stores email.
        // Ideally we should use the int ID.
        // For now, let's assume we can query by email if the backend syncs it,
        // OR we rely on the fact that we might have skipped syncing email to tickets.
        // Let's check what we plan to sync in TicketController.

        // In the plan: 'customerId' => $ticket->customer_id
        // We probably need to fetch the integer ID from the profile first or valid
        // login response.
        // Since we don't have it easily in TokenManager without an extra call or
        // storage,
        // let's try to query by something else or Update TokenManager to store int ID.

        // Actually, let's update TokenManager to store ID if possible.
        // But for this step, let's assume we sync 'customerEmail' to ticket in
        // Firestore for easier querying from Android.

        ticketListener = db.collection("tickets")
                .whereEqualTo("customer_email", email)
                .addSnapshotListener(MetadataChanges.INCLUDE, (snapshots, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }

                    if (snapshots != null) {
                        java.util.List<app.hub.api.TicketListResponse.TicketItem> tickets = new java.util.ArrayList<>();
                        for (DocumentSnapshot doc : snapshots) {
                            app.hub.api.TicketListResponse.TicketItem ticket = doc
                                    .toObject(app.hub.api.TicketListResponse.TicketItem.class);
                            if (ticket != null) {
                                if (ticket.getTicketId() == null || ticket.getTicketId().isEmpty()) {
                                    ticket.setTicketId(doc.getId());
                                }
                                tickets.add(ticket);
                            }
                        }
                        listener.onTicketsUpdated(tickets);
                    }
                });
    }

    public void stopTicketListening() {
        if (ticketListener != null) {
            ticketListener.remove();
            ticketListener = null;
        }
    }

    public static class PendingPayment {
        public int paymentId;
        public String ticketId;
        public String customerEmail;
        public String serviceName;
        public String technicianName;
        public String status;
        public String paymentMethod;
        public double amount;

        public PendingPayment() {
        }
    }

    public interface PendingPaymentListener {
        void onPaymentUpdated(PendingPayment payment);

        void onError(Exception e);
    }

    public interface PendingPaymentsListener {
        void onPaymentsUpdated(java.util.List<PendingPayment> payments);

        void onError(Exception e);
    }

    public void listenToPendingPayment(@Nullable String ticketId, PendingPaymentListener listener) {
        String email = tokenManager.getEmail();
        if (email == null || ticketId == null) {
            listener.onError(new Exception("Missing user or ticket id"));
            return;
        }

        paymentListener = db.collection("payments")
                .whereEqualTo("customerEmail", email)
                .whereEqualTo("ticketId", ticketId)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        DocumentSnapshot doc = snapshots.getDocuments().get(0);
                        PendingPayment payment = doc.toObject(PendingPayment.class);
                        if (payment != null) {
                            listener.onPaymentUpdated(payment);
                        }
                    }
                });
    }

    /**
     * Listen to payment for a specific ticket (any status)
     * Used by technician to monitor payment status changes
     */
    public void listenToPaymentByTicket(@Nullable String ticketId, PendingPaymentListener listener) {
        if (ticketId == null) {
            listener.onError(new Exception("Missing ticket id"));
            return;
        }

        paymentListener = db.collection("payments")
                .whereEqualTo("ticketId", ticketId)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        // Get the most recent payment for this ticket
                        DocumentSnapshot doc = snapshots.getDocuments().get(0);
                        PendingPayment payment = doc.toObject(PendingPayment.class);
                        if (payment != null) {
                            listener.onPaymentUpdated(payment);
                        }
                    }
                });
    }

    public void listenToCompletedPayment(@Nullable String ticketId, PendingPaymentListener listener) {
        String email = tokenManager.getEmail();
        if (email == null || ticketId == null) {
            listener.onError(new Exception("Missing user or ticket id"));
            return;
        }

        completedPaymentListener = db.collection("payments")
                .whereEqualTo("customerEmail", email)
                .whereEqualTo("ticketId", ticketId)
                .whereEqualTo("status", "completed")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        DocumentSnapshot doc = snapshots.getDocuments().get(0);
                        PendingPayment payment = doc.toObject(PendingPayment.class);
                        if (payment != null) {
                            listener.onPaymentUpdated(payment);
                        }
                    }
                });
    }

    public void listenToPendingPayments(PendingPaymentsListener listener) {
        String email = tokenManager.getEmail();
        if (email == null) {
            listener.onError(new Exception("Missing user email"));
            return;
        }

        pendingPaymentsListener = db.collection("payments")
                .whereEqualTo("customerEmail", email)
                .whereEqualTo("status", "pending")
                .orderBy("updatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }

                    if (snapshots != null) {
                        java.util.List<PendingPayment> payments = new java.util.ArrayList<>();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            PendingPayment payment = doc.toObject(PendingPayment.class);
                            if (payment != null) {
                                payments.add(payment);
                            }
                        }
                        listener.onPaymentsUpdated(payments);
                    }
                });
    }

    public void listenToCompletedPayments(PendingPaymentsListener listener) {
        String email = tokenManager.getEmail();
        if (email == null) {
            listener.onError(new Exception("Missing user email"));
            return;
        }

        completedPaymentsListener = db.collection("payments")
                .whereEqualTo("customerEmail", email)
                .whereEqualTo("status", "completed")
                .orderBy("updatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }

                    if (snapshots != null) {
                        java.util.List<PendingPayment> payments = new java.util.ArrayList<>();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            PendingPayment payment = doc.toObject(PendingPayment.class);
                            if (payment != null) {
                                payments.add(payment);
                            }
                        }
                        listener.onPaymentsUpdated(payments);
                    }
                });
    }

    public void stopPaymentListening() {
        if (paymentListener != null) {
            paymentListener.remove();
            paymentListener = null;
        }
        if (completedPaymentListener != null) {
            completedPaymentListener.remove();
            completedPaymentListener = null;
        }
        if (pendingPaymentsListener != null) {
            pendingPaymentsListener.remove();
            pendingPaymentsListener = null;
        }
        if (completedPaymentsListener != null) {
            completedPaymentsListener.remove();
            completedPaymentsListener = null;
        }
    }

    public interface BranchListListener {
        void onBranchesUpdated(java.util.List<Branch> branches);

        void onError(Exception e);
    }

    public static class Branch {
        public int id;
        public String name;
        public String location;
        public String address;
        public double latitude;
        public double longitude;
        public boolean isActive;

        public Branch() {
        }
    }

    private ListenerRegistration branchListener;

    public void listenToBranches(BranchListListener listener) {
        branchListener = db.collection("branches")
                .whereEqualTo("isActive", true)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }

                    if (snapshots != null) {
                        java.util.List<Branch> branches = new java.util.ArrayList<>();
                        for (DocumentSnapshot doc : snapshots) {
                            Branch branch = doc.toObject(Branch.class);
                            branches.add(branch);
                        }
                        listener.onBranchesUpdated(branches);
                    }
                });
    }

    public void stopBranchListening() {
        if (branchListener != null) {
            branchListener.remove();
            branchListener = null;
        }
    }
}
