#!/usr/bin/env python3
"""
Tasa VE Brand Asset Generator
Generates canonical SVG masters, PNG exports (dark & transparent),
Android VectorDrawables (adaptive foreground, monochrome, in-app mark),
legacy mipmap .webp launcher icons, and Play Store / Web assets.
"""

import math
import os
import shutil
import subprocess
from PIL import Image, ImageDraw, ImageFont

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
BRAND_DIR = os.path.join(REPO_ROOT, "brand")
RES_DIR = os.path.join(REPO_ROOT, "app", "src", "main", "res")
DOCS_ASSETS_DIR = os.path.join(REPO_ROOT, "docs", "assets")

os.makedirs(BRAND_DIR, exist_ok=True)
os.makedirs(DOCS_ASSETS_DIR, exist_ok=True)

# ---------------------------------------------------------------------------
# 1. Exact Parametric Geometry (Canonical 108x108 Adaptive Viewport)
# ---------------------------------------------------------------------------
CX = 54.0
ANGLE_DEG = 29.0
RAD = math.radians(ANGLE_DEG)
SIN_A = math.sin(RAD)
COS_A = math.cos(RAD)

uR = (SIN_A, -COS_A)
nR_out = (COS_A, SIN_A)
nR_in = (-COS_A, -SIN_A)

uL = (-SIN_A, -COS_A)
nL_out = (-COS_A, SIN_A)
nL_in = (COS_A, -SIN_A)

# Optically centered inside 66dp safe circle (r=33 around 54,54 -> [21..87])
TIP_Y = 33.0
TIP_DX = 22.0
TIP_R = (CX + TIP_DX, TIP_Y)
TIP_L = (CX - TIP_DX, TIP_Y)

HW = 4.8   # shaft half-width (9.6u)
AW = 9.0   # arrowhead half-width (18.0u)
AL = 12.8  # arrowhead length


def add(p, v, s=1.0):
    return (p[0] + v[0] * s, p[1] + v[1] * s)


def point_on_line(p0, direction, target_y):
    t = (target_y - p0[1]) / direction[1]
    return (p0[0] + direction[0] * t, target_y)


def f(p, ox=0.0, oy=0.0):
    return f"{p[0] + ox:.2f},{p[1] + oy:.2f}"


def cubic(p0, p1, p2, p3, t):
    mt = 1.0 - t
    return (
        mt**3 * p0[0] + 3 * mt**2 * t * p1[0] + 3 * mt * t**2 * p2[0] + t**3 * p3[0],
        mt**3 * p0[1] + 3 * mt**2 * t * p1[1] + 3 * mt * t**2 * p2[1] + t**3 * p3[1],
    )


def cubic_tan(p0, p1, p2, p3, t):
    mt = 1.0 - t
    dx = 3 * mt**2 * (p1[0] - p0[0]) + 6 * mt * t * (p2[0] - p1[0]) + 3 * t**2 * (p3[0] - p2[0])
    dy = 3 * mt**2 * (p1[1] - p0[1]) + 6 * mt * t * (p2[1] - p1[1]) + 3 * t**2 * (p3[1] - p2[1])
    length = math.hypot(dx, dy)
    return (dx / length, dy / length)


def split_cubic(p0, p1, p2, p3, t):
    def lerp(a, b, s):
        return (a[0] * (1 - s) + b[0] * s, a[1] * (1 - s) + b[1] * s)

    q0 = lerp(p0, p1, t)
    q1 = lerp(p1, p2, t)
    q2 = lerp(p2, p3, t)
    r0 = lerp(q0, q1, t)
    r1 = lerp(q1, q2, t)
    s0 = lerp(r0, r1, t)
    return (p0, q0, r0, s0), (s0, r1, q2, p3)


NECK_R = add(TIP_R, uR, -AL)
WING_R_OUT = add(NECK_R, nR_out, AW)
WING_R_IN = add(NECK_R, nR_in, AW)
SHAFT_R_TOP_OUT = add(NECK_R, nR_out, HW)
SHAFT_R_TOP_IN = add(NECK_R, nR_in, HW)

NECK_L = add(TIP_L, uL, -AL)
WING_L_OUT = add(NECK_L, nL_out, AW)
WING_L_IN = add(NECK_L, nL_in, AW)
SHAFT_L_TOP_OUT = add(NECK_L, nL_out, HW)
SHAFT_L_TOP_IN = add(NECK_L, nL_in, HW)

t_out_cross = (CX - SHAFT_L_TOP_OUT[0]) / SIN_A
OUTER_V_Y = SHAFT_L_TOP_OUT[1] + t_out_cross * COS_A

