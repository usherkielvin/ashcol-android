package app.hub.manager;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import app.hub.R;
import app.hub.api.TicketListResponse;

public class ManagerTicketsAdapter extends RecyclerView.Adapter<ManagerTicketsAdapter.ManagerTicketViewHolder> {

    private List<TicketListResponse.TicketItem> tickets;
    private OnTicketClickListener onTicketClickListener;

    public interface OnTicketClickListener {
        void onTicketClick(TicketListResponse.TicketItem ticket);
    }

    public ManagerTicketsAdapter(List<TicketListResponse.TicketItem> tickets) {
        this.tickets = tickets;
    }

    public void setOnTicketClickListener(OnTicketClickListener listener) {
        this.onTicketClickListener = listener;
    }

    @NonNull
    @Override
    public ManagerTicketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_employee_ticket, parent, false);
        return new ManagerTicketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ManagerTicketViewHolder holder, int position) {
        TicketListResponse.TicketItem ticket = tickets.get(position);
        holder.bind(ticket);

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (onTicketClickListener != null) {
                onTicketClickListener.onTicketClick(ticket);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tickets.size();
    }

    static class ManagerTicketViewHolder extends RecyclerView.ViewHolder {
        private TextView tvTitle;
        private TextView tvTicketId;
        private TextView tvServiceType;
        private TextView tvStatus;
        private TextView tvDate;
        private TextView tvDescription;
        private TextView tvCustomerName;
        private TextView tvAddress;
        private TextView tvScheduleDate;
        private TextView tvScheduleNotes;
        private android.widget.LinearLayout scheduleContainer;

        public ManagerTicketViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvTicketId = itemView.findViewById(R.id.tvTicketId);
            tvServiceType = itemView.findViewById(R.id.tvServiceType);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvAddress = itemView.findViewById(R.id.tvAddress);
            tvScheduleDate = itemView.findViewById(R.id.tvScheduleDate);
            tvScheduleNotes = itemView.findViewById(R.id.tvScheduleNotes);
            scheduleContainer = itemView.findViewById(R.id.scheduleContainer);

        }

        public void bind(TicketListResponse.TicketItem ticket) {
            tvTitle.setText(getTitleOrTicketId(ticket));
            tvTicketId.setText(ticket.getTicketId());
            tvServiceType.setText(buildServiceText(ticket));
            tvStatus.setText(buildStatusText(ticket));
            tvDescription.setText(ticket.getDescription());
            tvCustomerName
                    .setText("Customer: " + (ticket.getCustomerName() != null ? ticket.getCustomerName() : "Unknown"));
            if (tvAddress != null) {
                tvAddress.setText("Location: " + (ticket.getAddress() != null ? ticket.getAddress() : "Not specified"));
            }

            // Set status color
            String statusColor = ticket.getStatusColor();
            if (statusColor != null && !statusColor.isEmpty()) {
                try {
                    int color = Color.parseColor(statusColor);
                    tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
                    tvStatus.setTextColor(Color.WHITE);
                } catch (IllegalArgumentException e) {
                    setStatusColors(tvStatus, ticket.getStatus());
                }
            } else {
                setStatusColors(tvStatus, ticket.getStatus());
            }

            // Format date
            String formattedDate = formatDate(getHistoryDate(ticket));
            tvDate.setText("Updated: " + formattedDate);

            if (scheduleContainer != null) {
                scheduleContainer.setVisibility(View.GONE);
            }
        }

        private void setStatusColors(TextView textView, String status) {
            if (status == null)
                return;

            switch (status.toLowerCase()) {
                case "pending":
                case "open":
                    tintStatus(textView, "#FFA500");
                    break;
                case "accepted":
                case "in progress":
                case "ongoing":
                    tintStatus(textView, "#2196F3");
                    break;
                default:
                    tintStatus(textView, "#757575");
                    break;
            }
        }

        private void tintStatus(TextView textView, String colorHex) {
            int color = Color.parseColor(colorHex);
            textView.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
            textView.setTextColor(Color.WHITE);
        }

        private String buildStatusText(TicketListResponse.TicketItem ticket) {
            String status = ticket.getStatus();
            if (status == null || status.trim().isEmpty()) {
                return "Pending";
            }
            String normalized = status.trim().toLowerCase(Locale.ENGLISH);
            if (normalized.contains("ongoing") || normalized.contains("progress") || normalized.contains("accepted")) {
                return "Ongoing";
            }
            if (normalized.contains("pending") || normalized.contains("open")) {
                return "Pending";
            }
            return status.trim();
        }

        private String buildServiceText(TicketListResponse.TicketItem ticket) {
            String service = ticket.getServiceType();
            if (service == null || service.trim().isEmpty()) {
                service = ticket.getDescription();
            }
            if (service == null || service.trim().isEmpty()) {
                return "• Service";
            }
            return "• " + service.trim();
        }

        private String getHistoryDate(TicketListResponse.TicketItem ticket) {
            if (ticket.getUpdatedAt() != null && !ticket.getUpdatedAt().isEmpty()) {
                return ticket.getUpdatedAt();
            }
            return ticket.getCreatedAt();
        }

        private String getTitleOrTicketId(TicketListResponse.TicketItem ticket) {
            String title = ticket.getTitle();
            if (title != null && !title.trim().isEmpty()) {
                return title;
            }
            String ticketId = ticket.getTicketId();
            if (ticketId != null && !ticketId.trim().isEmpty()) {
                return ticketId;
            }
            return "Service";
        }

        private String formatDate(String dateString) {
            if (dateString == null || dateString.isEmpty()) {
                return "";
            }

            try {
                // Parse the date string (assuming ISO format from API)
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());

                Date date = inputFormat.parse(dateString);
                if (date != null) {
                    return outputFormat.format(date);
                }
            } catch (ParseException e) {
                // If parsing fails, try alternative format
                try {
                    SimpleDateFormat altInputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
                            Locale.getDefault());
                    SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());

                    Date date = altInputFormat.parse(dateString);
                    if (date != null) {
                        return outputFormat.format(date);
                    }
                } catch (ParseException ex) {
                    // Return original string if all parsing fails
                    return dateString;
                }
            }

            return dateString;
        }
    }
}
