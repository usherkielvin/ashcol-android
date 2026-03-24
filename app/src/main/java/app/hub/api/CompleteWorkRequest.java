package app.hub.api;


public class CompleteWorkRequest {
    
    private String paymentMethod;
    
    
    private double amount;
    
    
    private String notes;

    public CompleteWorkRequest(String paymentMethod, double amount, String notes) {
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.notes = notes;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
