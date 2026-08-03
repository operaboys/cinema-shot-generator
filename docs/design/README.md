# Handoff: Cinema Studio — AI Video Production App (Android)

## Overview
Cinema Studio is a mobile Android app that takes a user from a written story to AI-generated video shots: story input, AI-assisted scene/shot breakdown, a persistent "Project DNA" (visual/style consistency engine), per-shot camera/lighting/audio composition, multi-level validation, and prompt/output delivery across 13 external video-generation models. This bundle is the complete interactive prototype covering all 12 core screens, fully bilingual (Persian/English, RTL/LTR), with Dark and Light themes.

## About the Design Files
The file in this bundle (`Cinema Studio.html`) is a **design reference built in HTML/CSS/JS** — a high-fidelity, click-through prototype showing intended look, copy, states, and navigation. It is **not production code to copy directly**. The task is to **recreate this design natively in Android** (Jetpack Compose + Material 3, per the existing codebase at `operaboys/cinema-shot-generator`) using the app's real domain models and business logic — not to embed or wrap the HTML.

The repo currently has no UI beyond a Hello World `MainScreen.kt` and default M3 `Theme.kt`. All screen content, field values, and copy in this prototype were grounded in the repo's actual domain layer (see Screen → repo file mapping below) so the developer can wire real logic straight in.

