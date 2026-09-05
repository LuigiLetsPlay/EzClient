"""Generate crisp 64x64 RGBA icons for EzClient modules in the native mint/emerald theme."""
import os
import math
from PIL import Image, ImageDraw

DEST_DIR = os.path.join(os.path.dirname(__file__), "..", "client_mod", "src", "main", "resources", "assets", "ezclient", "textures", "icons")
os.makedirs(DEST_DIR, exist_ok=True)

MINT = (184, 255, 214, 255)
EMERALD = (34, 201, 110, 255)
WHITE = (255, 255, 255, 255)
MINT_FADED = (184, 255, 214, 80)
EMERALD_FADED = (34, 201, 110, 80)

def create_base():
    return Image.new("RGBA", (256, 256), (0, 0, 0, 0))

def save_icon(img_256, filename):
    img_64 = img_256.resize((64, 64), Image.Resampling.LANCZOS)
    out_path = os.path.join(DEST_DIR, filename)
    img_64.save(out_path, "PNG")
    print(f"Generated {filename}")

def draw_hitbox():
    im = create_base()
    d = ImageDraw.Draw(im)
    w = 12
    # 3D bounding box corners
    f_l, f_t, f_r, f_b = 60, 85, 170, 210
    b_l, b_t, b_r, b_b = 85, 50, 195, 175

    # Back face
    d.rectangle([b_l, b_t, b_r, b_b], outline=MINT_FADED, width=w)
    # Connecting edges
    d.line([(f_l, f_t), (b_l, b_t)], fill=MINT_FADED, width=w)
    d.line([(f_r, f_t), (b_r, b_t)], fill=MINT_FADED, width=w)
    d.line([(f_l, f_b), (b_l, b_b)], fill=MINT_FADED, width=w)
    d.line([(f_r, f_b), (b_r, b_b)], fill=MINT, width=w)
    # Front face
    d.rectangle([f_l, f_t, f_r, f_b], outline=MINT, width=w)

    # Eye height red/emerald line inside
    eye_y = 115
    d.line([(f_l + 10, eye_y), (f_r - 10, eye_y)], fill=EMERALD, width=w - 2)
    # Gaze vector extending forward
    d.line([(115, eye_y), (35, eye_y + 15)], fill=WHITE, width=w - 2)

    save_icon(im, "hitbox.png")

def draw_item_physics():
    im = create_base()
    d = ImageDraw.Draw(im)
    w = 12

    # Tilted floating diamond / gem
    cx, cy = 128, 105
    pts = [(cx, cy - 65), (cx + 55, cy - 10), (cx, cy + 55), (cx - 55, cy - 10)]
    d.polygon(pts, fill=EMERALD_FADED, outline=MINT)
    d.line([pts[0], pts[2]], fill=MINT, width=w)
    d.line([pts[1], pts[3]], fill=MINT, width=w)
    d.line(pts + [pts[0]], fill=MINT, width=w)

    # Ground plane reflection / shadow
    d.ellipse([70, 195, 186, 225], outline=MINT_FADED, width=w)

    # Motion physics curves showing bounce/rotation
    d.arc([165, 80, 225, 170], start=280, end=70, fill=MINT, width=w)
    d.polygon([(215, 160), (225, 185), (200, 180)], fill=MINT)

    save_icon(im, "item_physics.png")

def draw_time_weather():
    im = create_base()
    d = ImageDraw.Draw(im)
    w = 12

    # Sun top-left
    sun_cx, sun_cy = 90, 85
    d.ellipse([sun_cx - 32, sun_cy - 32, sun_cx + 32, sun_cy + 32], fill=EMERALD, outline=MINT, width=w - 2)
    # Sun rays
    rays = [(0, -48), (34, -34), (48, 0), (-34, -34), (-48, 0), (-34, 34)]
    for rx, ry in rays:
        d.line([(sun_cx + int(rx * 0.75), sun_cy + int(ry * 0.75)), (sun_cx + rx, sun_cy + ry)], fill=MINT, width=w)

    # Cloud overlapping bottom-right
    d.ellipse([100, 120, 160, 180], fill=MINT_FADED, outline=MINT, width=w)
    d.ellipse([140, 105, 205, 170], fill=MINT_FADED, outline=MINT, width=w)
    d.rectangle([115, 140, 195, 180], fill=MINT_FADED)
    d.line([(110, 180), (200, 180)], fill=MINT, width=w)

    # Raindrops falling
    for drop_x in [120, 150, 180]:
        d.line([(drop_x, 195), (drop_x - 8, 225)], fill=WHITE, width=w - 2)

    save_icon(im, "time_weather.png")

