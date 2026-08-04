package com.joshreimer.balloonpop.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

/**
 * The bucket of water an alien tips over an overheated gun. Rather than animating a canned splash,
 * this runs a small ballistic particle sim: droplets are emitted from the bucket's lip (which the
 * owner keeps up to date via {@link #setSource} as the bucket rolls over), fall under gravity,
 * break off the stream as it stretches, ricochet off the barrel, run down it as rivulets, and pool
 * on the ground.
 *
 * <p>The stream is drawn as circles bridged along consecutive emissions, so near the lip it reads
 * as one coherent rope of water and only separates into droplets once it has stretched — which is
 * what a dumped bucket actually looks like. Stream droplets live in their own ring buffer so
 * "emitted next" is simply the next index; ricochet fragments are kept separate precisely so they
 * can't interleave and break that ordering.
 *
 * <p>Decorative only: like {@link Explosion} it has no {@code overlaps()} and is never collision
 * checked. {@code GameScreen} watches {@link #alive} to know when the gun is usable again.
 *
 * <p>Everything fades by shrinking rather than by alpha, since {@code ShapeRenderer} draws with
 * blending off and a translucent colour would come out solid black.
 */
public class WaterSplash {

    private static final float GRAVITY = -900f;
    /** Kept alive a beat past the last droplet so the puddle and steam have time to settle. */
    private static final float LINGER_AFTER_POUR = 1.5f;

    // Emission. The stream leaves the lip with a modest outward push, not a throw.
    private static final float EMIT_PER_SECOND = 95f;
    private static final float EMIT_SPEED_MIN = 25f;
    private static final float EMIT_SPEED_MAX = 60f;
    private static final float EMIT_SPREAD = 8f;
    private static final float DROP_RADIUS_MIN = 2.2f;
    private static final float DROP_RADIUS_MAX = 4.6f;
    private static final int MAX_DROPS = 200;

    /** Consecutive droplets closer than this are bridged into a continuous rope of water. */
    private static final float ROPE_LINK_DISTANCE = 26f;

    // Ricochet off the barrel.
    private static final int MAX_FRAGMENTS = 90;
    private static final int FRAGMENTS_PER_IMPACT = 3;
    private static final float FRAGMENT_SPEED_MIN = 45f;
    private static final float FRAGMENT_SPEED_MAX = 135f;

    // Rivulets running down the sides of the gun after impact.
    private static final int MAX_RIVULETS = 14;
    private static final float RIVULET_SPEED = 55f;
    private static final float RIVULET_LENGTH = 13f;
    /** Only some impacts start a rivulet, otherwise the barrel is solid with them. */
    private static final float RIVULET_CHANCE = 0.22f;

    // Steam boiling off the barrel where water lands.
    private static final int MAX_STEAM = 40;
    private static final float STEAM_PER_IMPACT = 0.3f;
    private static final float STEAM_RISE_SPEED = 62f;
    private static final float STEAM_LIFETIME = 1.1f;
    private static final float STEAM_RADIUS_MIN = 6f;
    private static final float STEAM_RADIUS_MAX = 14f;

    private static final float PUDDLE_MAX_WIDTH = 110f;
    private static final float PUDDLE_HEIGHT = 8f;
    private static final float PUDDLE_PER_DROP = 0.014f;

    private static final Color WATER_COLOR = new Color(0.24f, 0.58f, 0.95f, 1f);
    private static final Color WATER_PALE = new Color(0.62f, 0.86f, 1f, 1f);
    private static final Color WATER_DEEP = new Color(0.15f, 0.4f, 0.78f, 1f);
    private static final Color PUDDLE_COLOR = new Color(0.19f, 0.48f, 0.85f, 1f);
    private static final Color PUDDLE_RIM_COLOR = new Color(0.55f, 0.8f, 1f, 1f);
    private static final Color STEAM_COLOR = new Color(0.93f, 0.95f, 0.97f, 1f);

