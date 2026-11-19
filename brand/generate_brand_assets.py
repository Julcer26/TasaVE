#!/usr/bin/env python3
"""
Tasa VE Brand Asset Generator (Proposal 2 — 3D Esculpido)
Generates canonical SVG masters, multi-resolution PNG exports (dark, transparent, lightbg, monochrome),
Android VectorDrawables (adaptive background/foreground/monochrome, in-app mark with clipped 3D bevels),
legacy mipmap .webp launcher icons, and Play Store / Web assets.
"""

import math
import os
import shutil
import subprocess
from PIL import Image, ImageDraw, ImageFilter, ImageFont

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

amber_seg, emerald_seg = split_cubic(U_L_OUT, C1_OUT, C2_OUT, U_R_OUT, 0.22)
U_SPLIT = amber_seg[3]
T_SPLIT = cubic_tan(U_L_OUT, C1_OUT, C2_OUT, U_R_OUT, 0.22)
N_SPLIT_IN = (T_SPLIT[1], -T_SPLIT[0])

amber_mid_in = add(cubic(U_L_OUT, C1_OUT, C2_OUT, U_R_OUT, 0.34), (0, -2.5))
amber_top_in = add(APEX_IN, (2.2, 2.5))

fold_c1 = add(U_SPLIT, N_SPLIT_IN, 4.0)
fold_c2 = add(APEX_IN, (-uR[0], -uR[1]), 3.8)

fold_sh_c1 = add(U_SPLIT, N_SPLIT_IN, 4.4)
fold_sh_c2 = add(APEX_IN, (-uR[0], -uR[1]), 4.2)

# Uniform-channel monochrome left arm (~1.9u clean channel along entire fold)
mono_amber_seg, _ = split_cubic(U_L_OUT, C1_OUT, C2_OUT, U_R_OUT, 0.175)
mono_a_end = mono_amber_seg[3]
mono_shift = (-1.9, -0.55)
mono_c1 = add(fold_c1, mono_shift, 1.0)
mono_c2 = add(fold_c2, mono_shift, 1.0)
mono_top_in = point_on_line(SHAFT_L_TOP_IN, (-uL[0], -uL[1]), APEX_IN[1] - 2.15)


