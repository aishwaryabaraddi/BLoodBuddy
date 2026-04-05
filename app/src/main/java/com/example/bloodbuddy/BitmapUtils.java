package com.example.bloodbuddy;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;

public class BitmapUtils {

    public static BitmapDrawable getBitmapDrawable(Context context, int vectorDrawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, vectorDrawableId);
        if (drawable instanceof BitmapDrawable) {
            return (BitmapDrawable) drawable;
        }

        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return new BitmapDrawable(context.getResources(), bitmap);
    }
}