    // Stream droplets, in parallel arrays so a full stream allocates nothing per frame. Emitted
    // strictly in ring order, which is what lets renderStream() bridge neighbours by index.
    private final float[] dropX = new float[MAX_DROPS];
    private final float[] dropY = new float[MAX_DROPS];
    private final float[] dropVX = new float[MAX_DROPS];
    private final float[] dropVY = new float[MAX_DROPS];
    private final float[] dropR = new float[MAX_DROPS];
    private final boolean[] dropLive = new boolean[MAX_DROPS];
    /** True where the droplet at index+1 was the very next one emitted (not a wrap or a gap). */
    private final boolean[] dropLinked = new boolean[MAX_DROPS];
    private int dropCursor = 0;

    private final float[] fragX = new float[MAX_FRAGMENTS];
    private final float[] fragY = new float[MAX_FRAGMENTS];
    private final float[] fragVX = new float[MAX_FRAGMENTS];
    private final float[] fragVY = new float[MAX_FRAGMENTS];
    private final float[] fragR = new float[MAX_FRAGMENTS];
    private final boolean[] fragLive = new boolean[MAX_FRAGMENTS];
    private int fragCursor = 0;

    private final float[] rivX = new float[MAX_RIVULETS];
    private final float[] rivY = new float[MAX_RIVULETS];
    private final float[] rivLen = new float[MAX_RIVULETS];
    private final boolean[] rivLive = new boolean[MAX_RIVULETS];
    private int rivCursor = 0;

    private final float[] steamX = new float[MAX_STEAM];
    private final float[] steamY = new float[MAX_STEAM];
    private final float[] steamDrift = new float[MAX_STEAM];
    private final float[] steamR = new float[MAX_STEAM];
    private final float[] steamAge = new float[MAX_STEAM];
    private final boolean[] steamLive = new boolean[MAX_STEAM];
    private int steamCursor = 0;

    private final float gunX, gunTopY, gunHalfWidth, groundY;
    private final float pourDirection;

    private float sourceX, sourceY;
    private boolean emitting = false;
    private float emitAccumulator = 0f;
    private float steamAccumulator = 0f;
    private float puddleVolume = 0f;
    private float sinceStopped = 0f;

    public boolean alive = true;

    /**
     * @param gunX centre of the gun being doused
     * @param gunTopY top of the barrel, where water lands and steam boils off
     * @param gunHalfWidth half the gun's width, for deciding what counts as a hit
     * @param groundY ground level, where the puddle spreads
     * @param pourDirection +1 if the water is being tipped rightward, -1 leftward
     */
    public WaterSplash(float gunX, float gunTopY, float gunHalfWidth, float groundY, float pourDirection) {
        this.gunX = gunX;
        this.gunTopY = gunTopY;
        this.gunHalfWidth = gunHalfWidth;
        this.groundY = groundY;
        this.pourDirection = pourDirection;
        this.sourceX = gunX;
        this.sourceY = gunTopY;
    }

    /** Moves the spout to the bucket's current lip, so the stream stays attached as it tips. */
    public void setSource(float x, float y) {
        sourceX = x;
        sourceY = y;
    }

    public void setEmitting(boolean emitting) {
        this.emitting = emitting;
    }

    public void update(float delta) {
        if (emitting) {
            sinceStopped = 0f;
            emitAccumulator += EMIT_PER_SECOND * delta;
            while (emitAccumulator >= 1f) {
                emitAccumulator -= 1f;
                spawnDrop();
            }
        } else {
            sinceStopped += delta;
        }

        updateDrops(delta);
        updateFragments(delta);
        updateRivulets(delta);
        updateSteam(delta);

        if (!emitting && sinceStopped >= LINGER_AFTER_POUR) {
            alive = false;
        }
    }

    private void spawnDrop() {
        int i = dropCursor;
        int next = (dropCursor + 1) % MAX_DROPS;
        dropCursor = next;

        float speed = MathUtils.random(EMIT_SPEED_MIN, EMIT_SPEED_MAX);
        dropX[i] = sourceX + MathUtils.random(-1.5f, 1.5f);
        dropY[i] = sourceY;
        dropVX[i] = pourDirection * speed + MathUtils.random(-EMIT_SPREAD, EMIT_SPREAD);
        dropVY[i] = MathUtils.random(-18f, 4f);
        dropR[i] = MathUtils.random(DROP_RADIUS_MIN, DROP_RADIUS_MAX);
        dropLive[i] = true;

        // This droplet now leads into whatever is emitted next; the one it displaced does not.
        dropLinked[i] = true;
        dropLinked[next] = false;
    }

