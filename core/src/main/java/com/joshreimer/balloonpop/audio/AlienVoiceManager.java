package com.joshreimer.balloonpop.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

/**
 * Plays a taunt when an alien is shot, and keeps a chain of kills from turning into noise.
 *
 * <p>Each {@link AlienVoiceType} owns a pool of short clips, loaded as {@link Sound} (not
 * {@code Music}) through a shared {@link AssetManager} so they are fully resident before the first
 * shot and cost nothing to trigger. Selection never repeats the clip it just played, and every play
 * gets a small random pitch offset, so a pool of eight doesn't start sounding mechanical.
 *
 * <p>Three separate mechanisms keep it from becoming a wall of sound:
 * <ul>
 *   <li>at most one line is <em>started</em> per frame, so an area kill can't fire eight at once;
 *   <li>queued lines are spaced by a stagger delay, so a chain reads as a sequence of hecklers;
 *   <li>each type has its own cooldown, and lines that come up during it are dropped rather than
 *       backing up — a kill streak should thin out, not queue a backlog that plays over the
 *       following ten seconds.
 * </ul>
 *
 * <p>Every line that plays also surfaces its text through {@link #getActiveInsults()} at the
 * position the alien died, so the joke still lands with the sound off or turned down. The text is
 * driven by the same queue as the audio, which is what keeps the two in sync through the stagger,
 * and it is shown even when the game is muted.
 */
public class AlienVoiceManager implements Disposable {

    /** Pitch jitter applied per play so a small pool doesn't sound robotic on repeat. */
    private static final float PITCH_MIN = 0.92f;
    private static final float PITCH_MAX = 1.08f;

    /** Minimum spacing between two lines starting, whoever they belong to. */
    private static final float STAGGER_MIN = 0.15f;
    private static final float STAGGER_MAX = 0.25f;

    /** Per-type quiet period after a line, so one kind of kill can't machine-gun its pool. */
    private static final float TYPE_COOLDOWN = 0.9f;

    /** Anything queued but not started within this long is stale — the moment has passed. */
    private static final float QUEUE_EXPIRY = 1.2f;
    private static final int MAX_QUEUED = 6;

    private static final float BASE_VOLUME = 0.85f;
    /** Floor for the distance attenuation, so a far-off alien is quieter but never inaudible. */
    private static final float MIN_DISTANCE_VOLUME = 0.45f;

    /** How long an insult stays on screen. Comfortably longer than any clip. */
    public static final float INSULT_TEXT_DURATION = 1.6f;

    /** A line waiting its turn: everything needed to play it is captured at the moment of death. */
    private static class QueuedLine {
        AlienVoiceType type;
        int clipIndex;
        float x, y;
        float pan;
        float volume;
        float age;
    }

    /** A line currently being said, for the caller to draw. */
    public static class ActiveInsult {
        public final String text;
        public final float x, y;
        public float timer = INSULT_TEXT_DURATION;

        ActiveInsult(String text, float x, float y) {
            this.text = text;
            this.x = x;
            this.y = y;
        }
    }

    private final AssetManager assets;
    private final float worldWidth;

    /** Resolved clip paths per type; an entry is null if that clip is missing from the assets. */
    private final String[][] clipPaths;
    private final int[] lastPlayedIndex;
    private final float[] typeCooldown;

    private final Array<QueuedLine> queue = new Array<>();
    private final Array<QueuedLine> pool = new Array<>();
    private final Array<ActiveInsult> activeInsults = new Array<>();

    private float nextLineDelay = 0f;
    private boolean loaded = false;

    /**
     * Queues every clip that actually exists on disk for loading. Missing clips are skipped rather
     * than fatal, so a half-recorded pool still works and the game never dies over a typo'd filename.
     */
    public AlienVoiceManager(AssetManager assets, float worldWidth) {
        this.assets = assets;
        this.worldWidth = worldWidth;

        AlienVoiceType[] types = AlienVoiceType.values();
        clipPaths = new String[types.length][];
        lastPlayedIndex = new int[types.length];
        typeCooldown = new float[types.length];

        for (int t = 0; t < types.length; t++) {
            AlienVoiceType type = types[t];
            clipPaths[t] = new String[type.getClipCount()];
            lastPlayedIndex[t] = -1;

            int found = 0;
            for (int i = 0; i < type.getClipCount(); i++) {
                String path = resolveClipPath(type.getClipBasePath(i));
                clipPaths[t][i] = path;
                if (path != null) {
                    assets.load(path, Sound.class);
                    found++;
                }
            }
            // Worth saying out loud: a silent pool is otherwise indistinguishable from a working
            // one, and the usual cause is a filename that doesn't match what's in assets/voice.
            if (found < type.getClipCount()) {
                Gdx.app.log("AlienVoice", "only " + found + "/" + type.getClipCount()
                    + " clips found for " + type + " (expected " + type.getClipBasePath(0) + ".ogg/.wav)");
            }
        }
    }

    /**
     * Prefers a hand-recorded {@code .ogg} and falls back to the synthesized {@code .wav}
     * placeholder, so dropping real recordings into {@code assets/voice/} is all it takes to
     * replace them.
     */
    private static String resolveClipPath(String basePath) {
        String ogg = basePath + ".ogg";
        if (Gdx.files.internal(ogg).exists()) return ogg;
        String wav = basePath + ".wav";
        if (Gdx.files.internal(wav).exists()) return wav;
        return null;
    }