def build_paths(scale=1.0, ox=0.0, oy=0.0):
    def pt(p, dx=0.0, dy=0.0):
        return f"{(p[0] + dx) * scale + ox:.2f},{(p[1] + dy) * scale + oy:.2f}"

    def xy(x, y):
        return f"{x * scale + ox:.2f},{y * scale + oy:.2f}"

    amber_d = " ".join([
        f"M {pt(TIP_L)}",
        f"L {pt(WING_L_OUT)}",
        f"L {pt(SHAFT_L_TOP_OUT)}",
        f"L {pt(U_L_OUT)}",
        f"C {pt(amber_seg[1])} {pt(amber_seg[2])} {pt(U_SPLIT)}",
        f"L {pt(amber_mid_in)}",
        f"L {pt(amber_top_in)}",
        f"L {pt(APEX_IN)}",
        f"L {pt(SHAFT_L_TOP_IN)}",
        f"L {pt(WING_L_IN)}",
        "Z",
    ])

    emerald_d = " ".join([
        f"M {pt(TIP_R)}",
        f"L {pt(WING_R_OUT)}",
        f"L {pt(SHAFT_R_TOP_OUT)}",
        f"L {pt(U_R_OUT)}",
        f"C {pt(emerald_seg[2])} {pt(emerald_seg[1])} {pt(U_SPLIT)}",
        f"C {pt(fold_c1)} {pt(fold_c2)} {pt(APEX_IN)}",
        f"L {pt(SHAFT_R_TOP_IN)}",
        f"L {pt(WING_R_IN)}",
        "Z",
    ])

    fold_shadow_band_d = " ".join([
        f"M {pt(add(APEX_IN, (-uL[0], -uL[1]), -4.0))}",
        f"L {pt(add(APEX_IN, (2.4, 2.2)))}",
        f"L {pt(add(amber_mid_in, (2.2, 0.5)))}",
        f"L {pt(U_SPLIT)}",
        f"C {pt(fold_sh_c1)} {pt(fold_sh_c2)} {pt(APEX_IN)}",
        "Z",
    ])

    fold_seam_curve_d = " ".join([
        f"M {pt(APEX_IN)}",
        f"C {pt(fold_c2)} {pt(fold_c1)} {pt(U_SPLIT)}",
    ])

    amber_lit_edge_d = " ".join([
        f"M {pt(TIP_L)}",
        f"L {pt(WING_L_OUT)}",
        f"L {pt(SHAFT_L_TOP_OUT)}",
        f"L {pt(U_L_OUT)}",
        f"C {pt(amber_seg[1])} {pt(amber_seg[2])} {pt(U_SPLIT)}",
    ])

    amber_inner_edge_d = " ".join([
        f"M {pt(TIP_L)}",
        f"L {pt(WING_L_IN)}",
        f"L {pt(SHAFT_L_TOP_IN)}",
        f"L {pt(APEX_IN)}",
    ])

    emerald_dark_edge_d = " ".join([
        f"M {pt(U_SPLIT)}",
        f"C {pt(emerald_seg[1])} {pt(emerald_seg[2])} {pt(U_R_OUT)}",
        f"L {pt(SHAFT_R_TOP_OUT)}",
        f"L {pt(WING_R_OUT)}",
        f"L {pt(TIP_R)}",
    ])

    emerald_lit_ridge_d = " ".join([
        f"M {pt(U_SPLIT)}",
        f"C {pt(fold_c1)} {pt(fold_c2)} {pt(APEX_IN)}",
        f"L {pt(SHAFT_R_TOP_IN)}",
        f"L {pt(WING_R_IN)}",
        f"L {pt(TIP_R)}",
    ])

    mono_amber_d = " ".join([
        f"M {pt(TIP_L)}",
        f"L {pt(WING_L_OUT)}",
        f"L {pt(SHAFT_L_TOP_OUT)}",
        f"L {pt(U_L_OUT)}",
        f"C {pt(mono_amber_seg[1])} {pt(mono_amber_seg[2])} {pt(mono_a_end)}",
        f"C {pt(mono_c1)} {pt(mono_c2)} {pt(mono_top_in)}",
        f"L {pt(SHAFT_L_TOP_IN)}",
        f"L {pt(WING_L_IN)}",
        "Z",
    ])

    def make_dollar(dx=0.0, dy=0.0):
        bar = (
            f"M {xy(52.85 + dx, 33.60 + dy)} "
            f"L {xy(55.15 + dx, 33.60 + dy)} "
            f"L {xy(55.15 + dx, 52.20 + dy)} "
            f"L {xy(52.85 + dx, 52.20 + dy)} Z"
        )
        s_path = (
            f"M {xy(58.15 + dx, 38.40 + dy)} "
            f"C {xy(57.70 + dx, 36.65 + dy)} {xy(56.05 + dx, 35.85 + dy)} {xy(54.00 + dx, 35.85 + dy)} "
            f"C {xy(51.20 + dx, 35.85 + dy)} {xy(49.55 + dx, 37.40 + dy)} {xy(49.55 + dx, 39.35 + dy)} "
            f"C {xy(49.55 + dx, 41.60 + dy)} {xy(51.40 + dx, 42.35 + dy)} {xy(54.00 + dx, 42.95 + dy)} "
            f"C {xy(56.65 + dx, 43.55 + dy)} {xy(58.55 + dx, 44.35 + dy)} {xy(58.55 + dx, 46.60 + dy)} "
            f"C {xy(58.55 + dx, 48.75 + dy)} {xy(56.65 + dx, 50.15 + dy)} {xy(54.00 + dx, 50.15 + dy)} "
            f"C {xy(51.70 + dx, 50.15 + dy)} {xy(49.95 + dx, 49.15 + dy)} {xy(49.40 + dx, 47.30 + dy)}"
        )
        return bar, s_path

    dollar_bar_d, dollar_s_d = make_dollar(0.0, 0.0)
    dollar_bar_sh_far_d, dollar_s_sh_far_d = make_dollar(0.45, 1.50)
    dollar_bar_sh_near_d, dollar_s_sh_near_d = make_dollar(0.18, 0.72)

    def make_v_shadow(dx=0.0, dy=0.0):
        am = " ".join([
            f"M {pt(TIP_L, dx, dy)}",
            f"L {pt(WING_L_OUT, dx, dy)}",
            f"L {pt(SHAFT_L_TOP_OUT, dx, dy)}",
            f"L {pt(U_L_OUT, dx, dy)}",
            f"C {pt(amber_seg[1], dx, dy)} {pt(amber_seg[2], dx, dy)} {pt(U_SPLIT, dx, dy)}",
            f"L {pt(amber_mid_in, dx, dy)}",
            f"L {pt(amber_top_in, dx, dy)}",
            f"L {pt(APEX_IN, dx, dy)}",
            f"L {pt(SHAFT_L_TOP_IN, dx, dy)}",
            f"L {pt(WING_L_IN, dx, dy)}",
            "Z",
        ])
        em = " ".join([
            f"M {pt(TIP_R, dx, dy)}",
            f"L {pt(WING_R_OUT, dx, dy)}",
            f"L {pt(SHAFT_R_TOP_OUT, dx, dy)}",
            f"L {pt(U_R_OUT, dx, dy)}",
            f"C {pt(emerald_seg[2], dx, dy)} {pt(emerald_seg[1], dx, dy)} {pt(U_SPLIT, dx, dy)}",
            f"C {pt(fold_c1, dx, dy)} {pt(fold_c2, dx, dy)} {pt(APEX_IN, dx, dy)}",
            f"L {pt(SHAFT_R_TOP_IN, dx, dy)}",
            f"L {pt(WING_R_IN, dx, dy)}",
            "Z",
        ])
        return am, em

    v_sh_far_am, v_sh_far_em = make_v_shadow(0.10, 1.95)
    v_sh_mid_am, v_sh_mid_em = make_v_shadow(0.25, 1.15)
    v_sh_near_am, v_sh_near_em = make_v_shadow(0.18, 0.55)

    return {
        "amber_d": amber_d,
        "emerald_d": emerald_d,
        "fold_shadow_band_d": fold_shadow_band_d,
        "fold_seam_curve_d": fold_seam_curve_d,
        "amber_lit_edge_d": amber_lit_edge_d,
        "amber_inner_edge_d": amber_inner_edge_d,
        "emerald_dark_edge_d": emerald_dark_edge_d,
        "emerald_lit_ridge_d": emerald_lit_ridge_d,
        "mono_amber_d": mono_amber_d,
        "mono_em_d": emerald_d,
        "dollar_bar_d": dollar_bar_d,
        "dollar_s_d": dollar_s_d,
        "dollar_bar_sh_far_d": dollar_bar_sh_far_d,
        "dollar_s_sh_far_d": dollar_s_sh_far_d,
        "dollar_bar_sh_near_d": dollar_bar_sh_near_d,
        "dollar_s_sh_near_d": dollar_s_sh_near_d,
        "v_sh_far_am": v_sh_far_am,
        "v_sh_far_em": v_sh_far_em,
        "v_sh_mid_am": v_sh_mid_am,
        "v_sh_mid_em": v_sh_mid_em,
        "v_sh_near_am": v_sh_near_am,
        "v_sh_near_em": v_sh_near_em,
    }


