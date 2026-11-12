package com.lgm.androidlab.core.physics;

/**
 * Vetor 2D imutável usado pelo motor de partículas.
 */
public record Vec2(double x, double y) {

    public static final Vec2 ZERO = new Vec2(0, 0);

    public Vec2 plus(Vec2 other) {
        return new Vec2(x + other.x, y + other.y);
    }

    public Vec2 minus(Vec2 other) {
        return new Vec2(x - other.x, y - other.y);
    }

    public Vec2 times(double factor) {
        return new Vec2(x * factor, y * factor);
    }

    public double dot(Vec2 other) {
        return x * other.x + y * other.y;
    }

    public double length() {
        return Math.hypot(x, y);
    }

    public Vec2 normalized() {
        double len = length();
        return len == 0 ? ZERO : times(1.0 / len);
    }

    public static Vec2 fromAngle(double radians, double magnitude) {
        return new Vec2(Math.cos(radians) * magnitude, Math.sin(radians) * magnitude);
    }
}
