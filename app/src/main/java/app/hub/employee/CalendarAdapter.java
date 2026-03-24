package app.hub.employee;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.Calendar;
import java.util.List;

import app.hub.R;
import app.hub.api.EmployeeScheduleResponse;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {

    private List<CalendarDay> calendarDays;
    private OnDayClickListener onDayClickListener;
    private int currentMonth;
    private int currentYear;

    public interface OnDayClickListener {
        void onDayClick(CalendarDay day);
    }

    public static class CalendarDay {
        private int dayOfMonth;
        private int month;
        private int year;
        private boolean isCurrentMonth;
        private boolean isSelected;
        private List<EmployeeScheduleResponse.ScheduledTicket> scheduledTickets;

        public CalendarDay(int dayOfMonth, int month, int year, boolean isCurrentMonth) {
            this.dayOfMonth = dayOfMonth;
            this.month = month;
            this.year = year;
            this.isCurrentMonth = isCurrentMonth;
            this.isSelected = false;
            this.scheduledTickets = null;
        }

        // Getters and setters
        public int getDayOfMonth() {
            return dayOfMonth;
        }

        public int getMonth() {
            return month;
        }

        public int getYear() {
            return year;
        }

        public boolean isCurrentMonth() {
            return isCurrentMonth;
        }

        public boolean isSelected() {
            return isSelected;
        }

        public void setSelected(boolean selected) {
            isSelected = selected;
        }

        public List<EmployeeScheduleResponse.ScheduledTicket> getScheduledTickets() {
            return scheduledTickets;
        }

        public void setScheduledTickets(List<EmployeeScheduleResponse.ScheduledTicket> tickets) {
            this.scheduledTickets = tickets;
        }

        public String getDateKey() {
            return String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
        }
    }

    public CalendarAdapter(List<CalendarDay> calendarDays, int currentMonth, int currentYear) {
        this.calendarDays = calendarDays;
        this.currentMonth = currentMonth;
        this.currentYear = currentYear;
    }

    public void setOnDayClickListener(OnDayClickListener listener) {
        this.onDayClickListener = listener;
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);
        return new CalendarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        CalendarDay day = calendarDays.get(position);
        holder.bind(day);

        holder.itemView.setOnClickListener(v -> {
            if (onDayClickListener != null && day.isCurrentMonth()) {
                // Clear previous selection
                for (CalendarDay d : calendarDays) {
                    d.setSelected(false);
                }
                day.setSelected(true);
                notifyDataSetChanged();
                onDayClickListener.onDayClick(day);
            }
        });
    }

    @Override
    public int getItemCount() {
        return calendarDays.size();
    }

    static class CalendarViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView cardDay;
        private TextView tvDayNumber;
        private View indicator1, indicator2, indicator3;
        private View indicatorContainer;

        public CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            cardDay = itemView.findViewById(R.id.cardDay);
            tvDayNumber = itemView.findViewById(R.id.tvDayNumber);
            indicator1 = itemView.findViewById(R.id.indicator1);
            indicator2 = itemView.findViewById(R.id.indicator2);
            indicator3 = itemView.findViewById(R.id.indicator3);
            indicatorContainer = itemView.findViewById(R.id.indicatorContainer);
        }

        public void bind(CalendarDay day) {
            // Hide placeholders for non-date cells
            if (day.getDayOfMonth() <= 0 || !day.isCurrentMonth()) {
                tvDayNumber.setText("");
                tvDayNumber.setVisibility(View.INVISIBLE);
                indicatorContainer.setVisibility(View.GONE);
                indicator1.setVisibility(View.GONE);
                indicator2.setVisibility(View.GONE);
                indicator3.setVisibility(View.GONE);
                cardDay.setCardBackgroundColor(Color.TRANSPARENT);
                cardDay.setStrokeWidth(0);
                return;
            }

            tvDayNumber.setVisibility(View.VISIBLE);
            // Set day number
            tvDayNumber.setText(String.valueOf(day.getDayOfMonth()));

            // Style based on month
            tvDayNumber.setTextColor(Color.parseColor("#333333"));
            cardDay.setCardBackgroundColor(Color.WHITE);
            cardDay.setStrokeColor(Color.parseColor("#EEEEEE"));

            // Highlight today
            Calendar today = Calendar.getInstance();
            if (day.getDayOfMonth() == today.get(Calendar.DAY_OF_MONTH) &&
                    day.getMonth() == today.get(Calendar.MONTH) &&
                    day.getYear() == today.get(Calendar.YEAR)) {
                cardDay.setCardBackgroundColor(Color.parseColor("#E8F5E8"));
                cardDay.setStrokeColor(Color.parseColor("#4CAF50"));
                cardDay.setStrokeWidth(2);
            } else if (day.isSelected()) {
                cardDay.setCardBackgroundColor(Color.parseColor("#1B4332"));
                tvDayNumber.setTextColor(Color.WHITE);
                cardDay.setStrokeColor(Color.parseColor("#1B4332"));
                cardDay.setStrokeWidth(2);
            }

            // Show indicators for scheduled tickets
            if (day.getScheduledTickets() != null && !day.getScheduledTickets().isEmpty()) {
                indicatorContainer.setVisibility(View.VISIBLE);
                List<EmployeeScheduleResponse.ScheduledTicket> tickets = day.getScheduledTickets();

                // Show up to 3 indicators
                if (tickets.size() >= 1) {
                    indicator1.setVisibility(View.VISIBLE);
                    setIndicatorColor(indicator1, tickets.get(0));
                }
                if (tickets.size() >= 2) {
                    indicator2.setVisibility(View.VISIBLE);
                    setIndicatorColor(indicator2, tickets.get(1));
                }
                if (tickets.size() >= 3) {
                    indicator3.setVisibility(View.VISIBLE);
                    setIndicatorColor(indicator3, tickets.get(2));
                }
            } else {
                indicatorContainer.setVisibility(View.GONE);
                indicator1.setVisibility(View.GONE);
                indicator2.setVisibility(View.GONE);
                indicator3.setVisibility(View.GONE);
            }
        }

        private void setIndicatorColor(View indicator, EmployeeScheduleResponse.ScheduledTicket ticket) {
            String status = ticket.getStatus() != null ? ticket.getStatus().toLowerCase() : "";

            if (status.contains("completed") || status.contains("resolved") || status.contains("closed")) {
                indicator
                        .setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            } else if (status.contains("cancel")) {
                indicator
                        .setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336")));
            } else if (status.contains("scheduled")) {
                indicator
                        .setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#6366F1")));
            } else if (status.contains("ongoing") || status.contains("progress") || status.contains("accepted")) {
                indicator
                        .setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#2196F3")));
            } else if (status.contains("pending")) {
                indicator
                        .setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FF9800")));
            } else {
                indicator
                        .setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
            }
            indicator.setBackgroundResource(R.drawable.shape_circle_gray);
        }
    }
}
