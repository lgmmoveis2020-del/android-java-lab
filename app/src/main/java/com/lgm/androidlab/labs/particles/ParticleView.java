package com.lgm.androidlab.labs.particles;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.lgm.androidlab.R;
import com.lgm.androidlab.core.color.ColorMath;
import com.lgm.androidlab.core.physics.Particle;
import com.lgm.androidlab.core.physics.ParticleSystem;
import com.lgm.androidlab.core.physics.Vec2;

import java.util.Random;

/**
 * Renderiza um {@link ParticleSystem} a 60 fps usando o {@link Choreographer}.
 * Toque/arraste cria explosões; a gravidade pode vir do acelerômetro.
 */
public final class ParticleView extends View {

    /** Observa estatísticas para o HUD da activity. */
    public interface StatsListener {
        void onStats(int count, double fps, double kineticEnergy);
    }

    private static final double GRAVITY_PIXELS = 980.0;
    private static final int TAP_BURST = 24;
    private static final int DRAG_BURST = 4;
    private static final long STATS_INTERVAL_MS = 250;

    private final ParticleSystem system = new ParticleSystem(new Random(), 800);
    private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Choreographer choreographer = Choreographer.getInstance();
    private final Choreographer.FrameCallback frameCallback = this::onFrame;

    private boolean running = true;
    private boolean attached;
    private boolean framePending;
    private long lastFrameNanos;
    private double smoothedFps;
    private long lastStatsMillis;
    @Nullable
    private StatsListener statsListener;

    public ParticleView(Context context) {
        this(context, null);
    }

    public ParticleView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ParticleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        particlePaint.setStyle(Paint.Style.FILL);
        glowPaint.setStyle(Paint.Style.FILL);
        float density = getResources().getDisplayMetrics().density;
        system.setRadiusRange(4 * density, 11 * density);
        system.setGravity(new Vec2(0, GRAVITY_PIXELS * density));
        setBackgroundColor(ContextCompat.getColor(context, R.color.lab_canvas_bg));
    }

    // ------------------------------------------------------------------ API para a activity

    public void setStatsListener(@Nullable StatsListener listener) {
        this.statsListener = listener;
    }

    public void setRunning(boolean run) {
        if (running == run) {
            return;
        }
        running = run;
        lastFrameNanos = 0;
        if (running) {
            scheduleFrame();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void setCollisionsEnabled(boolean enabled) {
        system.setCollisionsEnabled(enabled);
    }

    public void setBouncy(boolean bouncy) {
        system.setRestitution(bouncy ? 0.95 : 0.6);
        system.setAirDrag(bouncy ? 0.01 : 0.15);
    }

    /** Gravidade em unidades de g (eixo da tela): (0, 1) = para baixo. */
    public void setGravityDirection(double gx, double gy) {
        float density = getResources().getDisplayMetrics().density;
        system.setGravity(new Vec2(gx * GRAVITY_PIXELS * density, gy * GRAVITY_PIXELS * density));
    }

    public void clear() {
        system.clear();
        invalidate();
    }

    /** Explosão no centro da tela. */
    public void burstAtCenter() {
        if (getWidth() > 0 && getHeight() > 0) {
            float density = getResources().getDisplayMetrics().density;
            system.spawnBurst(getWidth() / 2.0, getHeight() / 2.0, TAP_BURST * 3, 420 * density);
            invalidate();
        }
    }

    public int particleCount() {
        return system.count();
    }

    // ------------------------------------------------------------------ ciclo de vida

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attached = true;
        lastFrameNanos = 0;
        scheduleFrame();
    }

    @Override
    protected void onDetachedFromWindow() {
        attached = false;
        framePending = false;
        choreographer.removeFrameCallback(frameCallback);
        super.onDetachedFromWindow();
    }

    private void scheduleFrame() {
        if (attached && !framePending) {
            framePending = true;
            choreographer.postFrameCallback(frameCallback);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            system.resize(w, h);
        }
    }

    private void onFrame(long frameTimeNanos) {
        framePending = false;
        if (!attached) {
            return;
        }
        if (running) {
            if (lastFrameNanos != 0) {
                double dt = (frameTimeNanos - lastFrameNanos) / 1_000_000_000.0;
                if (dt > 0) {
                    double instantFps = 1.0 / dt;
                    smoothedFps = smoothedFps == 0 ? instantFps : smoothedFps * 0.9 + instantFps * 0.1;
                }
                system.step(dt);
            }
            lastFrameNanos = frameTimeNanos;
            invalidate();
            publishStats();
            scheduleFrame();
        }
    }

    private void publishStats() {
        long now = System.currentTimeMillis();
        if (statsListener != null && now - lastStatsMillis >= STATS_INTERVAL_MS) {
            lastStatsMillis = now;
            statsListener.onStats(system.count(), smoothedFps, system.kineticEnergy());
        }
    }

    // ------------------------------------------------------------------ desenho

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        for (Particle p : system.particles()) {
            int color = ColorMath.hsvToArgb(p.hue(), 0.75f, 1f);
            float r = (float) p.radius();
            float x = (float) p.x();
            float y = (float) p.y();
            glowPaint.setColor(ColorMath.withAlpha(color, 0x40));
            canvas.drawCircle(x, y, r * 1.6f, glowPaint);
            particlePaint.setColor(color);
            canvas.drawCircle(x, y, r, particlePaint);
        }
    }

    // ------------------------------------------------------------------ toque

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float density = getResources().getDisplayMetrics().density;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                system.spawnBurst(event.getX(), event.getY(), TAP_BURST, 360 * density);
                if (!running) {
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < event.getPointerCount(); i++) {
                    system.spawnBurst(event.getX(i), event.getY(i), DRAG_BURST, 180 * density);
                }
                if (!running) {
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
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