U_TAN_Y = OUTER_V_Y - 13.5
U_L_OUT = point_on_line(SHAFT_L_TOP_OUT, (-uL[0], -uL[1]), U_TAN_Y)
U_R_OUT = (2 * CX - U_L_OUT[0], U_TAN_Y)

T_U_BEZ = 9.2
C1_OUT = add(U_L_OUT, (-uL[0], -uL[1]), T_U_BEZ)
C2_OUT = add(U_R_OUT, (-uR[0], -uR[1]), T_U_BEZ)

t_apex = (CX - SHAFT_L_TOP_IN[0]) / SIN_A
APEX_IN = (CX, SHAFT_L_TOP_IN[1] + t_apex * COS_A)
U_BOTTOM = cubic(U_L_OUT, C1_OUT, C2_OUT, U_R_OUT, 0.5)

amber_seg, emerald_seg = split_cubic(U_L_OUT, C1_OUT, C2_OUT, U_R_OUT, 0.22)
U_SPLIT = amber_seg[3]
T_SPLIT = cubic_tan(U_L_OUT, C1_OUT, C2_OUT, U_R_OUT, 0.22)
N_SPLIT_IN = (T_SPLIT[1], -T_SPLIT[0])

amber_mid_in = add(cubic(U_L_OUT, C1_OUT, C2_OUT, U_R_OUT, 0.34), (0, -2.5))
amber_top_in = add(APEX_IN, (2.2, 2.5))

fold_c1 = add(U_SPLIT, N_SPLIT_IN, 4.0)
fold_c2 = add(APEX_IN, (-uR[0], -uR[1]), 3.8)
sh_top = point_on_line(SHAFT_L_TOP_IN, (-uL[0], -uL[1]), APEX_IN[1] - 2.6)
sh_ctrl = add(U_SPLIT, N_SPLIT_IN, 6.0)

# Sculpted emerald bottom-outer shading strip (follows lower U-turn -> right shaft outer edge smoothly)
em_shade_u1 = add(U_R_OUT, (-COS_A, -SIN_A), 2.2)
em_shade_c1 = add(emerald_seg[2], (-0.8, -2.0), 1.0)
em_shade_c2 = add(emerald_seg[1], (-0.4, -1.7), 1.0)
em_shade_tip_top = add(SHAFT_R_TOP_OUT, (-COS_A, -SIN_A), 1.5)

# Uniform-channel monochrome left arm (~1.9u clean channel along entire fold!)
mono_amber_seg, _ = split_cubic(U_L_OUT, C1_OUT, C2_OUT, U_R_OUT, 0.175)
mono_a_end = mono_amber_seg[3]
mono_shift = (-1.9, -0.55)
mono_c1 = add(fold_c1, mono_shift, 1.0)
mono_c2 = add(fold_c2, mono_shift, 1.0)
mono_top_in = point_on_line(SHAFT_L_TOP_IN, (-uL[0], -uL[1]), APEX_IN[1] - 2.15)


