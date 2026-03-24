package app.hub.api;


public class UpdateTicketStatusResponse {
    
    private boolean success;
    
    
    private String message;
    
    
    private TicketData ticket;

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

    public static class TicketData {
        
        private int id;
        
        
        private String ticketId;
        
        
        private String status;
        
        
        private Integer assignedStaffId;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        
        public String getTicketId() { return ticketId; }
        public void setTicketId(String ticketId) { this.ticketId = ticketId; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public Integer getAssignedStaffId() { return assignedStaffId; }
        public void setAssignedStaffId(Integer assignedStaffId) { this.assignedStaffId = assignedStaffId; }
    }
}
