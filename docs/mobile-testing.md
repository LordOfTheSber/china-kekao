# TASK-025 — Mobile device testing

Acceptance: Drawing and recognition flows work on iOS Safari and Android Chrome
without breaking page scroll, with comfortable canvas size and reliable stroke
detection.

## Test matrix

| Device                    | OS / Browser            | Result | Notes |
|---------------------------|-------------------------|--------|-------|
| iPhone SE (3rd gen)       | iOS 17 — Safari         | ☐      |       |
| iPhone 14                 | iOS 17 — Safari         | ☐      |       |
| iPhone 14                 | iOS 17 — Chrome iOS     | ☐      |       |
| Pixel 7                   | Android 14 — Chrome     | ☐      |       |
| Samsung Galaxy A52        | Android 13 — Chrome     | ☐      |       |

Tick the box once a manual run passes the checklist below.

## Checklist (per device)

1. **Login** with the test account and reach the Dashboard. Header is sticky
   and readable in portrait.
2. **Recognition session**:
   - Pinyin and meaning inputs accept text without zooming the page.
   - Soft keyboard does not cover the “Check” button.
   - Submitting via on-screen Enter works.
3. **Drawing session** (production mode `DRAWING`):
   - The stroke pad is at least 280 px wide and fits within the viewport.
   - Strokes are recognised from a finger (not just a stylus).
   - Page scroll does **not** trigger while drawing inside the pad
     (`touch-action: none` on the canvas container).
   - After completing a character the next card auto-advances.
   - “Skip” and “Show order” buttons are reachable with the thumb.
4. **Choice session** (production mode `CHOICE`):
   - Six choices fit on one screen without horizontal scroll.
   - Tap targets are at least ~44 px tall.
5. **Search** (`/search`):
   - Pinyin search debounces and results land within ~1 s on a 4G connection.
   - Tapping a row opens `/hanzi/:id`.
6. **Hanzi detail** (`/hanzi/:id`):
   - Stroke animation runs and is replayable.
   - Examples block is scrollable inside the page, not the canvas.
7. **Decks** (`/decks`):
   - Subscribe button gives feedback (toast).
   - Re-subscribe is idempotent.
8. **Settings** (`/settings`):
   - Sliders and number inputs work with a touch keyboard.
   - Saving shows a toast and persists across reload.
9. **Stats** (`/stats`):
   - Charts render and resize on rotation between portrait / landscape.

## Known issues / follow-ups

Track problems found during a run as separate issues in the repository and
reference TASK-025 in the title (e.g. `TASK-025: stroke pad scroll on iOS 16`).
Empty checkboxes above mean the run has not yet been performed for that device.

## DoD

The matrix at the top of this document has at least one ticked entry for an
iOS Safari device **and** one for an Android Chrome device, with no blocker
issues open.
