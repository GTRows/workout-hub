---
name: "frontend-design"
description: "Use when designing, redesigning, implementing, or reviewing WorkoutHub frontend UI/UX."
---

# frontend-design

## When to use

- The user asks to improve, redesign, review, or build frontend screens.
- The work touches `frontend/src/app/**`, `frontend/src/components/**`, Tailwind styling, layout, navigation, or interaction flows.
- The user reports that the UI feels amateur, unusable, generic, or not like a real web product.

Skip if the task is backend-only, infrastructure-only, or a tiny copy change with no layout or interaction impact.

## Product direction

WorkoutHub should feel like a serious self-hosted fitness product: compact, fast, mobile-first, and built around repeated daily use. Prefer a quiet operational interface over marketing-page composition.

The first screen should be the usable app surface, not a landing page. Prioritize the user's core loops:

- See today's workout and start it quickly.
- Log sets with minimal friction during a workout.
- Review weekly adherence, recent volume, and progress trends.
- Manage plans, exercises, metrics, profile, and exports without hunting.

## UI rules

- Use dense but calm layouts, predictable navigation, and strong information hierarchy.
- Keep cards for repeated items, modals, and genuinely framed tools; do not nest cards.
- Avoid decorative hero sections, gradient blobs, oversized marketing headings, and one-note palettes.
- Use icons for common actions, segmented controls for modes, toggles for binary settings, sliders or inputs for numeric values, tabs for views, and menus for option sets.
- Keep text inside controls short and responsive. Verify mobile widths.
- User-facing text must come from the i18n layer. Do not hard-code Turkish or English strings inside components.
- Prefer existing component and styling patterns before adding new abstractions.

## Verification

After significant UI changes:

- Run the relevant frontend checks (`pnpm lint`, `pnpm typecheck`, tests, or the closest available scripts).
- Start the dev server when the app needs one.
- Use the Browser plugin to inspect the affected local pages at desktop and mobile widths.
- Capture and review screenshots for layout overlap, clipped text, unreadable contrast, and awkward empty states.

## Anti-patterns

- Do not ship a cosmetic skin over broken navigation or unclear workflows.
- Do not add new dependencies without discussing the tradeoff first.
- Do not hide important actions behind vague labels.
- Do not build one-off utilities in a generic `utils` dumping ground.
