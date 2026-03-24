package app.hub.api;

import java.util.List;

public class DashboardStatsResponse {
    
    private boolean success;

    
    private Stats stats;

    
    private List<RecentTicket> recentTickets;

    
    private String message;

    // Getters and setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Stats getStats() {
        return stats;
    }

    public void setStats(Stats stats) {
        this.stats = stats;
    }

    public List<RecentTicket> getRecentTickets() {
        return recentTickets;
    }

    public void setRecentTickets(List<RecentTicket> recentTickets) {
        this.recentTickets = recentTickets;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // Stats class for workload statistics
    public static class Stats {
        
        private int totalTickets;

        
        private int pending;

        
        private int inProgress;

        
        private int completed;

        
        private int cancelled;

        public int getTotalTickets() {
            return totalTickets;
        }

        public void setTotalTickets(int totalTickets) {
            this.totalTickets = totalTickets;
        }

        public int getPending() {
            return pending;
        }

        public void setPending(int pending) {
            this.pending = pending;
        }

        public int getInProgress() {
            return inProgress;
        }

        public void setInProgress(int inProgress) {
            this.inProgress = inProgress;
        }

        public int getCompleted() {
            return completed;
        }

        public void setCompleted(int completed) {
            this.completed = completed;
        }

        public int getCancelled() {
            return cancelled;
        }

        public void setCancelled(int cancelled) {
            this.cancelled = cancelled;
        }
    }

    // RecentTicket class for recent activity
    public static class RecentTicket {
        
        private String ticketId;

        
        private String status;

        
        private String statusColor;

        
        private String customerName;

        
        private String serviceType;

        
        private String description;

        
        private String address;

        
        private String createdAt;

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

        public String getCustomerName() {
            return customerName;
        }

        public void setCustomerName(String customerName) {
            this.customerName = customerName;
        }

        public String getServiceType() {
            return serviceType;
        }

        public void setServiceType(String serviceType) {
            this.serviceType = serviceType;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }
    }
}
