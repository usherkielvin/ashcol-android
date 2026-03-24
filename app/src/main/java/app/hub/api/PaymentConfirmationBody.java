package app.hub.api;


public class PaymentConfirmationBody {
    
    private String ticketId;

    
    private int customerId;

    
    private String paymentMethod;

    
    private double amount;

    public PaymentConfirmationBody(String ticketId, int customerId, String paymentMethod, double amount) {
        this.ticketId = ticketId;
        this.customerId = customerId;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
    }

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
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
}