def build_paths(ox=0.0, oy=0.0):
    amber_d = " ".join([
        f"M {f(TIP_L, ox, oy)}",
        f"L {f(WING_L_OUT, ox, oy)}",
        f"L {f(SHAFT_L_TOP_OUT, ox, oy)}",
        f"L {f(U_L_OUT, ox, oy)}",
        f"C {f(amber_seg[1], ox, oy)} {f(amber_seg[2], ox, oy)} {f(U_SPLIT, ox, oy)}",
        f"L {f(amber_mid_in, ox, oy)}",
        f"L {f(amber_top_in, ox, oy)}",
        f"L {f(APEX_IN, ox, oy)}",
        f"L {f(SHAFT_L_TOP_IN, ox, oy)}",
        f"L {f(WING_L_IN, ox, oy)}",
        "Z",
    ])

    # Subtle lower-inner depth bevel along amber outer contour (tapers cleanly into shaft neck)
    amber_depth_d = " ".join([
        f"M {f(SHAFT_L_TOP_OUT, ox, oy)}",
        f"L {f(U_L_OUT, ox, oy)}",
        f"C {f(amber_seg[1], ox, oy)} {f(amber_seg[2], ox, oy)} {f(U_SPLIT, ox, oy)}",
        f"L {f(add(U_SPLIT, N_SPLIT_IN, 1.9), ox, oy)}",
        f"L {f(add(U_L_OUT, nL_in, 1.7), ox, oy)}",
        "Z",
    ])

    shadow_d = " ".join([
        f"M {f(sh_top, ox, oy)}",
        f"L {f(APEX_IN, ox, oy)}",
        f"L {f(amber_mid_in, ox, oy)}",
        f"L {f(U_SPLIT, ox, oy)}",
        f"Q {f(sh_ctrl, ox, oy)} {f(sh_top, ox, oy)}",
        "Z",
    ])

    emerald_d = " ".join([
        f"M {f(TIP_R, ox, oy)}",
        f"L {f(WING_R_OUT, ox, oy)}",
        f"L {f(SHAFT_R_TOP_OUT, ox, oy)}",
        f"L {f(U_R_OUT, ox, oy)}",
        f"C {f(emerald_seg[2], ox, oy)} {f(emerald_seg[1], ox, oy)} {f(U_SPLIT, ox, oy)}",
        f"C {f(fold_c1, ox, oy)} {f(fold_c2, ox, oy)} {f(APEX_IN, ox, oy)}",
        f"L {f(SHAFT_R_TOP_IN, ox, oy)}",
        f"L {f(WING_R_IN, ox, oy)}",
        "Z",
    ])

    # Subtle bottom-right depth plane on emerald U-loop & right outer edge (tapers cleanly into neck)
    emerald_depth_d = " ".join([
        f"M {f(SHAFT_R_TOP_OUT, ox, oy)}",
        f"L {f(U_R_OUT, ox, oy)}",
        f"C {f(emerald_seg[2], ox, oy)} {f(emerald_seg[1], ox, oy)} {f(U_SPLIT, ox, oy)}",
        f"C {f(add(U_SPLIT, N_SPLIT_IN, 1.8), ox, oy)} {f(em_shade_c2, ox, oy)} {f(em_shade_c1, ox, oy)}",
        f"L {f(em_shade_u1, ox, oy)}",
        "Z",
    ])

    # Crisp subtle top highlight strictly INSIDE the emerald arrowhead triangle
    def mix(pA, pB, t):
        return (pA[0] * (1.0 - t) + pB[0] * t, pA[1] * (1.0 - t) + pB[1] * t)

    hi_in = mix(TIP_R, WING_R_IN, 0.82)
    hi_out = mix(TIP_R, WING_R_OUT, 0.82)
    hi_mid = mix(TIP_R, NECK_R, 0.22)
    hi_in_inner = mix(hi_mid, WING_R_IN, 0.58)
    hi_out_inner = mix(hi_mid, WING_R_OUT, 0.58)

    emerald_hi_d = " ".join([
        f"M {f(TIP_R, ox, oy)}",
        f"L {f(hi_in, ox, oy)}",
        f"L {f(hi_in_inner, ox, oy)}",
        f"L {f(hi_mid, ox, oy)}",
        f"L {f(hi_out_inner, ox, oy)}",
        f"L {f(hi_out, ox, oy)}",
        "Z",
    ])

    mono_amber_d = " ".join([
        f"M {f(TIP_L, ox, oy)}",
        f"L {f(WING_L_OUT, ox, oy)}",
        f"L {f(SHAFT_L_TOP_OUT, ox, oy)}",
        f"L {f(U_L_OUT, ox, oy)}",
        f"C {f(mono_amber_seg[1], ox, oy)} {f(mono_amber_seg[2], ox, oy)} {f(mono_a_end, ox, oy)}",
        f"C {f(mono_c1, ox, oy)} {f(mono_c2, ox, oy)} {f(mono_top_in, ox, oy)}",
        f"L {f(SHAFT_L_TOP_IN, ox, oy)}",
        f"L {f(WING_L_IN, ox, oy)}",
        "Z",
    ])

    # Dollar Sign & subtle shadow offset (+0.85, +1.45)
    dollar_bar_d = (
        f"M {52.85+ox:.2f},{33.60+oy:.2f} "
        f"L {55.15+ox:.2f},{33.60+oy:.2f} "
        f"L {55.15+ox:.2f},{52.20+oy:.2f} "
        f"L {52.85+ox:.2f},{52.20+oy:.2f} Z"
    )
    dollar_s_d = (
        f"M {58.15+ox:.2f},{38.40+oy:.2f} "
        f"C {57.70+ox:.2f},{36.65+oy:.2f} {56.05+ox:.2f},{35.85+oy:.2f} {54.00+ox:.2f},{35.85+oy:.2f} "
        f"C {51.20+ox:.2f},{35.85+oy:.2f} {49.55+ox:.2f},{37.40+oy:.2f} {49.55+ox:.2f},{39.35+oy:.2f} "
        f"C {49.55+ox:.2f},{41.60+oy:.2f} {51.40+ox:.2f},{42.35+oy:.2f} {54.00+ox:.2f},{42.95+oy:.2f} "
        f"C {56.65+ox:.2f},{43.55+oy:.2f} {58.55+ox:.2f},{44.35+oy:.2f} {58.55+ox:.2f},{46.60+oy:.2f} "
        f"C {58.55+ox:.2f},{48.75+oy:.2f} {56.65+ox:.2f},{50.15+oy:.2f} {54.00+ox:.2f},{50.15+oy:.2f} "
        f"C {51.70+ox:.2f},{50.15+oy:.2f} {49.95+ox:.2f},{49.15+oy:.2f} {49.40+ox:.2f},{47.30+oy:.2f}"
    )

    sx, sy = ox + 0.85, oy + 1.45
    dollar_bar_shadow_d = (
        f"M {52.85+sx:.2f},{33.60+sy:.2f} "
        f"L {55.15+sx:.2f},{33.60+sy:.2f} "
        f"L {55.15+sx:.2f},{52.20+sy:.2f} "
        f"L {52.85+sx:.2f},{52.20+sy:.2f} Z"
    )
    dollar_s_shadow_d = (
        f"M {58.15+sx:.2f},{38.40+sy:.2f} "
        f"C {57.70+sx:.2f},{36.65+sy:.2f} {56.05+sx:.2f},{35.85+sy:.2f} {54.00+sx:.2f},{35.85+sy:.2f} "
        f"C {51.20+sx:.2f},{35.85+sy:.2f} {49.55+sx:.2f},{37.40+sy:.2f} {49.55+sx:.2f},{39.35+sy:.2f} "
        f"C {49.55+sx:.2f},{41.60+sy:.2f} {51.40+sx:.2f},{42.35+sy:.2f} {54.00+sx:.2f},{42.95+sy:.2f} "
        f"C {56.65+sx:.2f},{43.55+sy:.2f} {58.55+sx:.2f},{44.35+sy:.2f} {58.55+sx:.2f},{46.60+sy:.2f} "
        f"C {58.55+sx:.2f},{48.75+sy:.2f} {56.65+sx:.2f},{50.15+sy:.2f} {54.00+sx:.2f},{50.15+sy:.2f} "
        f"C {51.70+sx:.2f},{50.15+sy:.2f} {49.95+sx:.2f},{49.15+sy:.2f} {49.40+sx:.2f},{47.30+sy:.2f}"
    )

    return {
        "amber_d": amber_d,
        "amber_depth_d": amber_depth_d,
        "shadow_d": shadow_d,
        "emerald_d": emerald_d,
        "emerald_depth_d": emerald_depth_d,
        "emerald_hi_d": emerald_hi_d,
        "mono_amber_d": mono_amber_d,
        "mono_em_d": emerald_d,
        "dollar_bar_d": dollar_bar_d,
        "dollar_s_d": dollar_s_d,
        "dollar_bar_shadow_d": dollar_bar_shadow_d,
        "dollar_s_shadow_d": dollar_s_shadow_d,
    }


