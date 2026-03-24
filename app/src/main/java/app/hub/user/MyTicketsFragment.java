package app.hub.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import app.hub.R;
import app.hub.common.Ticket;
import app.hub.common.TicketAdapter;
import app.hub.util.TokenManager;

public class MyTicketsFragment extends Fragment {

    private RecyclerView ticketsRecyclerView;
    private TextView noTicketsTextView;
    private TicketAdapter ticketAdapter;
    private List<Ticket> ticketList = new ArrayList<>();
    private TokenManager tokenManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_tickets, container, false);

        ticketsRecyclerView = view.findViewById(R.id.ticketsRecyclerView);
        noTicketsTextView = view.findViewById(R.id.noTicketsTextView);
        tokenManager = new TokenManager(getContext());

        setupRecyclerView();
        loadTickets();

        return view;
    }

    private void setupRecyclerView() {
        ticketAdapter = new TicketAdapter(ticketList);
        ticketsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        ticketsRecyclerView.setAdapter(ticketAdapter);
    }

    private void loadTickets() {
        // This is where you would make an API call to get the tickets.
        // For now, let's use the same sample data.
        ticketList.add(new Ticket("Fix the printer", "The printer in the main office is jammed.", "Pending"));

        if (ticketList.isEmpty()) {
            noTicketsTextView.setVisibility(View.VISIBLE);
            ticketsRecyclerView.setVisibility(View.GONE);
        } else {
            noTicketsTextView.setVisibility(View.GONE);
            ticketsRecyclerView.setVisibility(View.VISIBLE);
            ticketAdapter.notifyDataSetChanged();
        }
    }
}
