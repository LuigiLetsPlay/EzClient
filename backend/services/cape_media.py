"""Bounded GIF/video conversion for animated EzClient capes."""
from __future__ import annotations

import json
import math
import os
import shutil
import subprocess
import tempfile
from dataclasses import asdict, dataclass
from pathlib import Path

from PIL import Image


SUPPORTED_MEDIA = {".gif", ".mp4", ".webm"}
MAX_SOURCE_BYTES = 64 * 1024 * 1024
MAX_DURATION_SECONDS = 10.0
MAX_FPS = 20
MAX_FRAMES = 200
ATLAS_SIZE = (256, 128)
FACE_BOX = (4, 4, 44, 68)  # vanilla 10x16 back face at 4x texture scale
MAX_SHEET_SIDE = 4096


@dataclass(frozen=True)
class MediaInfo:
    duration: float
    width: int
    height: int
    source_fps: float


@dataclass(frozen=True)
class AnimationOptions:
    start: float = 0.0
    end: float = 5.0
    fps: int = 12
    ping_pong: bool = False


@dataclass(frozen=True)
class AnimationManifest:
    version: int
    sheet: str
    frame_count: int
    fps: int
    ping_pong: bool
    frame_width: int
    frame_height: int
    columns: int
    duration: float


def _binary(name: str) -> str:
    executable = shutil.which(name)
    if not executable:
        raise RuntimeError(f"{name} wurde nicht gefunden. Bitte EzClient reparieren oder neu installieren.")
    return executable


def _run(command: list[str]) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        command,
        check=True,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        creationflags=subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0,
    )


def probe_media(source: str | Path) -> MediaInfo:
    path = Path(source).resolve()
    if path.suffix.lower() not in SUPPORTED_MEDIA or not path.is_file():
        raise ValueError("Unterstützt werden GIF, MP4 und WebM.")
    if path.stat().st_size > MAX_SOURCE_BYTES:
        raise ValueError("Die Quelldatei darf maximal 64 MB groß sein.")

    result = _run([
        _binary("ffprobe"), "-v", "error", "-select_streams", "v:0",
        "-show_entries", "stream=width,height,avg_frame_rate,duration:format=duration",
        "-of", "json", str(path),
    ])
    payload = json.loads(result.stdout)
    streams = payload.get("streams") or []
    if not streams:
        raise ValueError("Die Datei enthält keine lesbare Videospur.")
    stream = streams[0]
    duration = float(stream.get("duration") or payload.get("format", {}).get("duration") or 0)
    rate = str(stream.get("avg_frame_rate") or "0/1").split("/", 1)
    source_fps = float(rate[0]) / max(1.0, float(rate[1]))
    if duration <= 0 or int(stream.get("width") or 0) <= 0 or int(stream.get("height") or 0) <= 0:
        raise ValueError("Mediendauer oder Bildgröße ist ungültig.")
    return MediaInfo(duration, int(stream["width"]), int(stream["height"]), source_fps)


def _validated_options(info: MediaInfo, options: AnimationOptions) -> AnimationOptions:
    start = max(0.0, float(options.start))
    end = min(float(options.end), info.duration, start + MAX_DURATION_SECONDS)
    fps = max(1, min(MAX_FPS, int(options.fps)))
    if end - start < 0.1:
        raise ValueError("Der gewählte Bereich muss mindestens 0,1 Sekunden lang sein.")
    frames = math.ceil((end - start) * fps)
    if options.ping_pong:
        frames = max(1, frames * 2 - 2)
    if frames > MAX_FRAMES:
        raise ValueError(f"Maximal {MAX_FRAMES} Animationsframes sind erlaubt.")
    return AnimationOptions(start, end, fps, bool(options.ping_pong))


def _atlas_from_face(face: Image.Image) -> Image.Image:
    atlas = Image.new("RGBA", ATLAS_SIZE, (0, 0, 0, 0))
    fitted = face.convert("RGBA")
    source_ratio = fitted.width / fitted.height
    target_ratio = 10 / 16
    if source_ratio > target_ratio:
        width = round(fitted.height * target_ratio)
        left = (fitted.width - width) // 2
        fitted = fitted.crop((left, 0, left + width, fitted.height))
    else:
        height = round(fitted.width / target_ratio)
        top = (fitted.height - height) // 2
        fitted = fitted.crop((0, top, fitted.width, top + height))
    fitted = fitted.resize((40, 64), Image.Resampling.LANCZOS)
    atlas.paste(fitted, FACE_BOX[:2], fitted)
    return atlas


def generate_frame_sheet(
    source: str | Path,
    output_dir: str | Path,
    options: AnimationOptions,
) -> AnimationManifest:
    """Convert one bounded media clip atomically into a PNG sheet + JSON manifest."""
    source_path = Path(source).resolve()
    target = Path(output_dir).resolve()
    if target.exists():
        raise FileExistsError("Der Animations-Ausgabeordner existiert bereits.")
    info = probe_media(source_path)
    selected = _validated_options(info, options)
    parent = target.parent
    parent.mkdir(parents=True, exist_ok=True)

    temporary = Path(tempfile.mkdtemp(prefix="ezclient-cape-", dir=parent))
    try:
        raw_dir = temporary / "raw"
        raw_dir.mkdir()
        duration = selected.end - selected.start
        _run([
            _binary("ffmpeg"), "-nostdin", "-hide_banner", "-loglevel", "error", "-y",
            "-ss", f"{selected.start:.3f}", "-t", f"{duration:.3f}", "-i", str(source_path),
            "-vf", f"fps={selected.fps}", "-frames:v", str(MAX_FRAMES),
            str(raw_dir / "frame-%04d.png"),
        ])
        frames = [_atlas_from_face(Image.open(path)) for path in sorted(raw_dir.glob("frame-*.png"))]
        if not frames:
            raise ValueError("Aus dem gewählten Bereich konnten keine Frames gelesen werden.")
        if selected.ping_pong and len(frames) > 1:
            frames.extend(frame.copy() for frame in frames[-2:0:-1])
        if len(frames) > MAX_FRAMES:
            raise ValueError(f"Maximal {MAX_FRAMES} Animationsframes sind erlaubt.")

        columns = min(MAX_SHEET_SIDE // ATLAS_SIZE[0], len(frames))
        rows = math.ceil(len(frames) / columns)
        sheet = Image.new("RGBA", (columns * ATLAS_SIZE[0], rows * ATLAS_SIZE[1]), (0, 0, 0, 0))
        for index, frame in enumerate(frames):
            sheet.paste(frame, ((index % columns) * ATLAS_SIZE[0], (index // columns) * ATLAS_SIZE[1]))
        sheet.save(temporary / "framesheet.png", "PNG", optimize=True, compress_level=9)

        manifest = AnimationManifest(
            version=1,
            sheet="framesheet.png",
            frame_count=len(frames),
            fps=selected.fps,
            ping_pong=selected.ping_pong,
            frame_width=ATLAS_SIZE[0],
            frame_height=ATLAS_SIZE[1],
            columns=columns,
            duration=len(frames) / selected.fps,
        )
        (temporary / "animation.json").write_text(
            json.dumps(asdict(manifest), ensure_ascii=False, indent=2), encoding="utf-8"
        )
        shutil.rmtree(raw_dir)
        temporary.replace(target)
        return manifest
    except Exception:
        shutil.rmtree(temporary, ignore_errors=True)
        raise
