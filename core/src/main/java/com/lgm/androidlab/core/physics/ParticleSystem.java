package com.lgm.androidlab.core.physics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Motor de partículas 2D determinístico (dado um {@link Random} com semente).
 *
 * <ul>
 *   <li>Integração semi-implícita de Euler com passo de tempo limitado.</li>
 *   <li>Gravidade configurável (no app ela vem do acelerômetro).</li>
 *   <li>Colisão com as paredes usando coeficiente de restituição.</li>
 *   <li>Colisão elástica entre partículas com broadphase em grade uniforme.</li>
 *   <li>Limite de partículas: ao estourar, as mais antigas são descartadas.</li>
 * </ul>
 *
 * <p>Todas as unidades são em pixels e segundos; a classe não conhece nada do Android.</p>
 */
public final class ParticleSystem {

    public static final int DEFAULT_MAX_PARTICLES = 600;
    /** Passo máximo de simulação (s). Protege contra "tunneling" quando o app trava um quadro. */
    public static final double MAX_STEP_SECONDS = 1.0 / 30.0;

    private final List<Particle> particles = new ArrayList<>();
    private final List<Particle> readOnlyView = Collections.unmodifiableList(particles);
    private final Random random;
    private final int maxParticles;

    private double width = 1;
    private double height = 1;
    private Vec2 gravity = new Vec2(0, 980);
    private double restitution = 0.8;
    private double airDrag = 0.05;
    private boolean collisionsEnabled = true;
    private double minRadius = 6;
    private double maxRadius = 16;
    private long stepsSimulated;

    public ParticleSystem(Random random) {
        this(random, DEFAULT_MAX_PARTICLES);
    }

    public ParticleSystem(Random random, int maxParticles) {
        if (maxParticles <= 0) {
            throw new IllegalArgumentException("maxParticles deve ser > 0");
        }
        this.random = Objects.requireNonNull(random, "random");
        this.maxParticles = maxParticles;
    }

    // ------------------------------------------------------------------ configuração

    /** Redefine o tamanho da "caixa" e empurra para dentro quem ficou fora. */
    public void resize(double newWidth, double newHeight) {
        if (newWidth <= 0 || newHeight <= 0) {
            throw new IllegalArgumentException("dimensões devem ser > 0");
        }
        width = newWidth;
        height = newHeight;
        for (Particle p : particles) {
            clampInside(p);
        }
    }

    public double width() {
        return width;
    }

    public double height() {
        return height;
    }

    public void setGravity(Vec2 newGravity) {
        gravity = Objects.requireNonNull(newGravity, "gravity");
    }

    public Vec2 gravity() {
        return gravity;
    }

    /** Fração da velocidade preservada ao bater na parede (0 = gruda, 1 = quique perfeito). */
    public void setRestitution(double value) {
        if (value < 0 || value > 1) {
            throw new IllegalArgumentException("restitution deve estar em [0, 1]");
        }
        restitution = value;
    }

    public double restitution() {
        return restitution;
    }

    /** Arrasto do ar como fração da velocidade perdida por segundo (0 = vácuo). */
    public void setAirDrag(double value) {
        if (value < 0 || value > 1) {
            throw new IllegalArgumentException("airDrag deve estar em [0, 1]");
        }
        airDrag = value;
    }

    public void setCollisionsEnabled(boolean enabled) {
        collisionsEnabled = enabled;
    }

    public boolean collisionsEnabled() {
        return collisionsEnabled;
    }

    public void setRadiusRange(double min, double max) {
        if (min <= 0 || max < min) {
            throw new IllegalArgumentException("intervalo de raio inválido");
        }
        minRadius = min;
        maxRadius = max;
    }

    public int maxParticles() {
        return maxParticles;
    }

    // ------------------------------------------------------------------ criação

    /**
     * Cria uma partícula com velocidade explícita. Se o limite for excedido, a partícula
     * mais antiga é removida.
     */
    public Particle spawn(double x, double y, double vx, double vy, double radius, float hue) {
        Particle p = new Particle(x, y, vx, vy, radius, hue);
        clampInside(p);
        if (particles.size() >= maxParticles) {
            particles.remove(0);
        }
        particles.add(p);
        return p;
    }