S512 = 512.0 / 76.0
p512 = build_paths(scale=S512, ox=-16.0 * S512, oy=-16.0 * S512)
p108 = build_paths(scale=1.0, ox=0.0, oy=0.0)
p76 = build_paths(scale=1.0, ox=-16.0, oy=-16.0)


def make_3d_svg(
    viewport_size,
    width,
    height,
    paths,
    scale_to_512,
    include_bg=True,
    monochrome=False,
    lightbg=False,
    coord_offset=0.0,
):
    """Generates Proposal 2 (3D Esculpido) SVG at arbitrary viewport scale."""
    u = scale_to_512  # ratio relative to 512px reference

    def gp(x512, y512):
        # Convert 512px reference coordinate back into current viewport coordinates
        return f"{(x512 * u) + coord_offset:.2f}", f"{(y512 * u) + coord_offset:.2f}"

    if monochrome:
        sw = 2.80 * (viewport_size / 108.0 if viewport_size == 108.0 else S512)
        return f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {viewport_size:g} {viewport_size:g}" width="{width}" height="{height}">
  <path d="{paths['mono_amber_d']}" fill="#FFFFFF"/>
  <path d="{paths['mono_em_d']}" fill="#FFFFFF"/>
  <path d="{paths['dollar_bar_d']}" fill="#FFFFFF"/>
  <path d="{paths['dollar_s_d']}" fill="none" stroke="#FFFFFF" stroke-width="{sw:.2f}" stroke-linecap="butt"/>
