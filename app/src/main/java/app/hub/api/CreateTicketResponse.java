package app.hub.api;


public class CreateTicketResponse {
    
    private String message;
    
    
    private TicketData ticket;
    
    
    private String ticketId;
    
    
    private String status;
    
    
    private boolean success = true; // Default to true for successful responses

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
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }

    public static class TicketData {
        
        private int id;
        
        
        private String ticketId;
        
        
        private String title;
        
        
        private String description;
        
        
        private String serviceType;
        
        
        private String unitType;
        
        
        private String address;
        
        
        private String contact;

        
        private Double amount;
        
        
        private StatusData status;
        
        
        private BranchData branch;
        
        
        private CustomerData customer;

        // Getters and setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        
        public String getTicketId() { return ticketId; }
        public void setTicketId(String ticketId) { this.ticketId = ticketId; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getServiceType() { return serviceType; }
        public void setServiceType(String serviceType) { this.serviceType = serviceType; }
        
        public String getUnitType() { return unitType; }
        public void setUnitType(String unitType) { this.unitType = unitType; }
        
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        
        public String getContact() { return contact; }
        public void setContact(String contact) { this.contact = contact; }

        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        
        public StatusData getStatus() { return status; }
        public void setStatus(StatusData status) { this.status = status; }
        
        public BranchData getBranch() { return branch; }
        public void setBranch(BranchData branch) { this.branch = branch; }
        
        public CustomerData getCustomer() { return customer; }
        public void setCustomer(CustomerData customer) { this.customer = customer; }
    }

    public static class StatusData {
        
        private int id;
        
        
        private String name;
        
        
        private String color;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
    }

    public static class BranchData {
        
        private int id;
        
        
        private String name;
        
        
        private String location;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
    }

    public static class CustomerData {
        
        private int id;
        
        
        private String firstName;
        
        
        private String lastName;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
    }
}
