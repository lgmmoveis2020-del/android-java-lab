package com.lgm.androidlab.labs.life;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.lgm.androidlab.R;
import com.lgm.androidlab.core.life.LifeGrid;
import com.lgm.androidlab.core.life.Pattern;

import java.util.Random;

/** Desenha e anima um {@link LifeGrid}; toque alterna células, arraste pinta. */
public final class LifeView extends View {

    /** Notifica mudanças de geração/população. */
    public interface Listener {
        void onGridChanged(long generation, int population, boolean running);
    }

    private static final int COLUMNS = 48;
    private static final int MIN_ROWS = 12;

    private final Paint cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint();
    private final RectF cellRect = new RectF();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = this::onTick;
    private final Random random = new Random();

    @Nullable
    private LifeGrid grid;
    private float cellSize = 1f;
    private boolean running;
    private long intervalMs = 120;
    private int lastTouchedX = -1;
    private int lastTouchedY = -1;
    private boolean paintAlive = true;
    @Nullable
    private Listener listener;
    @Nullable
    private Pattern pendingPattern = Pattern.GOSPER_GUN;

    public LifeView(Context context) {
        this(context, null);
    }

    public LifeView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LifeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        cellPaint.setColor(ContextCompat.getColor(context, R.color.accent_life));
        gridPaint.setColor(ContextCompat.getColor(context, R.color.lab_grid_line));
        gridPaint.setStrokeWidth(1f);
        setBackgroundColor(ContextCompat.getColor(context, R.color.lab_canvas_bg));
    }

    // ------------------------------------------------------------------ API

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
        notifyListener();
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean run) {
        if (running == run) {
            return;
        }
        running = run;
        handler.removeCallbacks(tick);
        if (running) {
            handler.post(tick);
        }
        notifyListener();
    }

    /** Intervalo entre gerações em milissegundos (>= 16). */
    public void setIntervalMs(long ms) {
        intervalMs = Math.max(16, ms);
    }

    public void step() {
        if (grid != null) {
            grid.step();
            invalidate();
            notifyListener();
        }
    }

    public void clear() {
        if (grid != null) {
            grid.clear();
            invalidate();
            notifyListener();
        }
    }

    public void randomize(double density) {
        if (grid != null) {
            grid.randomize(random, density);
            invalidate();
            notifyListener();
        }
    }

    /** Limpa a grade e coloca o padrão centralizado; se a view ainda não tem tamanho, adia. */
    public void placePattern(@NonNull Pattern pattern) {
        if (grid == null) {
            pendingPattern = pattern;
            return;
        }
        grid.clear();
        if (pattern.width() <= grid.width() && pattern.height() <= grid.height()) {
            grid.placeCentered(pattern);
        }
        invalidate();
        notifyListener();
    }

    @Nullable
    public LifeGrid grid() {
        return grid;
    }

    // ------------------------------------------------------------------ ciclo de vida

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0 || h <= 0) {
            return;
        }
        cellSize = (float) w / COLUMNS;
        int rows = Math.max(MIN_ROWS, (int) Math.floor(h / cellSize));
        if (grid == null || grid.width() != COLUMNS || grid.height() != rows) {
            LifeGrid fresh = new LifeGrid(COLUMNS, rows);
            if (grid != null) {
                copyOverlap(grid, fresh);
            }
            grid = fresh;
            if (pendingPattern != null) {
                Pattern p = pendingPattern;
                pendingPattern = null;
                placePattern(p);
            }
            notifyListener();
        }
    }

    private static void copyOverlap(LifeGrid from, LifeGrid to) {
        int w = Math.min(from.width(), to.width());
        int h = Math.min(from.height(), to.height());
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                to.set(x, y, from.get(x, y));
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacks(tick);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (running) {
            handler.post(tick);
        }
    }

    private void onTick() {
        if (!running || grid == null) {
            return;
        }
        int changed = grid.step();
        invalidate();
        notifyListener();
        if (changed == 0 && grid.population() == 0) {
            setRunning(false);
            return;
        }
        handler.postDelayed(tick, intervalMs);
    }

    private void notifyListener() {
        if (listener != null) {
            long gen = grid == null ? 0 : grid.generation();
            int pop = grid == null ? 0 : grid.population();
            listener.onGridChanged(gen, pop, running);
        }
    }

    // ------------------------------------------------------------------ desenho

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (grid == null) {
            return;
        }
        float w = getWidth();
        float h = grid.height() * cellSize;
        for (int x = 0; x <= grid.width(); x++) {
            float px = x * cellSize;
            canvas.drawLine(px, 0, px, h, gridPaint);
        }
        for (int y = 0; y <= grid.height(); y++) {
            float py = y * cellSize;
            canvas.drawLine(0, py, w, py, gridPaint);
        }
        float inset = Math.max(0.5f, cellSize * 0.08f);
        float radius = cellSize * 0.2f;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if (grid.get(x, y)) {
                    cellRect.set(x * cellSize + inset, y * cellSize + inset,
                            (x + 1) * cellSize - inset, (y + 1) * cellSize - inset);
                    canvas.drawRoundRect(cellRect, radius, radius, cellPaint);
                }
            }
        }
    }

    // ------------------------------------------------------------------ toque

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (grid == null) {
            return super.onTouchEvent(event);
        }
        int cx = (int) Math.floor(event.getX() / cellSize);
        int cy = (int) Math.floor(event.getY() / cellSize);
        boolean inside = cx >= 0 && cy >= 0 && cx < grid.width() && cy < grid.height();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (inside) {
                    paintAlive = !grid.get(cx, cy);
                    grid.set(cx, cy, paintAlive);
                    lastTouchedX = cx;
                    lastTouchedY = cy;
                    invalidate();
                    notifyListener();
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (inside && (cx != lastTouchedX || cy != lastTouchedY)) {
                    grid.set(cx, cy, paintAlive);
                    lastTouchedX = cx;
                    lastTouchedY = cy;
                    invalidate();
                    notifyListener();
                }
                return true;
            case MotionEvent.ACTION_UP:
                lastTouchedX = -1;
                lastTouchedY = -1;
                performClick();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
