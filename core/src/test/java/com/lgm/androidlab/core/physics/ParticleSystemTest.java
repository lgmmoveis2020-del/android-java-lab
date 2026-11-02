package com.lgm.androidlab.core.physics;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ParticleSystemTest {

    private ParticleSystem system;

    @BeforeEach
    void setUp() {
        system = new ParticleSystem(new Random(42), 50);
        system.resize(400, 300);
    }

    @Test
    @DisplayName("sem gravidade e sem arrasto, uma partícula se move em linha reta")
    void movesInStraightLineWithoutForces() {
        system.setGravity(Vec2.ZERO);
        system.setAirDrag(0);
        system.setCollisionsEnabled(false);
        Particle p = system.spawn(100, 100, 60, 0, 5, 0);

        for (int i = 0; i < 30; i++) {
            system.step(1.0 / 30.0);
        }

        assertEquals(160, p.x(), 1e-6);
        assertEquals(100, p.y(), 1e-6);
        assertEquals(1.0, p.age(), 1e-9);
    }

    @Test
    @DisplayName("gravidade acelera para baixo (y cresce)")
    void gravityAccelerates() {
        system.setAirDrag(0);
        system.setGravity(new Vec2(0, 100));
        Particle p = system.spawn(200, 50, 0, 0, 5, 0);

        system.step(0.02);
        double vyAfterOne = p.vy();
        system.step(0.02);

        assertEquals(2.0, vyAfterOne, 1e-9);
        assertEquals(4.0, p.vy(), 1e-9);
        assertTrue(p.y() > 50);
    }

    @Test
    @DisplayName("quica no chão respeitando a restituição")
    void bouncesOffFloorWithRestitution() {
        system.setGravity(Vec2.ZERO);
        system.setAirDrag(0);
        system.setRestitution(0.5);
        Particle p = system.spawn(200, 290, 0, 1200, 10, 0);

        system.step(1.0 / 30.0);

        assertEquals(290, p.y(), 1e-9, "fica encostada no chão (height - radius)");
        assertEquals(-600, p.vy(), 1e-9, "velocidade invertida e reduzida pela metade");
    }

    @Test
    @DisplayName("partículas nunca saem da caixa, mesmo com passos grandes")
    void particlesStayInsideBounds() {
        system.setGravity(new Vec2(300, 900));
        system.spawnBurst(200, 150, 50, 2000);

        for (int i = 0; i < 600; i++) {
            system.step(1.0 / 60.0);
            for (Particle p : system.particles()) {
                assertTrue(p.x() >= p.radius() - 1e-6 && p.x() <= system.width() - p.radius() + 1e-6, p::toString);
                assertTrue(p.y() >= p.radius() - 1e-6 && p.y() <= system.height() - p.radius() + 1e-6, p::toString);
            }
        }
    }

    @Test
    @DisplayName("passo de tempo é limitado a MAX_STEP_SECONDS")
    void stepIsClamped() {
        assertEquals(ParticleSystem.MAX_STEP_SECONDS, system.step(5.0), 1e-12);
        assertEquals(0.0, system.step(-1), 0.0);
        assertEquals(0.0, system.step(Double.NaN), 0.0);
    }

    @Test
    @DisplayName("ao exceder o máximo, a partícula mais antiga é descartada")
    void evictsOldestWhenFull() {
        Particle first = system.spawn(10, 10, 0, 0, 2, 0);
        for (int i = 0; i < 49; i++) {
            system.spawn(20, 20, 0, 0, 2, 0);
        }
        assertEquals(50, system.count());

        system.spawn(30, 30, 0, 0, 2, 0);

        assertEquals(50, system.count());
        assertFalse(system.particles().contains(first));
    }

    @Test
    @DisplayName("spawnBurst é determinístico para a mesma semente")
    void burstIsDeterministic() {
        ParticleSystem a = new ParticleSystem(new Random(7));
        ParticleSystem b = new ParticleSystem(new Random(7));
        a.resize(100, 100);
        b.resize(100, 100);

        List<Particle> pa = a.spawnBurst(50, 50, 10, 100);
        List<Particle> pb = b.spawnBurst(50, 50, 10, 100);

        for (int i = 0; i < 10; i++) {
            assertEquals(pa.get(i).vx(), pb.get(i).vx(), 0.0);
            assertEquals(pa.get(i).vy(), pb.get(i).vy(), 0.0);
            assertEquals(pa.get(i).hue(), pb.get(i).hue(), 0.0);
        }
    }

    @Test
    @DisplayName("colisão frontal elástica entre massas iguais troca as velocidades")
    void headOnElasticCollisionSwapsVelocities() {
        system.setGravity(Vec2.ZERO);
        system.setAirDrag(0);
        system.setRestitution(1.0);
        Particle a = system.spawn(100, 150, 100, 0, 10, 0);
        Particle b = system.spawn(130, 150, -100, 0, 10, 0);

        for (int i = 0; i < 10; i++) {
            system.step(1.0 / 60.0);
        }

        assertAll(
                () -> assertEquals(-100, a.vx(), 1e-6),
                () -> assertEquals(100, b.vx(), 1e-6),
                () -> assertTrue(a.x() < b.x(), "não atravessam uma à outra"));
    }

    @Test
    @DisplayName("colisões entre partículas conservam energia cinética quando restituição = 1")
    void collisionsConserveEnergy() {
        system.setGravity(Vec2.ZERO);
        system.setAirDrag(0);
        system.setRestitution(1.0);
        system.spawnBurst(200, 150, 40, 300);
        double before = system.kineticEnergy();

        for (int i = 0; i < 120; i++) {
            system.step(1.0 / 60.0);
        }

        assertEquals(before, system.kineticEnergy(), before * 0.01);
    }

    @Test
    @DisplayName("colisões desligadas permitem sobreposição")
    void collisionsCanBeDisabled() {
        system.setGravity(Vec2.ZERO);
        system.setAirDrag(0);
        system.setCollisionsEnabled(false);
        Particle a = system.spawn(100, 150, 100, 0, 10, 0);
        Particle b = system.spawn(130, 150, -100, 0, 10, 0);

        for (int i = 0; i < 10; i++) {
            system.step(1.0 / 60.0);
        }

        assertEquals(100, a.vx(), 1e-9);
        assertEquals(-100, b.vx(), 1e-9);
    }

    @Test
    @DisplayName("validações de argumentos")
    void validatesArguments() {
        assertThrows(IllegalArgumentException.class, () -> new ParticleSystem(new Random(), 0));
        assertThrows(IllegalArgumentException.class, () -> system.resize(0, 10));
        assertThrows(IllegalArgumentException.class, () -> system.setRestitution(1.5));
        assertThrows(IllegalArgumentException.class, () -> system.setAirDrag(-0.1));
        assertThrows(IllegalArgumentException.class, () -> system.spawnBurst(0, 0, -1, 10));
        assertThrows(IllegalArgumentException.class, () -> system.spawn(0, 0, 0, 0, 0, 0));
        assertThrows(UnsupportedOperationException.class, () -> system.particles().clear());
    }

    @Test
    @DisplayName("clear e resize")
    void clearAndResize() {
        Particle p = system.spawn(395, 295, 0, 0, 10, 0);
        system.resize(100, 100);
        assertEquals(90, p.x(), 1e-9);
        assertEquals(90, p.y(), 1e-9);

        system.clear();
        assertTrue(system.isEmpty());
        assertEquals(0, system.kineticEnergy(), 0.0);
    }

    @Test
    @DisplayName("Vec2 operações básicas")
    void vec2Math() {
        Vec2 v = new Vec2(3, 4);
        assertEquals(5, v.length(), 1e-12);
        assertEquals(new Vec2(6, 8), v.times(2));
        assertEquals(new Vec2(4, 6), v.plus(new Vec2(1, 2)));
        assertEquals(new Vec2(2, 2), v.minus(new Vec2(1, 2)));
        assertEquals(25, v.dot(v), 1e-12);
        assertEquals(1.0, v.normalized().length(), 1e-12);
        assertEquals(Vec2.ZERO, Vec2.ZERO.normalized());
        Vec2 fromAngle = Vec2.fromAngle(Math.PI / 2, 2);
        assertEquals(0, fromAngle.x(), 1e-12);
        assertEquals(2, fromAngle.y(), 1e-12);
    }

    @Test
    @DisplayName("matiz é normalizada para [0, 360)")
    void hueIsNormalized() {
        assertEquals(10f, system.spawn(1, 1, 0, 0, 1, 370f).hue(), 1e-5);
        assertEquals(350f, system.spawn(1, 1, 0, 0, 1, -10f).hue(), 1e-5);
    }
}
