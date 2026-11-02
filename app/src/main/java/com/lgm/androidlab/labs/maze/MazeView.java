package com.lgm.androidlab.labs.maze;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.lgm.androidlab.R;
import com.lgm.androidlab.core.color.ColorMath;
import com.lgm.androidlab.core.maze.Cell;
import com.lgm.androidlab.core.maze.Direction;
import com.lgm.androidlab.core.maze.Maze;
import com.lgm.androidlab.core.maze.MazeSolver;

import java.util.List;

/** Desenha um {@link Maze} e anima a busca em largura (células visitadas → caminho final). */
public final class MazeView extends View {

    /** Avisa quando a animação da solução termina. */
    public interface Listener {
        void onSolutionRevealed(@NonNull MazeSolver.Solution solution);
    }

    private static final long FRAME_MS = 16;

    private final Paint wallPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint visitedPaint = new Paint();
    private final Paint pathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path pathShape = new Path();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable animate = this::onAnimate;

    @Nullable
    private Maze maze;
    @Nullable
    private MazeSolver.Solution solution;
    private int revealedVisited;
    private int revealedPath;
    private boolean animating;
    @Nullable
    private Listener listener;

    private float cellSize;
    private float offsetX;
    private float offsetY;

    public MazeView(Context context) {
        this(context, null);
    }

    public MazeView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MazeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        wallPaint.setColor(ContextCompat.getColor(context, R.color.maze_wall));
        wallPaint.setStyle(Paint.Style.STROKE);
        wallPaint.setStrokeCap(Paint.Cap.ROUND);
        int accent = ContextCompat.getColor(context, R.color.accent_maze);
        visitedPaint.setColor(ColorMath.withAlpha(accent, 0x38));
        pathPaint.setColor(accent);
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setStrokeCap(Paint.Cap.ROUND);
        pathPaint.setStrokeJoin(Paint.Join.ROUND);
        markerPaint.setStyle(Paint.Style.FILL);
        setBackgroundColor(ContextCompat.getColor(context, R.color.lab_canvas_bg));
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    public void setMaze(@NonNull Maze newMaze) {
        stopAnimation();
        maze = newMaze;
        solution = null;
        revealedVisited = 0;
        revealedPath = 0;
        computeGeometry();
        invalidate();
    }

    @Nullable
    public Maze maze() {
        return maze;
    }

    public boolean hasSolution() {
        return solution != null;
    }

    /** Mostra a solução com animação; {@code instant} pula a animação. */
    public void showSolution(@NonNull MazeSolver.Solution newSolution, boolean instant) {
        stopAnimation();
        solution = newSolution;
        if (instant) {
            revealedVisited = newSolution.visitedOrder().size();
            revealedPath = newSolution.path().size();
            invalidate();
            if (listener != null) {
                listener.onSolutionRevealed(newSolution);
            }
        } else {
            revealedVisited = 0;
            revealedPath = 0;
            animating = true;
            handler.post(animate);
        }
    }

    public void clearSolution() {
        stopAnimation();
        solution = null;
        revealedVisited = 0;
        revealedPath = 0;
        invalidate();
    }

    private void stopAnimation() {
        animating = false;
        handler.removeCallbacks(animate);
    }

    private void onAnimate() {
        if (!animating || solution == null) {
            return;
        }
        int totalVisited = solution.visitedOrder().size();
        int totalPath = solution.path().size();
        // Revela a busca em ~1,5 s e o caminho em ~0,7 s, independentemente do tamanho.
        int visitedPerFrame = Math.max(1, totalVisited / 90);
        int pathPerFrame = Math.max(1, totalPath / 40);
        if (revealedVisited < totalVisited) {
            revealedVisited = Math.min(totalVisited, revealedVisited + visitedPerFrame);
        } else if (revealedPath < totalPath) {
            revealedPath = Math.min(totalPath, revealedPath + pathPerFrame);
        } else {
            animating = false;
            invalidate();
            if (listener != null) {
                listener.onSolutionRevealed(solution);
            }
            return;
        }
        invalidate();
        handler.postDelayed(animate, FRAME_MS);
    }

