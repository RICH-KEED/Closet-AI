'use strict';
const pptxgen = require('pptxgenjs');
const sharp = require('sharp');
const path = require('path');

// Module-level instance (used for pptx.shapes.* in helpers and slide builders)
let pptx;

// ─── Palette — Professional Light Theme (NO # prefix for PptxGenJS) ──────────
const C = {
  bg:       'FFFFFF',   // pure white slide background
  card:     'FFFFFF',   // white card face
  cardAlt:  'F1F5F9',  // light blue-gray for subtle alternate cards
  border:   '1E293B',   // deep navy border — strong, professional
  borderLt: 'CBD5E1',  // light divider lines
  navy:     '0F172A',   // deepest dark (header bands, primary heading)
  purple:   '7C3AED',  // brand purple accent
  purpleLt: 'EDE9FE',  // light purple fill
  pink:     'BE185D',  // deep rose accent (better contrast on white)
  pinkLt:   'FCE7F3',  // light pink fill
  text:     '0F172A',  // near-black body text
  subtext:  '64748B',  // muted gray subtext
  green:    '065F46',  // deep green
  greenLt:  'D1FAE5',  // light green fill
  red:      'B91C1C',  // deep red
  redLt:    'FEE2E2',  // light red fill
  yellow:   '92400E',  // deep amber
  yellowLt: 'FEF3C7',  // light amber fill
  white:    'FFFFFF',
  gray:     '64748B',   // alias → same as subtext, prevents C.gray undefined errors
  grayDk:   '94A3B8',  // lighter muted (for captions)
};
const F = 'Arial';

// ─── Asset paths ──────────────────────────────────────────────────────────────
const SCREENS_DIR = path.join(__dirname, '../report/figures/screenshots');
const SC = (name) => path.join(SCREENS_DIR, name);

const ASSETS = {
  purpleBlob: path.join(__dirname, 'assets/purple_blob.png'),
  pinkBlob:   path.join(__dirname, 'assets/pink_blob.png'),
};

// Screenshot aspect ratio: 1080×2392 → height/width = 2.2148
const SC_RATIO = 2392 / 1080;

// ─── No blob assets needed for light theme (shapes used directly in slides) ───
async function makeAssets() {
  console.log('Assets: light theme — using vector shapes, no blobs needed');
}

// ─── Shared helpers ───────────────────────────────────────────────────────────

const bgSlide = (sl) => { sl.background = { color: C.bg }; };

// Card with rounded corners — white fill, dark border by default
function addCard(sl, x, y, w, h, { fill = C.card, border = C.border, lw = 1.3, r = 0.08 } = {}) {
  sl.addShape(pptx.shapes.ROUNDED_RECTANGLE, {
    x, y, w, h, rectRadius: r,
    fill: { color: fill },
    line: { color: border, width: lw }
  });
}

// Thin horizontal rule
const hRule = (sl, x, y, w, color = C.borderLt) =>
  sl.addShape(pptx.shapes.RECTANGLE, { x, y, w, h: 0.018, fill: { color }, line: { type: 'none' } });

// Thin vertical rule
const vRule = (sl, x, y, h, color = C.borderLt) =>
  sl.addShape(pptx.shapes.RECTANGLE, { x: x - 0.009, y, w: 0.018, h, fill: { color }, line: { type: 'none' } });

// Section label pill — dark navy fill, white text
function addPill(sl, label, x = 0.4, y = 0.28, w = 1.55) {
  sl.addShape(pptx.shapes.ROUNDED_RECTANGLE, {
    x, y, w, h: 0.27, rectRadius: 0.135,
    fill: { color: C.navy }, line: { type: 'none' }
  });
  sl.addText(label, {
    x, y, w, h: 0.27, fontSize: 8.5, bold: true,
    color: 'FFFFFF', align: 'center', valign: 'middle', fontFace: F
  });
}

// Standard slide heading — dark text on white
function addHeading(sl, text, y = 0.67, size = 24, color = C.navy) {
  sl.addText(text, {
    x: 0.4, y, w: 9.2, h: 0.6, fontSize: size, bold: true,
    color, fontFace: F, wrap: true
  });
}

// Right-pointing arrow
function addArrowH(sl, x, y, w, color = C.border) {
  sl.addShape(pptx.shapes.RECTANGLE, {
    x, y: y - 0.01, w: w - 0.1, h: 0.022,
    fill: { color }, line: { type: 'none' }
  });
  sl.addText('▶', { x: x + w - 0.18, y: y - 0.13, w: 0.2, h: 0.27,
    fontSize: 9, color, fontFace: F, align: 'center' });
}

// Left-pointing arrow
function addArrowLeft(sl, x, y, w, color = C.border) {
  sl.addShape(pptx.shapes.RECTANGLE, {
    x: x + 0.1, y: y - 0.01, w: w - 0.1, h: 0.022,
    fill: { color }, line: { type: 'none' }
  });
  sl.addText('◀', { x, y: y - 0.13, w: 0.2, h: 0.27,
    fontSize: 9, color, fontFace: F, align: 'center' });
}

// Up arrow
function addArrowUp(sl, x, y, h, color = C.border) {
  sl.addShape(pptx.shapes.RECTANGLE, {
    x: x - 0.01, y: y + 0.1, w: 0.022, h: h - 0.1,
    fill: { color }, line: { type: 'none' }
  });
  sl.addText('▲', { x: x - 0.12, y, w: 0.26, h: 0.22,
    fontSize: 9, color, fontFace: F, align: 'center' });
}

// Down arrow
function addArrowV(sl, x, y, h, color = C.border) {
  sl.addShape(pptx.shapes.RECTANGLE, {
    x: x - 0.01, y, w: 0.022, h: h - 0.1,
    fill: { color }, line: { type: 'none' }
  });
  sl.addText('▼', { x: x - 0.12, y: y + h - 0.2, w: 0.26, h: 0.25,
    fontSize: 9, color, fontFace: F, align: 'center' });
}

// Flow box — white fill, colored/dark border, dark text
function flowBox(sl, x, y, w, h, label, sublabel = '', { fill = C.card, border = C.border, lw = 1.5, fSize = 11 } = {}) {
  addCard(sl, x, y, w, h, { fill, border, lw });
  sl.addText(label, {
    x: x + 0.05, y, w: w - 0.1, h: sublabel ? h * 0.52 : h,
    fontSize: fSize, bold: true, color: C.text,
    fontFace: F, align: 'center', valign: sublabel ? 'bottom' : 'middle', wrap: true
  });
  if (sublabel) {
    sl.addText(sublabel, {
      x: x + 0.07, y: y + h * 0.52, w: w - 0.14, h: h * 0.48,
      fontSize: fSize - 2, color: C.subtext,
      fontFace: F, align: 'center', valign: 'top', wrap: true
    });
  }
}

