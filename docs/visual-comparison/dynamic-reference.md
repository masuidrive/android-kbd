# Keyboard visual reference: live browser and native comparison

This document complements docs/visual-reference-analysis.md. The preserved source is docs/reference/mock-source.html. Measurements below were taken in Chromium at a 412 × 915 CSS pixel viewport, dark color scheme, cover-device mode. Fractional values are retained where they explain raster alignment.

## Live QWERTY geometry

The q hit target is 40.594 × 45px; its visible face is 34.594 × 45px because the hit target contributes 3px transparent padding on each side. Adjacent faces therefore have a 6px visual gap. The face has a 5px radius and a one-pixel bottom shadow rgb(20,20,22) 0 1px 0.

The face uses padding-top 10px. Its main q glyph occupies a 12.453 × 26px DOM box at y keyTop + 14.5px, with computed 22px system font and normal line-height. The secondary occupies the full face width, a 12px line box at keyTop + 3px, with 11px/12px system font. This confirms that centering both labels independently in the native face cannot reproduce the reference resting composition.

## Live transition frames

The browser applies 90ms ease; sampled wall-clock frames include normal scheduling variation. The stable endpoints are the portable contract.

| Gesture | Main endpoint | Secondary endpoint | Intended relationship |
|---|---|---|---|
| Up | uppercase remains 22px, translates -3px; secondary opacity reaches 0 | no scale | uppercase takes the existing main position and moves slightly upward |
| Down | main translates +22px, opacity reaches 0 | secondary translates +13px, scales 1 → 1.7 | the secondary starts at its top legend position and grows downward into the face |

For a real down transition, the measured secondary transform at approximately 45ms was translateY(8.299px) scale(1.44688) and the main was translateY(14.045px), opacity .3616. At the settled endpoint the secondary was translateY(13px) scale(1.7) and the main was translateY(22px), opacity 0. The easing curve therefore must drive position, scale, and opacity from one shared progress value; independently recentering a newly sized font changes the visual path.

For a clean up transition, the 22/45/68ms main offsets were -0.791/-1.919/-2.871px and the endpoint was -3px. Its font and 26px line box never change size. The secondary disappears immediately because its opacity has no declared transition; only its transform and color are transitioned. Native may redraw per frame, but must preserve that observed state relationship. Pressed-center CSS changes fill and ink while retaining idle main, secondary, and composite-ghost geometry.

The reference secondary endpoint can exceed a narrow face horizontally (58.81px transformed line box versus a 34.59px face). The native implementation must preserve the later product requirement that actual glyphs remain within the key bounds. It should retain the same relative trajectory and cap the final scale by measured glyph width rather than copying the overflowing DOM line box.

frame-fixture.html freezes logical progress at 0, .25, .5, .75, and 1 for up, down, and corrected Enter→Paste states. It leaves the reference file unchanged.

## Enter → Paste source defect and correction

The live source changes the Enter main text to paste through navigationChoice, adds mode-on, and never adds flick-selected. Consequently neither the main fade/translation nor the secondary grow/translation CSS runs. This is the source defect, not an alternative animation design.

The corrected native contract keeps two stable visual roles: Enter is the main label and paste is the top secondary. During a down selection, Enter follows the QWERTY main exit (+22px, opacity 1 → 0) while paste follows the QWERTY secondary entrance (+13px, scale toward 1.7, bounded to the face). The action remains Paste. The correction does not copy the source’s text-replacement bug.

The live Enter hit target is 107.438 × 45px and its face is 101.438 × 45px. The face has a computed 15px normal-line-height font. The Enter main line box is 36.141 × 18px at face top +20px. The paste hint is 30.172 × 11px at face top +7px, computed 10px/11px with opacity .7. These are the correction’s fixed progress-zero anchors; runtime label adjustment was removed on 2026-09-12.

## Live five-way popup

An upward drag on あ produced these browser-computed bounds:

- Key: 81.203 × 51px at x 84.188.
- Popup container: 150 × 150px, centered horizontally on the key and positioned with its top 50px above key top.
- Five independent tiles: 50 × 50px, cross positions center/left/up/right/down, 6px radius, 1px #74747a border.
- Unselected fill #55555a, ink #f4f4f6; selected fill/border #a8ceff, selected ink #102844.
- Computed text is 27px / 47.25px system font.
- Container shadow is drop-shadow(rgba(0,0,0,.533) 0 3px 7px).

The current native popup draws one rounded 150dp backing plate (#40424a, radius 8dp) and only overlays a selected tile. That produces a solid square instead of five separate rounded tiles, omits the unselected tile borders, uses 20sp text, and uses a black shadow with different spread. This is a structural mismatch. The rebuild should draw five tile primitives and apply one shadow to their combined cross silhouette.

The source dims non-pressed key faces to .42 while a popup is active. A later user requirement explicitly removed that treatment because it flickers on-device, so native must keep surrounding keys at normal opacity.

The live ordinary-letter preview is 58 × 66px, placed at key top -62px and horizontally clamped. It computes to 40px/56px text, padding 4px 10px, border 1px #74747a, radius 9px 9px 5px 5px, and the same drop shadow as the five-way popup. Modifier preview widens this primitive to at least 90px with 24px text. Accent preview is a separate horizontal strip: 3px parent padding and bordered rounded parent, with independent 34 × 48px, 25px borderless choices. These are distinct primitives; routing all previews through the Kana cross popup is incorrect.

## Native comparison and implementation contract

| Surface | Current native | Measured target / override | Required structural change |
|---|---|---|---|
| Key face | radius 5dp, flat fill, no one-pixel edge shadow | radius 5px, dark 0 1px 0 #141416 | shared face primitive with bottom edge shadow; preserve selected colors |
| QWERTY idle labels | Canvas baselines derived separately; no face top padding model | face top padding 10px, main and secondary line boxes above | one label-composition function with explicit main/secondary anchor points |
| Up selection | selected glyph starts at 13sp and scales to 22.1sp | existing 22px uppercase translated only -3px | keep main size; animate translation/color and hide secondary |
| Down selection | secondary size grows and baseline moves toward generic visual center | top legend transforms as one object by +13px, scale 1.7 | animate from the idle secondary anchor using shared progress, with glyph-bound scale cap |
| Enter/Paste | special path draws only selected label | two-label transition equivalent to down QWERTY | route Enter through the same main/secondary composition |
| Popup | one large backing plate plus selected cell | five separate bordered 50px tiles forming a cross | replace backing plate with five tile primitives and silhouette shadow |
| Popup surroundings | normal opacity | source .42, overridden by user | retain normal opacity |

The same face and label primitives should serve QWERTY, Symbols, Kana, Numbers, and Cursor, with layer-specific label roles. Keep the current later requirements: Kana key height equals QWERTY, QWERTY Backspace displays ⌫ while retaining tap-delete/down-Escape behavior, Dual Flick duplicates only the central Kana keys, and layer-key hold voice behavior is unchanged.

Two additional user overrides apply after the preserved HTML: layer navigation is left Symbols, up Kana, right QWERTY, down Numbers; and candidate presence must not change the background color. KeyboardView reads the former from KeySpec rather than embedding labels, while CandidateStrip owns the latter.

## Evidence

- reference-kana-popup-up.png: live reference popup after crossing the upward threshold.
- frame-fixture.html: deterministic logical-frame viewer for up/down and corrected Enter→Paste.
- Native v0.4 final images remain under docs/screenshots/v0.4/*-final.png; they are the baseline for the implementation phase.
