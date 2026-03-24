package app.hub.api;


public class CreateTicketRequest {
    
    private String title;

    
    private String description;

    
    private String serviceType;

    
    private String address;

    
    private String contact;

    
    private String preferredDate;

    
    private Double latitude;

    
    private Double longitude;

    
    private Double amount;

    public CreateTicketRequest(String title, String description, String serviceType, String address, String contact) {
        this(title, description, serviceType, address, contact, null, null, null, null);
    }

    public CreateTicketRequest(String title, String description, String serviceType, String address, String contact,
            String preferredDate) {
        this(title, description, serviceType, address, contact, preferredDate, null, null, null);
    }

    public CreateTicketRequest(String title, String description, String serviceType, String address, String contact,
            String preferredDate, Double latitude, Double longitude, Double amount) {
        this.title = title;
        this.description = description;
        this.serviceType = serviceType;
        this.address = address;
        this.contact = contact;
        this.preferredDate = preferredDate;
        this.latitude = latitude;
        this.longitude = longitude;
        this.amount = amount;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getPreferredDate() {
        return preferredDate;
    }

    public void setPreferredDate(String preferredDate) {
        this.preferredDate = preferredDate;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
}
