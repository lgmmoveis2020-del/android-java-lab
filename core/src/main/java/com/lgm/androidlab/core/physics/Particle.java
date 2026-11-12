package com.lgm.androidlab.core.physics;

/**
 * Uma partícula circular com posição, velocidade, raio e matiz (0..360).
 *
 * <p>É mutável de propósito: o motor roda a 60 fps e alocar objetos novos a cada
 * quadro geraria pressão desnecessária no garbage collector do Android.</p>
 */
public final class Particle {

    private double x;
    private double y;
    private double vx;
    private double vy;
    private final double radius;
    private final float hue;
    private double age;

    Particle(double x, double y, double vx, double vy, double radius, float hue) {
        if (radius <= 0) {
            throw new IllegalArgumentException("radius deve ser > 0");
        }
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.radius = radius;
        this.hue = ((hue % 360f) + 360f) % 360f;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double vx() {
        return vx;
    }

    public double vy() {
        return vy;
    }

    public double radius() {
        return radius;
    }

    /** Matiz HSV em graus, no intervalo [0, 360). */
    public float hue() {
        return hue;
    }

    /** Tempo de vida em segundos desde a criação. */
    public double age() {
        return age;
    }

    /** Massa proporcional à área (densidade unitária). */
    public double mass() {
        return radius * radius;
    }

    public double speed() {
        return Math.hypot(vx, vy);
    }

    public Vec2 position() {
        return new Vec2(x, y);
    }

    public Vec2 velocity() {
        return new Vec2(vx, vy);
    }

    void setPosition(double newX, double newY) {
        x = newX;
        y = newY;
    }

    void setVelocity(double newVx, double newVy) {
        vx = newVx;
        vy = newVy;
    }

    void grow(double dt) {
        age += dt;
    }

    @Override
    public String toString() {
        return String.format("Particle[pos=(%.1f, %.1f) vel=(%.1f, %.1f) r=%.1f]", x, y, vx, vy, radius);
    }
}
