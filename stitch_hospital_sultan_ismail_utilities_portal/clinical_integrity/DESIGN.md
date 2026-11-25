---
name: Clinical Integrity
colors:
  surface: '#f8f9ff'
  surface-dim: '#cbdbf5'
  surface-bright: '#f8f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#eff4ff'
  surface-container: '#e5eeff'
  surface-container-high: '#dce9ff'
  surface-container-highest: '#d3e4fe'
  on-surface: '#0b1c30'
  on-surface-variant: '#3e4942'
  inverse-surface: '#213145'
  inverse-on-surface: '#eaf1ff'
  outline: '#6e7a72'
  outline-variant: '#bdcac0'
  surface-tint: '#006c4b'
  primary: '#006949'
  on-primary: '#ffffff'
  primary-container: '#00855d'
  on-primary-container: '#f5fff7'
  inverse-primary: '#71daab'
  secondary: '#545f73'
  on-secondary: '#ffffff'
  secondary-container: '#d5e0f8'
  on-secondary-container: '#586377'
  tertiary: '#15684b'
  on-tertiary: '#ffffff'
  tertiary-container: '#358263'
  on-tertiary-container: '#f5fff7'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#8ef7c6'
  primary-fixed-dim: '#71daab'
  on-primary-fixed: '#002114'
  on-primary-fixed-variant: '#005137'
  secondary-fixed: '#d8e3fb'
  secondary-fixed-dim: '#bcc7de'
  on-secondary-fixed: '#111c2d'
  on-secondary-fixed-variant: '#3c475a'
  tertiary-fixed: '#a5f3cd'
  tertiary-fixed-dim: '#89d6b2'
  on-tertiary-fixed: '#002114'
  on-tertiary-fixed-variant: '#005138'
  background: '#f8f9ff'
  on-background: '#0b1c30'
  surface-variant: '#d3e4fe'
typography:
  headline-xl:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-xl-mobile:
    fontFamily: Inter
    fontSize: 26px
    fontWeight: '700'
    lineHeight: 34px
    letterSpacing: -0.01em
  headline-lg:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: 0em
  headline-md:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 26px
  headline-sm:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 18px
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Inter
    fontSize: 11px
    fontWeight: '700'
    lineHeight: 14px
    letterSpacing: 0.04em
  tabular-data:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '500'
    lineHeight: 18px
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  gutter: 1.5rem
  gutter-mobile: 0.75rem
  margin: 2rem
  margin-mobile: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system establishes an authoritative, calm, and highly dependable interface for clinical administration, ward record management, and hospital information technology services. Crafted specifically for hospital personnel operating under high-stress, shift-dependent conditions, the interface eliminates cognitive friction in favor of systematic legibility, uncompromising clarity, and functional speed.

The design philosophy combines **Corporate Modernism** with an **Institutional Precision** framework:
- **Calm Authority:** Surfaces evoke stability, hygiene, and public service through clinical white cards, subtle sage-tinted neutrals, and deep forestry tones.
- **Zero Decorative Noise:** Eliminates non-functional visual trends such as glassmorphism, heavy saturated gradient fields, skeuomorphic shading, and aggressive corner curves.
- **Predictable Ergonomics:** Strict layout cadences, consistent interaction targets (minimum 44px), explicit visual boundaries via hairline borders, and high-visibility keyboard focus rings.
- **Information Density with Breathing Room:** Dense data presentations (MRN lists, patient rosters, inventory counts) paired with ample structural framing to prevent scanning fatigue during long administrative shifts.

## Colors

The palette is engineered to honor the corporate identity of the Ministry of Health Malaysia (KKM) while maintaining WCAG 2.1 AA and AAA contrast compliance across clinical and ward workstations.

### Primary & Functional Roles
- **Primary Brand Green (`#0D8A61`):** Serves as the primary operational hue—used for core interactive controls, confirmed primary actions, and brand identification.
- **Dark Forest Green (`#086245`):** Reserved for interactive hover states, active navigation accents, and high-emphasis textual green highlights.
- **Light Green Tint (`#E6F4EF`):** Applied exclusively as a restful soft fill for active selection highlights, subtle chip indicators, and contextual success banners.
- **Slate Navy (`#1E293B` & `#0F172A`):** Anchors primary architectural surfaces including left navigation sidebars, high-tier section headers, and persistent shell framing, delivering visual grounding and contrast separation from the central workspace.

