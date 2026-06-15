package com.example.bloodbuddy;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class BloodInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.information);

        int primaryColor = ContextCompat.getColor(this, R.color.colorPrimary);

        // For the first TextView (textView10)
        TextView textView1 = findViewById(R.id.textView10);
        String text1 = "Health Benefits of Donating Blood\n\n" +
                "• May reveal health problems.\n" +
                "• Prevents Hemochromatosis.\n" +
                "• Maintain Cardiovascular health.\n" +
                "• May reduce the risk of developing cancer.\n" +
                "• Stimulates blood cell production.\n" +
                "• Maintains healthy liver.\n" +
                "• Weight loss.\n" +
                "• Help improve your mental state.";

        SpannableStringBuilder spannable1 = new SpannableStringBuilder(text1);
        int end1 = text1.indexOf("\n");
        if (end1 != -1) {
            spannable1.setSpan(new StyleSpan(Typeface.BOLD), 0, end1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable1.setSpan(new RelativeSizeSpan(1.25f), 0, end1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable1.setSpan(new ForegroundColorSpan(primaryColor), 0, end1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        textView1.setText(spannable1);

        // For the second TextView (textView8)
        TextView textView2 = findViewById(R.id.textView8);
        String text2 = "How much blood do we donate?\n\n" +
                "During a standard whole blood donation, you typically donate about 1 pint (approximately 470 millilitres) of blood. This amount is relatively safe and won’t significantly affect your overall health.\n\nYour body quickly replenishes the donated blood, usually within a few weeks. However, specific donation types like platelets or plasma may involve different amounts and procedures.";

        SpannableStringBuilder spannable2 = new SpannableStringBuilder(text2);
        int end2 = text2.indexOf("\n");
        if (end2 != -1) {
            spannable2.setSpan(new StyleSpan(Typeface.BOLD), 0, end2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable2.setSpan(new RelativeSizeSpan(1.25f), 0, end2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable2.setSpan(new ForegroundColorSpan(primaryColor), 0, end2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        textView2.setText(spannable2);

        // For the third TextView (textView7)
        TextView textView3 = findViewById(R.id.textView7);
        String text3 = "What is the age for blood donation?\n\n" +
                "The age for blood donation usually falls between 18 and 65 years. However, regulations can vary depending on the country and blood bank. Some places may allow 16-year-olds to donate with parental consent.\n\nDonors should be generally healthy, meet weight requirements, and not have specific medical conditions. Always check with your local blood donation center for specific age eligibility criteria.";

        SpannableStringBuilder spannable3 = new SpannableStringBuilder(text3);
        int end3 = text3.indexOf("\n");
        if (end3 != -1) {
            spannable3.setSpan(new StyleSpan(Typeface.BOLD), 0, end3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable3.setSpan(new RelativeSizeSpan(1.25f), 0, end3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable3.setSpan(new ForegroundColorSpan(primaryColor), 0, end3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        textView3.setText(spannable3);

        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        ImageView imageViewBack = findViewById(R.id.imageView10);
        imageViewBack.setOnClickListener(v -> finish());
    }
}
