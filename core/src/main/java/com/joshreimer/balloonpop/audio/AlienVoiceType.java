package com.joshreimer.balloonpop.audio;

/**
 * The kinds of alien that have something to say when they're shot. Each type owns its own pool of
 * voice clips and its own cooldown, so a streak of one kind of kill can't drown out the other.
 *
 * <p>The game only has one {@code Alien} class, but it shows up in two situations that read very
 * differently — a lone bonus alien drifting down mid-round, and the swarm that rains down while the
 * gun is overheated — so the types are split along that line and their lines are written to match.
 */
public enum AlienVoiceType {

    /** The rare bonus alien: higher, chirpier, smug about being hard to hit. */
    SCOUT("alien_scout_insult_", 8, new String[] {
        "Nice aim, meatbag!",
        "You call that shooting?",
        "My hatchling aims better!",
        "Worth it!",
        "That tickled!",
        "I'll be back!",
        "You wasted a shot!",
        "Blorp off!",
    }),

    /** The overheat swarm: lower, gruffer, and entirely about the gun the player just cooked. */
    SWARM("alien_swarm_insult_", 8, new String[] {
        "Your gun is junk!",
        "Too hot to handle?",
        "Cool off, human!",
        "Overheated already?",
        "That's what you get!",
        "Enjoy the wait!",
        "Bring a bucket!",
        "We brought friends!",
    });

    private final String prefix;
    private final int clipCount;
    private final String[] insults;

    AlienVoiceType(String prefix, int clipCount, String[] insults) {
        this.prefix = prefix;
        this.clipCount = clipCount;
        this.insults = insults;
    }

    public int getClipCount() {
        return clipCount;
    }

    /**
     * Asset path for clip {@code index}, without an extension — {@link AlienVoiceManager} resolves
     * that, preferring {@code .ogg} so hand-recorded clips can replace the generated {@code .wav}
     * ones by filename alone, with no code change.
     *
     * <p>Clip {@code index} is spoken over insult {@code index}: the audio and the on-screen text
     * are the same line, so the arrays above and {@code scripts/generate-alien-voices.sh} — which
     * renders the clips from those same lines — have to stay in the same order.
     */
    public String getClipBasePath(int index) {
        return String.format("voice/%s%02d", prefix, index + 1);
    }

    /** The line the clip says, shown on screen so the joke still lands with the sound off. */
    public String getInsult(int index) {
        return insults[Math.max(0, Math.min(insults.length - 1, index))];
    }
}
