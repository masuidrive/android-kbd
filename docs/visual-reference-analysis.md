# Keyboard visual reference: static source measurements

Source of truth: `docs/reference/mock-source.html`. Line numbers below refer to that preserved file. This document records declared CSS/JavaScript values; browser-computed measurements and native screenshot deltas are recorded separately.

## Surface and color tokens

| Element | Static contract | Source |
|---|---|---|
| Keyboard | `background: --ik-bg`; padding `8px 3px 0`; no tap highlight | lines 146, 166 |
| Dark background | `#29292c` | line 146 |
| Dark regular key | `#414144` | line 148 |
| Dark special key | `#303034` | line 148 |
| Dark selected key / ink | `#a8ceff` / `#102844` | line 148 |
| Dark popup / popup ink / border | `#55555a` / `#f4f4f6` / `#74747a` | line 148 |
| Key face | radius `5px`; shadow `0 1px 0`, dark shadow `#141416`; full hitbox width/height | line 176 |
| Pressed/mode-on | selected background and selected ink | line 179 |
| Popup-active background treatment | non-pressed key faces opacity `.42` | line 231 |

The user’s later requirement overrides line 231: native flick feedback must not dim the other key backgrounds because it produces visible flicker. That override stays in force during the fidelity rebuild.

## Geometry by layer

| Layer | Row geometry | Width rules | Source |
|---|---|---|---|
| Japanese Kana | 5 equal grid columns; `51px` row height, `6px` row gap; Enter is rightmost `20%`, two rows high with `calc(200% + 6px)` | side keys at columns 1 and 5 | lines 198–205, 267–272 |
| Numbers | same Kana 5-column grid and dimensions | center 3×3 plus `-`, `0`, `.`; shared side controls | lines 200–205, 273–278 |
| Cursor | same Kana 5-column grid and dimensions | Home/Up/End, Left/Space/Right, Down; shared side controls | lines 200–205, 279–285 |
| QWERTY | `45px` rows, `10px` row gap; keys have horizontal padding `3px`, producing a `6px` visual gap | row 2 edge C/A and BS each `5%`, letters `10%`; bottom flex `1.45 : 4.2 : 2` | lines 175–176, 206–212, 234–237, 578–584 |
| Symbols | QWERTY geometry | digits row; symbol rows; same bottom row | lines 586–592 |
| 840-wide reference | keyboard horizontal padding `10px`; English rows `52px`; Kana rows `58px` | layout topology unchanged in the original mock | lines 253–256 |

The user’s later requirements override the original Kana height split: native Japanese/Numbers/Cursor rows must use the same height as QWERTY at each width. Dual Flick still duplicates only the Japanese central 12 keys, and the layer key’s 1-second voice hold remains available.

The user also overrides the original layer navigation at mock lines 292 and 599: left remains Symbols, up remains Kana, right becomes QWERTY, and down becomes Numbers. Tap and the 1-second voice hold remain unchanged.

## Typography and label placement

| Label type | Static contract | Source |
|---|---|---|
| Generic main | inherited system font stack; key `25px`, weight `400`, centered | lines 146, 175–176 |
| QWERTY main | `24px`; keys with flick secondary `22px` | lines 206–208 |
| Special main | `16px`; Return specifically `15px`; Space `16px` | lines 177, 197, 236 |
| Generic small/hint | `10px`, line-height `1.1`, margin-top `2px`, opacity `.7`, letter spacing `.7px` | line 178 |
| QWERTY secondary | absolute top `3px`; centered; `11px/12px`; muted; transition `.09s ease` | line 220 |
| Space/Return hint | placed before the main label with `0 0 2px` margin | lines 221–224 |
| Corner label | right `5px`, bottom `4px`, `10px`, opacity `.76` | lines 194–195 |
| C/A modifier | main `18px`; C top `6px`, A bottom `6px`; directional labels `10px` | lines 188–193 |
| Layer composite (`あ/ん` etc.) | main translated left `3px`, z-index 1; ghost at `50%/50%`, translated `(2px,-35%)`, size `75%`, opacity `.72`, z-index 0; ghost is prepended before main | lines 215–219, 673–678 |

