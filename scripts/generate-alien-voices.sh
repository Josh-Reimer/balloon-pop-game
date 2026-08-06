#!/usr/bin/env bash
#
# Regenerates the alien taunt clips in assets/voice/.
#
# These used to be a purely synthetic buzz (glottal source + gliding formants + ring mod), which
# sounded alien but said nothing an English speaker could make out. The voice is now real
# text-to-speech from macOS `say`, so the words are the words, and the "alien" is a light effect
# chain layered on top: a pitch/formant shift, a short comb-delay chorus, and a slow vibrato. Every
# stage is deliberately mild — anything heavier (ring modulation especially) eats the consonants and
# puts us back where we started.
#
# The lines here must stay in the same order as the insult arrays in AlienVoiceType.java: clip N is
# played with insult N drawn on screen, so a reordering here silently desyncs the subtitles.
#
# macOS only (needs `say`); also needs ffmpeg. Run from anywhere:
#     bash scripts/generate-alien-voices.sh
#
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="$REPO_ROOT/assets/voice"
WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

command -v say >/dev/null || { echo "needs macOS \`say\`"; exit 1; }
command -v ffmpeg >/dev/null || { echo "needs ffmpeg"; exit 1; }

SAMPLE_RATE=22050

# The rare bonus alien: a young, high voice, shifted up further — chirpy and smug.
SCOUT_VOICE="Junior"
SCOUT_RATE=190
SCOUT_PITCH=1.12
SCOUT_LINES=(
  "Nice aim, meatbag!"
  "You call that shooting?"
  "My hatchling aims better!"
  "Worth it!"
  "That tickled!"
  "I'll be back!"
  "You wasted a shot!"
  "Blorp off!"
)

# The overheat swarm: the flat, robotic classic voice pitched down into a gruff drawl.
SWARM_VOICE="Fred"
SWARM_RATE=170
SWARM_PITCH=0.90
SWARM_LINES=(
  "Your gun is junk!"
  "Too hot to handle?"
  "Cool off, human!"
  "Overheated already?"
  "That's what you get!"
  "Enjoy the wait!"
  "Bring a bucket!"
  "We brought friends!"
)

# asetrate/atempo shifts pitch *and* formants (the alien timbre) while restoring the original
# length. The silenceremove runs first so `say`'s leading/trailing padding doesn't survive into the
# game, where a clip has to land the instant the alien is hit.
render() {
  local voice="$1" rate="$2" pitch="$3" text="$4" out="$5"
  local raw="$WORK_DIR/raw.aiff"

  say -v "$voice" -r "$rate" -o "$raw" "$text"
  ffmpeg -y -v error -i "$raw" -filter_complex "\
aresample=$SAMPLE_RATE,aformat=channel_layouts=mono,\
silenceremove=start_periods=1:start_silence=0.02:start_threshold=-45dB\
:stop_periods=-1:stop_silence=0.04:stop_threshold=-45dB:detection=peak,\
asetrate=$SAMPLE_RATE*$pitch,aresample=$SAMPLE_RATE,atempo=1/$pitch,\
highpass=f=170,lowpass=f=7200,\
chorus=0.8:0.85:25:0.25:0.3:2,\
vibrato=f=5.5:d=0.08,\
acompressor=threshold=-18dB:ratio=4:attack=5:release=90,\
loudnorm=I=-14:TP=-1.5:LRA=11,\
aresample=$SAMPLE_RATE,alimiter=limit=0.95" \
    -c:a pcm_s16le -ar "$SAMPLE_RATE" -ac 1 "$out"
}

generate_pool() {
  local prefix="$1" voice="$2" rate="$3" pitch="$4"; shift 4
  local lines=("$@")
  local i=1
  for line in "${lines[@]}"; do
    local out
    out="$(printf '%s/%s%02d.wav' "$OUT_DIR" "$prefix" "$i")"
    render "$voice" "$rate" "$pitch" "$line" "$out"
    printf '%-38s %5.2fs  %s\n' "$(basename "$out")" \
      "$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$out")" "$line"
    i=$((i + 1))
  done
}

mkdir -p "$OUT_DIR"
generate_pool "alien_scout_insult_" "$SCOUT_VOICE" "$SCOUT_RATE" "$SCOUT_PITCH" "${SCOUT_LINES[@]}"
generate_pool "alien_swarm_insult_" "$SWARM_VOICE" "$SWARM_RATE" "$SWARM_PITCH" "${SWARM_LINES[@]}"
