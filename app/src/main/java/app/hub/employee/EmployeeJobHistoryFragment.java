package app.hub.employee;

import android.content.Intent;
import android.os.Bundle;
import android.app.DatePickerDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.facebook.shimmer.ShimmerFrameLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import app.hub.R;
import app.hub.api.TicketListResponse;
import app.hub.util.TokenManager;

public class EmployeeJobHistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private EmployeeTicketsAdapter adapter;
    private final List<TicketListResponse.TicketItem> allTickets = new ArrayList<>();
    private final List<TicketListResponse.TicketItem> filteredTickets = new ArrayList<>();
    private TokenManager tokenManager;
    private ChipGroup chipGroupStatus;
    private Chip chipAll;
    private Chip chipCompleted;
    private Chip chipCancelled;
    private MaterialButton btnStartDate;
    private MaterialButton btnEndDate;
    private MaterialButton btnClearDates;
    private Calendar startDate;
    private Calendar endDate;
    private String statusFilter = "completed";
    private ShimmerFrameLayout jobHistoryShimmer;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_employee_job_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tokenManager = new TokenManager(requireContext());

        recyclerView = view.findViewById(R.id.recyclerJobHistory);
        tvEmpty = view.findViewById(R.id.tvJobHistoryEmpty);
        chipGroupStatus = view.findViewById(R.id.chipGroupStatus);
        chipAll = view.findViewById(R.id.chipAll);
        chipCompleted = view.findViewById(R.id.chipCompleted);
        chipCancelled = view.findViewById(R.id.chipCancelled);
        btnStartDate = view.findViewById(R.id.btnStartDate);
        btnEndDate = view.findViewById(R.id.btnEndDate);
        btnClearDates = view.findViewById(R.id.btnClearDates);
        jobHistoryShimmer = view.findViewById(R.id.jobHistoryShimmer);

        View btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> navigateBack());
        }

        adapter = new EmployeeTicketsAdapter(filteredTickets);
        adapter.setOnTicketClickListener(ticket -> {
            Intent intent = new Intent(getContext(), EmployeeTicketDetailActivity.class);
            intent.putExtra("ticket_id", ticket.getTicketId());
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        setupFilters();
        loadHistoryTickets();
    }

    private void setupFilters() {
        statusFilter = "completed";

        if (btnStartDate != null) {
            btnStartDate.setOnClickListener(v -> pickDate(true));
        }
        if (btnEndDate != null) {
            btnEndDate.setOnClickListener(v -> pickDate(false));
        }
        if (btnClearDates != null) {
            btnClearDates.setOnClickListener(v -> {
                startDate = null;
                endDate = null;
                updateDateButtons();
                applyFilters();
            });
        }
    }

    private void pickDate(boolean isStart) {
        Calendar calendar = isStart ? (startDate != null ? startDate : Calendar.getInstance())
                : (endDate != null ? endDate : Calendar.getInstance());

        DatePickerDialog dialog = new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, dayOfMonth, 0, 0, 0);
            if (isStart) {
                startDate = selected;
            } else {
                endDate = selected;
            }
            updateDateButtons();
            applyFilters();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        dialog.show();
    }

    private void updateDateButtons() {
        SimpleDateFormat format = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        if (btnStartDate != null) {
            btnStartDate.setText(startDate != null ? format.format(startDate.getTime()) : "Start date");
        }
        if (btnEndDate != null) {
            btnEndDate.setText(endDate != null ? format.format(endDate.getTime()) : "End date");
        }
    }

    private void loadHistoryTickets() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showEmptyState(true);
            return;
        }

        setLoading(true);
        filteredTickets.clear();
        adapter.notifyDataSetChanged();

        android.util.Log.d("JobHistory", "Loading job history from Firestore for user: " + user.getUid());

        FirebaseFirestore.getInstance().collection("tickets")
            .whereEqualTo("assigned_staff_id", user.getUid())
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!isAdded()) return;
                allTickets.clear();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    TicketListResponse.TicketItem ticket = doc.toObject(TicketListResponse.TicketItem.class);
                    if (ticket != null) {
                        ticket.setTicketId(doc.getId());
                        allTickets.add(ticket);
                    }
                }
                android.util.Log.d("JobHistory", "Loaded " + allTickets.size() + " tickets from Firestore");
                applyFilters();
                setLoading(false);
            })
            .addOnFailureListener(e -> {
                if (!isAdded()) return;
                setLoading(false);
                android.util.Log.e("JobHistory", "Error loading job history: " + e.getMessage());
                showEmptyState(true);
            });
    }

    private void applyFilters() {
        filteredTickets.clear();

        for (TicketListResponse.TicketItem ticket : allTickets) {
            if (!matchesStatus(ticket)) continue;
            if (!matchesTechnician(ticket)) continue;
            if (!matchesDate(ticket)) continue;
            filteredTickets.add(ticket);
        }

        adapter.notifyDataSetChanged();
        showEmptyState(filteredTickets.isEmpty());
    }

    private boolean matchesStatus(TicketListResponse.TicketItem ticket) {
        String status = ticket.getStatus();
        if (status == null) return false;
        String normalized = status.trim().toLowerCase(Locale.ENGLISH);

        return normalized.contains("completed")
                || normalized.contains("resolved")
                || normalized.contains("closed");
    }

    private boolean matchesTechnician(TicketListResponse.TicketItem ticket) {
        String assigned = ticket.getAssignedStaff();
        String name = tokenManager.getName();
        String email = tokenManager.getEmail();

        if (assigned == null || assigned.trim().isEmpty()) {
            return true;
        }

        String normalizedAssigned = assigned.trim().toLowerCase(Locale.ENGLISH);
        if (name != null && !name.trim().isEmpty()) {
            if (normalizedAssigned.contains(name.trim().toLowerCase(Locale.ENGLISH))) {
                return true;
            }
        }
        if (email != null && !email.trim().isEmpty()) {
            return normalizedAssigned.contains(email.trim().toLowerCase(Locale.ENGLISH));
        }
        return false;
    }

    private boolean matchesDate(TicketListResponse.TicketItem ticket) {
        if (startDate == null && endDate == null) return true;

        Date ticketDate = parseTicketDate(ticket);
        if (ticketDate == null) return false;

        if (startDate != null && ticketDate.before(startDate.getTime())) return false;
        if (endDate != null && ticketDate.after(endDate.getTime())) return false;
        return true;
    }

    private Date parseTicketDate(TicketListResponse.TicketItem ticket) {
        String dateValue = ticket.getScheduledDate();
        if (dateValue == null || dateValue.isEmpty()) {
            dateValue = ticket.getUpdatedAt();
        }
        if (dateValue == null || dateValue.isEmpty()) {
            dateValue = ticket.getCreatedAt();
        }
        if (dateValue == null || dateValue.isEmpty()) return null;

        Date parsed = tryParse(dateValue, "yyyy-MM-dd");
        if (parsed != null) return parsed;
        parsed = tryParse(dateValue, "yyyy-MM-dd HH:mm:ss");
        if (parsed != null) return parsed;
        return tryParse(dateValue, "yyyy-MM-dd'T'HH:mm:ss");
    }

    private Date tryParse(String value, String pattern) {
        try {
            return new SimpleDateFormat(pattern, Locale.getDefault()).parse(value);
        } catch (ParseException e) {
            return null;
        }
    }

    private void showEmptyState(boolean show) {
        if (tvEmpty != null) {
            tvEmpty.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void setLoading(boolean isLoading) {
        if (jobHistoryShimmer != null) {
            jobHistoryShimmer.stopShimmer();
            jobHistoryShimmer.setVisibility(View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        }
        if (tvEmpty != null) {
            tvEmpty.setVisibility(isLoading ? View.GONE : tvEmpty.getVisibility());
        }
    }

    private void navigateBack() {
        if (getParentFragmentManager().getBackStackEntryCount() > 0) {
            getParentFragmentManager().popBackStack();
        } else if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }
}
