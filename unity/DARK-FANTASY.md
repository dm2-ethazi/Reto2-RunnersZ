# Dark fantasy UI and cipher masks

Original artwork: procedural blue stone, gothic columns, ward sigils, ruined towers, wraith silhouettes and an iron gate. Existing navy / aged gold / parchment / muted teal palette retained. No third-party artwork shipped.

Visual references reviewed:
- https://www.darkestdungeon.com/ (official hero illustration)
- https://www.eldenring.jp/ (official presentation)

Rules:
- Easy: no hidden cipher letters.
- Normal: 8 hidden cipher letters; each of 2 hints reveals 4.
- Hard: 14 hidden cipher letters; the single hint restores them.
- Reference row stays readable. Hidden cipher cells and corresponding message characters show ?.
- Separate Fisher-Yates shuffles for alphabet order and hidden letter selection.
- Mask remains stable when changing trial offset; actual cipher shift controls message masking.
- Existing scoring, hint budgets, enemies and round progression preserved.

Validation: 19 EditMode tests passed; Windows build succeeded. Manual player checks cover normal hint recovery and randomized positions; visual captures used to correct ornament alignment and gold text contrast.