p108 = build_paths(0.0, 0.0)
p76 = build_paths(-16.0, -16.0)


def make_svg(viewbox, width, height, include_bg=True, bg_rect=(0, 0, 108, 108), monochrome=False, dollar_color="#FFFFFF", paths=p108):
    ox = -bg_rect[0] if bg_rect == (0, 0, 76, 76) else 0.0
    oy = -bg_rect[1] if bg_rect == (0, 0, 76, 76) else 0.0
    defs = ""
    if not monochrome or include_bg:
        defs = f"""  <defs>
    <linearGradient id="bgGrad" x1="{bg_rect[0]}" y1="{bg_rect[1]}" x2="{bg_rect[0]+bg_rect[2]}" y2="{bg_rect[1]+bg_rect[3]}" gradientUnits="userSpaceOnUse">
      <stop offset="0%" stop-color="#0A101D"/>
      <stop offset="100%" stop-color="#172133"/>
    </linearGradient>
    <linearGradient id="amberGrad" x1="{28+ox:.1f}" y1="{33+oy:.1f}" x2="{52+ox:.1f}" y2="{76+oy:.1f}" gradientUnits="userSpaceOnUse">
      <stop offset="0%" stop-color="#F59E0B"/>
      <stop offset="55%" stop-color="#D97706"/>
      <stop offset="100%" stop-color="#B45309"/>
    </linearGradient>
    <linearGradient id="foldShadowGrad" x1="{46+ox:.1f}" y1="{66+oy:.1f}" x2="{54+ox:.1f}" y2="{62+oy:.1f}" gradientUnits="userSpaceOnUse">
      <stop offset="0%" stop-color="#78350F" stop-opacity="0.15"/>
      <stop offset="100%" stop-color="#78350F" stop-opacity="0.78"/>
    </linearGradient>
    <linearGradient id="emeraldGrad" x1="{46+ox:.1f}" y1="{78+oy:.1f}" x2="{80+ox:.1f}" y2="{33+oy:.1f}" gradientUnits="userSpaceOnUse">
      <stop offset="0%" stop-color="#047857"/>
      <stop offset="52%" stop-color="#059669"/>
      <stop offset="100%" stop-color="#10B981"/>
    </linearGradient>
  </defs>\n"""

    bg_el = f'  <rect x="{bg_rect[0]}" y="{bg_rect[1]}" width="{bg_rect[2]}" height="{bg_rect[3]}" fill="url(#bgGrad)"/>\n' if include_bg else ""

    if monochrome:
        body = (
            f'  <path d="{paths["mono_amber_d"]}" fill="#FFFFFF"/>\n'
            f'  <path d="{paths["mono_em_d"]}" fill="#FFFFFF"/>\n'
            f'  <path d="{paths["dollar_bar_d"]}" fill="#FFFFFF"/>\n'
            f'  <path d="{paths["dollar_s_d"]}" fill="none" stroke="#FFFFFF" stroke-width="2.80" stroke-linecap="butt"/>\n'
        )
    else:
        dollar_shadow = ""
        if dollar_color.upper() == "#FFFFFF":
            dollar_shadow = (
                f'  <path d="{paths["dollar_bar_shadow_d"]}" fill="#040812" opacity="0.48"/>\n'
                f'  <path d="{paths["dollar_s_shadow_d"]}" fill="none" stroke="#040812" stroke-width="2.90" stroke-linecap="butt" opacity="0.48"/>\n'
            )
        body = (
            f'  <path d="{paths["amber_d"]}" fill="url(#amberGrad)"/>\n'
            f'  <path d="{paths["amber_depth_d"]}" fill="#92400E" opacity="0.32"/>\n'
            f'  <path d="{paths["shadow_d"]}" fill="url(#foldShadowGrad)"/>\n'
            f'  <path d="{paths["emerald_d"]}" fill="url(#emeraldGrad)"/>\n'
            f'  <path d="{paths["emerald_depth_d"]}" fill="#022C22" opacity="0.30"/>\n'
            f'  <path d="{paths["emerald_hi_d"]}" fill="#34D399" opacity="0.20"/>\n'
            f'{dollar_shadow}'
            f'  <path d="{paths["dollar_bar_d"]}" fill="{dollar_color}"/>\n'
            f'  <path d="{paths["dollar_s_d"]}" fill="none" stroke="{dollar_color}" stroke-width="2.80" stroke-linecap="butt"/>\n'
        )

    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{viewbox}" width="{width}" height="{height}">\n'
        f"{defs}{bg_el}{body}</svg>\n"
    )