// ─── SLIDE 01 — Title ─────────────────────────────────────────────────────────
function slide01() {
  const sl = pptx.addSlide();
  bgSlide(sl);

  // Deep navy header band (top ~50% of slide)
  sl.addShape(pptx.shapes.RECTANGLE, {
    x: 0, y: 0, w: 10, h: 2.9,
    fill: { color: C.navy }, line: { type: 'none' }
  });

  // Subtle decorative circles inside the header band (semi-transparent)
  sl.addShape(pptx.shapes.OVAL, {
    x: 7.0, y: -1.0, w: 3.8, h: 3.8,
    fill: { color: C.purple, transparency: 78 }, line: { type: 'none' }
  });
  sl.addShape(pptx.shapes.OVAL, {
    x: -0.8, y: 0.5, w: 2.0, h: 2.0,
    fill: { color: C.pink, transparency: 82 }, line: { type: 'none' }
  });

  // Brand name
  sl.addText('StyleSense AI', {
    x: 0, y: 0.38, w: 10, h: 1.35,
    fontSize: 60, bold: true, color: 'FFFFFF',
    align: 'center', fontFace: F
  });

  // Subtitle (in header, muted white)
  sl.addText('Personalized Fashion Recommendations & Virtual Try-On', {
    x: 1.0, y: 1.85, w: 8.0, h: 0.52,
    fontSize: 15, color: 'B0BAD0',
    align: 'center', fontFace: F, wrap: true
  });

  // Purple accent stripe — transition between header and white body
  sl.addShape(pptx.shapes.RECTANGLE, {
    x: 0, y: 2.9, w: 10, h: 0.07,
    fill: { color: C.purple }, line: { type: 'none' }
  });

  // Team names — dark text on white
  sl.addText('JAPJIT  ·  PRABHSIMRAN  ·  PANSHUL  ·  RATIK  ·  ABHINEET', {
    x: 0, y: 3.32, w: 10, h: 0.40,
    fontSize: 12.5, color: C.text,
    align: 'center', fontFace: F, charSpacing: 0.5
  });

  // Thin divider
  hRule(sl, 3.2, 3.82, 3.6, C.borderLt);

  // Minor project label
  sl.addText('Minor Project  |  April 2026', {
    x: 0, y: 3.96, w: 10, h: 0.32,
    fontSize: 11, color: C.subtext,
    align: 'center', fontFace: F
  });

  // Three keyword tags at bottom
  const tags = ['Kotlin + Jetpack Compose', 'FastAPI + Redis', 'IDM-VTON Try-On'];
  tags.forEach((tag, i) => {
    const tw = 2.4, gap = 0.25;
    const totalW = tags.length * tw + (tags.length - 1) * gap;
    const x = (10 - totalW) / 2 + i * (tw + gap);
    sl.addShape(pptx.shapes.ROUNDED_RECTANGLE, {
      x, y: 4.62, w: tw, h: 0.33, rectRadius: 0.165,
      fill: { color: C.cardAlt }, line: { color: C.borderLt, width: 1 }
    });
    sl.addText(tag, {
      x, y: 4.62, w: tw, h: 0.33,
      fontSize: 9.5, color: C.subtext, align: 'center', valign: 'middle', fontFace: F
    });
  });
}

// ─── SLIDE 02 — The Problem ───────────────────────────────────────────────────
function slide02() {
  const sl = pptx.addSlide();
  bgSlide(sl);

  // Left purple accent bar
  sl.addShape(pptx.shapes.RECTANGLE, {
    x: 0, y: 0, w: 0.18, h: 5.625,
    fill: { color: C.purple }, line: { type: 'none' }
  });

  addHeading(sl, 'Why Does Fashion Shopping Feel Broken?', 0.3, 22);
  hRule(sl, 0.4, 0.98, 9.2, C.border);

  const cards = [
    { emoji: '🧠', title: 'Decision Fatigue',   body: 'Too many choices, zero personalization — shoppers abandon carts overwhelmed by options.' },
    { emoji: '👗', title: 'No Try-Before-Buy',  body: "Can't visualize fit or style match until the item arrives at your door — returns spike." },
    { emoji: '🌤️', title: 'Context Blindness',  body: 'Ignores local weather, current occasion, and existing wardrobe when suggesting outfits.' },
  ];

  cards.forEach(({ emoji, title, body }, i) => {
    const x = 0.3 + i * 3.18;
    addCard(sl, x, 1.14, 3.0, 2.95, { border: C.purple, lw: 1.8 });
    sl.addText(emoji, { x, y: 1.22, w: 3.0, h: 0.7,  fontSize: 32, align: 'center', fontFace: F });
    sl.addText(title, { x: x + 0.1, y: 1.95, w: 2.8, h: 0.38, fontSize: 13.5, bold: true, color: C.purple, align: 'center', fontFace: F });
    sl.addText(body,  { x: x + 0.1, y: 2.38, w: 2.8, h: 1.5,  fontSize: 11,  color: C.gray, align: 'center', fontFace: F, wrap: true, valign: 'top' });
  });

  // Stat callout
  addCard(sl, 0.3, 4.2, 9.4, 0.7, { fill: C.pinkLt, border: C.pink, lw: 1.8 });
  sl.addText('"20–35% of online fashion orders are returned — driven by poor fit and zero personalization"', {
    x: 0.55, y: 4.22, w: 8.9, h: 0.66,
    fontSize: 12.5, bold: true, color: C.pink,
    align: 'center', fontFace: F, wrap: true, valign: 'middle'
  });
}