### Neutral & Canvas Palette
- **Application Canvas (`#F3F6F4`):** An ultra-soft, low-glare grey with a faint hospital green undertone, optimized for continuous viewing under fluorescent ward lighting.
- **Surface Elevation Cards (`#FFFFFF`):** High-clarity white containers providing contrast for forms, tables, and data modules.
- **Structural Borders (`#E2E8F0`):** Neutral slate hairlines establishing discrete modular cell boundaries and card outlines without visual clutter.
- **Primary Slate Text (`#0F172A`):** Core typography color achieving greater than 10:1 contrast against white surfaces.
- **Muted Label Slate (`#64748B`):** Secondary metadata, column titles, and structural captions.

### Status & Semantic Indicators
- **Success (`#16A34A` / Background `#DCFCE7`):** Validated states, completed records, 'Ready' statuses.
- **Warning (`#D97706` / Background `#FEF3C7`):** Pending approvals, maintenance notices, low consumable stock alerts.
- **Danger (`#C44B4B` / Background `#FEE2E2`):** System errors, urgent record flags, critical ward alerts, destructive action prompts.

## Typography

Inter serves as the primary typographic driver across all roles, providing tall x-heights, unyielding baseline alignment, and superior glyph differentiation crucial for medical and IT record entry.

### Tabular Formatting
To prevent layout jitter and ensure absolute scannability across clinical data sets:
- **Numerical OpenType Settings:** All date displays, Malaysian New NRIC formats (`YYMMDD-PB-###G`), Medical Record Numbers (MRN), bed numbers, and IP addresses must enforce OpenType tabular figures (`font-variant-numeric: tabular-nums; font-feature-settings: "tnum" 1`).
- **Data Table Cells:** Implement `tabular-data` for row contents, paired with uppercase tracking via `label-sm` for table headers.

## Layout & Spacing

The portal layout adheres to a fixed-fluid hybrid structure:
- **Application Shell:** Standard desktop configurations implement a fixed 260px administrative dark slate sidebar, a persistent 64px operational utility topbar, and a fluid main content stage framed by `margin` bounds.
- **12-Column Responsive Grid:** Data surfaces align to a 12-column grid utilizing `gutter` (24px) spacing on desktop screens, collapsing cleanly into 4 columns with `gutter-mobile` (12px) on mobile and workstation tablet form factors.
- **Rhythmic Scale:** Component interiors and form gaps strictly utilize base-4 increments (`space-xs` = 4px, `space-sm` = 8px, `space-md` = 16px, `space-lg` = 24px, `space-xl` = 32px).
- **Reflow Rules:** Tablet devices (below 1024px) collapse the persistent navigation drawer into an off-canvas slide-out, preserving horizontal screen space for full-width records and data grids.

## Elevation & Depth

To ensure immediate visual comprehension and eliminate interface clutter, visual separation relies on **structural hairline containment** rather than exaggerated floating dropshadows.

1. **Flat Base Layers:** The application canvas sits at `#F3F6F4`. Cards and data containers sit flush at Level 0 with a solid `#FFFFFF` background and a mandatory 1px border (`#E2E8F0`).
2. **Interactive Rest Elevation (Level 1):** Essential modular cards receive an ultra-subtle, non-distracting ambient shadow:
   `box-shadow: 0 1px 2px 0 rgba(15, 23, 42, 0.05);`
3. **Floating Overlays & Menus (Level 2):** Context menus, popover datepickers, and dropdown selects elevate above surfaces with:
   `box-shadow: 0 4px 6px -1px rgba(15, 23, 42, 0.08), 0 2px 4px -2px rgba(15, 23, 42, 0.04);`
