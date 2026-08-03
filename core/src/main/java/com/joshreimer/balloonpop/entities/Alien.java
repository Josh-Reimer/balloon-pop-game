package com.joshreimer.balloonpop.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

/**
 * A rare bonus target that drifts down slowly under a parachute instead of falling freely like a
 * {@link Balloon} — see {@code GameScreen.spawnAlien}. Its body is a green-to-blue vertical
 * gradient, built by feeding per-vertex colours (derived from each vertex's world Y) into
 * {@link ShapeRenderer}'s coloured triangle/rect overloads rather than a single flat fill. It
 * squirms in its harness while falling, and if it isn't popped in time it lands, sheds the
 * parachute, and waddles off screen instead of simply vanishing.
 */
public class Alien {
    public static final float BODY_WIDTH = 22f;
    public static final float BODY_HEIGHT = 30f;
    public static final float HEAD_RX = 14f;
    public static final float HEAD_RY = 20f;
    public static final float ARM_LENGTH = 19f;
    public static final float LEG_LENGTH = 21f;
    public static final float CANOPY_WIDTH = 60f;
    public static final float CANOPY_HEIGHT = 26f;
    public static final float RIG_LENGTH = 22f;
    public static final float RADIUS = 24f; // collision radius, torso-centred

    public static final int POINTS = 25;

    private static final float POP_DURATION = 0.2f;

    // Parachute sway while falling.
    private static final float SWAY_AMPLITUDE = 14f;
    private static final float SWAY_FREQUENCY = 1.1f;

    // Squirm-in-harness wriggle: every limb (plus the head and torso) gets its own frequency and
    // a random phase per instance, so a batch of aliens never wriggles in lockstep.
    private static final float ARM_WRIGGLE_FREQ_L = 5.3f;
    private static final float ARM_WRIGGLE_FREQ_R = 4.6f;
    private static final float ARM_WRIGGLE_AMP = 0.6f;
    private static final float ARM_BASE_SPLAY = 0.55f;
    private static final float LEG_WRIGGLE_FREQ_L = 6.1f;
    private static final float LEG_WRIGGLE_FREQ_R = 5.5f;
    private static final float LEG_WRIGGLE_AMP = 0.5f;
    private static final float LEG_BASE_SPLAY = 0.25f;
    private static final float HEAD_WOBBLE_FREQ = 3.4f;
    private static final float HEAD_WOBBLE_AMP = 0.18f;
    private static final float BODY_WOBBLE_FREQ = 2.7f;
    private static final float BODY_WOBBLE_AMP = 5f;
    private static final float LIMB_WIDTH = 3.2f;

    // Approximate distance from the torso centre down to the feet with legs hanging straight,
    // used both to trigger landing and to snap the alien onto the ground at that moment.
    private static final float FEET_OFFSET = BODY_HEIGHT / 2f + LEG_LENGTH;

    // Waddling-away walk cycle, once landed.
    private static final float WALK_SPEED = 26f;
    private static final float STRIDE_LENGTH = 26f;
    private static final float LEG_SWING_AMP = 0.5f;
    private static final float ARM_SWING_AMP = 0.4f;
    private static final float BODY_BOB_AMOUNT = 3f;
    private static final float HEAD_WALK_LEAN = 0.15f;
    private static final float WALK_OFFSCREEN_MARGIN = BODY_WIDTH + ARM_LENGTH;

    // The shed parachute drifts up and shrinks away rather than just disappearing.
    private static final float PARACHUTE_DRIFT_DISTANCE = 50f;
    private static final float PARACHUTE_FADE_DURATION = 0.6f;

    private static final Color TOP_COLOR = new Color(0.25f, 0.85f, 0.4f, 1f);
    private static final Color BOTTOM_COLOR = new Color(0.2f, 0.5f, 0.9f, 1f);
    private static final Color CANOPY_EDGE_COLOR = new Color(0.15f, 0.35f, 0.55f, 1f);
    private static final Color EYE_COLOR = new Color(0.05f, 0.08f, 0.1f, 1f);

    private enum Phase { FALLING, WALKING }

    private final float baseX;
    private final float groundY;
    public float x, y;
    public float fallSpeed;

    public boolean alive = true;
    public boolean popping = false;
    private float popTimer = 0f;
    private float age = 0f;