// ─── SLIDE 03 — Our Solution ──────────────────────────────────────────────────
function slide03() {
  const sl = pptx.addSlide();
  bgSlide(sl);

  addPill(sl, 'OUR SOLUTION');
  addHeading(sl, 'StyleSense AI: Context-Aware Fashion Intelligence', 0.65, 22);
  hRule(sl, 0.4, 1.32, 9.2, C.border);

  // Left: real app screenshot (portrait 1080×2392, ratio 1:2.215)
  // Fit within left column (0.28–3.9") at correct portrait ratio, max h=3.87"
  const scW = 1.75, scH = +(scW * SC_RATIO).toFixed(3);  // 1.75" × 3.878"
  const scX = 0.28 + (3.62 - scW) / 2;                   // center in left column
  sl.addShape(pptx.shapes.ROUNDED_RECTANGLE, {
    x: scX - 0.07, y: 1.40, w: scW + 0.14, h: scH + 0.14, rectRadius: 0.12,
    fill: { color: C.cardAlt }, line: { color: C.border, width: 2 }
  });
  sl.addImage({ path: SC('screen_home.png'), x: scX, y: 1.44, w: scW, h: scH });

  // Divider
  vRule(sl, 3.95, 1.44, scH + 0.06, C.border);

  // Right: features
  const features = [
    ['Profile-driven 10-step onboarding', 'Body type, skin tone, style prefs, budget, occasions'],
    ['Live Myntra product scraping',       'Real-time catalog via BeautifulSoup + curl_cffi'],
    ['MatchScorer multi-factor ranking',   'Style · Color · Brand · Weather · Wardrobe · Feedback'],
    ['Weather-aware suggestions via GPS',  'OpenWeatherMap REST API integration'],
    ['Wardrobe-compatible recommendations','Avoids duplicates, suggests complementary pieces'],
    ['AI Virtual Try-On (IDM-VTON)',       'Diffusion-based try-on via hosted HuggingFace Space'],
  ];

  features.forEach(([title, sub], i) => {
    const y = 1.44 + i * 0.65;
    sl.addText('▸', { x: 4.1, y, w: 0.3, h: 0.34, fontSize: 13, bold: true, color: C.purple, fontFace: F });
    sl.addText([
      { text: title + '\n', options: { bold: true, color: C.text, fontSize: 12.5 } },
      { text: sub,          options: { color: C.gray, fontSize: 10.5 } }
    ], { x: 4.38, y, w: 5.3, h: 0.62, fontFace: F, valign: 'top', wrap: true });
  });
}

// ─── SLIDE 04 — System Architecture ──────────────────────────────────────────
function slide04() {
  const sl = pptx.addSlide();
  bgSlide(sl);

  addHeading(sl, 'System Architecture', 0.25, 24);
  hRule(sl, 0.4, 0.9, 9.2, C.border);

  // ── Android App box (left, purple border) ──────────────────
  addCard(sl, 0.25, 1.05, 2.35, 3.5, { border: C.purple, lw: 2 });
  sl.addText('Android App', { x: 0.25, y: 1.1, w: 2.35, h: 0.38,
    fontSize: 12, bold: true, color: C.purple, align: 'center', fontFace: F });
  hRule(sl, 0.35, 1.5, 2.15, C.border);
  const appItems = ['Kotlin + Jetpack Compose', 'Material 3 UI', 'Firebase Auth SDK',
    'Firestore SDK', 'Retrofit / OkHttp', 'DataStore (local cache)'];
  appItems.forEach((t, i) => {
    sl.addText('• ' + t, { x: 0.38, y: 1.56 + i * 0.33, w: 2.1, h: 0.32,
      fontSize: 9.5, color: C.gray, fontFace: F, wrap: true });
  });

  // Arrow: App → Backend
  addArrowH(sl, 2.6, 2.8, 0.9, C.purple);

  // ── FastAPI Backend box (center, pink border) ───────────────
  addCard(sl, 3.5, 1.05, 2.6, 3.5, { border: C.pink, lw: 2 });
  sl.addText('FastAPI Backend', { x: 3.5, y: 1.1, w: 2.6, h: 0.38,
    fontSize: 12, bold: true, color: C.pink, align: 'center', fontFace: F });
  hRule(sl, 3.6, 1.5, 2.4, C.border);
  const apiItems = ['Python + Uvicorn', '/recommendations', '/tryon', '/wardrobe/*',
    'Redis client', 'Gradio client (HF)'];
  apiItems.forEach((t, i) => {
    sl.addText('• ' + t, { x: 3.62, y: 1.56 + i * 0.33, w: 2.36, h: 0.32,
      fontSize: 9.5, color: C.gray, fontFace: F, wrap: true });
  });

  // Arrow: Backend → Services
  addArrowH(sl, 6.1, 2.1, 0.75, C.border);
  addArrowH(sl, 6.1, 2.6, 0.75, C.border);
  addArrowH(sl, 6.1, 3.1, 0.75, C.border);
  addArrowH(sl, 6.1, 3.6, 0.75, C.border);

  // ── External services (right column) ───────────────────────
  const services = [
    ['Firebase / Firestore', 'Auth + DB (direct from App)', C.yellow],
    ['Redis Cache',          'In-memory ranked results',    C.green],
    ['Myntra Scraper',       'BeautifulSoup + curl_cffi',   C.purple],
    ['HuggingFace IDM-VTON', 'Gradio client → try-on AI',  C.pink],
  ];
  services.forEach(([name, sub, color], i) => {
    const y = 1.82 + i * 0.83;
    addCard(sl, 6.85, y, 2.9, 0.68, { border: color, lw: 1.5 });
    sl.addText(name, { x: 6.9, y: y + 0.04, w: 2.8, h: 0.3,
      fontSize: 10, bold: true, color, fontFace: F });
    sl.addText(sub, { x: 6.9, y: y + 0.33, w: 2.8, h: 0.28,
      fontSize: 8.5, color: C.gray, fontFace: F });
  });

  // Arrow: Backend ↔ OpenWeatherMap (below)
  addArrowV(sl, 4.8, 4.58, 0.5, C.green);
  addCard(sl, 3.5, 5.1, 2.6, 0.38, { border: C.green, lw: 1.5 });
  sl.addText('OpenWeatherMap API', { x: 3.5, y: 5.1, w: 2.6, h: 0.38,
    fontSize: 9.5, bold: true, color: C.green, align: 'center', valign: 'middle', fontFace: F });

  // Caption
  sl.addText('All communication via REST/HTTP  ·  Firebase accessed directly from the Android app', {
    x: 0.3, y: 4.62, w: 6.2, h: 0.35, fontSize: 8.5, color: C.grayDk,
    fontFace: F, wrap: true
  });
}

