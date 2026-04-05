package com.example.bloodbuddy;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DonorAdapter extends RecyclerView.Adapter<DonorAdapter.DonorViewHolder> {

    private Context context;
    private List<Donor> donorList;

    public DonorAdapter(Context context, List<Donor> donorList) {
        this.context = context;
        this.donorList = donorList;
    }

    @NonNull
    @Override
    public DonorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_donor, parent, false);
        return new DonorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DonorViewHolder holder, int position) {
        Donor donor = donorList.get(position);
        holder.tvName.setText(donor.getName());
        holder.tvPhoneNumber.setText(donor.getPhoneNumber());

        // Set up Spinner adapters and listeners
        setupSpinner(holder.tvBloodGroup, R.array.blood_groups, donor.getBloodGroup());
        setupSpinner(holder.tvLocation, R.array.locations, donor.getDistrict() + ", " + donor.getTaluk());

        // Handle item click to open RequestActivity with donorId
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, RequestActivity.class);
                intent.putExtra("donorId", donor.getid()); // Pass donor ID to RequestActivity
                intent.putExtra("userId", donor.getRequestUserId()); // Pass user ID who requested
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return donorList.size();
    }

    public static class DonorViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPhoneNumber;
        Spinner tvBloodGroup, tvLocation;

        public DonorViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.etName);
            tvPhoneNumber = itemView.findViewById(R.id.etPhoneNumber);
            tvBloodGroup = itemView.findViewById(R.id.etBloodGroup);
            tvLocation = itemView.findViewById(R.id.etTaluk);
        }
    }

    private void setupSpinner(Spinner spinner, int arrayResId, String selectedValue) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context, arrayResId, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        if (selectedValue != null) {
            spinner.setSelection(getIndex(spinner, selectedValue));
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Handle spinner item selection if needed
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle case where nothing is selected
            }
        });
    }

    private int getIndex(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(value)) {
                return i;
            }
        }
        return 0; // Default to the first item if not found
    }
}
