package app.hub.api;


public class TicketStatusResponse {
    
    private boolean success;
    
    
    private String message;
    
    
    private TicketStatusData ticket;

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

    public TicketStatusData getTicket() {
        return ticket;
    }

    public void setTicket(TicketStatusData ticket) {
        this.ticket = ticket;
    }

    public static class TicketStatusData {
        
        private String ticketId;
        
        
        private String status;
        
        
        private String statusColor;
        
        
        private String assignedStaff;

        public String getTicketId() { return ticketId; }
        public void setTicketId(String ticketId) { this.ticketId = ticketId; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getStatusColor() { return statusColor; }
        public void setStatusColor(String statusColor) { this.statusColor = statusColor; }
        
        public String getAssignedStaff() { return assignedStaff; }
        public void setAssignedStaff(String assignedStaff) { this.assignedStaff = assignedStaff; }
    }
}