    private void updateDrops(float delta) {
        for (int i = 0; i < MAX_DROPS; i++) {
            if (!dropLive[i]) continue;

            dropVY[i] += GRAVITY * delta;
            dropX[i] += dropVX[i] * delta;
            dropY[i] += dropVY[i] * delta;

            if (dropY[i] <= gunTopY && dropY[i] > groundY
                && Math.abs(dropX[i] - gunX) <= gunHalfWidth) {
                dropLive[i] = false;
                dropLinked[i] = false;
                onBarrelImpact(dropX[i]);
                continue;
            }

            if (dropY[i] <= groundY) {
                dropLive[i] = false;
                dropLinked[i] = false;
                puddleVolume = Math.min(1f, puddleVolume + PUDDLE_PER_DROP);
            }
        }
    }

    /** Water hitting hot metal: it sprays back, runs down the side, and flashes off as steam. */
    private void onBarrelImpact(float x) {
        for (int n = 0; n < FRAGMENTS_PER_IMPACT; n++) {
            spawnFragment(x, gunTopY);
        }
        if (MathUtils.random() < RIVULET_CHANCE) {
            startRivulet(x);
        }
        steamAccumulator += STEAM_PER_IMPACT;
        while (steamAccumulator >= 1f) {
            steamAccumulator -= 1f;
            spawnSteam(x, gunTopY);
        }
    }

    private void spawnFragment(float x, float y) {
        int i = fragCursor;
        fragCursor = (fragCursor + 1) % MAX_FRAGMENTS;

        float angle = MathUtils.random(0.35f, MathUtils.PI - 0.35f);
        float speed = MathUtils.random(FRAGMENT_SPEED_MIN, FRAGMENT_SPEED_MAX);
        fragX[i] = x;
        fragY[i] = y + 1f;
        fragVX[i] = MathUtils.cos(angle) * speed;
        fragVY[i] = MathUtils.sin(angle) * speed;
        fragR[i] = MathUtils.random(1.4f, 2.8f);
        fragLive[i] = true;
    }

    private void updateFragments(float delta) {
        for (int i = 0; i < MAX_FRAGMENTS; i++) {
            if (!fragLive[i]) continue;
            fragVY[i] += GRAVITY * delta;
            fragX[i] += fragVX[i] * delta;
            fragY[i] += fragVY[i] * delta;
            if (fragY[i] <= groundY) {
                fragLive[i] = false;
                puddleVolume = Math.min(1f, puddleVolume + PUDDLE_PER_DROP * 0.5f);
            }
        }
    }

    private void startRivulet(float x) {
        int i = rivCursor;
        rivCursor = (rivCursor + 1) % MAX_RIVULETS;
        // Rivulets run down the outside of the barrel, so nudge them to the nearer edge.
        float side = x >= gunX ? 1f : -1f;
        rivX[i] = gunX + side * MathUtils.random(gunHalfWidth * 0.45f, gunHalfWidth);
        rivY[i] = gunTopY;
        rivLen[i] = RIVULET_LENGTH * MathUtils.random(0.7f, 1.3f);
        rivLive[i] = true;
    }

    private void updateRivulets(float delta) {
        for (int i = 0; i < MAX_RIVULETS; i++) {
            if (!rivLive[i]) continue;
            rivY[i] -= RIVULET_SPEED * delta;
            rivLen[i] -= RIVULET_LENGTH * 0.5f * delta;
            if (rivY[i] <= groundY || rivLen[i] <= 1f) {
                rivLive[i] = false;
                puddleVolume = Math.min(1f, puddleVolume + PUDDLE_PER_DROP * 2f);
            }
        }
    }

    private void spawnSteam(float x, float y) {
        int i = steamCursor;
        steamCursor = (steamCursor + 1) % MAX_STEAM;
        steamX[i] = x + MathUtils.random(-6f, 6f);
        steamY[i] = y;
        steamDrift[i] = MathUtils.random(-22f, 22f);
        steamR[i] = MathUtils.random(STEAM_RADIUS_MIN, STEAM_RADIUS_MAX);
        steamAge[i] = 0f;
        steamLive[i] = true;
    }

