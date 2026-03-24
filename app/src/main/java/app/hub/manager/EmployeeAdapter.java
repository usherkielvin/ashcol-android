package app.hub.manager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.squareup.picasso.Picasso;

import java.util.List;

import app.hub.R;
import app.hub.api.EmployeeResponse;

public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder> {

    private List<EmployeeResponse.Employee> employees;

    public EmployeeAdapter(List<EmployeeResponse.Employee> employees) {
        this.employees = employees;
    }

    @NonNull
    @Override
    public EmployeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_employee_manager, parent, false);
        return new EmployeeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmployeeViewHolder holder, int position) {
        EmployeeResponse.Employee employee = employees.get(position);
        
        // Set employee name
        String firstName = employee.getFirstName() != null ? employee.getFirstName() : "";
        String lastName = employee.getLastName() != null ? employee.getLastName() : "";
        String displayName = (firstName + " " + lastName).trim();
        
        if (displayName.isEmpty()) {
            displayName = employee.getEmail() != null ? employee.getEmail() : "Unknown Technician";
        }
        holder.employeeName.setText(displayName);
        
        // Set role
        holder.employeeDept.setText(employee.getRole() != null ? employee.getRole() : "Technician");
        
        // Set branch
        holder.employeeStatus.setText(employee.getBranch() != null ? employee.getBranch() : "No Branch");
        
        // Load profile image with Picasso - optimized for smooth loading
        String imageUrl = employee.getProfilePhoto();
        
        // Make displayName final for use in callback
        final String finalDisplayName = displayName;
        
        android.util.Log.d("EmployeeAdapter", "Employee: " + finalDisplayName + ", Profile Photo URL: " + imageUrl);
        
        if (imageUrl != null && !imageUrl.isEmpty()) {
            android.util.Log.d("EmployeeAdapter", "Loading image from: " + imageUrl);
            
            // Use Picasso with optimized settings for smooth loading
            Picasso.get()
                .load(imageUrl)
                .placeholder(R.drawable.profile_icon)
                .error(R.drawable.profile_icon)
                .resize(200, 200) // Resize to reasonable size for performance
                .centerCrop()
                .priority(com.squareup.picasso.Picasso.Priority.HIGH)
                .into(holder.employeeImage, new com.squareup.picasso.Callback() {
                    @Override
                    public void onSuccess() {
                        android.util.Log.d("EmployeeAdapter", "Image loaded successfully for: " + finalDisplayName);
                    }

                    @Override
                    public void onError(Exception e) {
                        android.util.Log.e("EmployeeAdapter", "Failed to load image for: " + finalDisplayName + ", Error: " + e.getMessage());
                        // Set default icon on error
                        holder.employeeImage.setImageResource(R.drawable.profile_icon);
                    }
                });
        } else {
            android.util.Log.d("EmployeeAdapter", "No profile photo URL for: " + finalDisplayName);
            holder.employeeImage.setImageResource(R.drawable.profile_icon);
        }
    }

    @Override
    public int getItemCount() {
        return employees != null ? employees.size() : 0;
    }

    public void updateEmployees(List<EmployeeResponse.Employee> newEmployees) {
        this.employees = newEmployees;
        notifyDataSetChanged();
        
        // Preload images for smooth scrolling
        preloadImages();
    }
    
    /**
     * Preload images in background for smooth scrolling
     */
    private void preloadImages() {
        if (employees == null) return;
        
        // Preload first 10 images
        int preloadCount = Math.min(employees.size(), 10);
        for (int i = 0; i < preloadCount; i++) {
            EmployeeResponse.Employee employee = employees.get(i);
            String imageUrl = employee.getProfilePhoto();
            
            if (imageUrl != null && !imageUrl.isEmpty()) {
                // Preload into Picasso cache
                Picasso.get()
                    .load(imageUrl)
                    .resize(200, 200)
                    .centerCrop()
                    .fetch();
            }
        }
    }

    static class EmployeeViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView employeeImage;
        TextView employeeName;
        TextView employeeDept;
        TextView employeeStatus;

        public EmployeeViewHolder(@NonNull View itemView) {
            super(itemView);
            employeeImage = itemView.findViewById(R.id.employeeImage);
            employeeName = itemView.findViewById(R.id.employeeName);
            employeeDept = itemView.findViewById(R.id.employeeDept);
            employeeStatus = itemView.findViewById(R.id.employeeStatus);
        }
    }
}
