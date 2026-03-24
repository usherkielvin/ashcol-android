package app.hub.api;


public class TestResponse {
    
    private boolean success;
    
    
    private String message;
    
    
    private UserInfo user;
    
    
    private String timestamp;

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

    public UserInfo getUser() {
        return user;
    }

    public void setUser(UserInfo user) {
        this.user = user;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public static class UserInfo {
        
        private int id;
        
        
        private String name;
        
        
        private String email;
        
        
        private String role;
        
        
        private boolean isCustomer;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public boolean isCustomer() {
            return isCustomer;
        }

        public void setCustomer(boolean customer) {
            isCustomer = customer;
        }
    }
}
