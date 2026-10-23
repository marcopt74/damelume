#!/usr/bin/env python3
"""Render the Lume completion chime.

Three sine partials of a C-major chord, matching the reference web app:
523.25 Hz at t=0 for 0.55 s, 659.25 Hz at 0.16 s for 0.62 s,
783.99 Hz at 0.34 s for 0.70 s. Envelope sin(min(1, 14u) * pi/2) * exp(-3.1u)
with u normalised to each note. Gain 0.34, summed. Mono PCM 16-bit at
22 050 Hz, 23 152 samples (~1.05 s).
"""

from __future__ import annotations

import math
import struct
import subprocess
import wave
from pathlib import Path

SAMPLE_RATE = 22050
SAMPLE_COUNT = 23152
NOTES = (
    (523.25, 0.0, 0.55),
    (659.25, 0.16, 0.62),
    (783.99, 0.34, 0.70),
)


def render() -> list[float]:
    samples: list[float] = []
    for index in range(SAMPLE_COUNT):
        time = index / SAMPLE_RATE
        mixed = 0.0
        for frequency, start, duration in NOTES:
            if start <= time < start + duration:
                normalised = (time - start) / duration
                envelope = math.sin(min(1.0, 14.0 * normalised) * math.pi / 2.0) * math.exp(
                    -3.1 * normalised
                )
                mixed += 0.34 * envelope * math.sin(2.0 * math.pi * frequency * (time - start))
        samples.append(max(-1.0, min(1.0, mixed)))
    return samples


def write_wav(path: Path, samples: list[float]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(path), "w") as handle:
        handle.setnchannels(1)
        handle.setsampwidth(2)
        handle.setframerate(SAMPLE_RATE)
        frames = bytearray()
        for sample in samples:
            clamped = max(-1.0, min(1.0, sample))
            value = int(clamped * 32767) if clamped >= 0 else int(clamped * 32768)
            frames.extend(struct.pack("<h", value))
        handle.writeframes(frames)


def main() -> None:
    root = Path(__file__).resolve().parents[1]
    wav_path = root / "app" / "src" / "test" / "resources" / "lume_chime.wav"
    ogg_path = root / "app" / "src" / "main" / "res" / "raw" / "lume_chime.ogg"
    samples = render()
    write_wav(wav_path, samples)
    ogg_path.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        [
            "ffmpeg",
            "-y",
            "-i",
            str(wav_path),
            "-c:a",
            "libvorbis",
            "-q:a",
            "6",
            str(ogg_path),
        ],
        check=True,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )
    print(f"wrote {wav_path} and {ogg_path} ({len(samples)} samples)")


if __name__ == "__main__":
    main()
