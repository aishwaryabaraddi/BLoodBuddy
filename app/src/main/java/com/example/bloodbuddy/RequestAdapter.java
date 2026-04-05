package com.example.bloodbuddy;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {
    private List<Request> requestList;

    public static class RequestViewHolder extends RecyclerView.ViewHolder {
        public TextView requesterNameTextView;
        public TextView requesterPhoneTextView;
        public TextView requesterTalukTextView;
        public TextView messageTextView;

        public RequestViewHolder(View itemView) {
            super(itemView);
            requesterNameTextView = itemView.findViewById(R.id.requesterNameTextView);
            requesterPhoneTextView = itemView.findViewById(R.id.requesterPhoneNumberTextView);
            requesterTalukTextView = itemView.findViewById(R.id.requesterTalukTextView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
        }
    }

    public RequestAdapter(List<Request> requestList) {
        this.requestList = requestList;
    }

    @Override
    public RequestViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_request, parent, false);
        return new RequestViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RequestViewHolder holder, int position) {
        Request request = requestList.get(position);
        holder.requesterNameTextView.setText(request.getRequesterName());
        holder.requesterPhoneTextView.setText(request.getRequesterPhone());
        holder.requesterTalukTextView.setText(request.getRequesterTaluk());
        holder.messageTextView.setText(request.getMessage());
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }
}
