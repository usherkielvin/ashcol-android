package app.hub.user;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import app.hub.R;

public class TicketConfirmationActivity extends AppCompatActivity {

    private TextView tvTicketId;
    private TextView tvStatus;
    private Button btnDone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_confirmation);

        initViews();
        setupData();
        setupClickListeners();
    }

    private void initViews() {
        tvTicketId = findViewById(R.id.tvTicketId);
        tvStatus = findViewById(R.id.tvStatus);
        btnDone = findViewById(R.id.btnDone);
    }

    private void setupData() {
        // Get data from intent
        Intent intent = getIntent();
        String ticketId = intent.getStringExtra("ticket_id");
        String status = intent.getStringExtra("status");

        if (ticketId != null) {
            tvTicketId.setText(ticketId);
        }

        if (status != null) {
            tvStatus.setText(status);
        } else {
            tvStatus.setText("Pending");
        }
    }

    private void setupClickListeners() {
        btnDone.setOnClickListener(v -> {
            // Navigate to customer dashboard and show My Tickets tab
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("show_my_tickets", true);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        // Navigate to dashboard and show My Tickets tab
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("show_my_tickets", true);
        startActivity(intent);
        finish();
    }
}
