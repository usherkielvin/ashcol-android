package app.hub.api;

import java.util.List;

public class BranchResponse {
    
    private boolean success;
    
    
    private String message;
    
    
    private List<Branch> branches;
    
    
    private int totalBranches;

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

    public List<Branch> getBranches() {
        return branches;
    }

    public void setBranches(List<Branch> branches) {
        this.branches = branches;
    }

    public int getTotalBranches() {
        return totalBranches;
    }

    public void setTotalBranches(int totalBranches) {
        this.totalBranches = totalBranches;
    }

    public static class Branch {
        
        private int id;
        
        
        private String name;
        
        
        private String location;
        
        
        private String address;
        
        
        private double latitude;
        
        
        private double longitude;
        
        
        private String manager;
        
        
        private int employeeCount;
        
        
        private String description;

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        
        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }
        
        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
        
        public String getManager() { return manager; }
        public void setManager(String manager) { this.manager = manager; }
        
        public int getEmployeeCount() { return employeeCount; }
        public void setEmployeeCount(int employeeCount) { this.employeeCount = employeeCount; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
