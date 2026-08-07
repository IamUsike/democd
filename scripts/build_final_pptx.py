#!/usr/bin/env python3
"""Build 5-slide professional PPT matching runtime_rebels template style."""

from pptx import Presentation
from pptx.util import Inches, Pt, Emu
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.enum.shapes import MSO_SHAPE
from pptx.oxml.ns import nsmap
from pptx.oxml import parse_xml
from lxml import etree
import copy

# Brand colors from template
RED_DARK = RGBColor(0x73, 0x00, 0x14)  # #730014
RED_ACCENT = RGBColor(0xC4, 0x1E, 0x3A)  # #C41E3A
CHARCOAL = RGBColor(0x2B, 0x2B, 0x2B)
GRAY = RGBColor(0x5A, 0x5A, 0x5A)
GRAY_LIGHT = RGBColor(0xF2, 0xF2, 0xF2)
GRAY_CARD = RGBColor(0xEB, 0xEB, 0xEB)
WHITE = RGBColor(0xFF, 0xFF, 0xFF)
GREEN_OK = RGBColor(0x1B, 0x7A, 0x3D)
AMBER = RGBColor(0xB8, 0x5C, 0x00)

SLIDE_W = Inches(13.333)
SLIDE_H = Inches(7.5)

ASSETS = "/tmp/rr_assets/crops"
OUT = "/home/wofo/Downloads/Projects/democd/FINAL-PRESENTATION.pptx"


def set_run(run, size, bold=False, color=CHARCOAL, font="Arial"):
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.color.rgb = color
    run.font.name = font


def add_textbox(slide, left, top, width, height, text, size=18, bold=False,
                color=CHARCOAL, align=PP_ALIGN.LEFT, font="Arial"):
    box = slide.shapes.add_textbox(left, top, width, height)
    tf = box.text_frame
    tf.word_wrap = True
    p = tf.paragraphs[0]
    p.alignment = align
    run = p.add_run()
    run.text = text
    set_run(run, size, bold, color, font)
    return box


def add_para(tf, text, size=14, bold=False, color=CHARCOAL, space_before=6,
             space_after=2, align=PP_ALIGN.LEFT):
    p = tf.add_paragraph()
    p.alignment = align
    p.space_before = Pt(space_before)
    p.space_after = Pt(space_after)
    run = p.add_run()
    run.text = text
    set_run(run, size, bold, color)
    return p


def rect(slide, left, top, width, height, fill):
    shape = slide.shapes.add_shape(MSO_SHAPE.RECTANGLE, left, top, width, height)
    shape.fill.solid()
    shape.fill.fore_color.rgb = fill
    shape.line.fill.background()
    return shape


def rounded_rect(slide, left, top, width, height, fill):
    shape = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, left, top, width, height)
    shape.fill.solid()
    shape.fill.fore_color.rgb = fill
    shape.line.fill.background()
    return shape


def chevron(slide, left, top, width, height, fill):
    shape = slide.shapes.add_shape(MSO_SHAPE.CHEVRON, left, top, width, height)
    shape.fill.solid()
    shape.fill.fore_color.rgb = fill
    shape.line.fill.background()
    return shape


def slide_number(slide, n, total=5):
    add_textbox(
        slide, Inches(12.4), Inches(7.1), Inches(0.7), Inches(0.3),
        f"{n}/{total}", size=10, color=GRAY, align=PP_ALIGN.RIGHT,
    )


