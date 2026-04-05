package com.example.bloodbuddy;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class BloodInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.information);

        // For the first TextView (textView10)
        TextView textView1 = findViewById(R.id.textView10);
        String text1 = "Health Benefits of Donating Blood\n" +
                "1) May reveal health problems.\n" +
                "2) Prevents Hemochromatosis.\n" +
                "3) Maintain Cardiovascular health.\n" +
                "4) May reduce the risk of developing cancer.\n" +
                "5) Stimulates blood cell production.\n" +
                "6) Maintains healthy liver.\n" +
                "7) Weight loss.\n" +
                "8) Help improve your mental state.";

        // Create a SpannableStringBuilder to apply styling for textView10
        SpannableStringBuilder spannable1 = new SpannableStringBuilder(text1);

        // Apply bold style to the first line of textView10
        int start1 = 0;
        int end1 = text1.indexOf("\n"); // Find the end of the first line
        if (end1 == -1) {
            end1 = text1.length(); // If there's no newline, take the whole text
        }
        spannable1.setSpan(new StyleSpan(Typeface.BOLD), start1, end1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        spannable1.setSpan(new RelativeSizeSpan(1.5f), start1, end1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Set the styled text to textView10
        textView1.setText(spannable1);

        // For the second TextView (textView8)
        TextView textView2 = findViewById(R.id.textView8);
        String text2 = "How much blood do we donate?\n" +
                "During a standard whole blood donation, you typically donate about 1 pint (approximately 470 millilitres) of blood. This amount is relatively safe and won’t significantly affect your overall health. Your body quickly replenishes the donated blood, usually within a few weeks. However, specific donation types like platelets or plasma may involve different amounts and procedures.";

        // Create a SpannableStringBuilder to apply styling for textView8
        SpannableStringBuilder spannable2 = new SpannableStringBuilder(text2);

        // Apply bold style to the first line of textView8
        int start2 = 0;
        int end2 = text2.indexOf("\n"); // Find the end of the first line
        if (end2 == -1) {
            end2 = text2.length(); // If there's no newline, take the whole text
        }
        spannable2.setSpan(new StyleSpan(Typeface.BOLD), start2, end2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Increase the size of the first line of textView8
        spannable2.setSpan(new RelativeSizeSpan(1.5f), start2, end2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Set the styled text to textView8
        textView2.setText(spannable2);

        // For the third TextView (textView8)
        TextView textView3 = findViewById(R.id.textView7);
        String text3 = "What is the age for blood donation?\n" +
                "The age for blood donation usually falls between 18 and 65 years. However, regulations can vary depending on the country and blood bank. Some places may allow 16-year-olds to donate with parental consent. Donors should be generally healthy, meet weight requirements, and not have specific medical conditions. Always check with your local blood donation center for specific age eligibility criteria.";
        // Create a SpannableStringBuilder to apply styling for textView8
        SpannableStringBuilder spannable3 = new SpannableStringBuilder(text3);

        // Apply bold style to the first line of textView8
        int start3 = 0;
        int end3 = text3.indexOf("\n"); // Find the end of the first line
        if (end3 == -1) {
            end3 = text3.length(); // If there's no newline, take the whole text
        }
        spannable3.setSpan(new StyleSpan(Typeface.BOLD), start3, end3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Increase the size of the first line of textView8
        spannable3.setSpan(new RelativeSizeSpan(1.5f), start3, end3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // Set the styled text to textView8
        textView3.setText(spannable3);

        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        ImageView imageViewBack = findViewById(R.id.imageView10);
        imageViewBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to DomainActivity
                Intent intent = new Intent(BloodInfoActivity.this, DomainActivity.class);
                startActivity(intent);
            }
        });
    }
}