def write_file(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as fp:
        fp.write(content)


def render_svg_png(svg_path, png_path, size):
    subprocess.run(["rsvg-convert", "-w", str(size), "-h", str(size), svg_path, "-o", png_path], check=True)


# ---------------------------------------------------------------------------
# 2. Generate SVG & PNG Masters in brand/
# ---------------------------------------------------------------------------
svg_master_108 = make_svg("0 0 108 108", 1024, 1024, include_bg=True, bg_rect=(0, 0, 108, 108))
svg_square = make_svg("16 16 76 76", 1024, 1024, include_bg=True, bg_rect=(16, 16, 76, 76))
svg_emblem = make_svg("16 16 76 76", 1024, 1024, include_bg=False, bg_rect=(16, 16, 76, 76), dollar_color="#FFFFFF")
svg_emblem_light = make_svg("16 16 76 76", 1024, 1024, include_bg=False, bg_rect=(16, 16, 76, 76), dollar_color="#0F172A")
svg_mono = make_svg("0 0 108 108", 512, 512, include_bg=False, monochrome=True)
svg_mono_sq = make_svg("16 16 76 76", 512, 512, include_bg=False, monochrome=True)

path_svg_master = os.path.join(BRAND_DIR, "tasave_logo_master_108.svg")
path_svg_square = os.path.join(BRAND_DIR, "tasave_logo_square.svg")
path_svg_emblem = os.path.join(BRAND_DIR, "tasave_emblem_transparent.svg")
path_svg_emblem_light = os.path.join(BRAND_DIR, "tasave_emblem_lightbg.svg")
path_svg_mono = os.path.join(BRAND_DIR, "tasave_emblem_monochrome.svg")
path_svg_mono_sq = os.path.join(BRAND_DIR, "tasave_emblem_monochrome_square.svg")

write_file(path_svg_master, svg_master_108)
write_file(path_svg_square, svg_square)
write_file(path_svg_emblem, svg_emblem)
write_file(path_svg_emblem_light, svg_emblem_light)
write_file(path_svg_mono, svg_mono)

write_file(path_svg_mono_sq, svg_mono_sq)

png_1024 = os.path.join(BRAND_DIR, "tasave_logo_1024x1024.png")
png_512 = os.path.join(BRAND_DIR, "tasave_logo_512x512.png")
png_emb_1024 = os.path.join(BRAND_DIR, "tasave_emblem_transparent_1024x1024.png")
png_emb_512 = os.path.join(BRAND_DIR, "tasave_emblem_transparent_512x512.png")
png_emb_light_1024 = os.path.join(BRAND_DIR, "tasave_emblem_lightbg_1024x1024.png")
png_emb_light_512 = os.path.join(BRAND_DIR, "tasave_emblem_lightbg_512x512.png")
png_mono_512 = os.path.join(BRAND_DIR, "tasave_emblem_monochrome_512x512.png")

render_svg_png(path_svg_square, png_1024, 1024)
render_svg_png(path_svg_square, png_512, 512)
render_svg_png(path_svg_emblem, png_emb_1024, 1024)
render_svg_png(path_svg_emblem, png_emb_512, 512)
render_svg_png(path_svg_emblem_light, png_emb_light_1024, 1024)
render_svg_png(path_svg_emblem_light, png_emb_light_512, 512)
render_svg_png(path_svg_mono_sq, png_mono_512, 512)
os.remove(path_svg_mono_sq)

shutil.copyfile(png_512, os.path.join(DOCS_ASSETS_DIR, "app_icon_512x512.png"))
shutil.copyfile(png_emb_512, os.path.join(DOCS_ASSETS_DIR, "logo_tasave_transparent_512.png"))

# ---------------------------------------------------------------------------
# 3. Generate Android VectorDrawables & Adaptive Icon XMLs
# ---------------------------------------------------------------------------
ic_fg_xml = f"""<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">

    <!-- Left Amber Arrow Arm (Deep Rich Amber Gold) -->
    <path android:pathData="{p108['amber_d']}">
        <aapt:attr name="android:fillColor">
            <gradient
                android:startX="28"
                android:startY="33"
                android:endX="52"
                android:endY="76"
                android:type="linear">
                <item android:offset="0" android:color="#F59E0B" />
                <item android:offset="0.55" android:color="#D97706" />
                <item android:offset="1" android:color="#B45309" />
            </gradient>
        </aapt:attr>
    </path>

    <!-- Amber Outer-Lower Edge Depth -->
    <path
        android:pathData="{p108['amber_depth_d']}"
        android:fillColor="#92400E"
        android:fillAlpha="0.32" />

    <!-- Layered 3D Ribbon Fold Shadow at V-Vertex -->
    <path android:pathData="{p108['shadow_d']}">
        <aapt:attr name="android:fillColor">
            <gradient
                android:startX="46"
                android:startY="66"
                android:endX="54"
                android:endY="62"
                android:type="linear">
                <item android:offset="0" android:color="#2678350F" />
                <item android:offset="1" android:color="#C778350F" />
            </gradient>
        </aapt:attr>
    </path>

    <!-- Right Continuous Emerald Arrow Arm (Deep Fintech Emerald) -->
    <path android:pathData="{p108['emerald_d']}">
        <aapt:attr name="android:fillColor">
            <gradient
                android:startX="46"
                android:startY="78"
                android:endX="80"
                android:endY="33"
                android:type="linear">
                <item android:offset="0" android:color="#047857" />
                <item android:offset="0.52" android:color="#059669" />
                <item android:offset="1" android:color="#10B981" />
            </gradient>
        </aapt:attr>
    </path>

    <!-- Emerald Lower U-Loop & Outer Edge Depth Shading -->
    <path
        android:pathData="{p108['emerald_depth_d']}"
        android:fillColor="#022C22"
        android:fillAlpha="0.30" />

    <!-- Subtle Top Rim Highlight on Emerald Arrowhead -->
    <path
        android:pathData="{p108['emerald_hi_d']}"
        android:fillColor="#34D399"
        android:fillAlpha="0.20" />

    <!-- Soft Drop Shadow Behind Dollar Sign -->
    <path
        android:pathData="{p108['dollar_bar_shadow_d']}"
        android:fillColor="#040812"
        android:fillAlpha="0.48" />
    <path
        android:pathData="{p108['dollar_s_shadow_d']}"
        android:strokeColor="#040812"
        android:strokeAlpha="0.48"
        android:strokeWidth="2.90"
        android:strokeLineCap="butt" />

    <!-- Crisp Upper-Center Geometric Dollar Sign -->
    <path
        android:pathData="{p108['dollar_bar_d']}"
        android:fillColor="#FFFFFF" />
    <path
        android:pathData="{p108['dollar_s_d']}"
        android:strokeColor="#FFFFFF"
        android:strokeWidth="2.80"
        android:strokeLineCap="butt" />
</vector>
"""

ic_mono_xml = f"""<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">

    <path
        android:pathData="{p108['mono_amber_d']}"
        android:fillColor="#FFFFFF" />
    <path
        android:pathData="{p108['mono_em_d']}"
        android:fillColor="#FFFFFF" />
    <path
        android:pathData="{p108['dollar_bar_d']}"
        android:fillColor="#FFFFFF" />
    <path
        android:pathData="{p108['dollar_s_d']}"
        android:strokeColor="#FFFFFF"
        android:strokeWidth="2.80"
        android:strokeLineCap="butt" />
</vector>
"""

ic_brand_mark_xml = f"""<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="76dp"
    android:height="76dp"
    android:viewportWidth="76"
    android:viewportHeight="76">

    <path android:pathData="{p76['amber_d']}">
        <aapt:attr name="android:fillColor">
            <gradient
                android:startX="12"
                android:startY="17"
                android:endX="36"
                android:endY="60"
                android:type="linear">
                <item android:offset="0" android:color="#F59E0B" />
                <item android:offset="0.55" android:color="#D97706" />
                <item android:offset="1" android:color="#B45309" />
            </gradient>
        </aapt:attr>
    </path>

    <path
        android:pathData="{p76['amber_depth_d']}"
        android:fillColor="#92400E"
        android:fillAlpha="0.32" />

    <path android:pathData="{p76['shadow_d']}">
        <aapt:attr name="android:fillColor">
            <gradient
                android:startX="30"
                android:startY="50"
                android:endX="38"
                android:endY="46"
                android:type="linear">
                <item android:offset="0" android:color="#2678350F" />
                <item android:offset="1" android:color="#C778350F" />
            </gradient>
        </aapt:attr>
    </path>

    <path android:pathData="{p76['emerald_d']}">
        <aapt:attr name="android:fillColor">
            <gradient
                android:startX="30"
                android:startY="62"
                android:endX="64"
                android:endY="17"
                android:type="linear">
                <item android:offset="0" android:color="#047857" />
                <item android:offset="0.52" android:color="#059669" />
                <item android:offset="1" android:color="#10B981" />
            </gradient>
        </aapt:attr>
    </path>

    <path
        android:pathData="{p76['emerald_depth_d']}"
        android:fillColor="#022C22"
        android:fillAlpha="0.30" />

    <path
        android:pathData="{p76['emerald_hi_d']}"
        android:fillColor="#34D399"
        android:fillAlpha="0.20" />

    <path
        android:pathData="{p76['dollar_bar_shadow_d']}"
        android:fillColor="#040812"
        android:fillAlpha="0.48" />
    <path
        android:pathData="{p76['dollar_s_shadow_d']}"
        android:strokeColor="#040812"
        android:strokeAlpha="0.48"
        android:strokeWidth="2.90"
        android:strokeLineCap="butt" />

    <path
        android:pathData="{p76['dollar_bar_d']}"
        android:fillColor="#FFFFFF" />
    <path
        android:pathData="{p76['dollar_s_d']}"
        android:strokeColor="#FFFFFF"
        android:strokeWidth="2.80"
        android:strokeLineCap="butt" />
</vector>
"""

adaptive_xml = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
"""

write_file(os.path.join(RES_DIR, "drawable", "ic_launcher_foreground.xml"), ic_fg_xml)
write_file(os.path.join(RES_DIR, "drawable", "ic_launcher_monochrome.xml"), ic_mono_xml)
write_file(os.path.join(RES_DIR, "drawable", "ic_brand_mark.xml"), ic_brand_mark_xml)
write_file(os.path.join(RES_DIR, "mipmap-anydpi-v26", "ic_launcher.xml"), adaptive_xml)
write_file(os.path.join(RES_DIR, "mipmap-anydpi-v26", "ic_launcher_round.xml"), adaptive_xml)

# ---------------------------------------------------------------------------
# 4. Generate Legacy Mipmap .webp Launchers (4x supersampled anti-aliased masks)
# ---------------------------------------------------------------------------
MIPMAP_SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

src_square = Image.open(png_1024).convert("RGBA")

for folder, px in MIPMAP_SIZES.items():
    out_dir = os.path.join(RES_DIR, folder)
    os.makedirs(out_dir, exist_ok=True)
    ss = px * 4
    pad = round(ss * 0.04)
    inner_size = ss - 2 * pad
    scaled_bg = src_square.resize((inner_size, inner_size), Image.Resampling.LANCZOS)

    sq_canvas = Image.new("RGBA", (ss, ss), (0, 0, 0, 0))
    sq_mask = Image.new("L", (inner_size, inner_size), 0)
    ImageDraw.Draw(sq_mask).rounded_rectangle(
        [0, 0, inner_size - 1, inner_size - 1],
        radius=round(inner_size * 0.21),
        fill=255,
    )
    sq_tile = Image.new("RGBA", (inner_size, inner_size), (0, 0, 0, 0))
    sq_tile.paste(scaled_bg, (0, 0), sq_mask)
    sq_canvas.paste(sq_tile, (pad, pad), sq_tile)
    sq_out = sq_canvas.resize((px, px), Image.Resampling.LANCZOS)
    sq_out.save(os.path.join(out_dir, "ic_launcher.webp"), format="WEBP", quality=95, method=6)

    rd_canvas = Image.new("RGBA", (ss, ss), (0, 0, 0, 0))
    rd_mask = Image.new("L", (inner_size, inner_size), 0)
    ImageDraw.Draw(rd_mask).ellipse([0, 0, inner_size - 1, inner_size - 1], fill=255)
    rd_tile = Image.new("RGBA", (inner_size, inner_size), (0, 0, 0, 0))
    rd_tile.paste(scaled_bg, (0, 0), rd_mask)
    rd_canvas.paste(rd_tile, (pad, pad), rd_tile)
    rd_out = rd_canvas.resize((px, px), Image.Resampling.LANCZOS)
    rd_out.save(os.path.join(out_dir, "ic_launcher_round.webp"), format="WEBP", quality=95, method=6)

# ---------------------------------------------------------------------------
# 5. Generate Play Store & Web Feature Graphic (1024x500)
# ---------------------------------------------------------------------------
W, H = 1024, 500
fg = Image.new("RGBA", (W, H))
pix = fg.load()
for y in range(H):
    for x in range(W):
        t = (x / W * 0.65 + y / H * 0.35)
        r = int(10 + t * 13)
        g = int(16 + t * 17)
        b = int(29 + t * 22)
        d_em = math.hypot(x - 235, y - 250)
        if d_em < 260:
            glow = ((1.0 - d_em / 260.0) ** 2) * 0.16
            r = min(255, int(r + 5 * glow))
            g = min(255, int(g + 150 * glow))
            b = min(255, int(b + 105 * glow))
        pix[x, y] = (r, g, b, 255)

overlay = Image.new("RGBA", (W, H), (0, 0, 0, 0))
draw = ImageDraw.Draw(overlay)

TILE = 280
TX, TY = 92, 110
tile_img = src_square.resize((TILE, TILE), Image.Resampling.LANCZOS)
tile_mask = Image.new("L", (TILE, TILE), 0)
ImageDraw.Draw(tile_mask).rounded_rectangle([0, 0, TILE - 1, TILE - 1], radius=58, fill=255)
fg.paste(tile_img, (TX, TY), tile_mask)
draw.rounded_rectangle([TX, TY, TX + TILE, TY + TILE], radius=58, outline=(16, 185, 129, 95), width=3)

font_bold = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
font_reg = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
f_title = ImageFont.truetype(font_bold, 76)
f_sub = ImageFont.truetype(font_reg, 27)
f_pill = ImageFont.truetype(font_bold, 21)

RX = 418
draw.text((RX, 126), "Tasa VE", font=f_title, fill=(248, 250, 252, 255))
draw.text((RX, 224), "Cotizaciones y Calculadora en Tiempo Real", font=f_sub, fill=(148, 163, 184, 255))

pills = [
    ("Dólar BCV", (16, 185, 129), (16, 185, 129, 28), (52, 211, 153, 255)),
    ("Euro BCV", (59, 130, 246), (59, 130, 246, 28), (96, 165, 250, 255)),
    ("USDT P2P", (217, 119, 6), (217, 119, 6, 28), (245, 158, 11, 255)),
]
px_cursor = RX
py_top = 294
for label, border_rgb, fill_rgba, text_rgba in pills:
    bbox = draw.textbbox((0, 0), label, font=f_pill)
    tw = bbox[2] - bbox[0]
    th = bbox[3] - bbox[1]
    pw = tw + 40
    ph = 48
    draw.rounded_rectangle([px_cursor, py_top, px_cursor + pw, py_top + ph], radius=24, fill=fill_rgba, outline=(*border_rgb, 210), width=2)
    draw.text((px_cursor + 20, py_top + (ph - th) // 2 - 3), label, font=f_pill, fill=text_rgba)
    px_cursor += pw + 16

fg = Image.alpha_composite(fg, overlay)
fg.convert("RGB").save(os.path.join(DOCS_ASSETS_DIR, "feature_graphic_1024x500.png"), format="PNG", optimize=True)

print("All rich-depth Tasa VE brand assets generated successfully!")