    @Override
    protected void onDetachedFromWindow() {
        stopAnimation();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        computeGeometry();
    }

    private void computeGeometry() {
        if (maze == null || getWidth() == 0 || getHeight() == 0) {
            return;
        }
        float padding = getResources().getDisplayMetrics().density * 12;
        float availableW = getWidth() - padding * 2;
        float availableH = getHeight() - padding * 2;
        cellSize = Math.min(availableW / maze.width(), availableH / maze.height());
        offsetX = (getWidth() - cellSize * maze.width()) / 2f;
        offsetY = (getHeight() - cellSize * maze.height()) / 2f;
        wallPaint.setStrokeWidth(Math.max(1.5f, cellSize * 0.12f));
        pathPaint.setStrokeWidth(Math.max(2f, cellSize * 0.3f));
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (maze == null || cellSize <= 0) {
            return;
        }
        drawVisited(canvas);
        drawPath(canvas);
        drawMarkers(canvas);
        drawWalls(canvas);
    }

    private void drawVisited(Canvas canvas) {
        if (solution == null) {
            return;
        }
        List<Cell> visited = solution.visitedOrder();
        int n = Math.min(revealedVisited, visited.size());
        for (int i = 0; i < n; i++) {
            Cell c = visited.get(i);
            float left = offsetX + c.x() * cellSize;
            float top = offsetY + c.y() * cellSize;
            canvas.drawRect(left, top, left + cellSize, top + cellSize, visitedPaint);
        }
    }

    private void drawPath(Canvas canvas) {
        if (solution == null || revealedPath < 2) {
            return;
        }
        List<Cell> path = solution.path();
        int n = Math.min(revealedPath, path.size());
        pathShape.rewind();
        for (int i = 0; i < n; i++) {
            Cell c = path.get(i);
            float cx = offsetX + (c.x() + 0.5f) * cellSize;
            float cy = offsetY + (c.y() + 0.5f) * cellSize;
            if (i == 0) {
                pathShape.moveTo(cx, cy);
            } else {
                pathShape.lineTo(cx, cy);
            }
        }
        canvas.drawPath(pathShape, pathPaint);
    }

    private void drawMarkers(Canvas canvas) {
        if (maze == null) {
            return;
        }
        float r = cellSize * 0.28f;
        markerPaint.setColor(ContextCompat.getColor(getContext(), R.color.maze_start));
        canvas.drawCircle(offsetX + 0.5f * cellSize, offsetY + 0.5f * cellSize, r, markerPaint);
        markerPaint.setColor(ContextCompat.getColor(getContext(), R.color.maze_goal));
        canvas.drawCircle(offsetX + (maze.width() - 0.5f) * cellSize,
                offsetY + (maze.height() - 0.5f) * cellSize, r, markerPaint);
    }

    private void drawWalls(Canvas canvas) {
        if (maze == null) {
            return;
        }
        float right = offsetX + maze.width() * cellSize;
        float bottom = offsetY + maze.height() * cellSize;
        for (int y = 0; y < maze.height(); y++) {
            for (int x = 0; x < maze.width(); x++) {
                Cell cell = new Cell(x, y);
                float left = offsetX + x * cellSize;
                float top = offsetY + y * cellSize;
                if (!maze.isOpen(cell, Direction.NORTH)) {
                    canvas.drawLine(left, top, left + cellSize, top, wallPaint);
                }
                if (!maze.isOpen(cell, Direction.WEST)) {
                    canvas.drawLine(left, top, left, top + cellSize, wallPaint);
                }
            }
        }
        canvas.drawLine(offsetX, bottom, right, bottom, wallPaint);
        canvas.drawLine(right, offsetY, right, bottom, wallPaint);
    }
}