def build():
    prs = Presentation()
    prs.slide_width = SLIDE_W
    prs.slide_height = SLIDE_H
    blank = prs.slide_layouts[6]

    # ─── SLIDE 1: Title ───────────────────────────────────────────────
    s1 = prs.slides.add_slide(blank)
    rect(s1, 0, 0, Inches(4.6), SLIDE_H, RED_DARK)
    rect(s1, Inches(4.6), 0, Inches(8.733), SLIDE_H, WHITE)

    # Diamond accents on red/white boundary
    for i, y in enumerate([0.8, 1.5, 2.2, 2.9, 3.6, 4.3, 5.0, 5.7]):
        d = s1.shapes.add_shape(
            MSO_SHAPE.DIAMOND, Inches(4.35), Inches(y), Inches(0.28), Inches(0.28)
        )
        d.fill.solid()
        d.fill.fore_color.rgb = RED_ACCENT if i % 2 == 0 else RGBColor(0x90, 0x20, 0x30)
        d.line.fill.background()

    add_textbox(
        s1, Inches(0.35), Inches(0.55), Inches(4.0), Inches(2.2),
        "TRANSACTION\nMONITORING\n& ALERTS",
        size=32, bold=True, color=WHITE,
    )
    add_textbox(
        s1, Inches(0.35), Inches(4.55), Inches(4.0), Inches(0.4),
        "Presented by Team:", size=12, color=WHITE,
    )
    add_textbox(
        s1, Inches(0.35), Inches(4.9), Inches(4.0), Inches(0.4),
        "RunTime_Rebels", size=20, bold=True, color=WHITE,
    )
    add_textbox(
        s1, Inches(0.35), Inches(5.45), Inches(4.0), Inches(0.8),
        "Team Members:\nshreya  ·  sathwik  ·  Rameez",
        size=13, color=WHITE,
    )

    try:
        s1.shapes.add_picture(
            f"{ASSETS}/hsbc.png", Inches(0.3), Inches(6.7), height=Inches(0.45)
        )
    except Exception:
        add_textbox(s1, Inches(0.35), Inches(6.75), Inches(2), Inches(0.35),
                    "HSBC", size=14, bold=True, color=WHITE)

    try:
        s1.shapes.add_picture(
            f"{ASSETS}/how_we_lead.png",
            Inches(5.3), Inches(1.8), height=Inches(3.2),
        )
    except Exception:
        pass

    try:
        # Diamond-ish photo on right
        pic = s1.shapes.add_picture(
            f"{ASSETS}/plaza.png",
            Inches(9.0), Inches(0.9), width=Inches(3.8), height=Inches(5.5),
        )
    except Exception:
        pass

    add_textbox(
        s1, Inches(5.2), Inches(6.5), Inches(4.5), Inches(0.5),
        "Detect  →  Alert  →  Investigate  →  Close",
        size=14, bold=True, color=RED_DARK,
    )
    slide_number(s1, 1)

    # ─── SLIDE 2: Problem & Solution ──────────────────────────────────
    s2 = prs.slides.add_slide(blank)
    rect(s2, 0, 0, SLIDE_W, SLIDE_H, WHITE)
    add_textbox(
        s2, Inches(0.5), Inches(0.3), Inches(10), Inches(0.6),
        "The Problem & The Solution", size=32, bold=True, color=CHARCOAL,
    )

    # Left: Problem
    add_textbox(
        s2, Inches(0.5), Inches(1.15), Inches(5.5), Inches(0.4),
        "The Problem", size=20, bold=True, color=RED_ACCENT,
    )
    problems = [
        ("High-volume payment traffic",
         "Banks and merchants push transactions continuously. Risk must be spotted as money moves — not hours later."),
        ("Manual triage does not scale",
         "Without automated rules, operators miss velocity spikes, new payees, and daily-limit breaches."),
        ("Need a full alert lifecycle",
         "Detection alone is not enough — OPEN → ACKNOWLEDGED → INVESTIGATING → CLOSED / DISMISSED with a clear trail."),
    ]
    y = 1.65
    for title, body in problems:
        add_textbox(s2, Inches(0.5), Inches(y), Inches(5.8), Inches(0.35),
                    title, size=15, bold=True, color=CHARCOAL)
        add_textbox(s2, Inches(0.5), Inches(y + 0.32), Inches(5.8), Inches(0.7),
                    body, size=12, color=GRAY)
        y += 1.15

    # Right: Solution card column
    add_textbox(
        s2, Inches(7.0), Inches(1.15), Inches(5.5), Inches(0.4),
        "The Solution", size=20, bold=True, color=RED_ACCENT,
    )
    add_textbox(
        s2, Inches(7.0), Inches(1.6), Inches(5.6), Inches(0.85),
        "One operator dashboard — ingest simulated bank & merchant feeds, evaluate four rules synchronously, and triage alerts end-to-end.",
        size=13, bold=True, color=CHARCOAL,
    )

    cards = [
        ("Four detection rules", "Amount · Velocity · New Payee · Daily Limit — configurable in UI"),
        ("Soft multi-source tenancy", "sourceType / sourceId / sourceName on every row — one DB, many feeds"),
        ("Proven under load", "k6 Pass 1–3 documented — indexes verified, bottleneck identified"),
    ]
    cy = 2.6
    for title, body in cards:
        card = rounded_rect(s2, Inches(7.0), Inches(cy), Inches(5.6), Inches(1.15), GRAY_CARD)
        add_textbox(s2, Inches(7.2), Inches(cy + 0.18), Inches(5.2), Inches(0.35),
                    title, size=14, bold=True, color=CHARCOAL)
        add_textbox(s2, Inches(7.2), Inches(cy + 0.55), Inches(5.2), Inches(0.5),
                    body, size=12, color=GRAY)
        cy += 1.3

    slide_number(s2, 2)

    # ─── SLIDE 3: Technical Design ────────────────────────────────────
    s3 = prs.slides.add_slide(blank)
    rect(s3, 0, 0, SLIDE_W, SLIDE_H, WHITE)
    add_textbox(
        s3, Inches(0.5), Inches(0.25), Inches(10), Inches(0.5),
        "TECHNICAL DESIGN", size=30, bold=True, color=CHARCOAL,
    )
    add_textbox(
        s3, Inches(0.5), Inches(0.75), Inches(12), Inches(0.35),
        "Modular monolith on branch  dev  — clean packages; rule engine extractable later",
        size=13, color=GRAY,
    )

    # Chevrons
    layers = [
        (0.4, "FRONTEND", "React · Vite\nDark Obsidian UI\n:8082 nginx"),
        (4.5, "BACKEND", "Spring Boot\nTxn · Rule · Alert\n:8081 REST"),
        (8.6, "DATABASE", "MySQL\nFlyway migrations\nSoft tenancy"),
    ]
    for left, title, body in layers:
        ch = chevron(s3, Inches(left), Inches(1.3), Inches(3.9), Inches(1.85), RED_DARK)
        tf = ch.text_frame
        tf.clear()
        tf.word_wrap = True
        p = tf.paragraphs[0]
        p.alignment = PP_ALIGN.CENTER
        r = p.add_run()
        r.text = title
        set_run(r, 16, True, WHITE)
        for line in body.split("\n"):
            add_para(tf, line, size=11, color=WHITE, space_before=2, align=PP_ALIGN.CENTER)

    # Flow labels
    add_textbox(s3, Inches(3.7), Inches(3.2), Inches(1.5), Inches(0.3),
                "REST APIs →", size=11, bold=True, color=RED_ACCENT, align=PP_ALIGN.CENTER)
    add_textbox(s3, Inches(7.85), Inches(3.2), Inches(1.5), Inches(0.3),
                "Persistence →", size=11, bold=True, color=RED_ACCENT, align=PP_ALIGN.CENTER)

    # Sync path box
    rounded_rect(s3, Inches(0.4), Inches(3.65), Inches(8.2), Inches(1.55), GRAY_LIGHT)
    add_textbox(s3, Inches(0.6), Inches(3.75), Inches(7.8), Inches(0.3),
                "Request path (MVP MTTD) — evaluate in the same request",
                size=13, bold=True, color=RED_DARK)
    add_textbox(
        s3, Inches(0.6), Inches(4.15), Inches(7.8), Inches(0.9),
        "POST /transactions  →  persist txn  →  RuleEngine.evaluate()  →  "
        "if match: create OPEN alert(s)  →  201 Created\n"
        "Packages: api · transaction · rule · alert · common",
        size=12, color=CHARCOAL,
    )

    # Side stack
    rounded_rect(s3, Inches(8.9), Inches(3.65), Inches(3.9), Inches(1.55), GRAY_CARD)
    add_textbox(s3, Inches(9.1), Inches(3.75), Inches(3.5), Inches(0.3),
                "DevOps & QA", size=13, bold=True, color=RED_DARK)
    add_textbox(
        s3, Inches(9.1), Inches(4.15), Inches(3.5), Inches(0.9),
        "Docker Compose · Jenkins\nSwagger / OpenAPI · k6\nActuator  rule.evaluate",
        size=12, color=CHARCOAL,
    )

    # Data decisions
    add_textbox(s3, Inches(0.4), Inches(5.4), Inches(12), Inches(0.3),
                "Data model decisions", size=14, bold=True, color=CHARCOAL)
    decisions = [
        ("Soft tenancy", "source_* on every row"),
        ("No Account/Payee tables", "IDs + names on txn"),
        ("alert_transactions", "junction for links"),
        ("KPIs = aggregations", "not stored columns"),
    ]
    dx = 0.4
    for title, body in decisions:
        rounded_rect(s3, Inches(dx), Inches(5.8), Inches(3.0), Inches(1.15), GRAY_LIGHT)
        add_textbox(s3, Inches(dx + 0.15), Inches(5.95), Inches(2.7), Inches(0.35),
                    title, size=12, bold=True, color=RED_DARK)
        add_textbox(s3, Inches(dx + 0.15), Inches(6.35), Inches(2.7), Inches(0.45),
                    body, size=11, color=GRAY)
        dx += 3.2

    slide_number(s3, 3)

    # ─── SLIDE 4: Performance & Challenge ─────────────────────────────
    s4 = prs.slides.add_slide(blank)
    rect(s4, 0, 0, SLIDE_W, SLIDE_H, WHITE)
    add_textbox(
        s4, Inches(0.45), Inches(0.22), Inches(12), Inches(0.45),
        "Performance Evidence & Ops Challenge", size=28, bold=True, color=CHARCOAL,
    )
    add_textbox(
        s4, Inches(0.45), Inches(0.7), Inches(12), Inches(0.3),
        "k6 (Windows) → Linux API  :8081   ·   JVM + MySQL co-located (~2 vCPU / 3.7 GiB)",
        size=12, color=GRAY,
    )

    # Metric cards
    metrics = [
        ("Pass 1 — Write ramp", "234 RPS", "p95 763 ms", "0% fail", GREEN_OK),
        ("Pass 2 — Mixed 80/20", "242 RPS", "p95 623 ms", "0% fail", GREEN_OK),
        ("Pass 3 — Soak 10m", "214 RPS", "p95 1.13 s", "0.09% fail", AMBER),
    ]
    mx = 0.45
    for title, rps, p95, fail, accent in metrics:
        rounded_rect(s4, Inches(mx), Inches(1.15), Inches(4.0), Inches(1.7), GRAY_LIGHT)
        bar = rect(s4, Inches(mx), Inches(1.15), Inches(0.12), Inches(1.7), accent)
        add_textbox(s4, Inches(mx + 0.3), Inches(1.25), Inches(3.5), Inches(0.3),
                    title, size=12, bold=True, color=GRAY)
        add_textbox(s4, Inches(mx + 0.3), Inches(1.55), Inches(3.5), Inches(0.4),
                    rps, size=26, bold=True, color=CHARCOAL)
        add_textbox(s4, Inches(mx + 0.3), Inches(2.05), Inches(3.5), Inches(0.55),
                    f"{p95}\n{fail}", size=13, color=CHARCOAL)
        mx += 4.2

    # Bar chart: p95
    add_textbox(s4, Inches(0.45), Inches(3.05), Inches(6), Inches(0.3),
                "p95 latency by pass (ms)", size=13, bold=True, color=CHARCOAL)
    chart_base_y = 5.35
    chart_max_h = 1.9
    bars = [("P1", 763, 1200), ("P2", 623, 1200), ("P3", 1130, 1200)]
    bx = 0.7
    for label, val, vmax in bars:
        h = chart_max_h * (val / vmax)
        top = chart_base_y - h
        color = AMBER if val > 1000 else RED_DARK
        rect(s4, Inches(bx), Inches(top), Inches(1.1), Inches(h), color)
        add_textbox(s4, Inches(bx - 0.15), Inches(top - 0.28), Inches(1.4), Inches(0.28),
                    str(val), size=11, bold=True, color=CHARCOAL, align=PP_ALIGN.CENTER)
        add_textbox(s4, Inches(bx - 0.15), Inches(chart_base_y + 0.05), Inches(1.4), Inches(0.28),
                    label, size=11, bold=True, color=GRAY, align=PP_ALIGN.CENTER)
        bx += 1.7

    # rule.evaluate trend
    add_textbox(s4, Inches(5.5), Inches(3.05), Inches(3.5), Inches(0.3),
                "rule.evaluate mean (ms)", size=13, bold=True, color=CHARCOAL)
    evals = [("P1–P2", 55), ("Mid soak", 91), ("End soak", 126)]
    ex = 5.7
    for label, val in evals:
        h = 1.7 * (val / 140)
        top = chart_base_y - h
        rect(s4, Inches(ex), Inches(top), Inches(0.85), Inches(h), RED_ACCENT)
        add_textbox(s4, Inches(ex - 0.2), Inches(top - 0.28), Inches(1.25), Inches(0.28),
                    str(val), size=11, bold=True, color=CHARCOAL, align=PP_ALIGN.CENTER)
        add_textbox(s4, Inches(ex - 0.25), Inches(chart_base_y + 0.05), Inches(1.35), Inches(0.35),
                    label, size=9, color=GRAY, align=PP_ALIGN.CENTER)
        ex += 1.2

    # Firewall challenge card
    rounded_rect(s4, Inches(9.0), Inches(3.05), Inches(3.9), Inches(3.9), GRAY_LIGHT)
    add_textbox(s4, Inches(9.2), Inches(3.2), Inches(3.5), Inches(0.35),
                "Phase 3 blocker", size=14, bold=True, color=RED_DARK)
    add_textbox(
        s4, Inches(9.2), Inches(3.6), Inches(3.5), Inches(1.5),
        "Planned: MySQL on a separate VM after soak showed co-location contention.\n\n"
        "Blocked: Linux → Linux TCP failed — even plain nc. Windows → Linux worked (curl + nc).",
        size=11, color=CHARCOAL,
    )
    add_textbox(
        s4, Inches(9.2), Inches(5.2), Inches(3.5), Inches(1.4),
        "Inference: security group / firewall / routing — not an app bug.\n\n"
        "Indexes OK (EXPLAIN). Bottleneck = shared box. Next: unblock network → split DB → async queue → re-soak.",
        size=11, color=GRAY,
    )

    slide_number(s4, 4)

    # ─── SLIDE 5: Thank You + Agile ───────────────────────────────────
    s5 = prs.slides.add_slide(blank)
    rect(s5, 0, 0, Inches(8.5), SLIDE_H, GRAY_LIGHT)
    rect(s5, Inches(8.5), 0, Inches(4.833), SLIDE_H, WHITE)

    add_textbox(
        s5, Inches(0.6), Inches(0.8), Inches(7.5), Inches(0.7),
        "THANK YOU", size=48, bold=True, color=CHARCOAL,
    )
    add_textbox(
        s5, Inches(0.6), Inches(1.6), Inches(7.5), Inches(0.4),
        "Questions welcome — architecture, k6 numbers, or the firewall finding.",
        size=14, color=GRAY,
    )

    add_textbox(
        s5, Inches(0.6), Inches(2.3), Inches(7.5), Inches(0.35),
        "How we worked (Agile)", size=16, bold=True, color=RED_DARK,
    )
    agile = [
        "MVP-first build order — Phase 2 rules only after E2E worked",
        "Role split (A/B/C) with shared API contracts before UI",
        "Kanban + honest milestones · stand-ups · retros",
        "TDD on rules & alert transitions · evidence over vibes (k6 + EXPLAIN)",
    ]
    ay = 2.75
    for item in agile:
        add_textbox(s5, Inches(0.6), Inches(ay), Inches(7.5), Inches(0.4),
                    f"•  {item}", size=12, color=CHARCOAL)
        ay += 0.45

    add_textbox(
        s5, Inches(0.6), Inches(4.8), Inches(7.5), Inches(0.35),
        "If we had more time", size=14, bold=True, color=RED_DARK,
    )
    add_textbox(
        s5, Inches(0.6), Inches(5.2), Inches(7.5), Inches(0.9),
        "Unblock Linux↔Linux → MySQL on own VM → async queue + rule workers → "
        "re-run Pass 3 soak → finish simulator polish & hardening.",
        size=12, color=CHARCOAL,
    )
    add_textbox(
        s5, Inches(0.6), Inches(6.4), Inches(7.5), Inches(0.4),
        "RunTime_Rebels  ·  shreya  ·  sathwik  ·  Rameez",
        size=13, bold=True, color=RED_DARK,
    )

    try:
        # Right photo with triangular crop feel via full image
        s5.shapes.add_picture(
            f"{ASSETS}/office.png",
            Inches(8.7), Inches(0.4), width=Inches(4.3), height=Inches(6.7),
        )
    except Exception:
        # Chevron accent if no photo
        chevron(s5, Inches(9.5), Inches(1.5), Inches(3.2), Inches(4.5), RED_DARK)

    # Diagonal accent bar
    tri = s5.shapes.add_shape(
        MSO_SHAPE.RIGHT_TRIANGLE, Inches(8.3), 0, Inches(0.45), SLIDE_H
    )
    tri.fill.solid()
    tri.fill.fore_color.rgb = RED_DARK
    tri.line.fill.background()
    # Flip triangle to point right visually - rotate
    tri.rotation = 180

    slide_number(s5, 5)

    prs.save(OUT)
    print(f"Wrote {OUT}")


if __name__ == "__main__":
    build()
