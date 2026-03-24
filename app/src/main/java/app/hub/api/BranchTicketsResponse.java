package app.hub.api;

import java.util.List;

public class BranchTicketsResponse {
    
    private boolean success;
    
    
    private String message;
    
    
    private BranchInfo branch;
    
    
    private List<Ticket> tickets;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public BranchInfo getBranch() {
        return branch;
    }

    public List<Ticket> getTickets() {
        return tickets;
    }

    public static class BranchInfo {
        
        private int id;
        
        
        private String name;
        
        
        private String location;

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getLocation() {
            return location;
        }
    }

    public static class Ticket {
        
        private int id;
        
        
        private String ticketId;
        
        
        private String title;
        
        
        private String description;
        
        
        private String serviceType;
        
        
        private double amount;
        
        
        private String address;
        
        
        private String contact;
        
        
        private String preferredDate;
        
        
        private String status;
        
        
        private String statusDetail;
        
        
        private String statusColor;
        
        
        private String customerName;
        
        
        private String assignedStaff;
        
        
        private String imagePath;
        
        
        private String createdAt;
        
        
        private String updatedAt;

        public int getId() {
            return id;
        }

        public String getTicketId() {
            return ticketId;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public String getServiceType() {
            return serviceType;
        }

        public double getAmount() {
            return amount;
        }

        public String getAddress() {
            return address;
        }

        public String getContact() {
            return contact;
        }

        public String getPreferredDate() {
            return preferredDate;
        }

        public String getStatus() {
            return status;
        }

        public String getStatusDetail() {
            return statusDetail;
        }

        public String getStatusColor() {
            return statusColor;
        }

        public String getCustomerName() {
            return customerName;
        }

        public String getAssignedStaff() {
            return assignedStaff;
        }

        public String getImagePath() {
            return imagePath;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }
    }
}
