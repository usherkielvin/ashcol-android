package app.hub.api;


public class PaymentRequestBody {
    
    private String ticketId;

    
    private int technicianId;

    public PaymentRequestBody(String ticketId, int technicianId) {
        this.ticketId = ticketId;
        this.technicianId = technicianId;
    }

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public int getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(int technicianId) {
        this.technicianId = technicianId;
    }
}