def draw_particle():
    im = create_base()
    d = ImageDraw.Draw(im)

    def draw_star(cx, cy, r_outer, r_inner, col):
        pts = []
        for i in range(8):
            ang = i * math.pi / 4
            r = r_outer if i % 2 == 0 else r_inner
            pts.append((cx + r * math.cos(ang), cy + r * math.sin(ang)))
        d.polygon(pts, fill=col)

    # Center big 4-point sparkle star
    draw_star(128, 128, 75, 18, MINT)
    draw_star(128, 128, 35, 10, WHITE)

    # 4 small corner sparkle stars
    draw_star(60, 60, 26, 8, EMERALD)
    draw_star(196, 65, 30, 9, MINT)
    draw_star(60, 195, 24, 7, MINT)
    draw_star(195, 190, 28, 8, EMERALD)

    save_icon(im, "particle.png")

def draw_block_overlay():
    im = create_base()
    d = ImageDraw.Draw(im)
    w = 12

    # Isometric block
    t_top = (128, 48)
    t_right = (205, 92)
    t_bottom = (128, 136)
    t_left = (51, 92)
    d.polygon([t_top, t_right, t_bottom, t_left], fill=MINT_FADED, outline=MINT)
    d.line([t_top, t_right, t_bottom, t_left, t_top], fill=MINT, width=w)

    # Left face
    b_left = (51, 172)
    b_bottom = (128, 216)
    d.polygon([t_left, t_bottom, b_bottom, b_left], fill=EMERALD_FADED, outline=MINT)
    d.line([t_left, b_left, b_bottom, t_bottom], fill=MINT, width=w)

    # Right face
    b_right = (205, 172)
    d.polygon([t_bottom, t_right, b_right, b_bottom], fill=(184, 255, 214, 40), outline=MINT)
    d.line([t_right, b_right, b_bottom], fill=MINT, width=w)

    # Center vertical edge
    d.line([t_bottom, b_bottom], fill=WHITE, width=w + 2)

    # Highlight brackets / corners
    d.line([(40, 80), (40, 65), (65, 65)], fill=WHITE, width=w)
    d.line([(216, 80), (216, 65), (191, 65)], fill=WHITE, width=w)

    save_icon(im, "block_overlay.png")

def draw_boss_bar():
    im = create_base()
    d = ImageDraw.Draw(im)
    w = 12

    # Crown / skull symbol at top
    crown = [(85, 95), (100, 60), (128, 80), (156, 60), (171, 95)]
    d.line(crown + [crown[0]], fill=MINT, width=w)
    d.polygon(crown, fill=EMERALD)
    d.ellipse([124, 76, 132, 84], fill=WHITE)

    # Main boss bar frame
    x1, y1, x2, y2 = 40, 125, 216, 165
    d.rounded_rectangle([x1, y1, x2, y2], radius=10, fill=MINT_FADED, outline=MINT, width=w)

    # Health fill (70% full)
    d.rounded_rectangle([x1 + 8, y1 + 8, x1 + 120, y2 - 8], radius=6, fill=EMERALD)

    # Bar notches / dividers
    for notch_x in [x1 + 45, x1 + 88, x1 + 131]:
        d.line([(notch_x, y1 + 4), (notch_x, y2 - 4)], fill=MINT, width=6)

    save_icon(im, "boss_bar.png")

def draw_bedwars():
    im = create_base()
    d = ImageDraw.Draw(im)
    w = 12

    # Minecraft bed (angled 3D view)
    # Headboard
    d.polygon([(48, 85), (80, 70), (80, 155), (48, 170)], fill=EMERALD, outline=MINT)
    d.line([(48, 85), (80, 70), (80, 155), (48, 170), (48, 85)], fill=MINT, width=w)

    # Pillow
    d.polygon([(80, 70), (120, 85), (120, 115), (80, 100)], fill=WHITE, outline=MINT)
    d.line([(80, 70), (120, 85), (120, 115), (80, 100), (80, 70)], fill=MINT, width=w - 2)

    # Blanket top
    d.polygon([(120, 85), (208, 120), (176, 160), (80, 120)], fill=EMERALD_FADED, outline=MINT)
    d.line([(120, 85), (208, 120), (176, 160), (80, 120), (120, 85)], fill=MINT, width=w)

    # Bed side
    d.polygon([(80, 120), (176, 160), (176, 185), (80, 145)], fill=EMERALD, outline=MINT)
    d.line([(80, 120), (176, 160), (176, 185), (80, 145), (80, 120)], fill=MINT, width=w)

    # Legs
    d.line([(48, 170), (48, 195)], fill=MINT, width=w)
    d.line([(80, 155), (80, 180)], fill=MINT, width=w)
    d.line([(176, 185), (176, 210)], fill=MINT, width=w)

    save_icon(im, "bedwars.png")

