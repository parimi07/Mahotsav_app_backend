package com.eventadmin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RegistrationAdapter extends RecyclerView.Adapter<RegistrationAdapter.ViewHolder> {
    
    private List<Registration> registrations;
    
    public RegistrationAdapter(List<Registration> registrations) {
        this.registrations = registrations;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_registration, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Registration reg = registrations.get(position);
        holder.userIdText.setText(reg.userId);
        holder.nameText.setText(reg.name);
        holder.collegeText.setText(reg.college);
        holder.amountText.setText("₹" + reg.amount);
        holder.coordinatorText.setText(reg.coordinator);
    }
    
    @Override
    public int getItemCount() {
        return registrations.size();
    }
    
    public void updateList(List<Registration> newList) {
        this.registrations = newList;
        notifyDataSetChanged();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView userIdText;
        TextView nameText;
        TextView collegeText;
        TextView amountText;
        TextView coordinatorText;
        
        ViewHolder(View itemView) {
            super(itemView);
            userIdText = itemView.findViewById(R.id.userIdText);
            nameText = itemView.findViewById(R.id.nameText);
            collegeText = itemView.findViewById(R.id.collegeText);
            amountText = itemView.findViewById(R.id.amountText);
            coordinatorText = itemView.findViewById(R.id.coordinatorText);
        }
    }
}
