package com.lgm.androidlab.labs.sort;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.lgm.androidlab.R;
import com.lgm.androidlab.core.sort.SortStep;

import java.util.Collections;
import java.util.List;

/** Barras que reproduzem uma lista de {@link SortStep}s gravada pelo core. */
public final class SortView extends View {

    /** Progresso da reprodução. */
    public interface Listener {
        void onProgress(int cursor, int total, int comparisons, int writes, boolean running);
    }

    private static final long FRAME_MS = 16;

    private final Paint barPaint = new Paint();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = this::onTick;

    private final int colorBar;
    private final int colorCompare;
    private final int colorWrite;
    private final int colorSorted;

    private int[] original = new int[0];
    private int[] data = new int[0];
    private List<SortStep> steps = Collections.emptyList();
    private int cursor;
    private int comparisons;
    private int writes;
    private int stepsPerFrame = 4;
    private boolean running;
    @Nullable
    private SortStep lastStep;
    @Nullable
    private Listener listener;

    public SortView(Context context) {
        this(context, null);
    }

    public SortView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SortView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        colorBar = ContextCompat.getColor(context, R.color.accent_sort);
        colorCompare = ContextCompat.getColor(context, R.color.sort_compare);
        colorWrite = ContextCompat.getColor(context, R.color.sort_write);
        colorSorted = ContextCompat.getColor(context, R.color.sort_sorted);
        setBackgroundColor(ContextCompat.getColor(context, R.color.lab_canvas_bg));
    }

    // ------------------------------------------------------------------ API

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
        notifyListener();
    }

    /** Define os dados e os passos gravados; volta ao início. */
    public void load(@NonNull int[] input, @NonNull List<SortStep> recordedSteps) {
        pause();
        original = input.clone();
        data = input.clone();
        steps = recordedSteps;
        cursor = 0;
        comparisons = 0;
        writes = 0;
        lastStep = null;
        invalidate();
        notifyListener();
    }

    /** Passos aplicados por quadro (16 ms); >= 1. */
    public void setStepsPerFrame(int n) {
        stepsPerFrame = Math.max(1, n);
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isFinished() {
        return cursor >= steps.size();
    }

    public void play() {
        if (running) {
            return;
        }
        if (isFinished()) {
            reset();
        }
        running = true;
        handler.removeCallbacks(tick);
        handler.post(tick);
        notifyListener();
    }

    public void pause() {
        if (!running) {
            return;
        }
        running = false;
        handler.removeCallbacks(tick);
        notifyListener();
    }

    public void stepOnce() {
        pause();
        applySteps(1);
        invalidate();
        notifyListener();
    }

    public void reset() {
        pause();
        data = original.clone();
        cursor = 0;
        comparisons = 0;
        writes = 0;
        lastStep = null;
        invalidate();
        notifyListener();
    }

    @NonNull
    public int[] currentData() {
        return data.clone();
    }

    // ------------------------------------------------------------------ reprodução

    private void onTick() {
        if (!running) {
            return;
        }
        applySteps(stepsPerFrame);
        invalidate();
        if (isFinished()) {
            running = false;
            lastStep = null;
            notifyListener();
            return;
        }
        notifyListener();
        handler.postDelayed(tick, FRAME_MS);
    }

    private void applySteps(int count) {
        for (int k = 0; k < count && cursor < steps.size(); k++) {
            SortStep step = steps.get(cursor++);
            if (step.mutates()) {
                step.applyTo(data);
                writes++;
            } else {
                comparisons++;
            }
            lastStep = step;
        }
    }

    private void notifyListener() {
        if (listener != null) {
            listener.onProgress(cursor, steps.size(), comparisons, writes, running);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacks(tick);
        running = false;
        super.onDetachedFromWindow();
    }

    // ------------------------------------------------------------------ desenho

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int n = data.length;
        if (n == 0) {
            return;
        }
        int max = 1;
        for (int v : data) {
            max = Math.max(max, v);
        }
        float w = getWidth();
        float h = getHeight();
        float barWidth = w / n;
        float gap = barWidth > 4 ? Math.min(2f, barWidth * 0.15f) : 0f;
        boolean finished = isFinished() && !steps.isEmpty();
        int hi = lastStep == null ? -1 : lastStep.i();
        int hj = lastStep == null ? -1 : lastStep.j();
        int highlightColor = lastStep == null ? colorBar
                : (lastStep.mutates() ? colorWrite : colorCompare);
        for (int i = 0; i < n; i++) {
            float left = i * barWidth + gap / 2;
            float right = left + barWidth - gap;
            float barHeight = h * (0.05f + 0.93f * data[i] / max);
            if (finished) {
                barPaint.setColor(colorSorted);
            } else if (i == hi || i == hj) {
                barPaint.setColor(highlightColor);
            } else {
                barPaint.setColor(colorBar);
            }
            canvas.drawRect(left, h - barHeight, right, h, barPaint);
        }
    }
}
