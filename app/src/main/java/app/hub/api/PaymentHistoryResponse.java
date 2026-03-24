package app.hub.api;

import java.util.List;

public class PaymentHistoryResponse {
    
    private boolean success;
    
    
    private String message;
    
    
    private List<PaymentItem> payments;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<PaymentItem> getPayments() {
        return payments;
    }

    public void setPayments(List<PaymentItem> payments) {
        this.payments = payments;
    }

    public static class PaymentItem {
        
        private int id;
        
        
        private String ticketId;
        
        
        private String customerName;
        
        
        private String technicianName;
        
        
        private String paymentMethod;
        
        
        private double amount;
        
        
        private String status;
        
        
        private String notes;
        
        
        private String collectedAt;
        
        
        private String submittedAt;
        
        
        private String completedAt;
        
        
        private String createdAt;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getTicketId() {
            return ticketId;
        }

        public void setTicketId(String ticketId) {
            this.ticketId = ticketId;
        }

        public String getCustomerName() {
            return customerName;
        }

        public void setCustomerName(String customerName) {
            this.customerName = customerName;
        }

        public String getTechnicianName() {
            return technicianName;
        }

        public void setTechnicianName(String technicianName) {
            this.technicianName = technicianName;
        }

        public String getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(String paymentMethod) {
            this.paymentMethod = paymentMethod;
        }

        public double getAmount() {
            return amount;
        }

        public void setAmount(double amount) {
            this.amount = amount;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        public String getCollectedAt() {
            return collectedAt;
        }

        public void setCollectedAt(String collectedAt) {
            this.collectedAt = collectedAt;
        }

        public String getSubmittedAt() {
            return submittedAt;
        }

        public void setSubmittedAt(String submittedAt) {
            this.submittedAt = submittedAt;
        }

        public String getCompletedAt() {
            return completedAt;
        }

        public void setCompletedAt(String completedAt) {
            this.completedAt = completedAt;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }
    }
}
