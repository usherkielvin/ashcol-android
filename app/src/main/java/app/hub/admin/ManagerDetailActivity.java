package app.hub.admin;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import app.hub.R;

public class ManagerDetailActivity extends AppCompatActivity {

    private TextView managerNameDetail;
    private TextView managerBranchDetail;
    private TextView managerEmailDetail;
    private TextView managerStatusDetail;
    private TextView managerPhoneDetail;
    private TextView managerJoinDateDetail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_manager_detail);
            setupToolbar();
            setupViews();
            loadManagerDetails();
        } catch (Exception e) {
            android.util.Log.e("ManagerDetailActivity", "Error in onCreate: " + e.getMessage(), e);
            finish();
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupViews() {
        managerNameDetail = findViewById(R.id.managerNameDetail);
        managerBranchDetail = findViewById(R.id.managerBranchDetail);
        managerEmailDetail = findViewById(R.id.managerEmailDetail);
        managerStatusDetail = findViewById(R.id.managerStatusDetail);
        managerPhoneDetail = findViewById(R.id.managerPhoneDetail);
        managerJoinDateDetail = findViewById(R.id.managerJoinDateDetail);
    }

    private void loadManagerDetails() {
        // Get data from intent
        String managerName = getIntent().getStringExtra("manager_name");
        String managerBranch = getIntent().getStringExtra("manager_branch");
        String managerEmail = getIntent().getStringExtra("manager_email");
        String managerStatus = getIntent().getStringExtra("manager_status");
        String managerPhone = getIntent().getStringExtra("manager_phone");
        String managerJoinDate = getIntent().getStringExtra("manager_join_date");

        // Set data to views
        if (managerName != null) {
            managerNameDetail.setText(managerName);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(managerName);
            }
        }
        
        if (managerBranch != null) {
            managerBranchDetail.setText(managerBranch);
        }
        
        if (managerEmail != null) {
            managerEmailDetail.setText(managerEmail);
        }
        
        if (managerStatus != null) {
            managerStatusDetail.setText(managerStatus);
        }
        
        if (managerPhone != null) {
            managerPhoneDetail.setText(managerPhone);
        }
        
        if (managerJoinDate != null) {
            managerJoinDateDetail.setText(managerJoinDate);
        }
    }
}
