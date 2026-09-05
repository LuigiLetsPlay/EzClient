"""Generate module-menu icons without ever touching the EzClient brand logo."""
from pathlib import Path

import cairosvg


ROOT = Path(__file__).resolve().parent / "src" / "main" / "resources" / "assets" / "ezclient"
SOURCE_DIR = ROOT / "icon_sources"
TEXTURE_DIR = ROOT / "textures" / "icons"

GLYPHS = {
    "module": '<rect x="4" y="4" width="6" height="6"/><rect x="14" y="4" width="6" height="6"/><rect x="4" y="14" width="6" height="6"/><rect x="14" y="14" width="6" height="6"/>',
    "zoom": '<circle cx="10.5" cy="10.5" r="6.5"/><path d="m15.5 15.5 5 5M10.5 7.5v6M7.5 10.5h6"/>',
    "tnt": '<path d="M5 8h14v11H5zM8 8V5h8v3M12 5V3M12 3l3-1"/><path d="M8 12h8M9 16h6"/>',
    "potion_effect": '<path d="M9 3h6M10 3v5l-5 9a3 3 0 0 0 2.6 4.5h8.8A3 3 0 0 0 19 17l-5-9V3"/><path d="M7 16h10M9 13h6"/>',
    "ping": '<path d="M4 10a11 11 0 0 1 16 0M7 13a7 7 0 0 1 10 0M10 16a3 3 0 0 1 4 0"/><circle cx="12" cy="20" r="1" fill="url(#g)" stroke="none"/>',
    "keystrokes": '<rect x="3" y="5" width="18" height="14" rx="2"/><path d="M7 9h2M11 9h2M15 9h2M7 13h2M11 13h2M15 13h2M8 17h8"/>',
    "fullbright": '<circle cx="12" cy="12" r="4"/><path d="M12 2v3M12 19v3M2 12h3M19 12h3M5 5l2 2M17 17l2 2M19 5l-2 2M7 17l-2 2"/>',
    "motion_blur": '<path d="M3 7h11M6 12h15M3 17h11"/><path d="m13 4 4 3-4 3M17 9l4 3-4 3M13 14l4 3-4 3"/>',
    "fps": '<path d="M4 17a9 9 0 1 1 16 0"/><path d="m12 14 5-5M7 17h10"/><circle cx="12" cy="14" r="1.3" fill="url(#g)" stroke="none"/>',
    "daycounter": '<rect x="4" y="5" width="16" height="15" rx="2"/><path d="M8 3v4M16 3v4M4 10h16"/><path d="M15 13a3.5 3.5 0 1 0 3 5 4 4 0 0 1-3-5z"/>',
    "crosshair": '<circle cx="12" cy="12" r="7"/><path d="M12 2v6M12 16v6M2 12h6M16 12h6"/><circle cx="12" cy="12" r="1.5" fill="url(#g)" stroke="none"/>',
    "cps": '<rect x="7" y="3" width="10" height="18" rx="5"/><path d="M12 3v6M7 10h10M4 5 2 3M20 5l2-2"/>',
    "coordinates": '<path d="M12 22s7-6 7-13a7 7 0 1 0-14 0c0 7 7 13 7 13z"/><circle cx="12" cy="9" r="2.5"/><path d="M2 18h4M4 16v4M18 18h4"/>',
    "clock": '<circle cx="12" cy="12" r="9"/><path d="M12 7v6l4 2"/>',
    "clearglass": '<rect x="5" y="5" width="12" height="12" rx="1"/><path d="M9 9h10v10H9zM7 13 13 7M16 4v3M14.5 5.5h3"/>',
    "chat": '<path d="M4 5h16v11H9l-5 4z"/><path d="M8 9h8M8 12h5"/>',
    "auto_sprint": '<path d="M4 17c4 0 5-4 7-8l3 4 5 2-2 4h-7l-3 2H3"/><path d="m16 5 3 2-3 2"/>',
    "armor_status": '<path d="M12 3 20 6v5c0 5-3.4 8.5-8 10-4.6-1.5-8-5-8-10V6z"/><path d="m8 12 2.5 2.5L16 9"/>',
}


def icon_svg(body: str) -> str:
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24">
  <defs><linearGradient id="g" x1="3" y1="3" x2="21" y2="21"><stop stop-color="#B8FFD6"/><stop offset="1" stop-color="#22C96E"/></linearGradient></defs>
  <g fill="none" stroke="url(#g)" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">{body}</g>
</svg>'''


def render(svg: str, target: Path, size: int) -> None:
    target.parent.mkdir(parents=True, exist_ok=True)
    cairosvg.svg2png(bytestring=svg.encode("utf-8"), write_to=str(target), output_width=size, output_height=size)


def main() -> None:
    SOURCE_DIR.mkdir(parents=True, exist_ok=True)
    for name, body in GLYPHS.items():
        svg = icon_svg(body)
        (SOURCE_DIR / f"{name}.svg").write_text(svg, encoding="utf-8")
        render(svg, TEXTURE_DIR / f"{name}.png", 64)

    print(f"Generated {len(GLYPHS)} unified module icons; brand assets were left untouched.")


if __name__ == "__main__":
    main()
