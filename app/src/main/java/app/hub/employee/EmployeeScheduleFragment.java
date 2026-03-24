package app.hub.employee;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.shimmer.ShimmerFrameLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import app.hub.R;
import app.hub.api.EmployeeScheduleResponse;
import app.hub.util.TokenManager;

public class EmployeeScheduleFragment extends Fragment {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView rvCalendarGrid;
    private RecyclerView rvDailyJobs;
    private TextView tvMonthYear;
    private TextView tvSelectedDate;
    private TextView tvSelectedCount;
    private TextView tvDailyEmpty;
    private ImageButton btnPreviousMonth;
    private ImageButton btnNextMonth;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private LinearLayout dailyListContainer;
    private ShimmerFrameLayout shimmerSchedule;

    private Calendar currentCalendar;
    private CalendarAdapter calendarAdapter;
    private DailyScheduleAdapter dailyScheduleAdapter;
    private final List<CalendarAdapter.CalendarDay> calendarDays = new ArrayList<>();
    private final Map<String, List<EmployeeScheduleResponse.ScheduledTicket>> allBufferedTickets = new HashMap<>();
    private final Map<String, List<EmployeeScheduleResponse.ScheduledTicket>> scheduledTicketsMap = new HashMap<>();

    private TokenManager tokenManager;
    private FirebaseEmployeeListener firebaseListener;
    private String selectedDateKey;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_employee_schedule, container, false);

        tokenManager = new TokenManager(requireContext());
        currentCalendar = Calendar.getInstance();

        swipeRefreshLayout = rootView.findViewById(R.id.swipeRefreshLayout);
        rvCalendarGrid = rootView.findViewById(R.id.rvCalendarGrid);
        rvDailyJobs = rootView.findViewById(R.id.rvDailyJobs);
        tvMonthYear = rootView.findViewById(R.id.tvMonthYear);
        tvSelectedDate = rootView.findViewById(R.id.tvSelectedDate);
        tvSelectedCount = rootView.findViewById(R.id.tvSelectedCount);
        tvDailyEmpty = rootView.findViewById(R.id.tvDailyEmpty);
        btnPreviousMonth = rootView.findViewById(R.id.btnPreviousMonth);
        btnNextMonth = rootView.findViewById(R.id.btnNextMonth);
        progressBar = rootView.findViewById(R.id.progressBar);
        emptyState = rootView.findViewById(R.id.emptyState);
        dailyListContainer = rootView.findViewById(R.id.dailyListContainer);
        shimmerSchedule = rootView.findViewById(R.id.shimmerSchedule);

        setupRecyclerViews();
        setupClickListeners();
        setupSwipeRefresh();
        setupFirebaseListener();
        initializeCalendar();

        loadScheduleData(true);

        return rootView;
    }

    private void setupRecyclerViews() {
        rvCalendarGrid.setLayoutManager(new GridLayoutManager(getContext(), 7));
        calendarAdapter = new CalendarAdapter(calendarDays,
                currentCalendar.get(Calendar.MONTH),
                currentCalendar.get(Calendar.YEAR));
        rvCalendarGrid.setAdapter(calendarAdapter);

        calendarAdapter.setOnDayClickListener(day -> {
            selectedDateKey = day.getDateKey();
            updateDailyListForDateKey(selectedDateKey);
        });

        rvDailyJobs.setLayoutManager(new LinearLayoutManager(getContext()));
        dailyScheduleAdapter = new DailyScheduleAdapter();
        dailyScheduleAdapter.setOnTicketClickListener(ticket -> openTicketDetail(ticket.getTicketId()));
        rvDailyJobs.setAdapter(dailyScheduleAdapter);
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                android.util.Log.d("EmployeeSchedule", "Pull-to-refresh triggered");
                loadScheduleData(true);
            });
        }
    }

    private void setupFirebaseListener() {
        firebaseListener = new FirebaseEmployeeListener(requireContext());
        firebaseListener.setOnScheduleChangeListener(new FirebaseEmployeeListener.OnScheduleChangeListener() {
            @Override
            public void onScheduleChanged() {
                android.util.Log.d("EmployeeSchedule", "Schedule changed detected via Firebase");
                loadScheduleData();
            }

            @Override
            public void onError(String error) {
                android.util.Log.e("EmployeeSchedule", "Firebase listener error: " + error);
            }
        });
    }

    private void setupClickListeners() {
        btnPreviousMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            updateCalendar();
            loadScheduleData();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            updateCalendar();
            loadScheduleData();
        });
    }

    private void initializeCalendar() {
        updateCalendar();
    }

    private void updateCalendar() {
        calendarDays.clear();

        Calendar calendar = (Calendar) currentCalendar.clone();
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        for (int i = 0; i < firstDayOfWeek; i++) {
            calendarDays.add(new CalendarAdapter.CalendarDay(0, 0, 0, false));
        }

        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        int currentMonth = calendar.get(Calendar.MONTH);
        int currentYear = calendar.get(Calendar.YEAR);

        for (int day = 1; day <= daysInMonth; day++) {
            CalendarAdapter.CalendarDay calendarDay = new CalendarAdapter.CalendarDay(
                    day, currentMonth, currentYear, true);

            String dateKey = calendarDay.getDateKey();
            if (scheduledTicketsMap.containsKey(dateKey)) {
                calendarDay.setScheduledTickets(scheduledTicketsMap.get(dateKey));
            }

            calendarDays.add(calendarDay);
        }

        while (calendarDays.size() < 42) {
            calendarDays.add(new CalendarAdapter.CalendarDay(0, 0, 0, false));
        }

        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthYear.setText(monthYearFormat.format(calendar.getTime()));

        ensureSelectedDateKey();
        applySelectionToCalendar();
        calendarAdapter.notifyDataSetChanged();
        updateDailyListForDateKey(selectedDateKey);
    }

    private void ensureSelectedDateKey() {
        if (selectedDateKey == null || !isDateKeyInCurrentMonth(selectedDateKey)) {
            String todayKey = getTodayDateKey();
            if (isDateKeyInCurrentMonth(todayKey)) {
                selectedDateKey = todayKey;
            } else {
                selectedDateKey = getFirstDayKey(currentCalendar);
            }
        }
    }

    private boolean isDateKeyInCurrentMonth(String dateKey) {
        int[] parts = parseDateKey(dateKey);
        if (parts == null) {
            return false;
        }
        int year = parts[0];
        int month = parts[1];
        return year == currentCalendar.get(Calendar.YEAR)
                && month == currentCalendar.get(Calendar.MONTH) + 1;
    }

    private int[] parseDateKey(String dateKey) {
        if (dateKey == null) {
            return null;
        }
        String[] parts = dateKey.split("-");
        if (parts.length != 3) {
            return null;
        }
        try {
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);
            return new int[] { year, month, day };
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String getFirstDayKey(Calendar calendar) {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        return String.format(Locale.getDefault(), "%04d-%02d-01", year, month);
    }

    private String getTodayDateKey() {
        Calendar today = Calendar.getInstance();
        return String.format(Locale.getDefault(), "%04d-%02d-%02d",
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH) + 1,
                today.get(Calendar.DAY_OF_MONTH));
    }

    private void applySelectionToCalendar() {
        for (CalendarAdapter.CalendarDay day : calendarDays) {
            if (day.isCurrentMonth()) {
                day.setSelected(day.getDateKey().equals(selectedDateKey));
            } else {
                day.setSelected(false);
            }
        }
    }

    private void updateDailyListForDateKey(@Nullable String dateKey) {
        if (dateKey == null) {
            tvSelectedDate.setText("Select a date");
            tvSelectedCount.setText("");
            tvDailyEmpty.setVisibility(View.VISIBLE);
            rvDailyJobs.setVisibility(View.GONE);
            dailyScheduleAdapter.setTickets(new ArrayList<>());
            return;
        }

        List<EmployeeScheduleResponse.ScheduledTicket> tickets = scheduledTicketsMap.get(dateKey);
        if (tickets == null) {
            tickets = new ArrayList<>();
        }

        tvSelectedDate.setText(formatDateKey(dateKey));
        tvSelectedCount.setText(tickets.size() + (tickets.size() == 1 ? " job" : " jobs"));

        if (tickets.isEmpty()) {
            tvDailyEmpty.setVisibility(View.VISIBLE);
            rvDailyJobs.setVisibility(View.GONE);
        } else {
            tvDailyEmpty.setVisibility(View.GONE);
            rvDailyJobs.setVisibility(View.VISIBLE);
        }

        dailyScheduleAdapter.setTickets(tickets);
    }

    private String formatDateKey(String dateKey) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault());
        try {
            String normalized = normalizeDateKey(dateKey);
            return outputFormat.format(inputFormat.parse(normalized));
        } catch (ParseException e) {
            return dateKey;
        }
    }

    private String normalizeDateKey(String rawDate) {
        if (rawDate == null) {
            return null;
        }

        String trimmed = rawDate.trim();
        String[] patterns = {
                "yyyy-MM-dd",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy/MM/dd"
        };
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (String pattern : patterns) {
            SimpleDateFormat inputFormat = new SimpleDateFormat(pattern, Locale.getDefault());
            inputFormat.setLenient(false);
            try {
                return outputFormat.format(inputFormat.parse(trimmed));
            } catch (ParseException ignored) {
                // Try next pattern.
            }
        }

        if (trimmed.length() >= 10) {
            return trimmed.substring(0, 10);
        }

        return trimmed;
    }

    private void loadScheduleData() {
        loadScheduleData(false);
    }

    private void loadScheduleData(boolean preserveUi) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
            return;
        }

        if (!preserveUi) {
            if (shimmerSchedule != null) {
                shimmerSchedule.setVisibility(View.VISIBLE);
                shimmerSchedule.startShimmer();
            }
            if (rvCalendarGrid != null) rvCalendarGrid.setVisibility(View.GONE);
            if (dailyListContainer != null) dailyListContainer.setVisibility(View.GONE);
        }

        android.util.Log.d("EmployeeSchedule", "Loading schedules from Firestore...");

        FirebaseFirestore.getInstance().collection("tickets")
            .whereEqualTo("assigned_staff_id", user.getUid())
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!isAdded()) return;
                
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                if (shimmerSchedule != null) {
                    shimmerSchedule.stopShimmer();
                    shimmerSchedule.setVisibility(View.GONE);
                }
                if (rvCalendarGrid != null) rvCalendarGrid.setVisibility(View.VISIBLE);
                if (dailyListContainer != null) dailyListContainer.setVisibility(View.VISIBLE);

                scheduledTicketsMap.clear();
                allBufferedTickets.clear();

                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    EmployeeScheduleResponse.ScheduledTicket ticket = doc.toObject(EmployeeScheduleResponse.ScheduledTicket.class);
                    if (ticket != null) {
                        ticket.setTicketId(doc.getId());
                        String dateKey = ticket.getScheduledDate();
                        if (dateKey != null) {
                            String normalized = normalizeDateKey(dateKey);
                            if (!scheduledTicketsMap.containsKey(normalized)) {
                                scheduledTicketsMap.put(normalized, new ArrayList<>());
                            }
                            scheduledTicketsMap.get(normalized).add(ticket);
                        }
                    }
                }

                allBufferedTickets.putAll(scheduledTicketsMap);
                updateCalendar();
                updateDailyListForDateKey(selectedDateKey);
            })
            .addOnFailureListener(e -> {
                if (!isAdded()) return;
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                if (shimmerSchedule != null) {
                    shimmerSchedule.stopShimmer();
                    shimmerSchedule.setVisibility(View.GONE);
                }
                android.util.Log.e("EmployeeSchedule", "Error loading schedules: " + e.getMessage());
            });
    }

    private void processScheduleData(List<EmployeeScheduleResponse.ScheduledTicket> tickets) {
        allBufferedTickets.clear();
        if (tickets != null) {
            for (EmployeeScheduleResponse.ScheduledTicket ticket : tickets) {
                if (ticket.getScheduledDate() != null) {
                    String dateKey = normalizeDateKey(ticket.getScheduledDate());
                    if (!allBufferedTickets.containsKey(dateKey)) {
                        allBufferedTickets.put(dateKey, new ArrayList<>());
                    }
                    allBufferedTickets.get(dateKey).add(ticket);
                }
            }
        }

        rebuildScheduleMap();
    }

    private void rebuildScheduleMap() {
        scheduledTicketsMap.clear();

        if (allBufferedTickets.isEmpty()) {
            showEmptyState();
            updateCalendar();
            return;
        }

        hideEmptyState();
        for (Map.Entry<String, List<EmployeeScheduleResponse.ScheduledTicket>> entry : allBufferedTickets.entrySet()) {
            String dateKey = entry.getKey();
            List<EmployeeScheduleResponse.ScheduledTicket> dayTickets = entry.getValue();
            if (dayTickets != null && !dayTickets.isEmpty()) {
                Collections.sort(dayTickets, Comparator.comparingInt(this::parseTimeToMinutes));
                scheduledTicketsMap.put(dateKey, dayTickets);
            }
        }

        android.util.Log.d("EmployeeSchedule", "Schedule entries: " + scheduledTicketsMap.size());
        updateCalendar();
    }

    private void openTicketDetail(String ticketId) {
        if (ticketId == null || ticketId.trim().isEmpty()) {
            Toast.makeText(getContext(), "Ticket details not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(getContext(), EmployeeTicketDetailActivity.class);
        intent.putExtra("ticket_id", ticketId);
        startActivity(intent);
    }

    private void showEmptyState() {
        emptyState.setVisibility(View.VISIBLE);
        rvCalendarGrid.setVisibility(View.VISIBLE);
    }

    private void hideEmptyState() {
        emptyState.setVisibility(View.GONE);
        rvCalendarGrid.setVisibility(View.VISIBLE);
    }

    private void setLoadingState(boolean isLoading) {
        if (shimmerSchedule == null || dailyListContainer == null) {
            return;
        }

        boolean showShimmer = isLoading && (swipeRefreshLayout == null || !swipeRefreshLayout.isRefreshing());
        if (showShimmer) {
            shimmerSchedule.setVisibility(View.VISIBLE);
            shimmerSchedule.startShimmer();
            dailyListContainer.setVisibility(View.GONE);
        } else {
            shimmerSchedule.stopShimmer();
            shimmerSchedule.setVisibility(View.GONE);
            dailyListContainer.setVisibility(View.VISIBLE);
        }
    }

    private int parseTimeToMinutes(EmployeeScheduleResponse.ScheduledTicket ticket) {
        if (ticket == null || ticket.getScheduledTime() == null) {
            return Integer.MAX_VALUE;
        }

        String rawTime = ticket.getScheduledTime().trim();
        String[] patterns = {
                "HH:mm",
                "HH:mm:ss",
                "hh:mm a",
                "h:mm a"
        };

        for (String pattern : patterns) {
            SimpleDateFormat inputFormat = new SimpleDateFormat(pattern, Locale.getDefault());
            inputFormat.setLenient(false);
            try {
                Date parsed = inputFormat.parse(rawTime);
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(parsed);
                return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
            } catch (ParseException ignored) {
                // Try next pattern.
            }
        }

        return Integer.MAX_VALUE;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (firebaseListener != null) {
            firebaseListener.startListening();
        }
        loadScheduleData(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (firebaseListener != null) {
            firebaseListener.stopListening();
        }
    }
}