## Fidelity
**High-fidelity (hifi).** Colors, typography, spacing, radii, shadows, and copy (in both languages) are final. Recreate pixel-perfectly using Jetpack Compose, translating the CSS tokens below into Compose `Color`/`Shape`/`Typography`/`Modifier` equivalents (glass/blur effects → Compose `Modifier.blur` / `HazeEffect` or platform equivalent since true `backdrop-filter` isn't native — see Visual Language section).

## Visual Language — "Liquid Glass"
- Translucent frosted-glass cards/bars over a colorful blurred backdrop (navy/blue/violet ambient gradient), NOT flat Material surfaces.
- Every screen has a fixed blurred hero image (the user's chosen "Home" image, or a placeholder gradient) behind ALL content, at low opacity (0.55) and heavy blur (28px), giving the whole app a consistent tinted-glass atmosphere.
- Header bar, bottom nav, cards, sheets, and modals all use a semi-transparent gradient fill + 1px hairline border + backdrop blur (~40px) + soft inset highlight (top edge) — recreate with Compose's blur/haze modifiers, NOT flat Material elevation.
- Accent color is violet/purple (#7C5CFF), used for primary actions, active states, and the FAB.
- Corner radii are large and consistent: 18px (chips/buttons/small controls), 22–24px (avatars/icons/cards), 28px (sheets/modals).

## Design Tokens

### Dark theme
| Token | Value | Use |
|---|---|---|
| page | radial-gradient(120% 80% at 50% 0%, #12141C 0%, #06070A 70%) | app background |
| bg | #0E1420 | base surface |
| ambient | 3 radial gradients: #3E6FA8 (top-right), #4A5FA8 (top-left), #24354F (bottom) fading to transparent | atmospheric color wash |
| scrim | linear-gradient(180deg, rgba(14,20,32,.30) 0%, rgba(14,20,32,.66) 52%, rgba(14,20,32,.78) 100%) | darkens toward bottom |
| fg (text primary) | #FFFFFF |
| fg2 (text secondary) | #C2C8D8 |
| fg3 (text tertiary) | #A3ABC2 |
| fg4 (text quaternary) | #8E96AE |
| card fill | linear-gradient(150deg, rgba(255,255,255,.10) 0%, rgba(255,255,255,.05) 42%, rgba(255,255,255,.07) 100%) |
| card border | rgba(255,255,255,.16) |
| card shadow | inset 0 1px 0 rgba(255,255,255,.42), inset 0 -1px 0 rgba(255,255,255,.10), 0 14px 36px rgba(0,0,0,.34) |
| inset (subtle fill) | rgba(255,255,255,.06) |
| hairline | rgba(255,255,255,.10) / hairline-strong rgba(255,255,255,.18) |
| accent (violet) | #7C5CFF, gradient with #8E74FF for buttons/FAB |
| success | #3DDC97 |
| warning | #FFB648 |
| error | #FF5A6A |
| orange (lock/DNA accent) | #FF9A4A |

### Light theme
| Token | Value |
|---|---|
| page | radial-gradient(120% 80% at 50% 0%, #E8EDF6 0%, #CFD8E8 70%) |
| bg | #EDF1F8 |
| fg | #141A2B |
| fg2 | #3B4462 |
| fg3 | #4C5678 |
| fg4 | #636C8B |
| card fill | linear-gradient(150deg, rgba(255,255,255,.86) 0%, rgba(255,255,255,.62) 45%, rgba(255,255,255,.74) 100%) |
| card border | rgba(255,255,255,.92) |
| inset | rgba(20,30,60,.05) |
| hairline | rgba(20,30,60,.10) / strong rgba(20,30,60,.14) |

All other tokens (accent, success, warning, error) are identical across themes.

### Typography
- Latin/UI: **Inter** (400/500/600/700)
- Persian: **Vazirmatn** (400/500/600/700) — used automatically whenever `lang = fa`
- Icons: **Material Symbols Rounded** (24px grid, weight 400, fill 0)
- Scale used throughout: 28px/36 (bold, big numbers/titles) · 20px/28 (section titles) · 17px/24 (card titles, semibold) · 15px/22 (body) · 13px/18 (captions/meta/labels)

### Spacing / radii
- Screen padding: 16px sides, 24px bottom
- Card padding: 16px
- Gaps: 8px (tight/chip rows), 16px (card stacks), 24px (section breaks)
- Radius: 18px (buttons, chips, small icon tiles), 22–24px (cards, avatars), 28px (bottom sheets, modals)
- Touch targets: minimum 48dp height on all interactive rows/buttons (44–56dp for primary CTAs/FAB)

## Bilingual / RTL Requirement
This is a **hard requirement**, not a nice-to-have:
- Full UI toggle between Persian (fa, RTL) and English (en, LTR) from both the header globe/flag icon and Settings.
- In RTL mode, Latin/numeric technical tokens (model names, enum values, code identifiers like `shot_description`, `03-SH.01`) must render **LTR inline within RTL text** (use Unicode bidi isolates `\u2068…\u2069` / Compose `BidiUtils` or `LocalLayoutDirection` overrides per-span) — do not let the whole numeric/Latin token get reversed.
- Model names, enum values (e.g. `WIDE`, `ESTABLISHING`, `LOCKED`), and code identifiers stay in English/Latin in BOTH languages — only UI chrome and descriptive copy translate.
- Icons that imply direction (back arrow, chevrons) flip between `arrow_back`/`arrow_forward` and `chevron_left`/`chevron_right` per language.
- Dark/Light theme toggle is independent of language and available from the same header controls plus Settings.

## Screens / Views

### 1. Home
- Full-bleed background image (user-selected in Settings, or placeholder gradient) behind the ENTIRE screen, including behind a transparent header.
- Header: hamburger menu (opens nav drawer) + app name/logo chip + language toggle (FA/EN) + theme toggle (sun/moon).
- Greeting: "وقت بخیر، Creator" / "Hello, Creator" + subtitle "بیایید چیزی سینمایی بسازیم." / "Let's create something cinematic."
- "New Project" quick-create row (icon tile + title + subtitle, violet icon tile).
- "Recent Projects" section header + "All" link.
- Project cards: thumbnail (image-slot), title, state chip (LOCKED/REVIEW/DRAFT with icon+color: orange/lock, warning/eye, secondary/edit), meta line (scene/shot counts · updated timestamp), overflow menu.
- App-level bottom nav is a fixed, always-on structure (DDR-002) — always visible on every root screen: Home, Projects, [center FAB — Quick Create], Studio, Assets.

### 2. Projects
- Header with title + subtitle ("۳ پروژه‌ی محلی" / "3 local projects").
- Same project card list as Home's "Recent", full list, no hero image.

### 3. Studio (per-project shell)
- Header: back arrow, project title, subtitle (scene/shot counts), "Saved" checkmark chip.
- 4 in-project tabs: Story / DNA / Scenes / Output (Persian: داستان / DNA / صحنه‌ها / خروجی).
- **Story tab**: title field, story textarea (read-only display in prototype), target-shots stepper, seconds-per-shot stepper.
- **DNA tab**: "Core Identity" locked-warning banner (Soft Lock — edits allowed with a warning; orange, bold, bordered — this must read as clearly urgent) + 6 collapsible field-groups: Core Identity, Master Palette (+ 5-swatch color strip), Global Mood Base, Lighting Preference, Output Constraints, Quality Directives. Each group: title + unit badge + expandable rows (label + value + expand chevron).
- **Scenes tab**: scene cards (thumbnail, title, role/time/weather meta, shot-count + state chip with icon/color matching project states).
- **Output tab**: same pattern as screen 10 (Output Delivery) scoped to the project.

### 4. AI Story Breakdown
- Header: back, title "تفکیک داستان با AI" / "AI Story Breakdown", subtitle "مرحله ۱ب — سه فاز" / "Step 1b — three phases".
- 3-phase stepper across the top: numbered circles connected by a hairline, each circle solid violet when active / bordered neutral tile when inactive (NOT a plain low-opacity fill — must stay legible against the blurred backdrop; see Implementation Notes).
- Phase 1 "Write story": story card + target-shots/seconds-per-shot steppers + "Generate Prompt" CTA.
- Phase 2 "Paste response": "Paste the AI response" textarea, "New chunk (ChunkCombiner)" action, "JSON error" warning row (opens repair modal).
- Phase 3 "Final review": summary of parsed Assets / Scenes / Shots counts + "Confirm & continue" CTA.
- JSON repair modal: error description + "Edit manually" / "Auto-repair" actions.

### 5. Scene Detail
- Hero image (scene still) with a lock chip overlay (top-right).
- 3 tabs: Overview / Shots (badge count) / Assets (badge count).
- Overview: scene info field list (Location, Type, Time of Day, Weather, Atmosphere, Narrative Role, Global Visual Style) + Quick Actions row (Edit Scene, Add Shot, Duplicate, Lock Scene — icon tiles).

### 6. Shots List
- Header: title + scene meta subtitle.
- Grid/Timeline view toggle (2-way segmented control).
- Shot cards: code (03-SH.01), type/goal meta, duration/motion meta, chevron → opens Shot Composer.

### 7. Shot Composer
- Hero preview strip (160px) for the shot.
- 4 tabs: اصلی/Main, دوربین/Camera, نور و محیط/Lighting, صدا/Audio — OR accordion-groups layout (A/B variant, see Interactions).
- Each tab: field rows (label/value/expand), an "Advanced" expandable section per tab where applicable (Camera, Lighting), "Attached References" chip row (character/style/composition), "Ambient — auto from Weather" info row marked "manual only / never auto-generated (Rule 5) — only on explicit user action".

### 8. Assets Library
- 3-way top filter: Characters / Locations / Objects (CHARACTER/LOCATION/OBJECT). **Must be rendered as solid, opaque, clearly bordered segmented buttons** — this was a contrast bug in the HTML prototype (see Implementation Notes) and must NOT be recreated as low-opacity/translucent chips.
- Sub-filter row (tier-specific: MAIN/SECONDARY/BACKGROUND for characters, INDOOR/OUTDOOR/MIXED for locations, PERSONAL_PROP/GENERAL_PROP/COSTUME for objects).
- Asset cards: thumbnail, name, tier badge (orange), description, `continuityLockLevel` meta line.

### 9. Validation
- Header: title + "3-level validation" subtitle.
- Top summary row: 2 solid, clearly bordered count cards — "N BLOCKING" (red) and "N WARNING" (amber) — large bold number + label. **Must read as an urgent, opaque alert block**, not a faint tint (this was a contrast bug — see Implementation Notes).
- 3 validation-level sections (Level 1 — data completeness / Level 2 — logical consistency / Level 3 — continuity & dependencies), each a list of issue cards: severity icon+label (color-coded: error/warning/check), field name, message, suggested fix.

### 10. Output Delivery
- Model picker: wrapping chip grid of all 13 models + token cost per chip (see Model List below). Selected chip = solid violet gradient + white text; unselected chips must be **solid, opaque, clearly bordered** (contrast bug fixed — see Implementation Notes), not low-opacity fills.
- Output Preview card: "Output Preview — {model}" + "Cleaned · Finalized" badge, read-only generated-prompt block, token-count line + over-limit warning, Copy / Regenerate row, full-width "Export" CTA (violet gradient).

### 11. Settings
- Sections: Display (Dynamic Font, Min Touch Target 48dp, Motion/reduced-motion), Workflow (Shot List default view, Auto-Save cadence, Jump Between Steps), Privacy (Storage encryption, Cloud Sync off, Analytics none).
- Language & Theme card: language radio (فارسی/English) + theme radio (Dark/Light) — mirrors the header quick-toggles.
- **Home Screen Image** card: 140px preview tile (drag-drop/tap to pick) + "Choose Image" (violet, primary) / "Remove Image" (secondary) actions. This image is the one used full-bleed on Home AND, blurred, as the ambient background on every other screen (see Implementation Notes).
- Layout variants card: A/B pickers for Home layout and Shot Composer layout (see Interactions).
- About/logo card with app mark + tagline.

### 12. Backups
- List of backup files: filename (`project-slug-YYYY-MM-DD.csgb`), size, kind (auto/manual), age.

## Interactions & Behavior
- Nav drawer (hamburger, root screens only): grouped links — STUDIO (Story Wizard, AI Breakdown, DNA Manager, Scenes, Shots, Assets), TOOLS (Validation, Prompt Generator, Output Delivery), SYSTEM (Settings, Backups). Active target highlighted violet-tinted. Backdrop veil closes on tap-outside.
- Bottom sheet ("Link Asset"): asset picker list, dismiss via veil tap.
- JSON repair modal: centered card over veil.
- Toast: bottom-anchored pill, auto-dismiss ~2s, used for confirmations (Saved, Copied, Asset linked, etc.)
- A/B layout variants (toggled in Settings, purely presentational — pick ONE per app, don't ship both):
  - Home: A "Hero" (current hero-image layout) vs B "Resume" (continue-where-you-left-off emphasis)
  - Shot Composer: A "4 tabs" vs B "Accordion" (collapsible groups)
- Navigation structure is fixed, not an A/B variant (DDR-002): the app-level bottom nav (Home/Projects/FAB/Studio/Assets) is always shown on every root screen, and the in-project top tab row (Story/DNA/Scenes/Output) is always shown additionally, only inside Studio — the two are complementary layers, never alternatives.
- Back navigation is contextual, not a plain stack pop: Composer→Shots, Shots/Breakdown→Studio, SceneDetail→Studio, everything else→Home.
- Language and theme changes apply instantly and globally (no restart), independent of each other.

## State Management
- Current screen + navigation history/back-target logic (contextual, see above)
- Active project tab (Studio: 0-3), scene tab (Overview/Shots/Assets: 0-2), composer tab (Main/Camera/Lighting/Audio: 0-3) and their "Advanced" expand flags
- Selected asset-kind filter + its sub-filter
- Selected output model
- Validation phase (breakdown wizard: 1-3)
- Language (fa/en), theme (dark/light) — persisted across sessions
- Home screen background image (user-uploaded, persisted; falls back to placeholder gradient) — reused app-wide as the blurred ambient background
- Drawer/sheet/modal open flags, transient toast message
- Layout A/B picks (Home, Composer) — persisted per Settings

## Model List (Output Delivery — 13 models + token costs)
| Model | Token cost |
|---|---|
| Veo 3.1 | 500 |
| Kling 3.0 | 625 |
| Seedance 2.5 | 500 |
| HappyHorse 1.0 | 500 |
| Runway Gen-4.5 | 250 |
| Luma Ray3 | 375 |
| Hailuo 2.3 | 500 |
| Wan 2.2 | 500 |
| HunyuanVideo 1.5 | 500 |
| LTX 2.3 | 500 |
| Vidu Q3 | 375 |
| Midjourney v7 | 250 |
| Stable Diffusion SD3 | 125 |
| (fallback) Universal Default | 500 |

Quick-access chips on Output Delivery surface Veo 3.1 / Kling 3.0 / Runway Gen-4.5 / Seedance 2.5 first.

## Implementation Notes (bugs fixed in the HTML prototype — carry these constraints into native)
1. **Never render a filter/segmented-control/alert as a low-opacity tinted fill on a photographic or blurred background.** The prototype's early drafts used ~10-12% opacity fills for the Assets Library kind-filter, the Validation BLOCKING/WARNING summary cards, and the Output Delivery model chips — all became nearly illegible over the blurred ambient backdrop. Final spec: solid/near-opaque fills (dark theme ~#212B4A / light theme white) + a visible 2dp border + drop shadow, selected/active state = solid brand color with white text. Apply this standard to ANY segmented control, chip, or alert card in the real implementation.
2. **Any element rendered directly over the blurred ambient background (not inside a blurred glass card) needs guaranteed elevation/compositing above that background layer** — e.g. the phase-stepper dots on AI Story Breakdown. In Compose this is naturally handled by normal view elevation/z-ordering; just don't rely on a plain transparent Box sitting at the same layer as the blur.
3. Text-contrast tiers (fg/fg2/fg3/fg4) were tuned to meet WCAG AA (≥4.5:1) against the glass-card surfaces specifically (not the raw page background) — reuse the exact hex values above rather than re-deriving a gray scale.

## Assets
- App logo: "Aperture C" mark — an 8-blade aperture/pinwheel glyph forming a stylized "C", violet-to-orange gradient blades (see `Cinema Studio.html` inline SVG `#csMark` symbol for exact path data/gradients). Used in header (root screens), drawer header, and Settings "About" card.
- All photo content (Home background, project thumbnails, scene/shot/asset thumbnails) are **placeholder drop-slots** in the prototype — no real images are final; treat every image area as needing real production photography/renders.

## Files
- `Cinema Studio.html` — the full interactive prototype (all 12 screens, both languages, both themes, both layout A/B variants), open directly in a browser to click through.