// ─── SLIDE 05 — Technology Stack ─────────────────────────────────────────────
function slide05() {
  const sl = pptx.addSlide();
  bgSlide(sl);

  addHeading(sl, 'Technology Stack', 0.25, 24);
  hRule(sl, 0.4, 0.88, 9.2, C.border);

  const techs = [
    { layer: 'Mobile UI',   tech: 'Kotlin + Jetpack Compose + Material 3', color: C.purple },
    { layer: 'Auth',        tech: 'Firebase Auth  (Google Sign-In via Credential Manager)', color: C.yellow },
    { layer: 'Database',    tech: 'Cloud Firestore  (NoSQL, realtime sync)', color: C.yellow },
    { layer: 'Backend',     tech: 'Python FastAPI + Uvicorn  (async REST API)', color: C.pink },
    { layer: 'Cache',       tech: 'Redis  (in-memory fallback for dev)', color: C.green },
    { layer: 'Scraping',    tech: 'BeautifulSoup + curl_cffi  (TLS bypass)', color: C.purple },
    { layer: 'Weather',     tech: 'OpenWeatherMap REST API  (GPS-based forecast)', color: C.green },
    { layer: 'Try-On AI',   tech: 'HuggingFace IDM-VTON via Gradio Client', color: C.pink },
  ];

  const cols = 2, rows = 4;
  const cw = 4.5, ch = 0.92, gx = 0.3, gy = 1.02, gap = 0.15;

  techs.forEach(({ layer, tech, color }, i) => {
    const col = i % cols;
    const row = Math.floor(i / cols);
    const x = gx + col * (cw + gap);
    const y = gy + row * (ch + gap);

    addCard(sl, x, y, cw, ch, { border: color, lw: 1.5 });

    // Left accent strip
    sl.addShape(pptx.shapes.ROUNDED_RECTANGLE, {
      x: x + 0.06, y: y + 0.06, w: 0.06, h: ch - 0.12, rectRadius: 0.03,
      fill: { color }, line: { type: 'none' }
    });

    sl.addText(layer, {
      x: x + 0.2, y: y + 0.08, w: cw - 0.26, h: 0.32,
      fontSize: 9.5, bold: true, color, fontFace: F
    });
    sl.addText(tech, {
      x: x + 0.2, y: y + 0.42, w: cw - 0.26, h: 0.44,
      fontSize: 12, bold: false, color: C.text, fontFace: F, wrap: true
    });
  });
}

// ─── SLIDE 06 — Module 1: Authentication ──────────────────────────────────────
function slide06() {
  const sl = pptx.addSlide();
  bgSlide(sl);

  addPill(sl, 'MODULE  01');
  addHeading(sl, 'Authentication & Identity', 0.65, 24);
  hRule(sl, 0.4, 1.3, 9.2, C.border);

  // Left: flow diagram
  const steps = [
    ['Splash Screen',         ''],
    ['Google Sign-In',        'Android Credential Manager'],
    ['Firebase Auth → UID',   'Token verified server-side'],
    ['Route: Onboard / Home', 'Based on Firestore profile'],
  ];
  steps.forEach(([label, sub], i) => {
    const y = 1.45 + i * 0.92;
    flowBox(sl, 0.3, y, 3.3, 0.72, label, sub, { border: C.purple, fSize: 10.5 });
    if (i < steps.length - 1)
      addArrowV(sl, 1.95, y + 0.72, 0.2, C.purple);
  });

  // Vertical divider
  vRule(sl, 3.95, 1.38, 3.98, C.border);

  // Right: bullets
  const bullets = [
    ['Google Sign-In via Credential Manager',  'No context switching — in-app auth flow, no external browser'],
    ['Firebase UID as primary key',             'Used across Firestore collections and Redis keying'],
    ['Persistent session management',           'Token refresh handled automatically; skip sign-in on return'],
    ['Zero-knowledge onboarding check',         'Firestore query determines first-time vs. returning user'],
    ['Screens: Splash, Sign-In',                'Minimal, dark-themed entry point'],
  ];
  bullets.forEach(([title, body], i) => {
    const y = 1.46 + i * 0.78;
    sl.addText('▸', { x: 4.1, y, w: 0.28, h: 0.32, fontSize: 12, bold: true, color: C.purple, fontFace: F });
    sl.addText([
      { text: title + '\n', options: { bold: true, color: C.text, fontSize: 12 } },
      { text: body,         options: { color: C.gray, fontSize: 10 } }
    ], { x: 4.38, y, w: 5.3, h: 0.72, fontFace: F, valign: 'top', wrap: true });
  });
}

// ─── SLIDE 07 — Module 2: Onboarding ─────────────────────────────────────────
function slide07() {
  const sl = pptx.addSlide();
  bgSlide(sl);

  addPill(sl, 'MODULE  02');
  addHeading(sl, '10-Step Profile Onboarding', 0.65, 24);
  hRule(sl, 0.4, 1.3, 9.2, C.border);

  // Step indicator — 10 numbered circles
  const steps = ['Gender', 'Body\nType', 'Measure\nments', 'Skin\nTone', 'Style\nPrefs',
                  'Fit\nPrefs', 'Occasions', 'Budget', 'Special\nReqs', 'Categories'];
  const circleD = 0.58, startX = 0.3, y = 1.5, spacing = 0.96;

  steps.forEach((label, i) => {
    const x = startX + i * spacing;
    // Circle
    sl.addShape(pptx.shapes.OVAL, {
      x, y, w: circleD, h: circleD,
      fill: { color: i === 0 ? C.purple : C.card },
      line: { color: i === 0 ? C.purple : C.border, width: i === 0 ? 0 : 1.5 }
    });
    sl.addText(String(i + 1), {
      x, y, w: circleD, h: circleD,
      fontSize: 13, bold: true, color: i === 0 ? C.bg : C.purple,
      align: 'center', valign: 'middle', fontFace: F
    });
    // Label below circle
    sl.addText(label, {
      x: x - 0.12, y: y + circleD + 0.05, w: circleD + 0.24, h: 0.5,
      fontSize: 7.5, color: C.gray, align: 'center', fontFace: F, wrap: true
    });
    // Connector line between circles
    if (i < steps.length - 1) {
      sl.addShape(pptx.shapes.RECTANGLE, {
        x: x + circleD, y: y + circleD/2 - 0.01,
        w: spacing - circleD, h: 0.018,
        fill: { color: C.border }, line: { type: 'none' }
      });
    }
  });

  // Description
  sl.addText('All 10 steps persisted to Firestore  users/{uid}  on completion', {
    x: 0.4, y: 2.62, w: 9.2, h: 0.32,
    fontSize: 11.5, color: C.gray, align: 'center', fontFace: F
  });

  hRule(sl, 0.4, 3.0, 9.2, C.border);

  // Two-column content below
  // Left: step descriptions
  const stepDetails = [
    'Gender, body type & key measurements',
    'Skin tone (8-point scale from selfie or manual)',
    'Style archetypes — casual, formal, streetwear …',
    'Preferred fit (slim, regular, relaxed, oversized)',
    'Occasions — daily, office, gym, date night, festival',
  ];
  stepDetails.forEach((t, i) => {
    sl.addText('▸  ' + t, {
      x: 0.4, y: 3.12 + i * 0.41, w: 4.5, h: 0.38,
      fontSize: 10.5, color: C.gray, fontFace: F, wrap: true
    });
  });

  const stepDetails2 = [
    'Budget range (₹ slider, saved to Firestore)',
    'Special requirements — plus-size, petite, maternity',
    'Preferred categories — tops, bottoms, dresses …',
    'No purchase history needed (cold-start solved)',
    'Profile editable anytime from Settings screen',
  ];
  stepDetails2.forEach((t, i) => {
    sl.addText('▸  ' + t, {
      x: 5.1, y: 3.12 + i * 0.41, w: 4.5, h: 0.38,
      fontSize: 10.5, color: C.gray, fontFace: F, wrap: true
    });
  });

  // Cold-start callout (below step details, full width)
  addCard(sl, 0.28, 5.12, 9.44, 0.36, { fill: C.purpleLt, border: C.purple, lw: 1.5 });
  sl.addText('Cold-Start Solved: No purchase history needed — profile data drives immediate personalization from day one.', {
    x: 0.44, y: 5.14, w: 9.12, h: 0.32,
    fontSize: 10.5, bold: false, color: C.purple, fontFace: F, wrap: true, align: 'center', valign: 'middle'
  });
}

