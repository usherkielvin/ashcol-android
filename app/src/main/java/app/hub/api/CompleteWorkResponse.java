package app.hub.api;


public class CompleteWorkResponse {
    
    private boolean success;
    
    
    private String message;
    
    
    private TicketData ticket;
    
    
    private PaymentData payment;

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

    public TicketData getTicket() {
        return ticket;
    }

    public void setTicket(TicketData ticket) {
        this.ticket = ticket;
    }

    public PaymentData getPayment() {
        return payment;
    }

    public void setPayment(PaymentData payment) {
        this.payment = payment;
    }

    public static class TicketData {
        
        private String ticketId;
        
        
        private String status;
        
        
        private String statusColor;

        public String getTicketId() {
            return ticketId;
        }

        public void setTicketId(String ticketId) {
            this.ticketId = ticketId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getStatusColor() {
            return statusColor;
        }

        public void setStatusColor(String statusColor) {
            this.statusColor = statusColor;
        }
    }

    public static class PaymentData {
        
        private int id;
        
        
        private String paymentMethod;
        
        
        private double amount;
        
        
        private String status;
        
        
        private String collectedAt;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
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

        public String getCollectedAt() {
            return collectedAt;
        }

        public void setCollectedAt(String collectedAt) {
            this.collectedAt = collectedAt;
        }
    }
}