The native rebuild must retain the later QWERTY BS `⌫` label. Fidelity defaults are measured from these CSS values. The five user-configurable label adjustment groups were removed on 2026-09-12, so the measured defaults are now the fixed runtime geometry.

## Flick and navigation animation

| State | Threshold / frame endpoint | Source |
|---|---|---|
| Axis lock | gesture starts at Euclidean distance `12px`; dominant axis chosen | lines 314–323, 330–350 |
| Selection hysteresis | enter at `18px`; remain selected down to `10px` | lines 321–322, 334–336, 346–350 |
| Down secondary selected | secondary translates down `13px`, scales `1.7`, selected ink | line 226 |
| Down secondary selected main | main translates down `22px`, opacity `0` | line 227 |
| Up uppercase selected | main translates up `3px`, selected ink; secondary opacity `0` | lines 228–229 |
| Transition | main transform/opacity and secondary transform/color use `.09s ease`; reduced motion disables transitions | lines 214, 220, 232 |
| Press feedback | pointerdown adds `pressed`; pointerup/cancel removes all gesture classes | lines 763–780, 834–849 |

Enter uses the navigation system rather than the QWERTY secondary-label direction: `pasteNavigation.up` is Paste and `pasteNavigation.down` is Ctrl+J at line 207. When the selected navigation action is Paste, pointer movement applies `paste-selected`. The idle Enter face places `C-j` near the top, `Enter` at center, and `paste` near the bottom. Paste moves upward from the lower hint to the center and grows to 1.7 scale while Enter fades. Down selects `C-j` and centers that label. Finish dispatches the selected navigation action at lines 1155–1166. Native must select these presentations by action as well, while preserving tap Enter and the conversion-specific Enter layout.

## Popup geometry

| Popup | Static contract | Placement | Source |
|---|---|---|---|
| Container | `drop-shadow(0 3px 7px #0008)`, z-index 10 | absolute in keyboard | lines 244–245 |
| Kana five-way | `150×150px`; children `50×50px`, radius `6px`, font `27px`, 1px border | center `[50,50]`, left `[0,50]`, up `[50,0]`, right `[100,50]`, down `[50,100]`; popup top is key top minus `50px`, horizontal clamp | lines 246–248, 732–740 |
| Letter preview | min-width `58px`, height `66px`, padding `4px 10px`, radius `9px 9px 5px 5px`, font `40px/56px`, 1px border | centered above key, top is key top minus `62px`, horizontal clamp | lines 249, 741–744 |
| Modifier preview | min-width `90px`, font `24px`, selected background | letter-popup positioning | lines 196, 241–744 |
| Accent strip | children `34×48px`, font `25px`; parent radius `8px`, padding `3px` | key x minus `8px`, clamped; top key minus `56px` | lines 250–251, 748–755 |

Popup selection uses selected fill/ink/border at line 248. The native comparison must cover initial press, threshold crossing, selected endpoint, return inside hysteresis, release, and cancel frames. Static values alone do not prove browser-computed bounds or easing frames; those require the separate live-browser measurement.

## Existing native policy to preserve

`KeyboardView` currently fixes the Canvas palette to the mock's dark tokens: background `41/41/44`, regular key `65/65/68`, special key `48/48/52`, and selected key `168/206/255`. Its `sp()` helper caps `scaledDensity` at `density`, so Android font scales above 1.0 do not enlarge key labels beyond their physical key bounds. This is the existing 1.3/2.0 fit policy and remains deliberate.

Historical note: the first fidelity rebuild applied five saved QWERTY adjustment groups after the default geometry. That preference serialization, adjustment Activity, and Service reload path was removed on 2026-09-12. Current rendering uses the measured default size and position directly while retaining the same key-bound fitting rules.