// ─── SLIDE 08 — Module 3: Wardrobe Manager ────────────────────────────────────
function slide08() {
  const sl = pptx.addSlide();
  bgSlide(sl);

  addPill(sl, 'MODULE  03');
  addHeading(sl, 'Smart Wardrobe Management', 0.65, 24);
  hRule(sl, 0.4, 1.3, 9.2, C.border);

  // Left: vertical flow diagram
  const flowSteps = [
    ['Photo / Pick Image',         'Camera or gallery picker'],
    ['POST /wardrobe/upload',       'Backend saves to /uploads dir'],
    ['URL stored in Firestore',     'users/{uid}/wardrobe/{id}'],
    ['Wardrobe Grid UI',            'LazyVerticalGrid — tap to manage'],
  ];
  flowSteps.forEach(([label, sub], i) => {
    const y = 1.44 + i * 0.94;
    flowBox(sl, 0.28, y, 3.35, 0.72, label, sub, { border: C.purple, fSize: 10.5 });
    if (i < flowSteps.length - 1)
      addArrowV(sl, 1.95, y + 0.72, 0.22, C.purple);
  });

  vRule(sl, 3.95, 1.38, 3.98, C.border);

  // Right: bullets
  const bullets = [
    ['Camera or gallery image selection',  'Uses Android photo picker & CameraX — no extra permissions'],
    ['Backend saves to /uploads dir',       'FastAPI saves file; returns public URL for Firestore storage'],
    ['Metadata stored per item',            'Firestore stores URL, category, color, date added'],
    ['Wardrobe items feed MatchScorer',     'Existing items influence recommendation ranking weights'],
    ['Prevents duplicate purchases',        'MatchScorer de-ranks items similar to already-owned pieces'],
    ['Screens: Wardrobe Grid, Add Item',    'Full CRUD — add, view, delete wardrobe items'],
  ];
  bullets.forEach(([title, body], i) => {
    const y = 1.46 + i * 0.68;
    sl.addText('▸', { x: 4.1, y, w: 0.28, h: 0.32, fontSize: 12, bold: true, color: C.purple, fontFace: F });
    sl.addText([
      { text: title + '\n', options: { bold: true, color: C.text, fontSize: 12 } },
      { text: body,         options: { color: C.gray, fontSize: 10 } }
    ], { x: 4.38, y, w: 5.3, h: 0.65, fontFace: F, valign: 'top', wrap: true });
  });
}

// ─── SLIDE 09 — Module 4: Recommendation Engine ───────────────────────────────
function slide09() {
  const sl = pptx.addSlide();
  bgSlide(sl);

  addPill(sl, 'MODULE  04');
  addHeading(sl, 'MatchScorer Recommendation Engine', 0.65, 24);
  hRule(sl, 0.4, 1.3, 9.2, C.border);

  // Full-width scoring pipeline diagram
  // Row 1: Input box
  flowBox(sl, 0.28, 1.44, 9.44, 0.66,
    'User Profile (Firestore)  +  Context (Occasion, Weather)  +  Wardrobe Items',
    '', { border: C.purple, fSize: 11 });

  addArrowV(sl, 5.0, 2.1, 0.24, C.purple);

  // Row 2: Scraper
  flowBox(sl, 0.28, 2.34, 9.44, 0.62,
    'Myntra Scraper',
    'BeautifulSoup + curl_cffi  →  200 raw candidates  →  Redis Cache (TTL 10 min)',
    { border: C.pink, fSize: 11 });

  addArrowV(sl, 5.0, 2.96, 0.24, C.pink);

  // Row 3: MatchScorer — split into 6 factor cards
  sl.addText('MatchScorer — Multi-Factor Ranking', {
    x: 0.28, y: 3.2, w: 9.44, h: 0.3,
    fontSize: 11, bold: true, color: C.text, fontFace: F, align: 'center'
  });

  const factors = ['Style Match', 'Color Harmony', 'Brand Pref', 'Weather Fit', 'Wardrobe Compat', 'Feedback Weight'];
  const factColors = [C.purple, C.pink, C.yellow, C.green, C.purple, C.pink];
  const fw = 1.45, fh = 0.52, fy = 3.52;
  factors.forEach((label, i) => {
    const fx = 0.28 + i * (fw + 0.1);
    addCard(sl, fx, fy, fw, fh, { border: factColors[i], lw: 1.5 });
    sl.addText(label, {
      x: fx + 0.04, y: fy, w: fw - 0.08, h: fh,
      fontSize: 9.5, bold: true, color: factColors[i],
      align: 'center', valign: 'middle', fontFace: F, wrap: true
    });
  });

  addArrowV(sl, 5.0, 4.04, 0.28, C.purple);

  // Row 4: Output
  flowBox(sl, 0.28, 4.32, 9.44, 0.62,
    'Ranked + Paginated Results  →  Android App',
    'Redis caches ranked list; re-ranks only when feedback version changes — no re-scraping',
    { border: C.green, fSize: 11 });
}