    /** Explosão radial de {@code count} partículas em torno de um ponto. */
    public List<Particle> spawnBurst(double x, double y, int count, double speed) {
        if (count < 0) {
            throw new IllegalArgumentException("count deve ser >= 0");
        }
        List<Particle> created = new ArrayList<>(count);
        float baseHue = random.nextFloat() * 360f;
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double magnitude = speed * (0.4 + random.nextDouble() * 0.6);
            Vec2 v = Vec2.fromAngle(angle, magnitude);
            double radius = minRadius + random.nextDouble() * (maxRadius - minRadius);
            float hue = baseHue + random.nextFloat() * 40f;
            created.add(spawn(x, y, v.x(), v.y(), radius, hue));
        }
        return created;
    }

    public void clear() {
        particles.clear();
    }

    // ------------------------------------------------------------------ simulação

    /**
     * Avança a simulação em {@code dt} segundos (limitado a {@link #MAX_STEP_SECONDS}).
     *
     * @return o dt efetivamente simulado
     */
    public double step(double dt) {
        if (dt <= 0 || Double.isNaN(dt)) {
            return 0;
        }
        double effective = Math.min(dt, MAX_STEP_SECONDS);
        double dragFactor = Math.pow(1.0 - airDrag, effective);

        for (Particle p : particles) {
            double vx = (p.vx() + gravity.x() * effective) * dragFactor;
            double vy = (p.vy() + gravity.y() * effective) * dragFactor;
            p.setVelocity(vx, vy);
            p.setPosition(p.x() + vx * effective, p.y() + vy * effective);
            p.grow(effective);
        }

        if (collisionsEnabled && particles.size() > 1) {
            resolveParticleCollisions();
        }

        for (Particle p : particles) {
            resolveWallCollision(p);
        }
        stepsSimulated++;
        return effective;
    }

    private void resolveWallCollision(Particle p) {
        double r = p.radius();
        double x = p.x();
        double y = p.y();
        double vx = p.vx();
        double vy = p.vy();

        if (x - r < 0) {
            x = r;
            vx = Math.abs(vx) * restitution;
        } else if (x + r > width) {
            x = width - r;
            vx = -Math.abs(vx) * restitution;
        }
        if (y - r < 0) {
            y = r;
            vy = Math.abs(vy) * restitution;
        } else if (y + r > height) {
            y = height - r;
            vy = -Math.abs(vy) * restitution;
        }
        p.setPosition(x, y);
        p.setVelocity(vx, vy);
    }

    /**
     * Broadphase em grade: cada célula tem o tamanho do maior diâmetro possível, então
     * só precisamos testar pares dentro da mesma célula ou de células vizinhas.
     */
    private void resolveParticleCollisions() {
        double cellSize = Math.max(1, maxRadius * 2);
        int cols = Math.max(1, (int) Math.ceil(width / cellSize));
        int rows = Math.max(1, (int) Math.ceil(height / cellSize));
        int[] head = new int[cols * rows];
        java.util.Arrays.fill(head, -1);
        int[] next = new int[particles.size()];

        for (int i = 0; i < particles.size(); i++) {
            Particle p = particles.get(i);
            int cell = cellIndex(p.x(), p.y(), cellSize, cols, rows);
            next[i] = head[cell];
            head[cell] = i;
        }

        for (int i = 0; i < particles.size(); i++) {
            Particle a = particles.get(i);
            int cx = clampIndex((int) (a.x() / cellSize), cols);
            int cy = clampIndex((int) (a.y() / cellSize), rows);
            for (int oy = -1; oy <= 1; oy++) {
                for (int ox = -1; ox <= 1; ox++) {
                    int nx = cx + ox;
                    int ny = cy + oy;
                    if (nx < 0 || ny < 0 || nx >= cols || ny >= rows) {
                        continue;
                    }
                    for (int j = head[ny * cols + nx]; j != -1; j = next[j]) {
                        if (j > i) {
                            collide(a, particles.get(j));
                        }
                    }
                }
            }
        }
    }

    private static int cellIndex(double x, double y, double cellSize, int cols, int rows) {
        int cx = clampIndex((int) (x / cellSize), cols);
        int cy = clampIndex((int) (y / cellSize), rows);
        return cy * cols + cx;
    }

    private static int clampIndex(int value, int size) {
        return value < 0 ? 0 : (value >= size ? size - 1 : value);
    }

    /** Colisão elástica entre dois círculos com separação posicional para evitar sobreposição. */
    private void collide(Particle a, Particle b) {
        double dx = b.x() - a.x();
        double dy = b.y() - a.y();
        double minDist = a.radius() + b.radius();
        double distSq = dx * dx + dy * dy;
        if (distSq >= minDist * minDist) {
            return;
        }
        double dist = Math.sqrt(distSq);
        double nx;
        double ny;
        if (dist == 0) {
            nx = 1;
            ny = 0;
            dist = 0.0001;
        } else {
            nx = dx / dist;
            ny = dy / dist;
        }

        double overlap = minDist - dist;
        double totalMass = a.mass() + b.mass();
        double pushA = overlap * (b.mass() / totalMass);
        double pushB = overlap * (a.mass() / totalMass);
        a.setPosition(a.x() - nx * pushA, a.y() - ny * pushA);
        b.setPosition(b.x() + nx * pushB, b.y() + ny * pushB);

        double rvx = b.vx() - a.vx();
        double rvy = b.vy() - a.vy();
        double velAlongNormal = rvx * nx + rvy * ny;
        if (velAlongNormal > 0) {
            return; // já estão se afastando
        }
        double impulse = -(1 + restitution) * velAlongNormal / (1 / a.mass() + 1 / b.mass());
        double ix = impulse * nx;
        double iy = impulse * ny;
        a.setVelocity(a.vx() - ix / a.mass(), a.vy() - iy / a.mass());
        b.setVelocity(b.vx() + ix / b.mass(), b.vy() + iy / b.mass());
    }

    private void clampInside(Particle p) {
        double r = p.radius();
        double x = Math.max(r, Math.min(width - r, p.x()));
        double y = Math.max(r, Math.min(height - r, p.y()));
        p.setPosition(x, y);
    }

    // ------------------------------------------------------------------ leitura

    /** Visão somente leitura; a lista subjacente muda a cada {@link #step(double)}. */
    public List<Particle> particles() {
        return readOnlyView;
    }

    public int count() {
        return particles.size();
    }

    public boolean isEmpty() {
        return particles.isEmpty();
    }

    public long stepsSimulated() {
        return stepsSimulated;
    }

    /** Energia cinética total: útil para mostrar no HUD e para testar conservação. */
    public double kineticEnergy() {
        double sum = 0;
        for (Particle p : particles) {
            double v2 = p.vx() * p.vx() + p.vy() * p.vy();
            sum += 0.5 * p.mass() * v2;
        }
        return sum;
    }
}
