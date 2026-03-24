package app.hub.api;


public class UpdateTicketStatusRequest {
    
    private String status;

    
    private String statusDetail;
    
    
    private Integer assignedStaffId;

    public UpdateTicketStatusRequest(String status) {
        this.status = status;
    }

    public UpdateTicketStatusRequest(String status, String statusDetail) {
        this.status = status;
        this.statusDetail = statusDetail;
    }

    public UpdateTicketStatusRequest(String status, Integer assignedStaffId) {
        this.status = status;
        this.assignedStaffId = assignedStaffId;
    }

    public UpdateTicketStatusRequest(String status, String statusDetail, Integer assignedStaffId) {
        this.status = status;
        this.statusDetail = statusDetail;
        this.assignedStaffId = assignedStaffId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getAssignedStaffId() {
        return assignedStaffId;
    }

    public void setAssignedStaffId(Integer assignedStaffId) {
        this.assignedStaffId = assignedStaffId;
    }

    public String getStatusDetail() {
        return statusDetail;
    }

    public void setStatusDetail(String statusDetail) {
        this.statusDetail = statusDetail;
    }
}