    private Phase phase = Phase.FALLING;
    private float walkDir = 1f;
    private float walkedDistance = 0f;
    private float parachuteFade = 0f;
    private float detachedCanopyX, detachedCanopyY;

    private final float swayPhase;
    private final float armPhaseL;
    private final float armPhaseR;
    private final float legPhaseL;
    private final float legPhaseR;
    private final float headPhase;
    private final float bodyPhase;

    private final Color tmpColor = new Color();
    private final Color gradCenter = new Color();
    private final Color gradPrev = new Color();
    private final Color gradCur = new Color();

    public Alien(float x, float y, float fallSpeed, float groundY) {
        this.baseX = x;
        this.x = x;
        this.y = y;
        this.fallSpeed = fallSpeed;
        this.groundY = groundY;
        this.swayPhase = MathUtils.random(MathUtils.PI2);
        this.armPhaseL = MathUtils.random(MathUtils.PI2);
        this.armPhaseR = MathUtils.random(MathUtils.PI2);
        this.legPhaseL = MathUtils.random(MathUtils.PI2);
        this.legPhaseR = MathUtils.random(MathUtils.PI2);
        this.headPhase = MathUtils.random(MathUtils.PI2);
        this.bodyPhase = MathUtils.random(MathUtils.PI2);
    }

    public void update(float delta) {
        if (popping) {
            popTimer += delta;
            if (popTimer >= POP_DURATION) {
                alive = false;
            }
            return;
        }

        if (phase == Phase.FALLING) {
            age += delta;
            y -= fallSpeed * delta;
            x = baseX + MathUtils.sin(age * SWAY_FREQUENCY + swayPhase) * SWAY_AMPLITUDE;
            if (y - FEET_OFFSET <= groundY) {
                land();
            }
        } else {
            x += walkDir * WALK_SPEED * delta;
            walkedDistance += WALK_SPEED * delta;
            if (parachuteFade > 0f) {
                parachuteFade = Math.max(0f, parachuteFade - delta / PARACHUTE_FADE_DURATION);
            }
        }
    }

    /** Cuts the parachute loose, snaps the alien onto the ground, and sends it off waddling. */
    private void land() {
        detachedCanopyX = x;
        detachedCanopyY = y + BODY_HEIGHT / 2f + RIG_LENGTH;
        phase = Phase.WALKING;
        y = groundY + FEET_OFFSET;
        walkDir = MathUtils.randomBoolean() ? 1f : -1f;
        walkedDistance = 0f;
        parachuteFade = 1f;
    }

    public void pop() {
        if (!popping) {
            popping = true;
            popTimer = 0f;
        }
    }

    /** True once a landed alien has waddled fully past either world edge. */
    public boolean isOffScreen(float worldWidth) {
        if (phase != Phase.WALKING) return false;
        return x + WALK_OFFSCREEN_MARGIN < 0 || x - WALK_OFFSCREEN_MARGIN > worldWidth;
    }

    public boolean overlaps(float ox, float oy, float otherRadius) {
        float dx = x - ox;
        float dy = y - oy;
        float distSq = dx * dx + dy * dy;
        float rSum = RADIUS + otherRadius;
        return distSq <= rSum * rSum;
    }

    /** Gradient colour for a vertex at world height vy, interpolated across [botY, topY]. */
    private Color colorAt(float vy, float topY, float botY) {
        float t = MathUtils.clamp((vy - botY) / (topY - botY), 0f, 1f);
        return tmpColor.set(BOTTOM_COLOR).lerp(TOP_COLOR, t);
    }

    public void render(ShapeRenderer sr) {
        float scale = popping ? Math.max(0f, 1f - popTimer / POP_DURATION) : 1f;
        if (popping && scale <= 0f) return;

        if (phase == Phase.FALLING) {
            renderFalling(sr, scale);
        } else {
            renderWalking(sr, scale);
        }
    }

