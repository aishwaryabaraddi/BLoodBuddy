package com.example.bloodbuddy;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ChatAiActivity extends AppCompatActivity {

    private static final String TAG = "ChatAiActivity";
    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private List<ChatMessage> messages;
    private EditText etMessage;
    private ImageButton btnSend;
    private ProgressBar progressBar;
    
    private GenerativeModelFutures model;
    private final Executor executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_ai);
        String apiKey = BuildConfig.GEMINI_API_KEY;
        try {
            // gemini-2.5-flash: free tier (5 RPM / 250K TPM) — confirmed on this key.
            GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", apiKey);
            model = GenerativeModelFutures.from(gm);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing AI model", e);
        }

        recyclerView = findViewById(R.id.chat_recycler_view);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        progressBar = findViewById(R.id.progress_bar);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        messages = new ArrayList<>();
        adapter = new ChatAdapter(messages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        addMessage("Hello! I'm your Blood Buddy Assistant. I can help answer questions about blood donation eligibility. How can I help you today?", false);

        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessageToAI(text);
            }
        });
    }

    private void sendMessageToAI(String userText) {
        addMessage(userText, true);
        etMessage.setText("");
        progressBar.setVisibility(View.VISIBLE);

        if (model == null) {
            progressBar.setVisibility(View.GONE);
            addMessage("AI Error: Model not initialized. Check your API Key.", false);
            return;
        }

        try {
            Content userContent = new Content.Builder().addText(userText).build();
            ListenableFuture<GenerateContentResponse> responseFuture = model.generateContent(userContent);

            Futures.addCallback(responseFuture, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        try {
                            String aiResponse = result.getText();
                            if (aiResponse != null && !aiResponse.isEmpty()) {
                                addMessage(aiResponse, false);
                            } else {
                                addMessage("AI Error: No text returned. (Safety filters might have blocked it).", false);
                            }
                        } catch (Exception e) {
                            addMessage("AI Error: Failed to parse response: " + e.getMessage(), false);
                        }
                    });
                }

                @Override
                public void onFailure(Throwable t) {
                    Log.e(TAG, "AI Error Detail: ", t);
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        String raw = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
                        // Extract just the human-readable part from the API error JSON
                        String friendlyMsg;
                        if (raw.contains("\"message\":")) {
                            try {
                                int start = raw.indexOf("\"message\":\"") + 11;
                                int end   = raw.indexOf("\"", start);
                                friendlyMsg = raw.substring(start, end);
                            } catch (Exception ignored) { friendlyMsg = raw; }
                        } else {
                            friendlyMsg = raw;
                        }
                        addMessage("⚠️ " + friendlyMsg, false);
                    });
                }
            }, executor);
        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            addMessage("AI Error: " + e.getMessage(), false);
        }
    }

    private void addMessage(String text, boolean isUser) {
        messages.add(new ChatMessage(text, isUser));
        adapter.notifyItemInserted(messages.size() - 1);
        recyclerView.scrollToPosition(messages.size() - 1);
    }
}