    /**
     * Records an alien's death. The line is queued rather than played immediately so simultaneous
     * kills stagger instead of stacking; pan and volume are captured now, from where it actually
     * died, not from wherever things have moved to by the time it is spoken.
     *
     * @param type which alien is speaking
     * @param x world x of the death
     * @param y world y of the death
     * @param listenerX world x to judge distance from — the gun, i.e. the player
     */
    public void onAlienDeath(AlienVoiceType type, float x, float y, float listenerX) {
        if (queue.size >= MAX_QUEUED) return;

        int t = type.ordinal();
        // Already talking; a streak should thin out rather than queue up a backlog.
        if (typeCooldown[t] > 0f) return;

        int clip = pickClip(t, type.getClipCount());
        if (clip < 0) return;

        QueuedLine line = pool.size > 0 ? pool.pop() : new QueuedLine();
        line.type = type;
        line.clipIndex = clip;
        line.x = x;
        line.y = y;
        line.pan = MathUtils.clamp(x / worldWidth * 2f - 1f, -1f, 1f);
        line.volume = distanceVolume(x, listenerX);
        line.age = 0f;
        queue.add(line);

        // Claim the cooldown at queue time, not at play time: two aliens dying on the same frame
        // should produce one line, not two that then discover each other's cooldown too late.
        typeCooldown[t] = TYPE_COOLDOWN;
        lastPlayedIndex[t] = clip;
    }

    /** Uniform pick over the pool, excluding whatever played last so nothing repeats back to back. */
    private int pickClip(int typeIndex, int count) {
        int available = 0;
        for (int i = 0; i < count; i++) {
            if (clipPaths[typeIndex][i] != null && i != lastPlayedIndex[typeIndex]) available++;
        }
        // Everything is either missing or is the one clip we just played; allow the repeat rather
        // than falling silent, unless the pool is genuinely empty.
        if (available == 0) {
            for (int i = 0; i < count; i++) {
                if (clipPaths[typeIndex][i] != null) return i;
            }
            return -1;
        }

        int target = MathUtils.random(available - 1);
        for (int i = 0; i < count; i++) {
            if (clipPaths[typeIndex][i] == null || i == lastPlayedIndex[typeIndex]) continue;
            if (target-- == 0) return i;
        }
        return -1;
    }

    /** Quieter the further the kill is from the player, to fake a little depth. */
    private float distanceVolume(float x, float listenerX) {
        float distance = Math.abs(x - listenerX) / worldWidth;
        return MathUtils.lerp(1f, MIN_DISTANCE_VOLUME, MathUtils.clamp(distance, 0f, 1f));
    }

    /**
     * Advances cooldowns, starts at most one queued line, and ages out on-screen text.
     *
     * @param muted when true the audio is skipped but the text still appears — that is the whole
     *              point of the text, so it must not be gated on the sound being on
     */
    public void update(float delta, boolean muted) {
        if (!loaded && assets.update()) {
            loaded = true;
        }

        for (int i = 0; i < typeCooldown.length; i++) {
            if (typeCooldown[i] > 0f) {
                typeCooldown[i] = Math.max(0f, typeCooldown[i] - delta);
            }
        }

        if (nextLineDelay > 0f) {
            nextLineDelay = Math.max(0f, nextLineDelay - delta);
        }

        for (int i = queue.size - 1; i >= 0; i--) {
            queue.get(i).age += delta;
            if (queue.get(i).age > QUEUE_EXPIRY) {
                pool.add(queue.removeIndex(i));
            }
        }

        // At most one line starts per frame, and only once the stagger gap has elapsed.
        if (queue.size > 0 && nextLineDelay <= 0f) {
            QueuedLine line = queue.removeIndex(0);
            speak(line, muted);
            pool.add(line);
            nextLineDelay = MathUtils.random(STAGGER_MIN, STAGGER_MAX);
        }

        for (int i = activeInsults.size - 1; i >= 0; i--) {
            ActiveInsult insult = activeInsults.get(i);
            insult.timer -= delta;
            if (insult.timer <= 0f) {
                activeInsults.removeIndex(i);
            }
        }
    }

    private void speak(QueuedLine line, boolean muted) {
        activeInsults.add(new ActiveInsult(
            line.type.getInsult(line.clipIndex), line.x, line.y));

        if (muted || !loaded) return;

        String path = clipPaths[line.type.ordinal()][line.clipIndex];
        if (path == null || !assets.isLoaded(path, Sound.class)) return;

        Sound sound = assets.get(path, Sound.class);
        // volume/pitch/pan in one call rather than play() then setPan(id, ...): the two-step
        // version plays a frame centred at full volume before the pan lands.
        sound.play(BASE_VOLUME * line.volume, MathUtils.random(PITCH_MIN, PITCH_MAX), line.pan);
    }

    /** Insults currently on screen, for the caller to draw. Never null; may be empty. */
    public Array<ActiveInsult> getActiveInsults() {
        return activeInsults;
    }

    /** Drops queued lines and on-screen text, e.g. when a run is restarted. */
    public void reset() {
        pool.addAll(queue);
        queue.clear();
        activeInsults.clear();
        nextLineDelay = 0f;
        for (int i = 0; i < typeCooldown.length; i++) {
            typeCooldown[i] = 0f;
            lastPlayedIndex[i] = -1;
        }
    }

    /**
     * Unloads only this manager's clips. The {@link AssetManager} itself is owned by the caller and
     * is deliberately left alone.
     */
    @Override
    public void dispose() {
        for (String[] paths : clipPaths) {
            for (String path : paths) {
                if (path != null && assets.isLoaded(path, Sound.class)) {
                    assets.unload(path);
                }
            }
        }
    }
}
