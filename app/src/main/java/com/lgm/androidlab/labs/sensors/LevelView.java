package com.lgm.androidlab.labs.sensors;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.lgm.androidlab.R;
import com.lgm.androidlab.core.color.ColorMath;

/** Nível de bolha: a bolha segue o vetor gravidade lido do acelerômetro. */
public final class LevelView extends View {

    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint crossPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int bubbleLevelColor;
    private final int bubbleTiltedColor;

    // Direção da gravidade nos eixos da tela, em g; (0,0) = aparelho deitado.
    private float gx;
    private float gy;
    private float smoothGx;
    private float smoothGy;

    public LevelView(Context context) {
        this(context, null);
    }

    public LevelView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LevelView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        float density = getResources().getDisplayMetrics().density;
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(2 * density);
        ringPaint.setColor(ContextCompat.getColor(context, R.color.lab_grid_line_strong));
        crossPaint.setStyle(Paint.Style.STROKE);
        crossPaint.setStrokeWidth(density);
        crossPaint.setColor(ContextCompat.getColor(context, R.color.lab_grid_line_strong));
        bubbleLevelColor = ContextCompat.getColor(context, R.color.sort_sorted);
        bubbleTiltedColor = ContextCompat.getColor(context, R.color.accent_sensors);
        bubblePaint.setStyle(Paint.Style.FILL);
        setBackgroundColor(ContextCompat.getColor(context, R.color.lab_canvas_bg));
    }

    /** Componentes da gravidade nos eixos da tela, em g. */
    public void setGravity(float screenGx, float screenGy) {
        gx = clamp(screenGx);
        gy = clamp(screenGy);
        // Filtro passa-baixa simples para a bolha não tremer.
        smoothGx += (gx - smoothGx) * 0.25f;
        smoothGy += (gy - smoothGy) * 0.25f;
        invalidate();
    }

    /** Inclinação em graus em relação ao plano horizontal (0 = deitado). */
    public float tiltDegrees() {
        double mag = Math.min(1.0, Math.hypot(smoothGx, smoothGy));
        return (float) Math.toDegrees(Math.asin(mag));
    }

    private static float clamp(float v) {
        return Math.max(-1f, Math.min(1f, v));
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(cx, cy) * 0.85f;
        if (radius <= 0) {
            return;
        }
        canvas.drawCircle(cx, cy, radius, ringPaint);
        canvas.drawCircle(cx, cy, radius * 0.15f, ringPaint);
        canvas.drawLine(cx - radius, cy, cx + radius, cy, crossPaint);
        canvas.drawLine(cx, cy - radius, cx, cy + radius, crossPaint);

        float bubbleRadius = radius * 0.12f;
        float bx = cx + smoothGx * (radius - bubbleRadius);
        float by = cy + smoothGy * (radius - bubbleRadius);
        float tilt = (float) Math.min(1.0, Math.hypot(smoothGx, smoothGy));
        bubblePaint.setColor(ColorMath.lerp(bubbleLevelColor, bubbleTiltedColor, Math.min(1f, tilt * 4f)));
        canvas.drawCircle(bx, by, bubbleRadius, bubblePaint);
    }
}
