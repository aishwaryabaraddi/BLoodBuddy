package com.example.bloodbuddy;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class DonorsAdapter extends RecyclerView.Adapter<DonorsAdapter.DonorViewHolder> {

    private Context context;
    private ArrayList<Donor> donorsList;

    public DonorsAdapter(Context context, ArrayList<Donor> donorsList) {
        this.context = context;
        this.donorsList = donorsList;
    }

    @NonNull
    @Override
    public DonorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.donor_item, parent, false);
        return new DonorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DonorViewHolder holder, int position) {
        Donor donor = donorsList.get(position);

        // Bind data to views
        holder.donorNameTextView.setText(donor.getName());
        holder.donorBloodGroupTextView.setText(donor.getBloodGroup());
        holder.donorPhoneNumberTextView.setText(donor.getPhoneNumber());

        // Handle call button click
        holder.callButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phoneNumber = donor.getPhoneNumber();
                if (phoneNumber != null && !phoneNumber.isEmpty()) {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + phoneNumber));
                    context.startActivity(intent);
                } else {
                    Toast.makeText(context, "Phone number not available", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return donorsList.size();
    }

    public static class DonorViewHolder extends RecyclerView.ViewHolder {
        TextView donorNameTextView;
        TextView donorBloodGroupTextView;
        TextView donorPhoneNumberTextView;
        TextView textView17 , textView18 , textView19;
        Button callButton;

        public DonorViewHolder(@NonNull View itemView) {
            super(itemView);
            donorNameTextView = itemView.findViewById(R.id.donorNameTextView);
            donorBloodGroupTextView = itemView.findViewById(R.id.donorBloodGroupTextView);
            donorPhoneNumberTextView = itemView.findViewById(R.id.donorPhoneNumberTextView);
            callButton = itemView.findViewById(R.id.callButton);
            textView17 = itemView.findViewById(R.id.textView17);
            textView18 = itemView.findViewById(R.id.textView18);
            textView19 = itemView.findViewById(R.id.textView19);
        }
    }
}