    private void renderFalling(ShapeRenderer sr, float scale) {
        // The torso wobbles independently of the canopy (which only sways as a whole), so the
        // harness reads as taut and the body underneath looks like it's genuinely squirming.
        float figureX = x + MathUtils.sin(age * BODY_WOBBLE_FREQ + bodyPhase) * BODY_WOBBLE_AMP * scale;

        float canopyY = y + BODY_HEIGHT / 2f * scale + RIG_LENGTH * scale;
        float topY = canopyY + CANOPY_HEIGHT * scale;
        float botY = y - BODY_HEIGHT / 2f * scale - LEG_LENGTH * scale;

        float shoulderY = y + BODY_HEIGHT / 2f * scale;
        float shoulderLX = figureX - BODY_WIDTH / 3f * scale;
        float shoulderRX = figureX + BODY_WIDTH / 3f * scale;

        Color rigColor = colorAt(canopyY, topY, botY);
        sr.setColor(rigColor);
        sr.rectLine(x - CANOPY_WIDTH / 2f * scale, canopyY, shoulderLX, shoulderY, 1.2f);
        sr.rectLine(x + CANOPY_WIDTH / 2f * scale, canopyY, shoulderRX, shoulderY, 1.2f);

        filledGradientDome(sr, x, canopyY, CANOPY_WIDTH / 2f * scale, CANOPY_HEIGHT * scale, 10, topY, botY);
        sr.setColor(CANOPY_EDGE_COLOR);
        sr.rectLine(x - CANOPY_WIDTH / 2f * scale, canopyY, x + CANOPY_WIDTH / 2f * scale, canopyY, 1.5f);

        // Legs drawn first so they read as tucked behind the torso, kicking under it.
        float hipY = y - BODY_HEIGHT / 2f * scale;
        float hipLX = figureX - BODY_WIDTH / 2f * 0.35f * scale;
        float hipRX = figureX + BODY_WIDTH / 2f * 0.35f * scale;
        float legThetaL = -LEG_BASE_SPLAY + MathUtils.sin(age * LEG_WRIGGLE_FREQ_L + legPhaseL) * LEG_WRIGGLE_AMP;
        float legThetaR = LEG_BASE_SPLAY + MathUtils.sin(age * LEG_WRIGGLE_FREQ_R + legPhaseR) * LEG_WRIGGLE_AMP;
        renderLimb(sr, hipLX, hipY, legThetaL, LEG_LENGTH * scale, LIMB_WIDTH * scale, topY, botY);
        renderLimb(sr, hipRX, hipY, legThetaR, LEG_LENGTH * scale, LIMB_WIDTH * scale, topY, botY);

        filledGradientEllipse(sr, figureX, y, BODY_WIDTH / 2f * scale, BODY_HEIGHT / 2f * scale, 16, topY, botY);

        float armThetaL = -ARM_BASE_SPLAY + MathUtils.sin(age * ARM_WRIGGLE_FREQ_L + armPhaseL) * ARM_WRIGGLE_AMP;
        float armThetaR = ARM_BASE_SPLAY + MathUtils.sin(age * ARM_WRIGGLE_FREQ_R + armPhaseR) * ARM_WRIGGLE_AMP;
        renderLimb(sr, shoulderLX, shoulderY, armThetaL, ARM_LENGTH * scale, LIMB_WIDTH * scale, topY, botY);
        renderLimb(sr, shoulderRX, shoulderY, armThetaR, ARM_LENGTH * scale, LIMB_WIDTH * scale, topY, botY);

        float headTilt = MathUtils.sin(age * HEAD_WOBBLE_FREQ + headPhase) * HEAD_WOBBLE_AMP;
        float headY = y + BODY_HEIGHT / 2f * scale + HEAD_RY * 0.7f * scale;
        renderHead(sr, figureX, headY, headTilt, scale, topY, botY);
        if (!popping) {
            renderEyes(sr, figureX, headY, headTilt, scale);
        }
    }

