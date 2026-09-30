"""Convert the six supplied clips to bounded mono Vorbis resources for a private local build.

Only reads InputDir; all output must stay in this project. FFmpeg is a developer tool, not a mod dependency.
User audio is kept in ignored .tools instead of asserting a distribution license for it.
"""
import argparse
import array
import hashlib
import json
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
CLIPS = {
    "hit_cricket": "cricket-bat-hitting-sound.mp3",
    "hit_2": "hitsound_2.mp3",
    "hit_critical": "critical-hit-sounds-effect.mp3",
    "hitmarker": "hitmarker_2.mp3",
    "alpha_damage": "minecraft-alpha-damage-sound-effect.mp3",
    "osu": "osu-hit-sound.mp3",
}
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--input-dir", type=Path, required=True)
parser.add_argument("--ffmpeg", type=Path, required=True)
parser.add_argument("--output", type=Path, default=ROOT / ".tools/user-sounds")
args = parser.parse_args()
output = args.output.resolve()
if output == ROOT or not output.is_relative_to(ROOT):
    raise SystemExit("Output must be a subdirectory of NexVisuals")
ffmpeg = args.ffmpeg.resolve()
ffprobe = ffmpeg.with_name("ffprobe.exe" if sys.platform == "win32" else "ffprobe")
if not ffmpeg.is_file() or not ffprobe.is_file():
    raise SystemExit("Supply the actual local FFmpeg/ffprobe binaries")
sound_dir = output / "assets/nexvisuals/sounds/user"
sound_dir.mkdir(parents=True, exist_ok=True)
manifest = {"schemaVersion": 1, "origin": "User-supplied audio; redistribution license not established", "clips": {}}
for name, source_name in CLIPS.items():
    source = args.input_dir / source_name
    if not source.is_file() or source.stat().st_size > 8 * 1024 * 1024:
        raise SystemExit(f"Missing or oversized source: {source_name}")
    decoded = subprocess.run([str(ffmpeg), "-v", "error", "-nostdin", "-i", str(source), "-map", "0:a:0",
                              "-vn", "-sn", "-dn", "-t", "5", "-ac", "1", "-ar", "44100", "-f", "f32le", "pipe:1"],
                             check=True, capture_output=True).stdout
    samples = array.array("f")
    samples.frombytes(decoded)
    if sys.byteorder != "little":
        samples.byteswap()
    if not samples or max(map(abs, samples)) < .00001:
        raise SystemExit(f"Empty or silent clip: {source_name}")
    # Keep a small pad around the audible segment, avoiding noticeable lead-in and clicks.
    audible = [i for i, value in enumerate(samples) if abs(value) > .0005]
    if not audible:
        raise SystemExit(f"No audible samples: {source_name}")
    samples = samples[max(0, audible[0]-176):min(len(samples), audible[-1]+883)]
    gain = min(4.0, .75 / max(map(abs, samples)))
    fade = min(132, len(samples)//2)
    for i in range(len(samples)):
        envelope = min(1, i/max(1, fade), (len(samples)-1-i)/max(1, fade))
        samples[i] *= gain * envelope
    if sys.byteorder != "little":
        samples.byteswap()
    target = sound_dir / (name + ".ogg")
    subprocess.run([str(ffmpeg), "-v", "error", "-nostdin", "-y", "-f", "f32le", "-ar", "44100", "-ac", "1",
                    "-i", "pipe:0", "-map_metadata", "-1", "-fflags", "+bitexact", "-flags:a", "+bitexact", "-c:a", "libvorbis", "-q:a", "4", str(target)],
                   input=samples.tobytes(), check=True, capture_output=True)
    metadata = json.loads(subprocess.run([str(ffprobe), "-v", "error", "-show_streams", "-show_format", "-of", "json", str(target)],
                                        check=True, capture_output=True).stdout)
    stream = metadata["streams"][0]
    duration = float(metadata["format"]["duration"])
    if stream["codec_name"] != "vorbis" or stream["channels"] != 1 or stream["sample_rate"] != "44100" or not 0 < duration <= 5.01:
        raise SystemExit(f"Invalid converted audio: {source_name}")
    # Decode to null as well; a plausible header alone does not prove playable Vorbis.
    subprocess.run([str(ffmpeg), "-v", "error", "-nostdin", "-i", str(target), "-f", "null", "-"], check=True, capture_output=True)
    manifest["clips"][name] = {"source": source_name, "sourceSha256": hashlib.sha256(source.read_bytes()).hexdigest(),
                              "oggSha256": hashlib.sha256(target.read_bytes()).hexdigest(), "durationSeconds": duration,
                              "channels": 1, "sampleRate": 44100}
    print(f"{source_name} -> {target.name}: {duration:.3f}s, {target.stat().st_size} bytes, mono Vorbis")
(output / "nexvisuals").mkdir(exist_ok=True)
(output / "nexvisuals/user-audio.json").write_text(json.dumps(manifest, indent=2, ensure_ascii=False)+"\n", encoding="utf-8")
