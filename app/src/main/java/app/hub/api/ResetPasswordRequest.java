package app.hub.api;

public class ResetPasswordRequest {
    private String email;
    private String code;
    private String password;
    private String password_confirmation;

    public ResetPasswordRequest(String email, String code, String password, String password_confirmation) {
        this.email = email;
        this.code = code;
        this.password = password;
        this.password_confirmation = password_confirmation;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword_confirmation() {
        return password_confirmation;
    }

    public void setPassword_confirmation(String password_confirmation) {
        this.password_confirmation = password_confirmation;
    }
}