    private void renderWalking(ShapeRenderer sr, float scale) {
        if (parachuteFade > 0f) {
            renderDetachedParachute(sr);
        }

        float facing = walkDir;
        float phase = walkedDistance / STRIDE_LENGTH * MathUtils.PI2;
        float bodyY = y + Math.abs(MathUtils.sin(phase)) * BODY_BOB_AMOUNT * scale;

        float topY = bodyY + BODY_HEIGHT / 2f * scale + HEAD_RY * 1.8f * scale;
        float botY = bodyY - BODY_HEIGHT / 2f * scale - LEG_LENGTH * scale;

        float hipY = bodyY - BODY_HEIGHT / 2f * scale;
        float hipLX = x - BODY_WIDTH / 2f * 0.35f * scale;
        float hipRX = x + BODY_WIDTH / 2f * 0.35f * scale;
        float legThetaL = facing * MathUtils.sin(phase) * LEG_SWING_AMP;
        float legThetaR = facing * MathUtils.sin(phase + MathUtils.PI) * LEG_SWING_AMP;
        renderLimb(sr, hipLX, hipY, legThetaL, LEG_LENGTH * scale, LIMB_WIDTH * scale, topY, botY);
        renderLimb(sr, hipRX, hipY, legThetaR, LEG_LENGTH * scale, LIMB_WIDTH * scale, topY, botY);

        filledGradientEllipse(sr, x, bodyY, BODY_WIDTH / 2f * scale, BODY_HEIGHT / 2f * scale, 16, topY, botY);

        float shoulderY = bodyY + BODY_HEIGHT / 2f * scale;
        float shoulderLX = x - BODY_WIDTH / 3f * scale;
        float shoulderRX = x + BODY_WIDTH / 3f * scale;
        float armThetaL = facing * MathUtils.sin(phase + MathUtils.PI) * ARM_SWING_AMP;
        float armThetaR = facing * MathUtils.sin(phase) * ARM_SWING_AMP;
        renderLimb(sr, shoulderLX, shoulderY, armThetaL, ARM_LENGTH * scale, LIMB_WIDTH * scale, topY, botY);
        renderLimb(sr, shoulderRX, shoulderY, armThetaR, ARM_LENGTH * scale, LIMB_WIDTH * scale, topY, botY);

        float headTilt = facing * HEAD_WALK_LEAN;
        float headY = bodyY + BODY_HEIGHT / 2f * scale + HEAD_RY * 0.7f * scale;
        renderHead(sr, x, headY, headTilt, scale, topY, botY);
        if (!popping) {
            renderEyes(sr, x, headY, headTilt, scale);
        }
    }

    /** The cut-loose canopy, drifting up and shrinking away above wherever the alien landed. */
    private void renderDetachedParachute(ShapeRenderer sr) {
        float driftY = (1f - parachuteFade) * PARACHUTE_DRIFT_DISTANCE;
        float cy = detachedCanopyY + driftY;
        filledGradientDome(sr, detachedCanopyX, cy, CANOPY_WIDTH / 2f * parachuteFade, CANOPY_HEIGHT * parachuteFade,
                10, cy + CANOPY_HEIGHT, cy - CANOPY_HEIGHT);
    }

    /** A limb hinged at (originX, originY): theta is a rotation away from hanging straight down. */
    private void renderLimb(ShapeRenderer sr, float originX, float originY, float theta, float length,
                             float width, float topY, float botY) {
        float endX = originX + MathUtils.sin(theta) * length;
        float endY = originY - MathUtils.cos(theta) * length;
        sr.setColor(colorAt((originY + endY) / 2f, topY, botY));
        sr.rectLine(originX, originY, endX, endY, width);
    }

    /**
     * The classic elongated grey-alien head: wide bulbous cranium tapering to a narrow chin,
     * built by shrinking the horizontal radius toward the bottom of the profile. Supports
     * rotation so it can wobble (falling) or lean into a stride (walking).
     */
    private void renderHead(ShapeRenderer sr, float cx, float cy, float rotation, float scale, float topY, float botY) {
        int segments = 22;
        float rx = HEAD_RX * scale;
        float ry = HEAD_RY * scale;
        float cosR = MathUtils.cos(rotation);
        float sinR = MathUtils.sin(rotation);

        gradCenter.set(colorAt(cy, topY, botY));

        float prevLX = rx * headWidthFactor(0f);
        float prevLY = 0f;
        float prevX = cx + (prevLX * cosR - prevLY * sinR);
        float prevY = cy + (prevLX * sinR + prevLY * cosR);
        gradPrev.set(colorAt(prevY, topY, botY));

        for (int i = 1; i <= segments; i++) {
            float angle = i * MathUtils.PI2 / segments;
            float v = MathUtils.sin(angle);
            float lx = MathUtils.cos(angle) * rx * headWidthFactor(v);
            float ly = v * ry;
            float vx = cx + (lx * cosR - ly * sinR);
            float vy = cy + (lx * sinR + ly * cosR);
            gradCur.set(colorAt(vy, topY, botY));
            sr.triangle(cx, cy, prevX, prevY, vx, vy, gradCenter, gradPrev, gradCur);
            prevX = vx;
            prevY = vy;
            gradPrev.set(gradCur);
        }
    }