// ─── SLIDE 10 — Module 5: Virtual Try-On ─────────────────────────────────────
function slide10() {
  const sl = pptx.addSlide();
  bgSlide(sl);

  addPill(sl, 'MODULE  05');
  addHeading(sl, 'AI Virtual Try-On', 0.65, 24);
  hRule(sl, 0.4, 1.3, 9.2, C.border);

  // Left: vertical flow
  const tryonSteps = [
    ['User Selfie + Garment URL',   ''],
    ['POST /tryon',                  'FastAPI endpoint'],
    ['Download garment image',       'Backend fetches from URL'],
    ['Gradio Client → HF Space',    'IDM-VTON inference'],
    ['Base64 image response',        ''],
    ['Display + Save to History',    'DataStore (local)'],
  ];
  const boxH = 0.58, stepGap = 0.76, startY = 1.42;
  tryonSteps.forEach(([label, sub], i) => {
    const y = startY + i * stepGap;
    flowBox(sl, 0.28, y, 3.3, boxH, label, sub, { border: C.purple, fSize: 9.5 });
    if (i < tryonSteps.length - 1)
      addArrowV(sl, 1.93, y + boxH, stepGap - boxH - 0.02, C.purple);
  });

  vRule(sl, 3.9, 1.38, 4.0, C.border);

  // Right: bullets
  const bullets = [
    ['IDM-VTON: diffusion-based try-on (2023)', 'State-of-art virtual garment fitting — no mesh or 3D model needed'],
    ['No GPU needed on our side',               'Inference runs on hosted HuggingFace free-tier Space'],
    ['Returns Base64 image',                    'No extra file hosting required — sent directly as response body'],
    ['Try-on history saved locally',            'Android DataStore keeps last 20 try-on results for offline view'],
    ['Screens: Try-On Select + Result',         'One-tap flow: pick garment, see yourself wearing it'],
    ['Latency: 20–60s on HF free tier',         'Handled gracefully with loading state + progress indicator'],
  ];
  bullets.forEach(([title, body], i) => {
    const y = 1.46 + i * 0.68;
    sl.addText('▸', { x: 4.08, y, w: 0.28, h: 0.32, fontSize: 12, bold: true, color: C.purple, fontFace: F });
    sl.addText([
      { text: title + '\n', options: { bold: true, color: C.text, fontSize: 11.5 } },
      { text: body,         options: { color: C.gray, fontSize: 10 } }
    ], { x: 4.36, y, w: 5.32, h: 0.65, fontFace: F, valign: 'top', wrap: true });
  });
}

// ─── SLIDE 11 — Module 6: Feedback Loop ──────────────────────────────────────
function slide11() {
  const sl = pptx.addSlide();
  bgSlide(sl);

  addPill(sl, 'MODULE  06');
  addHeading(sl, 'Like/Dislike Feedback Loop', 0.65, 24);
  hRule(sl, 0.4, 1.3, 9.2, C.border);

  // Top row (left → right): 3 steps
  const topSteps = [
    { label: 'User Sees\nRecommendations', color: C.purple },
    { label: 'Swipe\nLike / Dislike',      color: C.pink },
    { label: 'POST /recommendations\n/feedback', color: C.yellow },
  ];
  const bw = 2.7, bh = 0.9, topY = 1.5, topStart = 0.28;
  topSteps.forEach(({ label, color }, i) => {
    const x = topStart + i * (bw + 0.55);
    flowBox(sl, x, topY, bw, bh, label, '', { border: color, fSize: 10.5 });
    if (i < topSteps.length - 1)
      addArrowH(sl, x + bw, topY + bh / 2, 0.55, color);
  });

  // Right-side vertical arrow (down) — from POST /feedback center to Stored in Redis
  const postFeedbackCenterX = topStart + 2 * (bw + 0.55) + bw / 2;  // center of 3rd top box
  addArrowV(sl, postFeedbackCenterX, topY + bh, 0.55, C.yellow);

  // Bottom row — left to right: Updated Ranking | Re-rank Applied | Stored in Redis
  // Flow direction: Stored in Redis (right) ←← Re-rank ←← Updated Ranking (left) ↑ (up to top)
  const botSteps = [
    { label: 'Updated Ranking\nSent to App',      color: C.green },
    { label: 'Re-rank Applied\n(No re-scraping)',  color: C.green },
    { label: 'Stored in Redis\nKeyed by UID',      color: C.yellow },
  ];
  const botY = topY + bh + 0.55;
  botSteps.forEach(({ label, color }, i) => {
    const x = topStart + i * (bw + 0.55);
    flowBox(sl, x, botY, bw, bh, label, '', { border: color, fSize: 10.5 });
    // Left-pointing arrow between boxes (flow goes right→left)
    if (i > 0) {
      const prevBoxRight = topStart + (i - 1) * (bw + 0.55) + bw;
      addArrowLeft(sl, prevBoxRight, botY + bh / 2, 0.55, color);
    }
  });

  // Left-side vertical arrow (up — Updated Ranking → User sees recs)
  const updatedRankingCenterX = topStart + bw / 2;
  addArrowUp(sl, updatedRankingCenterX, topY + bh, 0.55, C.green);

  // Key insight callout
  addCard(sl, 0.28, 4.05, 9.44, 0.72, { fill: C.greenLt, border: C.green, lw: 2 });
  sl.addText('Key Insight:', {
    x: 0.5, y: 4.1, w: 1.2, h: 0.3, fontSize: 11, bold: true, color: C.green, fontFace: F
  });
  sl.addText('"No re-training. No re-scraping. Pure re-ranking — instant personalization from user feedback."', {
    x: 1.6, y: 4.1, w: 7.8, h: 0.6, fontSize: 12, bold: true, color: C.green, fontFace: F,
    wrap: true, valign: 'middle'
  });
}

