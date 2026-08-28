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

from PIL import Image, ImageSequence


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
    thumbnail_path: str = ""


@dataclass(frozen=True)
class AnimationOptions:
    start: float = 0.0
    end: float = 5.0
    fps: int = 12
    ping_pong: bool = False
    crop_box: tuple[float, float, float, float] | None = None


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

    thumb_dir = tempfile.gettempdir()
    thumb_path = Path(thumb_dir) / f"ezclient_thumb_{path.stem}.png"

    if path.suffix.lower() == ".gif":
        try:
            with Image.open(path) as gif:
                frames = 0
                total_duration = 0.0
                first_frame = None
                for frame in ImageSequence.Iterator(gif):
                    if first_frame is None:
                        first_frame = frame.copy().convert("RGBA")
                    frames += 1
                    dur = frame.info.get("duration", 100) / 1000.0
                    total_duration += dur if dur > 0 else 0.1
                w, h = gif.size
                fps = frames / max(0.1, total_duration)
                if first_frame:
                    first_frame.save(thumb_path, "PNG")
                return MediaInfo(max(0.1, total_duration), w, h, max(1.0, fps), str(thumb_path))
        except Exception:
            pass

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

    try:
        _run([
            _binary("ffmpeg"), "-nostdin", "-hide_banner", "-loglevel", "error", "-y",
            "-ss", "0.0", "-i", str(path), "-vframes", "1", str(thumb_path),
        ])
    except Exception:
        pass

    return MediaInfo(duration, int(stream["width"]), int(stream["height"]), source_fps, str(thumb_path) if thumb_path.exists() else "")


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
    return AnimationOptions(start, end, fps, bool(options.ping_pong), options.crop_box)


def _crop_frame(frame: Image.Image, crop_box: tuple[float, float, float, float] | None) -> Image.Image:
    if not crop_box:
        return frame
    cx, cy, cw, ch = crop_box
    w, h = frame.size
    left = max(0, min(w - 1, int(round(cx * w))))
    top = max(0, min(h - 1, int(round(cy * h))))
    width = max(1, min(w - left, int(round(cw * w))))
    height = max(1, min(h - top, int(round(ch * h))))
    return frame.crop((left, top, left + width, top + height))


def _atlas_from_face(face: Image.Image, crop_box: tuple[float, float, float, float] | None = None) -> Image.Image:
    atlas = Image.new("RGBA", ATLAS_SIZE, (0, 0, 0, 0))
    fitted = _crop_frame(face.convert("RGBA"), crop_box)
    source_ratio = fitted.width / max(1, fitted.height)
    target_ratio = 10 / 16
    if source_ratio > target_ratio:
        width = round(fitted.height * target_ratio)
        left = (fitted.width - width) // 2
        cropped = fitted.crop((left, 0, left + width, fitted.height))
    else:
        height = round(fitted.width / target_ratio)
        top = (fitted.height - height) // 2
        cropped = fitted.crop((0, top, fitted.width, top + height))

    # 1. Cape visible back face: 10x16 at 4x scale = (40, 64) at (4, 4)
    cape_face = cropped.resize((40, 64), Image.Resampling.LANCZOS)
    atlas.paste(cape_face, FACE_BOX[:2], cape_face)
    # Inner cape face: (48, 4)
    atlas.paste(cape_face, (48, 4), cape_face)
    # Cape top/bottom/sides borders
    atlas.paste(cape_face.resize((40, 4)), (4, 0), cape_face.resize((40, 4)))
    atlas.paste(cape_face.resize((4, 64)), (0, 4), cape_face.resize((4, 64)))
    atlas.paste(cape_face.resize((4, 64)), (44, 4), cape_face.resize((4, 64)))

    # 2. Elytra wings: texOffs(22, 0) at 4x scale -> (88, 0, 96, 88)
    # Wing face size: 10x20 at 4x scale = (40, 80)
    elytra_wing = cropped.resize((40, 80), Image.Resampling.LANCZOS)
    # Outer wing: (96, 8)
    atlas.paste(elytra_wing, (96, 8), elytra_wing)
    # Inner wing: (144, 8)
    atlas.paste(elytra_wing, (144, 8), elytra_wing)
    # Wing caps & side borders
    atlas.paste(elytra_wing.resize((8, 80)), (88, 8), elytra_wing.resize((8, 80)))
    atlas.paste(elytra_wing.resize((8, 80)), (136, 8), elytra_wing.resize((8, 80)))
    atlas.paste(elytra_wing.resize((80, 8)), (96, 0), elytra_wing.resize((80, 8)))

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
        frames: list[Image.Image] = []
        if source_path.suffix.lower() == ".gif":
            try:
                with Image.open(source_path) as gif:
                    gif_frames = [frame.copy() for frame in ImageSequence.Iterator(gif)]
                    if gif_frames:
                        # Sample gif frames evenly matching selected fps
                        duration = selected.end - selected.start
                        target_count = max(1, min(MAX_FRAMES, math.ceil(duration * selected.fps)))
                        step = max(1, len(gif_frames) / target_count)
                        sampled = [gif_frames[min(len(gif_frames) - 1, int(i * step))] for i in range(target_count)]
                        frames = [_atlas_from_face(f, selected.crop_box) for f in sampled]
            except Exception:
                frames = []

        if not frames:
            raw_dir = temporary / "raw"
            raw_dir.mkdir()
            duration = selected.end - selected.start
            _run([
                _binary("ffmpeg"), "-nostdin", "-hide_banner", "-loglevel", "error", "-y",
                "-ss", f"{selected.start:.3f}", "-t", f"{duration:.3f}", "-i", str(source_path),
                "-vf", f"fps={selected.fps}", "-frames:v", str(MAX_FRAMES),
                str(raw_dir / "frame-%04d.png"),
            ])
            frames = [_atlas_from_face(Image.open(path), selected.crop_box) for path in sorted(raw_dir.glob("frame-*.png"))]
            shutil.rmtree(raw_dir, ignore_errors=True)

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

        # Save animated GIF preview of visible 10:16 cape portrait
        try:
            portrait_frames = []
            for frame in frames:
                # Crop the (4, 4, 44, 68) visible back face from the 256x128 atlas and upscale to crisp 160x256
                portrait = frame.crop((4, 4, 44, 68)).resize((160, 256), Image.Resampling.NEAREST)
                portrait_frames.append(portrait)
            if portrait_frames:
                frame_duration_ms = max(20, int(round(1000 / selected.fps)))
                portrait_frames[0].save(
                    temporary / "preview.gif",
                    save_all=True,
                    append_images=portrait_frames[1:],
                    duration=frame_duration_ms,
                    loop=0,
                    disposal=2,
                    optimize=False,
                )
        except Exception as exc:
            print(f"[CapeMedia] Warning: could not generate preview.gif: {exc}")

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
        temporary.replace(target)
        return manifest
    except Exception:
        shutil.rmtree(temporary, ignore_errors=True)
        raise