    /** Horizontal taper for the head profile: full width near the crown (v=1), narrow at the chin (v=-1). */
    private static float headWidthFactor(float v) {
        return 0.35f + 0.65f * (float) Math.pow((v + 1f) / 2f, 0.6);
    }

    /** Big, black, slanted almond eyes wrapping toward the sides of the head -- the classic look. */
    private void renderEyes(ShapeRenderer sr, float cx, float cy, float rotation, float scale) {
        float cosR = MathUtils.cos(rotation);
        float sinR = MathUtils.sin(rotation);
        float ex0 = HEAD_RX * scale * 0.5f;
        float ey0 = HEAD_RY * scale * 0.05f;
        float eyeRx = HEAD_RX * scale * 0.4f;
        float eyeRy = HEAD_RY * scale * 0.2f;

        for (int side = -1; side <= 1; side += 2) {
            float lx = ex0 * side;
            float wx = cx + (lx * cosR - ey0 * sinR);
            float wy = cy + (lx * sinR + ey0 * cosR);
            float eyeTilt = rotation + 0.32f * side;
            filledTiltedEllipse(sr, wx, wy, eyeRx, eyeRy, eyeTilt, 10, EYE_COLOR);
        }
    }

    /** Fan-triangulated ellipse whose fill colour is derived per-vertex from world Y, not flat. */
    private void filledGradientEllipse(ShapeRenderer sr, float cx, float cy, float rx, float ry,
                                        int segments, float topY, float botY) {
        filledGradientFan(sr, cx, cy, rx, ry, segments, MathUtils.PI2, topY, botY);
    }

    /** Same as {@link #filledGradientEllipse}, but only the upper half — a parachute canopy dome. */
    private void filledGradientDome(ShapeRenderer sr, float cx, float cy, float rx, float ry,
                                     int segments, float topY, float botY) {
        filledGradientFan(sr, cx, cy, rx, ry, segments, MathUtils.PI, topY, botY);
    }

    /** Fan-triangulates an arc of an ellipse from angle 0 to sweep, colouring each vertex by its world Y. */
    private void filledGradientFan(ShapeRenderer sr, float cx, float cy, float rx, float ry,
                                    int segments, float sweep, float topY, float botY) {
        gradCenter.set(colorAt(cy, topY, botY));
        float prevX = cx + rx;
        float prevY = cy;
        gradPrev.set(colorAt(prevY, topY, botY));
        for (int i = 1; i <= segments; i++) {
            float angle = sweep * i / segments;
            float vx = cx + MathUtils.cos(angle) * rx;
            float vy = cy + MathUtils.sin(angle) * ry;
            gradCur.set(colorAt(vy, topY, botY));
            sr.triangle(cx, cy, prevX, prevY, vx, vy, gradCenter, gradPrev, gradCur);
            prevX = vx;
            prevY = vy;
            gradPrev.set(gradCur);
        }
    }

    /** Fan-triangulated ellipse rotated by tilt, filled with a single flat colour (used for eyes). */
    private void filledTiltedEllipse(ShapeRenderer sr, float cx, float cy, float rx, float ry,
                                      float tilt, int segments, Color color) {
        sr.setColor(color);
        float cosT = MathUtils.cos(tilt);
        float sinT = MathUtils.sin(tilt);
        float prevX = cx + rx * cosT;
        float prevY = cy + rx * sinT;
        for (int i = 1; i <= segments; i++) {
            float angle = i * MathUtils.PI2 / segments;
            float lx = MathUtils.cos(angle) * rx;
            float ly = MathUtils.sin(angle) * ry;
            float vx = cx + (lx * cosT - ly * sinT);
            float vy = cy + (lx * sinT + ly * cosT);
            sr.triangle(cx, cy, prevX, prevY, vx, vy);
            prevX = vx;
            prevY = vy;
        }
    }
}