// ─── SLIDE 12 — App Screens Showcase ─────────────────────────────────────────
function slide12() {
  const sl = pptx.addSlide();
  bgSlide(sl);

  addHeading(sl, 'App Screens', 0.25, 24);
  hRule(sl, 0.4, 0.88, 9.2, C.border);

  // 6 portrait screenshots (1080×2392, ratio 1:2.215) in a single row
  // Each frame: w=1.41", h=3.12" — 6 × 1.41 + 5 × 0.16 gaps + 2 × 0.41 margins = 10"
  const screens = [
    { file: 'screen_splash.png',      label: 'Splash Screen',  color: C.purple },
    { file: 'screen_signin.png',       label: 'Sign-In',        color: C.purple },
    { file: 'screen_recs.png',         label: 'Recommendations',color: C.pink },
    { file: 'screen_wardrobe_grid.png',label: 'Wardrobe Grid',  color: C.pink },
    { file: 'screen_tryon_result.png', label: 'Try-On Result',  color: C.purple },
    { file: 'screen_saved.png',        label: 'Saved Items',    color: C.pink },
  ];

  const imgW = 1.41;
  const imgH = +(imgW * SC_RATIO).toFixed(3);  // 1.41 × 2.215 = 3.123"
  const gap  = 0.156;
  const totalW = screens.length * imgW + (screens.length - 1) * gap;
  const startX = (10 - totalW) / 2;
  const imgY   = 1.02;

  screens.forEach(({ file, label, color }, i) => {
    const x = startX + i * (imgW + gap);

    // Rounded border frame
    sl.addShape(pptx.shapes.ROUNDED_RECTANGLE, {
      x: x - 0.05, y: imgY - 0.05, w: imgW + 0.1, h: imgH + 0.1, rectRadius: 0.1,
      fill: { color: C.cardAlt }, line: { color: C.border, width: 1.5 }
    });
    // Actual screenshot
    sl.addImage({ path: SC(file), x, y: imgY, w: imgW, h: imgH });

    // Label below
    sl.addText(label, {
      x: x - 0.1, y: imgY + imgH + 0.1, w: imgW + 0.2, h: 0.3,
      fontSize: 9.5, bold: true, color, align: 'center', fontFace: F, wrap: true
    });
  });

  sl.addText('Built with Jetpack Compose + Material 3  ·  Dark-mode first design', {
    x: 0, y: 5.18, w: 10, h: 0.28,
    fontSize: 10, color: C.grayDk, align: 'center', fontFace: F
  });
}

// ─── SLIDE 13 — Competitive Comparison ───────────────────────────────────────
function slide13() {
  const sl = pptx.addSlide();
  bgSlide(sl);

  addHeading(sl, 'How We Compare', 0.2, 24);
  hRule(sl, 0.4, 0.82, 9.2, C.border);

  const tick = '✓', cross = '✗', partial = '~';
  // Header: deep navy fill, white text
  const headerOpts = { fill: { color: C.navy }, color: 'FFFFFF', bold: true, align: 'center', valign: 'middle', fontFace: F, fontSize: 11 };
  // Data cells: white fill, dark text
  const dataOpts   = (bold=false, accent=false) => ({
    color: accent ? C.purple : C.text, fill: { color: accent ? C.purpleLt : C.card },
    bold, align: 'center', valign: 'middle', fontFace: F, fontSize: 11
  });
  const tickOpts  = (accent=false) => ({ color: C.green,  fill: { color: accent ? C.greenLt  : C.card }, bold: accent, align: 'center', valign: 'middle', fontFace: F, fontSize: 13 });
  const crossOpts = (accent=false) => ({ color: C.red,    fill: { color: accent ? C.redLt    : C.card }, bold: false,  align: 'center', valign: 'middle', fontFace: F, fontSize: 13 });
  const partOpts  = (accent=false) => ({ color: C.yellow, fill: { color: accent ? C.yellowLt : C.card }, bold: accent, align: 'center', valign: 'middle', fontFace: F, fontSize: 11 });

  const mk = (text, opts) => ({ text, options: opts });

  const rows = [
    // Header row
    [
      mk('System',          { ...headerOpts, fill: { color: '1A0F3C' } }),
      mk('Body-Type Aware', headerOpts),
      mk('Weather Aware',   headerOpts),
      mk('Wardrobe Aware',  headerOpts),
      mk('Virtual Try-On',  headerOpts),
      mk('Live Catalog',    headerOpts),
    ],
    // Data rows
    [mk('Myntra',         dataOpts()), mk(partial, partOpts()), mk(cross, crossOpts()), mk(cross, crossOpts()), mk('Limited', { ...partOpts(), fontSize: 10 }), mk(tick, tickOpts())],
    [mk('ASOS',           dataOpts()), mk('Size guide', { ...partOpts(), fontSize: 9.5 }), mk(cross, crossOpts()), mk(cross, crossOpts()), mk(cross, crossOpts()), mk(tick, tickOpts())],
    [mk('Stitch Fix',     dataOpts()), mk(tick, tickOpts()), mk(cross, crossOpts()), mk(partial, partOpts()), mk(cross, crossOpts()), mk(cross, crossOpts())],
    [mk('H&M Style Quiz', dataOpts()), mk(partial, partOpts()), mk(cross, crossOpts()), mk(cross, crossOpts()), mk(cross, crossOpts()), mk(cross, crossOpts())],
    [mk('Google Lens',    dataOpts()), mk(cross, crossOpts()), mk(cross, crossOpts()), mk(cross, crossOpts()), mk(cross, crossOpts()), mk(cross, crossOpts())],
    // StyleSense AI highlighted row
    [
      mk('StyleSense AI', { color: C.purple, fill: { color: C.purpleLt }, bold: true, align: 'center', valign: 'middle', fontFace: F, fontSize: 11 }),
      mk(tick, { ...tickOpts(true), fill: { color: C.greenLt } }),
      mk(tick, { ...tickOpts(true), fill: { color: C.greenLt } }),
      mk(tick, { ...tickOpts(true), fill: { color: C.greenLt } }),
      mk(tick, { ...tickOpts(true), fill: { color: C.greenLt } }),
      mk(tick, { ...tickOpts(true), fill: { color: C.greenLt } }),
    ],
  ];

  sl.addTable(rows, {
    x: 0.3, y: 0.95, w: 9.4,
    colW: [1.9, 1.5, 1.5, 1.5, 1.5, 1.5],
    rowH: [0.44, 0.42, 0.42, 0.42, 0.42, 0.42, 0.5],
    border: { pt: 1, color: C.border },
    valign: 'middle', align: 'center', fontSize: 11, fontFace: F
  });
}