def draw_nameplate():
    im = create_base()
    d = ImageDraw.Draw(im)
    w = 12

    # Tag badge frame
    d.rounded_rectangle([42, 75, 214, 180], radius=18, fill=MINT_FADED, outline=MINT, width=w)

    # User head avatar
    head_cx, head_cy = 82, 112
    d.ellipse([head_cx - 18, head_cy - 18, head_cx + 18, head_cy + 18], fill=EMERALD, outline=MINT, width=w - 4)
    # Shoulders
    d.arc([head_cx - 24, head_cy + 4, head_cx + 24, head_cy + 45], start=0, end=180, fill=MINT, width=w - 2)

    # Text / Name line bars
    d.rounded_rectangle([118, 102, 196, 118], radius=6, fill=WHITE)
    d.rounded_rectangle([118, 134, 175, 146], radius=5, fill=EMERALD)

    save_icon(im, "nameplate.png")

def draw_waypoints():
    im = create_base()
    d = ImageDraw.Draw(im)
    w = 12

    # Map pin teardrop
    cx, cy = 128, 100
    pin_r = 50
    d.arc([cx - pin_r, cy - pin_r, cx + pin_r, cy + pin_r], start=145, end=395, fill=MINT, width=w)
    tip_y = 195
    p1 = (cx - int(pin_r * 0.82), cy + int(pin_r * 0.57))
    p2 = (cx + int(pin_r * 0.82), cy + int(pin_r * 0.57))
    d.line([p1, (cx, tip_y)], fill=MINT, width=w)
    d.line([p2, (cx, tip_y)], fill=MINT, width=w)
    d.polygon([p1, (cx, tip_y), p2], fill=EMERALD)

    # Inner cutout dot
    d.ellipse([cx - 18, cy - 18, cx + 18, cy + 18], fill=WHITE)

    # Ground ripple ring
    d.ellipse([88, 205, 168, 228], outline=MINT_FADED, width=w - 2)

    save_icon(im, "waypoints.png")

def draw_sound():
    im = create_base()
    d = ImageDraw.Draw(im)
    w = 12

    # Speaker body
    d.rectangle([48, 105, 85, 151], fill=EMERALD, outline=MINT, width=w)
    # Horn flare
    horn = [(85, 105), (132, 68), (132, 188), (85, 151)]
    d.polygon(horn, fill=EMERALD, outline=MINT)
    d.line(horn + [horn[0]], fill=MINT, width=w)

    # Sound wave arcs
    d.arc([105, 92, 175, 164], start=305, end=55, fill=MINT, width=w)
    d.arc([100, 68, 215, 188], start=312, end=48, fill=WHITE, width=w)

    save_icon(im, "sound.png")

def draw_memory():
    im = create_base()
    d = ImageDraw.Draw(im)
    w = 12

    # RAM Stick PCB
    x1, y1, x2, y2 = 45, 90, 211, 166
    d.rounded_rectangle([x1, y1, x2, y2], radius=10, fill=MINT_FADED, outline=MINT, width=w)

    # 3 Memory chips on stick
    for chip_x in [65, 105, 145]:
        d.rectangle([chip_x, y1 + 18, chip_x + 28, y2 - 24], fill=EMERALD, outline=MINT, width=5)

    # Bottom edge notch
    notch_x = 128
    d.rectangle([notch_x - 8, y2 - 8, notch_x + 8, y2 + 4], fill=(0, 0, 0, 0))
    d.line([(x1 + 10, y2), (notch_x - 10, y2)], fill=MINT, width=w)
    d.line([(notch_x + 10, y2), (x2 - 10, y2)], fill=MINT, width=w)

    # Gold / mint contact pins
    for pin_x in range(58, 200, 14):
        if abs(pin_x - notch_x) > 10:
            d.line([(pin_x, y2 - 14), (pin_x, y2)], fill=WHITE, width=5)

    save_icon(im, "memory.png")

def main():
    draw_hitbox()
    draw_item_physics()
    draw_time_weather()
    draw_particle()
    draw_block_overlay()
    draw_boss_bar()
    draw_bedwars()
    draw_nameplate()
    draw_waypoints()
    draw_sound()
    draw_memory()
    print("All icons successfully created!")

if __name__ == "__main__":
    main()
