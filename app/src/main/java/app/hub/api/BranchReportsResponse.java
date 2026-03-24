package app.hub.api;

import java.util.List;

public class BranchReportsResponse {
    
    private boolean success;
    
    
    private String message;
    
    
    private List<BranchReport> branches;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public List<BranchReport> getBranches() {
        return branches;
    }

    public static class BranchReport {
        
        private int id;
        
        
        private String name;
        
        
        private String location;
        
        
        private int completedCount;
        
        
        private int cancelledCount;
        
        
        private int totalTickets;
        
        
        private String manager;

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getLocation() {
            return location;
        }

        public int getCompletedCount() {
            return completedCount;
        }

        public int getCancelledCount() {
            return cancelledCount;
        }

        public int getTotalTickets() {
            return totalTickets;
        }

        public String getManager() {
            return manager;
        }
    }
}