    private void updateSteam(float delta) {
        for (int i = 0; i < MAX_STEAM; i++) {
            if (!steamLive[i]) continue;
            steamAge[i] += delta;
            steamY[i] += STEAM_RISE_SPEED * delta;
            steamX[i] += steamDrift[i] * delta;
            if (steamAge[i] >= STEAM_LIFETIME) {
                steamLive[i] = false;
            }
        }
    }

    public void render(ShapeRenderer sr) {
        renderPuddle(sr);
        renderRivulets(sr);
        renderStream(sr);
        renderFragments(sr);
        renderSteam(sr);
    }

    /**
     * The stream. Consecutive emissions still close together are bridged with a thick line, making
     * the water near the lip a single unbroken rope; as it accelerates the gaps open past
     * ROPE_LINK_DISTANCE and it falls apart into separate droplets on its own.
     */
    private void renderStream(ShapeRenderer sr) {
        for (int i = 0; i < MAX_DROPS; i++) {
            if (!dropLive[i]) continue;

            int next = (i + 1) % MAX_DROPS;
            if (dropLinked[i] && dropLive[next]) {
                float dx = dropX[next] - dropX[i];
                float dy = dropY[next] - dropY[i];
                if (dx * dx + dy * dy <= ROPE_LINK_DISTANCE * ROPE_LINK_DISTANCE) {
                    sr.setColor(WATER_COLOR);
                    sr.rectLine(dropX[i], dropY[i], dropX[next], dropY[next],
                        Math.min(dropR[i], dropR[next]) * 1.7f);
                }
            }

            sr.setColor(i % 5 == 0 ? WATER_PALE : WATER_COLOR);
            sr.circle(dropX[i], dropY[i], dropR[i], 8);
        }
    }

    private void renderFragments(ShapeRenderer sr) {
        sr.setColor(WATER_PALE);
        for (int i = 0; i < MAX_FRAGMENTS; i++) {
            if (!fragLive[i]) continue;
            sr.circle(fragX[i], fragY[i], fragR[i], 8);
        }
    }

    private void renderRivulets(ShapeRenderer sr) {
        sr.setColor(WATER_DEEP);
        for (int i = 0; i < MAX_RIVULETS; i++) {
            if (!rivLive[i]) continue;
            sr.rectLine(rivX[i], rivY[i], rivX[i], rivY[i] + rivLen[i], 2.6f);
            sr.circle(rivX[i], rivY[i], 1.8f, 8);
        }
    }

    /** A puddle that grows with the water actually delivered, then soaks away. */
    private void renderPuddle(ShapeRenderer sr) {
        if (puddleVolume <= 0.01f) return;

        float soak = MathUtils.clamp((sinceStopped - 0.3f) / LINGER_AFTER_POUR, 0f, 1f);
        float width = PUDDLE_MAX_WIDTH * puddleVolume * (1f - soak * 0.55f);
        if (width <= 2f) return;

        fillEllipse(sr, PUDDLE_COLOR, gunX, groundY, width / 2f, PUDDLE_HEIGHT / 2f);
        fillEllipse(sr, PUDDLE_RIM_COLOR, gunX, groundY + PUDDLE_HEIGHT * 0.12f,
            width / 2f * 0.55f, PUDDLE_HEIGHT / 2f * 0.4f);
    }

    private void renderSteam(ShapeRenderer sr) {
        sr.setColor(STEAM_COLOR);
        for (int i = 0; i < MAX_STEAM; i++) {
            if (!steamLive[i]) continue;
            float t = steamAge[i] / STEAM_LIFETIME;
            // Swells then shrinks away, since it can't fade on alpha.
            float radius = steamR[i] * MathUtils.sin(t * MathUtils.PI);
            if (radius <= 0.5f) continue;
            sr.circle(steamX[i], steamY[i], radius, 12);
        }
    }

    private void fillEllipse(ShapeRenderer sr, Color color, float cx, float cy, float rx, float ry) {
        sr.setColor(color);
        float prevX = cx + rx;
        float prevY = cy;
        for (int i = 1; i <= 18; i++) {
            float angle = i * MathUtils.PI2 / 18;
            float vx = cx + MathUtils.cos(angle) * rx;
            float vy = cy + MathUtils.sin(angle) * ry;
            sr.triangle(cx, cy, prevX, prevY, vx, vy);
            prevX = vx;
            prevY = vy;
        }
    }
}