</svg>
"""

    # Stroke & filter scaling relative to 512px reference
    k = u
    sw_dollar = 18.86 * k

    # Shadow intensities tuned for dark vs light background
    v_amb_op = "0.28" if lightbg else "0.62"
    v_key_op = "0.34" if lightbg else "0.70"
    v_sh_col = "#0F172A" if lightbg else "#020617"
    d_amb_op = "0.26" if lightbg else "0.76"
    d_key_op = "0.22" if lightbg else "0.58"

    if lightbg:
        dollar_stops = """      <stop offset="0%" stop-color="#0F172A"/>
      <stop offset="75%" stop-color="#1E293B"/>
      <stop offset="100%" stop-color="#334155"/>"""
    else:
        dollar_stops = """      <stop offset="0%" stop-color="#FFFFFF"/>
      <stop offset="72%" stop-color="#F1F5F9"/>
      <stop offset="100%" stop-color="#CBD5E1"/>"""

    bg_defs = ""
    bg_nodes = ""
    if include_bg:
        bg_defs = f"""    <linearGradient id="bg2" x1="0" y1="0" x2="{viewport_size:g}" y2="{viewport_size:g}" gradientUnits="userSpaceOnUse">
      <stop offset="0%" stop-color="#060A12"/>
      <stop offset="60%" stop-color="#0A111F"/>
      <stop offset="100%" stop-color="#111B2D"/>
    </linearGradient>
    <radialGradient id="ambientAura2" cx="{viewport_size/2:.2f}" cy="{viewport_size*0.4785:.2f}" r="{viewport_size*0.43:.2f}" gradientUnits="userSpaceOnUse">
      <stop offset="0%" stop-color="#065F46" stop-opacity="0.22"/>
      <stop offset="60%" stop-color="#0F172A" stop-opacity="0.06"/>
      <stop offset="100%" stop-color="#060A12" stop-opacity="0"/>
    </radialGradient>\n"""
        bg_nodes = (
            f'  <rect x="0" y="0" width="{viewport_size:g}" height="{viewport_size:g}" fill="url(#bg2)"/>\n'
            f'  <circle cx="{viewport_size/2:.2f}" cy="{viewport_size*0.4785:.2f}" r="{viewport_size*0.43:.2f}" fill="url(#ambientAura2)"/>\n'
        )

    am_x1, am_y1 = gp(115, 115)
    am_x2, am_y2 = gp(262, 395)
    rim_x1, rim_y1 = gp(100, 115)
    rim_x2, rim_y2 = gp(235, 385)
    fg_x1, fg_y1 = gp(215, 350)
    fg_x2, fg_y2 = gp(255, 325)
    em_x1, em_y1 = gp(245, 405)
    em_x2, em_y2 = gp(405, 115)
    et_x1, et_y1 = gp(245, 335)
    et_x2, et_y2 = gp(405, 115)
    dl_x1, dl_y1 = gp(256, 115)
    dl_x2, dl_y2 = gp(256, 235)

    return f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {viewport_size:g} {viewport_size:g}" width="{width}" height="{height}">
  <defs>
{bg_defs}    <linearGradient id="amberVol" x1="{am_x1}" y1="{am_y1}" x2="{am_x2}" y2="{am_y2}" gradientUnits="userSpaceOnUse">
      <stop offset="0%" stop-color="#EA8C00"/>
      <stop offset="40%" stop-color="#C96400"/>
      <stop offset="78%" stop-color="#9A4100"/>
      <stop offset="100%" stop-color="#6B2B00"/>
    </linearGradient>
    <linearGradient id="amberRim" x1="{rim_x1}" y1="{rim_y1}" x2="{rim_x2}" y2="{rim_y2}" gradientUnits="userSpaceOnUse">
      <stop offset="0%" stop-color="#FDE68A" stop-opacity="0.58"/>
      <stop offset="65%" stop-color="#F59E0B" stop-opacity="0.22"/>
      <stop offset="100%" stop-color="#B45309" stop-opacity="0"/>
    </linearGradient>
    <linearGradient id="foldGrad2" x1="{fg_x1}" y1="{fg_y1}" x2="{fg_x2}" y2="{fg_y2}" gradientUnits="userSpaceOnUse">
      <stop offset="0%" stop-color="#3B1400" stop-opacity="0"/>
      <stop offset="100%" stop-color="#290D00" stop-opacity="0.82"/>
    </linearGradient>

    <linearGradient id="emeraldVol" x1="{em_x1}" y1="{em_y1}" x2="{em_x2}" y2="{em_y2}" gradientUnits="userSpaceOnUse">
      <stop offset="0%" stop-color="#022C22"/>
      <stop offset="22%" stop-color="#064E3B"/>
      <stop offset="56%" stop-color="#047857"/>
      <stop offset="86%" stop-color="#059669"/>
      <stop offset="100%" stop-color="#10B981"/>
    </linearGradient>
    <linearGradient id="emeraldTopLight" x1="{et_x1}" y1="{et_y1}" x2="{et_x2}" y2="{et_y2}" gradientUnits="userSpaceOnUse">
      <stop offset="0%" stop-color="#059669" stop-opacity="0"/>
      <stop offset="55%" stop-color="#34D399" stop-opacity="0.34"/>
      <stop offset="100%" stop-color="#A7F3D0" stop-opacity="0.58"/>
    </linearGradient>

    <linearGradient id="dollarGrad" x1="{dl_x1}" y1="{dl_y1}" x2="{dl_x2}" y2="{dl_y2}" gradientUnits="userSpaceOnUse">
{dollar_stops}
    </linearGradient>

    <clipPath id="clipAmber2">
      <path d="{paths['amber_d']}"/>
    </clipPath>
    <clipPath id="clipEmerald2">
      <path d="{paths['emerald_d']}"/>
    </clipPath>

    <filter id="vShadow2" x="-40%" y="-40%" width="180%" height="180%">
      <feGaussianBlur in="SourceAlpha" stdDeviation="{12*k:.2f}" result="blurAmb"/>
      <feOffset in="blurAmb" dx="0" dy="{14*k:.2f}" result="offAmb"/>
      <feFlood flood-color="#000000" flood-opacity="{v_amb_op}" result="colAmb"/>
      <feComposite in="colAmb" in2="offAmb" operator="in" result="shAmb"/>

      <feGaussianBlur in="SourceAlpha" stdDeviation="{5*k:.2f}" result="blurKey"/>
      <feOffset in="blurKey" dx="{2*k:.2f}" dy="{7*k:.2f}" result="offKey"/>
      <feFlood flood-color="{v_sh_col}" flood-opacity="{v_key_op}" result="colKey"/>
      <feComposite in="colKey" in2="offKey" operator="in" result="shKey"/>

      <feMerge>
        <feMergeNode in="shAmb"/>
        <feMergeNode in="shKey"/>
        <feMergeNode in="SourceGraphic"/>
      </feMerge>
    </filter>

    <filter id="softBlur6" x="-60%" y="-60%" width="220%" height="220%">
      <feGaussianBlur stdDeviation="{5.5*k:.2f}"/>
    </filter>
    <filter id="softBlur4" x="-60%" y="-60%" width="220%" height="220%">
      <feGaussianBlur stdDeviation="{3.5*k:.2f}"/>
    </filter>

    <filter id="dollarShadow2" x="-120%" y="-80%" width="340%" height="260%">
      <feGaussianBlur in="SourceAlpha" stdDeviation="{9*k:.2f}" result="b1"/>
      <feOffset in="b1" dx="{3*k:.2f}" dy="{10*k:.2f}" result="o1"/>
      <feFlood flood-color="{v_sh_col}" flood-opacity="{d_amb_op}" result="c1"/>
      <feComposite in="c1" in2="o1" operator="in" result="s1"/>

      <feGaussianBlur in="SourceAlpha" stdDeviation="{3*k:.2f}" result="b2"/>
      <feOffset in="b2" dx="{1*k:.2f}" dy="{4*k:.2f}" result="o2"/>
      <feFlood flood-color="#000000" flood-opacity="{d_key_op}" result="c2"/>
      <feComposite in="c2" in2="o2" operator="in" result="s2"/>

      <feMerge>
        <feMergeNode in="s1"/>
        <feMergeNode in="s2"/>
        <feMergeNode in="SourceGraphic"/>
      </feMerge>
    </filter>
  </defs>

{bg_nodes}  <g filter="url(#vShadow2)">
    <g clip-path="url(#clipAmber2)">
      <path d="{paths['amber_d']}" fill="url(#amberVol)"/>
      <path d="{paths['amber_lit_edge_d']}" fill="none" stroke="#FCD34D" stroke-width="{9*k:.2f}" stroke-linejoin="round" opacity="0.26" filter="url(#softBlur4)"/>
      <path d="{paths['amber_lit_edge_d']}" fill="none" stroke="url(#amberRim)" stroke-width="{2.2*k:.2f}" stroke-linejoin="round"/>
      <path d="{paths['amber_inner_edge_d']}" fill="none" stroke="#451A03" stroke-width="{10*k:.2f}" stroke-linejoin="round" opacity="0.34" filter="url(#softBlur4)"/>
      <path d="{paths['fold_shadow_band_d']}" fill="url(#foldGrad2)"/>
      <path d="{paths['fold_seam_curve_d']}" fill="none" stroke="#260C00" stroke-width="{14*k:.2f}" stroke-linecap="round" opacity="0.62" filter="url(#softBlur6)"/>
    </g>

    <g clip-path="url(#clipEmerald2)">
      <path d="{paths['emerald_d']}" fill="url(#emeraldVol)"/>
      <path d="{paths['emerald_dark_edge_d']}" fill="none" stroke="#011A13" stroke-width="{13*k:.2f}" stroke-linejoin="round" opacity="0.68" filter="url(#softBlur6)"/>
      <path d="{paths['emerald_lit_ridge_d']}" fill="none" stroke="#34D399" stroke-width="{10*k:.2f}" stroke-linejoin="round" opacity="0.24" filter="url(#softBlur4)"/>
      <path d="{paths['emerald_lit_ridge_d']}" fill="none" stroke="url(#emeraldTopLight)" stroke-width="{2.4*k:.2f}" stroke-linejoin="round"/>
    </g>
  </g>

  <g filter="url(#dollarShadow2)">
    <path d="{paths['dollar_bar_d']}" fill="url(#dollarGrad)"/>
    <path d="{paths['dollar_s_d']}" fill="none" stroke="url(#dollarGrad)" stroke-width="{sw_dollar:.2f}" stroke-linecap="butt"/>
  </g>
</svg>
"""


