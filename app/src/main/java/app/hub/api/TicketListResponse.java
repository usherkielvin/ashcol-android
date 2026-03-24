package app.hub.api;

import java.util.List;

public class TicketListResponse {
    
    private boolean success;

    
    private List<TicketItem> tickets;

    
    private String message;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<TicketItem> getTickets() {
        return tickets;
    }

    public void setTickets(List<TicketItem> tickets) {
        this.tickets = tickets;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static class TicketItem {
        
        private int id;

        
        private String ticketId;

        
        private String title;

        
        private String description;

        
        private String serviceType;

        
        private String unitType;

        
        private String address;

        
        private String contact;

        
        private String status;

        
        private String statusDetail;

        
        private String statusColor;

        
        private String customerName;

        
        private String assignedStaff;

        
        private String assignedStaffPhone;

        
        private String branch;

        
        private String imagePath;

        
        private String createdAt;

        
        private String updatedAt;

        
        private String scheduledDate;

        
        private String scheduledTime;

        
        private String scheduleNotes;

        
        private double latitude;

        
        private double longitude;

        
        private double amount;

        // Getters and setters
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

        public String getUnitType() {
            return unitType;
        }

        public void setUnitType(String unitType) {
            this.unitType = unitType;
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

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getStatusDetail() {
            return statusDetail;
        }

        public void setStatusDetail(String statusDetail) {
            this.statusDetail = statusDetail;
        }

        public String getStatusColor() {
            return statusColor;
        }

        public void setStatusColor(String statusColor) {
            this.statusColor = statusColor;
        }

        public String getCustomerName() {
            return customerName;
        }

        public void setCustomerName(String customerName) {
            this.customerName = customerName;
        }

        public String getAssignedStaff() {
            return assignedStaff;
        }

        public void setAssignedStaff(String assignedStaff) {
            this.assignedStaff = assignedStaff;
        }

        public String getAssignedStaffPhone() {
            return assignedStaffPhone;
        }

        public void setAssignedStaffPhone(String assignedStaffPhone) {
            this.assignedStaffPhone = assignedStaffPhone;
        }

        public String getBranch() {
            return branch;
        }

        public void setBranch(String branch) {
            this.branch = branch;
        }

        public String getImagePath() {
            return imagePath;
        }

        public void setImagePath(String imagePath) {
            this.imagePath = imagePath;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(String updatedAt) {
            this.updatedAt = updatedAt;
        }

        public String getScheduledDate() {
            return scheduledDate;
        }

        public void setScheduledDate(String scheduledDate) {
            this.scheduledDate = scheduledDate;
        }

        public String getScheduledTime() {
            return scheduledTime;
        }

        public void setScheduledTime(String scheduledTime) {
            this.scheduledTime = scheduledTime;
        }

        public String getScheduleNotes() {
            return scheduleNotes;
        }

        public void setScheduleNotes(String scheduleNotes) {
            this.scheduleNotes = scheduleNotes;
        }

        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }

        public double getAmount() {
            return amount;
        }

        public void setAmount(double amount) {
            this.amount = amount;
        }
    }

    /**
     * Build a TicketItem from CreateTicketResponse for instant optimistic display.
     */
    public static TicketItem fromCreateResponse(CreateTicketResponse.TicketData data, String statusName,
            String statusColor) {
        if (data == null)
            return null;
        TicketItem item = new TicketItem();
        item.setId(data.getId());
        item.setTicketId(data.getTicketId());
        item.setTitle(data.getTitle());
        item.setDescription(data.getDescription());
        item.setServiceType(data.getServiceType());
        item.setUnitType(data.getUnitType());
        item.setAddress(data.getAddress());
        item.setContact(data.getContact());
        if (data.getAmount() != null) {
            item.setAmount(data.getAmount());
        }
        item.setStatus(
                statusName != null ? statusName : (data.getStatus() != null ? data.getStatus().getName() : "Pending"));
        item.setStatusColor(
                statusColor != null ? statusColor : (data.getStatus() != null ? data.getStatus().getColor() : "#gray"));
        if (data.getCustomer() != null) {
            String name = (data.getCustomer().getFirstName() != null ? data.getCustomer().getFirstName() + " " : "")
                    + (data.getCustomer().getLastName() != null ? data.getCustomer().getLastName() : "").trim();
            item.setCustomerName(name.trim().isEmpty() ? "Customer" : name.trim());
        } else {
            item.setCustomerName("Customer");
        }
        item.setBranch(data.getBranch() != null ? data.getBranch().getName() : null);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                java.util.Locale.getDefault());
        String now = sdf.format(new java.util.Date());
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        return item;
    }
}