4. **Modal Dialogs (Level 3):** Urgent clinical alerts and system configuration modals introduce a semi-opaque slate backdrop (`rgba(15, 23, 42, 0.60)`) paired with:
   `box-shadow: 0 20px 25px -5px rgba(15, 23, 42, 0.12), 0 8px 10px -6px rgba(15, 23, 42, 0.06);`
5. **No Decorative Diffusion:** Never apply colored, luminous, or multi-layered glassmorphic ambient shadows. All depth must be crisp, clinical, and anchored.

## Shapes

The design system maintains a **Soft Architectural Shape Profile** (`roundedness: 1`), providing modern accessibility without compromising the disciplined, official demeanor of a government health-sector utility.

- **Standard Base Radii (0.25rem / 4px):** Form input fields, standard primary/secondary buttons, checkboxes, tooltips, and table cell tags.
- **Container Radii (`rounded-lg`, 0.5rem / 8px):** Clinical cards, system panels, dialog modals, and utility drawers.
- **Full Radius (Pill / 9999px):** Status badges, frequency filter tags (`Harian`, `Bulanan`, `Tahunan`), and counter chips.

## Components

### Buttons & Interactive Targets
- **Sizing & Target Area:** Minimum height of 40px for desktop workflows, scaling to a minimum 44px hit-box on mobile/tablet viewports to ensure error-free tactile operation.
- **Primary Action Button:** Solid Hospital Brand Green (`#0D8A61`) fill, text `#FFFFFF` (Semibold), 4px border radius. Hover state deepens to `#086245`. Focus ring presents an explicit 2px offset outline of `#0D8A61`.
- **Secondary Action Button:** Surface `#FFFFFF`, border 1px solid `#CBD5E1`, text `#1E293B`. Hover shifts to `#F8FAFC`.
- **Destructive Button:** Surface `#C44B4B`, text `#FFFFFF`. Hover transitions to `#A83838`.

### Status Badges & Chips
- **Structural Treatment:** Pill-shaped (`border-radius: 9999px`), padding 2px 10px, font `label-sm` with tabular alignment.
- **Ready / Sedia:** Background `#DCFCE7`, text `#15803D`, border 1px solid `#86EFAC`.
- **Placeholder / Dalam Proses:** Background `#FEF3C7`, text `#B45309`, border 1px solid `#FCD34D`.
- **Periodicity Chips (Harian, Bulanan, Tahunan):** Unselected state utilizes `#F1F5F9` text `#475569`. Active state transitions to `#E6F4EF` fill, text `#086245`, border 1px solid `#0D8A61`.

### Data Tables
- **Table Headings:** Background `#F8FAFC`, uppercase `label-sm`, tracking `0.04em`, text `#64748B`, bottom border 2px solid `#CBD5E1`.
- **Table Rows:** Base `#FFFFFF`, border-bottom 1px solid `#E2E8F0`. Hover row state `#F8FAFC`. Row height minimum 48px for rapid pointer targeting.
- **Data Presentation:** Left-align standard names and clinical diagnoses; right-align quantities, financial entries, and durations; center-align MRN and IC tabular codes.

### Form Inputs & Select Controls
- **Geometry:** Height 42px, radius 4px, background `#FFFFFF`, border 1px solid `#CBD5E1`.
- **Focus State:** 1px solid `#0D8A61` coupled with a distinct 3px ring of `rgba(13, 138, 97, 0.20)`.
- **Field Slots:** Dedicated persistent 20px bottom helper slot accommodating `body-sm` hint text (`#64748B`) or error messaging (`#C44B4B`).
- **Icons:** 18px stroke icons in `#64748B` anchored at 12px inset padding for search and calendar inputs.

### Selection Controls
- **Checkboxes & Radios:** Dimensions 18px × 18px. Border 1.5px solid `#94A3B8`. Selected state renders `#0D8A61` background with white checkmark or inner dot. Minimum touch wrapper area of 44px × 44px.

### Cards & Modular Containers
- Pure `#FFFFFF` background, encapsulated by a continuous 1px `#E2E8F0` border and `rounded-lg` (8px) corners. Header areas separate from card bodies using a 1px internal divider rule.