def write_file(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as fp:
        fp.write(content)


def render_svg_png(svg_path, png_path, size):
    subprocess.run(["rsvg-convert", "-w", str(size), "-h", str(size), svg_path, "-o", png_path], check=True)


# ---------------------------------------------------------------------------
# 2. Generate SVG & PNG Masters in brand/
# ---------------------------------------------------------------------------
svg_master_108 = make_3d_svg(
    viewport_size=108.0,
    width=1024,
    height=1024,
    paths=p108,
    scale_to_512=76.0 / 512.0,
    include_bg=True,
    coord_offset=16.0,
)
svg_square = make_3d_svg(
    viewport_size=512.0,
    width=512,
    height=512,
    paths=p512,
    scale_to_512=1.0,
    include_bg=True,
)
svg_emblem = make_3d_svg(
    viewport_size=512.0,
    width=512,
    height=512,
    paths=p512,
    scale_to_512=1.0,
    include_bg=False,
    lightbg=False,
)
svg_emblem_light = make_3d_svg(
    viewport_size=512.0,
    width=512,
    height=512,
    paths=p512,
    scale_to_512=1.0,
    include_bg=False,
    lightbg=True,
)
svg_mono = make_3d_svg(
    viewport_size=108.0,
    width=512,
    height=512,
    paths=p108,
    scale_to_512=76.0 / 512.0,
    include_bg=False,
    monochrome=True,
)
svg_mono_sq = make_3d_svg(
    viewport_size=512.0,
    width=512,
    height=512,
    paths=p512,
    scale_to_512=1.0,
    include_bg=False,
    monochrome=True,
)

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
# 3. Generate Android VectorDrawables & Adaptive Icon XMLs (Clipped 3D Sculpt)
# ---------------------------------------------------------------------------
def build_vector_drawable_3d(dp_size, paths, ox=0.0):
    def c(v):
        return f"{v + ox:.2f}"

    return f"""<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="{dp_size}dp"
    android:height="{dp_size}dp"
    android:viewportWidth="{dp_size}"
    android:viewportHeight="{dp_size}">

    <!-- Multi-Layer Ambient + Directional Drop Shadow Behind V -->
    <path
        android:pathData="{paths['v_sh_far_am']}"
        android:fillColor="#020617"
        android:fillAlpha="0.22"
        android:strokeColor="#020617"
        android:strokeAlpha="0.14"
        android:strokeWidth="2.4"
        android:strokeLineJoin="round" />
    <path
        android:pathData="{paths['v_sh_far_em']}"
        android:fillColor="#020617"
        android:fillAlpha="0.22"
        android:strokeColor="#020617"
        android:strokeAlpha="0.14"
        android:strokeWidth="2.4"
        android:strokeLineJoin="round" />
    <path
        android:pathData="{paths['v_sh_mid_am']}"
        android:fillColor="#020617"
        android:fillAlpha="0.30"
        android:strokeColor="#020617"
        android:strokeAlpha="0.20"
        android:strokeWidth="1.2"
        android:strokeLineJoin="round" />
    <path
        android:pathData="{paths['v_sh_mid_em']}"
        android:fillColor="#020617"
        android:fillAlpha="0.30"
        android:strokeColor="#020617"
        android:strokeAlpha="0.20"
        android:strokeWidth="1.2"
        android:strokeLineJoin="round" />
    <path
        android:pathData="{paths['v_sh_near_am']}"
        android:fillColor="#000000"
        android:fillAlpha="0.36" />
    <path
        android:pathData="{paths['v_sh_near_em']}"
        android:fillColor="#000000"
        android:fillAlpha="0.36" />

    <!-- Clipped Left Amber Arm (Volumetric Gradient + Soft Specular Rim + Fold Occlusion) -->
    <group>
        <clip-path android:pathData="{paths['amber_d']}" />
        <path android:pathData="{paths['amber_d']}">
            <aapt:attr name="android:fillColor">
                <gradient
                    android:startX="{c(33.0)}"
                    android:startY="{c(33.0)}"
                    android:endX="{c(54.8)}"
                    android:endY="{c(74.6)}"
                    android:type="linear">
                    <item android:offset="0" android:color="#EA8C00" />
                    <item android:offset="0.4" android:color="#C96400" />
                    <item android:offset="0.78" android:color="#9A4100" />
                    <item android:offset="1" android:color="#6B2B00" />
                </gradient>
            </aapt:attr>
        </path>
        <path
            android:pathData="{paths['amber_lit_edge_d']}"
            android:strokeColor="#FCD34D"
            android:strokeAlpha="0.22"
            android:strokeWidth="1.35"
            android:strokeLineJoin="round" />
        <path
            android:pathData="{paths['amber_lit_edge_d']}"
            android:strokeColor="#FDE68A"
            android:strokeAlpha="0.48"
            android:strokeWidth="0.36"
            android:strokeLineJoin="round" />
        <path
            android:pathData="{paths['amber_inner_edge_d']}"
            android:strokeColor="#451A03"
            android:strokeAlpha="0.30"
            android:strokeWidth="1.50"
            android:strokeLineJoin="round" />
        <path android:pathData="{paths['fold_shadow_band_d']}">
            <aapt:attr name="android:fillColor">
                <gradient
                    android:startX="{c(47.9)}"
                    android:startY="{c(68.0)}"
                    android:endX="{c(53.8)}"
                    android:endY="{c(64.2)}"
                    android:type="linear">
                    <item android:offset="0" android:color="#003B1400" />
                    <item android:offset="1" android:color="#D1290D00" />
                </gradient>
            </aapt:attr>
        </path>
        <path
            android:pathData="{paths['fold_seam_curve_d']}"
            android:strokeColor="#260C00"
            android:strokeAlpha="0.32"
            android:strokeWidth="2.10"
            android:strokeLineCap="round" />
        <path
            android:pathData="{paths['fold_seam_curve_d']}"
            android:strokeColor="#260C00"
            android:strokeAlpha="0.48"
            android:strokeWidth="1.05"
            android:strokeLineCap="round" />
    </group>

    <!-- Clipped Right Continuous Emerald Arm (Deep Volumetric + Dark Bevel + Lit Ridge) -->
    <group>
        <clip-path android:pathData="{paths['emerald_d']}" />
        <path android:pathData="{paths['emerald_d']}">
            <aapt:attr name="android:fillColor">
                <gradient
                    android:startX="{c(52.4)}"
                    android:startY="{c(76.1)}"
                    android:endX="{c(76.1)}"
                    android:endY="{c(33.0)}"
                    android:type="linear">
                    <item android:offset="0" android:color="#022C22" />
                    <item android:offset="0.22" android:color="#064E3B" />
                    <item android:offset="0.56" android:color="#047857" />
                    <item android:offset="0.86" android:color="#059669" />
                    <item android:offset="1" android:color="#10B981" />
                </gradient>
            </aapt:attr>
        </path>
        <path
            android:pathData="{paths['emerald_dark_edge_d']}"
            android:strokeColor="#011A13"
            android:strokeAlpha="0.48"
            android:strokeWidth="2.00"
            android:strokeLineJoin="round" />
        <path
            android:pathData="{paths['emerald_dark_edge_d']}"
            android:strokeColor="#011A13"
            android:strokeAlpha="0.45"
            android:strokeWidth="0.95"
            android:strokeLineJoin="round" />
        <path
            android:pathData="{paths['emerald_lit_ridge_d']}"
            android:strokeColor="#34D399"
            android:strokeAlpha="0.22"
            android:strokeWidth="1.50"
            android:strokeLineJoin="round" />
        <path
            android:pathData="{paths['emerald_lit_ridge_d']}"
            android:strokeColor="#A7F3D0"
            android:strokeAlpha="0.50"
            android:strokeWidth="0.38"
            android:strokeLineJoin="round" />
    </group>

    <!-- Multi-Layer Soft Drop Shadow Behind Floating Dollar Sign -->
    <path
        android:pathData="{paths['dollar_bar_sh_far_d']}"
        android:fillColor="#020617"
        android:fillAlpha="0.28"
        android:strokeColor="#020617"
        android:strokeAlpha="0.18"
        android:strokeWidth="1.30"
        android:strokeLineJoin="round" />
    <path
        android:pathData="{paths['dollar_s_sh_far_d']}"
        android:strokeColor="#020617"
        android:strokeAlpha="0.28"
        android:strokeWidth="3.90"
        android:strokeLineCap="butt" />
    <path
        android:pathData="{paths['dollar_bar_sh_near_d']}"
        android:fillColor="#000000"
        android:fillAlpha="0.44" />
    <path
        android:pathData="{paths['dollar_s_sh_near_d']}"
        android:strokeColor="#000000"
        android:strokeAlpha="0.44"
        android:strokeWidth="2.95"
        android:strokeLineCap="butt" />

    <!-- Crisp Floating Geometric Dollar Sign with Soft Vertical Metallic Sheen -->
    <path android:pathData="{paths['dollar_bar_d']}">
        <aapt:attr name="android:fillColor">
            <gradient
                android:startX="{c(54.0)}"
                android:startY="{c(33.1)}"
                android:endX="{c(54.0)}"
                android:endY="{c(50.9)}"
                android:type="linear">
                <item android:offset="0" android:color="#FFFFFF" />
                <item android:offset="0.72" android:color="#F1F5F9" />
                <item android:offset="1" android:color="#CBD5E1" />
            </gradient>
        </aapt:attr>
    </path>
    <path
        android:pathData="{paths['dollar_s_d']}"
        android:strokeWidth="2.80"
        android:strokeLineCap="butt">
        <aapt:attr name="android:strokeColor">
            <gradient
                android:startX="{c(54.0)}"
                android:startY="{c(33.1)}"
                android:endX="{c(54.0)}"
                android:endY="{c(50.9)}"
                android:type="linear">
                <item android:offset="0" android:color="#FFFFFF" />
                <item android:offset="0.72" android:color="#F1F5F9" />
                <item android:offset="1" android:color="#CBD5E1" />
            </gradient>
        </aapt:attr>
    </path>
</vector>
"""


ic_bg_xml = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:pathData="M0,0h108v108h-108z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:startX="0"
                android:startY="0"
                android:endX="108"
                android:endY="108"
                android:type="linear">
                <item android:offset="0" android:color="#060A12" />
                <item android:offset="0.6" android:color="#0A111F" />
                <item android:offset="1" android:color="#111B2D" />
            </gradient>
        </aapt:attr>
    </path>
    <path android:pathData="M54,6 A46.4,46.4 0 1,1 53.99,6 Z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:centerX="54"
                android:centerY="52.4"
                android:gradientRadius="46.4"
                android:type="radial">
                <item android:offset="0" android:color="#38065F46" />
                <item android:offset="0.6" android:color="#0F0F172A" />
                <item android:offset="1" android:color="#00060A12" />
            </gradient>
        </aapt:attr>
    </path>
</vector>
"""

ic_fg_xml = build_vector_drawable_3d(108, p108, ox=0.0)
ic_brand_mark_xml = build_vector_drawable_3d(76, p76, ox=-16.0)

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

adaptive_xml = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
"""

write_file(os.path.join(RES_DIR, "drawable", "ic_launcher_background.xml"), ic_bg_xml)
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
        r = int(6 + t * 11)
        g = int(10 + t * 17)
        b = int(18 + t * 27)
        d_em = math.hypot(x - 235, y - 250)
        if d_em < 270:
            glow = ((1.0 - d_em / 270.0) ** 2) * 0.18
            r = min(255, int(r + 6 * glow))
            g = min(255, int(g + 125 * glow))
            b = min(255, int(b + 88 * glow))
        pix[x, y] = (r, g, b, 255)

TILE = 280
TX, TY = 92, 110
tile_mask = Image.new("L", (TILE, TILE), 0)
ImageDraw.Draw(tile_mask).rounded_rectangle([0, 0, TILE - 1, TILE - 1], radius=62, fill=255)

# Soft elevation drop shadow behind icon tile
tile_shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
shadow_shape = Image.new("RGBA", (TILE, TILE), (0, 0, 0, 170))
tile_shadow.paste(shadow_shape, (TX, TY + 12), tile_mask)
tile_shadow = tile_shadow.filter(ImageFilter.GaussianBlur(18))
fg = Image.alpha_composite(fg, tile_shadow)

overlay = Image.new("RGBA", (W, H), (0, 0, 0, 0))
draw = ImageDraw.Draw(overlay)

tile_img = src_square.resize((TILE, TILE), Image.Resampling.LANCZOS)
fg.paste(tile_img, (TX, TY), tile_mask)
draw.rounded_rectangle([TX, TY, TX + TILE, TY + TILE], radius=62, outline=(16, 185, 129, 85), width=2)

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
    ("USDT P2P", (234, 140, 0), (234, 140, 0, 28), (251, 191, 36, 255)),
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

print("All Proposal 2 (3D Esculpido) Tasa VE brand assets generated successfully!")
