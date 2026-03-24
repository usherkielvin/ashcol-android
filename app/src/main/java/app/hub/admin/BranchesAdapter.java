package app.hub.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import app.hub.R;

public class BranchesAdapter extends RecyclerView.Adapter<BranchesAdapter.BranchViewHolder> {

    private List<BranchesActivity.Branch> branches;
    private OnBranchClickListener onBranchClickListener;

    public interface OnBranchClickListener {
        void onBranchClick(BranchesActivity.Branch branch);
    }

    public BranchesAdapter(List<BranchesActivity.Branch> branches, OnBranchClickListener listener) {
        this.branches = branches;
        this.onBranchClickListener = listener;
    }

    @NonNull
    @Override
    public BranchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_branch, parent, false);
        return new BranchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BranchViewHolder holder, int position) {
        BranchesActivity.Branch branch = branches.get(position);
        holder.bind(branch);
    }

    @Override
    public int getItemCount() {
        return branches.size();
    }

    class BranchViewHolder extends RecyclerView.ViewHolder {
        private TextView branchName;
        private TextView branchManager;
        private TextView employeeCount;

        public BranchViewHolder(@NonNull View itemView) {
            super(itemView);
            branchName = itemView.findViewById(R.id.branchName);
            branchManager = itemView.findViewById(R.id.branchManager);
            employeeCount = itemView.findViewById(R.id.employeeCount);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onBranchClickListener != null) {
                    onBranchClickListener.onBranchClick(branches.get(position));
                }
            });
        }

        public void bind(BranchesActivity.Branch branch) {
            branchName.setText(branch.getName());
            branchManager.setText("Manager: " + branch.getManager());
            employeeCount.setText(branch.getEmployeeCount() + " Employees");
        }
    }
}