// ─── SLIDE 14 — Future Work ───────────────────────────────────────────────────
function slide14() {
  const sl = pptx.addSlide();
  bgSlide(sl);

  addHeading(sl, 'Future Directions', 0.25, 24);
  hRule(sl, 0.4, 0.88, 9.2, C.border);

  const cards = [
    { emoji: '🛒', title: 'Multi-Store Scraping',
      body: 'Expand beyond Myntra to AJIO, Amazon Fashion, and Meesho for broader catalog coverage and price comparison.' },
    { emoji: '🧠', title: 'ML-Based Ranking',
      body: 'Replace rule-based MatchScorer with a learned ranking model (e.g. LightGBM/NCF) as interaction data grows.' },
    { emoji: '👥', title: 'Social Features',
      body: 'Share curated outfits, follow stylists, collaborative wishlists, and community-sourced style boards.' },
    { emoji: '📱', title: 'On-Device Try-On',
      body: 'Distilled IDM-VTON model for offline/real-time try-on — no server round-trip, instant results.' },
  ];

  const cw = 4.55, ch = 1.98, gx = 0.28, gy = 1.04, gap = 0.19;

  cards.forEach(({ emoji, title, body }, i) => {
    const col = i % 2;
    const row = Math.floor(i / 2);
    const x = gx + col * (cw + gap);
    const y = gy + row * (ch + gap);
    const color = i % 2 === 0 ? C.purple : C.pink;

    addCard(sl, x, y, cw, ch, { border: color, lw: 1.8 });

    sl.addText(emoji, { x, y: y + 0.12, w: cw, h: 0.56, fontSize: 28, align: 'center', fontFace: F });
    sl.addText(title, {
      x: x + 0.12, y: y + 0.7, w: cw - 0.24, h: 0.36,
      fontSize: 13.5, bold: true, color, fontFace: F, align: 'center'
    });
    sl.addText(body, {
      x: x + 0.12, y: y + 1.08, w: cw - 0.24, h: 0.82,
      fontSize: 10.5, color: C.gray, fontFace: F, wrap: true, align: 'center', valign: 'top'
    });
  });
}

// ─── SLIDE 15 — Thank You ─────────────────────────────────────────────────────
function slide15() {
  const sl = pptx.addSlide();
  bgSlide(sl);

  // Deep navy header band
  sl.addShape(pptx.shapes.RECTANGLE, {
    x: 0, y: 0, w: 10, h: 2.9,
    fill: { color: C.navy }, line: { type: 'none' }
  });

  // Decorative semi-transparent ovals in header (pink / purple swapped from slide01)
  sl.addShape(pptx.shapes.OVAL, {
    x: 7.2, y: -0.9, w: 3.5, h: 3.5,
    fill: { color: C.pink, transparency: 78 }, line: { type: 'none' }
  });
  sl.addShape(pptx.shapes.OVAL, {
    x: -0.8, y: 0.4, w: 2.2, h: 2.2,
    fill: { color: C.purple, transparency: 82 }, line: { type: 'none' }
  });

  // "Thank You" in header
  sl.addText('Thank You', {
    x: 0, y: 0.38, w: 10, h: 1.35,
    fontSize: 60, bold: true, color: 'FFFFFF',
    align: 'center', fontFace: F
  });

  // "Questions?" below, muted white in header
  sl.addText('Questions?', {
    x: 0, y: 1.88, w: 10, h: 0.52,
    fontSize: 18, color: 'B0BAD0',
    align: 'center', fontFace: F
  });

  // Pink accent stripe
  sl.addShape(pptx.shapes.RECTANGLE, {
    x: 0, y: 2.9, w: 10, h: 0.07,
    fill: { color: C.pink }, line: { type: 'none' }
  });

  // Team names
  sl.addText('JAPJIT  ·  PRABHSIMRAN  ·  PANSHUL  ·  RATIK  ·  ABHINEET', {
    x: 0, y: 3.32, w: 10, h: 0.40,
    fontSize: 12.5, color: C.text,
    align: 'center', fontFace: F, charSpacing: 0.5
  });

  hRule(sl, 3.2, 3.82, 3.6, C.borderLt);

  sl.addText('Minor Project  |  April 2026', {
    x: 0, y: 3.96, w: 10, h: 0.32,
    fontSize: 11, color: C.subtext,
    align: 'center', fontFace: F
  });

  sl.addText('github.com/your-org/stylesense-ai', {
    x: 0, y: 4.38, w: 10, h: 0.28,
    fontSize: 10, color: C.subtext,
    align: 'center', fontFace: F
  });
}

// ─── MAIN ─────────────────────────────────────────────────────────────────────
async function main() {
  console.log('StyleSense AI — Presentation Builder');
  console.log('=====================================');

  await makeAssets();

  pptx = new pptxgen();
  pptx.layout  = 'LAYOUT_16x9';
  pptx.author  = 'StyleSense AI Team';
  pptx.company = 'Minor Project 2026';
  pptx.title   = 'StyleSense AI — Personalized Fashion & Virtual Try-On';

  console.log('Building slides…');
  slide01();  console.log(' ✓ Slide 01 — Title');
  slide02();  console.log(' ✓ Slide 02 — Problem');
  slide03();  console.log(' ✓ Slide 03 — Solution');
  slide04();  console.log(' ✓ Slide 04 — Architecture');
  slide05();  console.log(' ✓ Slide 05 — Tech Stack');
  slide06();  console.log(' ✓ Slide 06 — Auth');
  slide07();  console.log(' ✓ Slide 07 — Onboarding');
  slide08();  console.log(' ✓ Slide 08 — Wardrobe');
  slide09();  console.log(' ✓ Slide 09 — Recommendation Engine');
  slide10();  console.log(' ✓ Slide 10 — Virtual Try-On');
  slide11();  console.log(' ✓ Slide 11 — Feedback Loop');
  slide12();  console.log(' ✓ Slide 12 — App Screens');
  slide13();  console.log(' ✓ Slide 13 — Comparison');
  slide14();  console.log(' ✓ Slide 14 — Future Work');
  slide15();  console.log(' ✓ Slide 15 — Thank You');

  const outFile = path.join(__dirname, 'output', 'stylesense_presentation.pptx');
  await pptx.writeFile({ fileName: outFile });
  console.log(`\n✅  Saved: ${outFile}`);
}

main().catch(err => { console.error('ERROR:', err); process.exit(1); });
