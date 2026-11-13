# RIKSA — Figma Week Plan (Mon Jul 27 → Fri Jul 31, 2026)

**Team:** Phaleap + Sombath (Sombath is stronger in design) · ~6 focused hours/day each
**Already done:** Phase 1 — Onboarding & Auth (12 screens). ✓ Not in this plan.
**Scope for the week:** the remaining 24 student-app screens (P2–P5) + clickable demo-flow prototype.
**Cut line:** Admin Panel (P6) is NOT in this week's target. Grayscale wireframes only if Friday finishes early.

Every screen's element list + API contract is in `screenDesign.md`. Never invent content — if something isn't in the spec, ask, don't guess.

---

## The split — matched to skill

- **Sombath** — the *learning journey (showpiece)*: P2 Core Learning Loop (6) + P5 AI Tutor (3) = **9 screens**, but the hardest & most unique layouts. This is what wins the demo. Also leads Friday's prototype wiring.
- **Phaleap** — the *compete & social journey*: P3 Gamification (6) + P4 Social & Blog (9) = **15 screens**, but full of repeated patterns: tab variants, list rows, form patterns.

## Assignment sheet — Sombath (9 screens)

**P2 — Core Learning Loop (6):**
| Screen | Day | Note |
|--------|-----|------|
| P2.S2 Lesson catalog | MON | kit test screen |
| P2.S1 Home/Dashboard · P2.S3 Lesson detail | TUE | both heavy |
| P2.S4 Read → Practice → Reflect | WED | THE showpiece, 3 frames, whole day allowed |
| P2.S5 Completion · P2.S6 Resume | THU | light |

**P5 — AI Tutor (3):**
| Screen | Day | Note |
|--------|-----|------|
| P5.S1 Chat list · P5.S2 Chat conversation | THU | P5.S3 context card is inline in S2 |

## Assignment sheet — Phaleap (15 screens)

**P3 — Gamification & Compete (6):**
| Screen | Day | Note |
|--------|-----|------|
| P3.S3 Achievements | MON | kit test screen (badge grid) |
| P3.S1 Gamification profile · P3.S2 XP history · P3.S4 Rank history | TUE | one shell + tab variants |
| P3.S5 Leaderboard · P3.S6 User profile | WED | |

**P4 — Social & Blog (9):**
| Screen | Day | Note |
|--------|-----|------|
| P4.S1 Social feed | WED | template for every P4 screen after it |
| P4.S3 Post detail · P4.S2 Communities · P4.S4 Compose · P4.S5 Profile tabs | THU | |
| P4.S6 Blog list · P4.S7 Blog detail · P4.S8 Blog compose · P4.S9 My posts | FRI | fast assembly from post-card + form patterns |

---

## Monday — Foundation day (screens are NOT the goal)

**Together (morning):**
- **Componentize P1:** extract the gold pill button, outline button, inputs, header, language toggle from the finished auth screens into real Figma components.
- **New components for P2–P5:** bottom nav, tab bar, lesson card, post card, XP badge, avatar, stat card, chat bubble.

**Split (afternoon):** one test screen each, built from components only:
- Sombath: **P2.S2 Lesson catalog** · Phaleap: **P3.S3 Achievements**
- End-of-day review: fix the **components**, not the screens.

## Tuesday — Momentum day → cumulative 7 screens

- **Sombath (2, both heavy):** P2.S1 Home/Dashboard (worth a full morning — most-seen screen in the app) · P2.S3 Lesson detail
- **Phaleap (3, one shell):** P3.S1 Gamification profile · P3.S2 XP history · P3.S4 Rank history

## Wednesday — Showpiece day → cumulative 11 screens

- **Sombath (1 screen, 3 frames, whole day):** P2.S4 Read → Practice → Reflect — the one design no other team has. Allowed to break the 2-hour rule.
- **Phaleap (3):** P3.S5 Leaderboard · P3.S6 User profile · P4.S1 Social feed

## Thursday — Finish the core app → cumulative 20 screens

- **Sombath (5, mostly light):** P2.S5 Completion · P2.S6 Resume · P5.S1 Chat list · P5.S2 Chat conversation (P5.S3 inline)
- **Phaleap (4):** P4.S3 Post detail + comments · P4.S2 Communities · P4.S4 Compose post · P4.S5 Profile tabs

## Friday — Blog + prototype + polish → all 24 + clickable demo

- **Phaleap (morning, 4 fast screens):** P4.S6 Blog list · P4.S7 Blog detail · P4.S8 Blog compose · P4.S9 My posts
- **Sombath (all day), then both:**
  1. Wire the demo flow (`screenDesign.md` → "Demo Flow 3 minutes") as a clickable prototype — include the done P1 screens.
  2. Consistency pass: same spacing, header, bottom nav everywhere; new screens must match P1's style.
  3. Demo-critical states only: empty state on Chat List + Social Feed, loading on Leaderboard.
  4. Stretch (only if done by mid-afternoon): P6 Admin grayscale wireframes.

---

## Daily rhythm (both)

- **Start of day (10 min):** open this plan, confirm today's list.
- **End of day (15 min):** cross-review — Sombath checks Phaleap's screens for visual polish, Phaleap checks Sombath's against the spec.
- Anything unfinished rolls to tomorrow **before** new work starts.
- **2-hour rule:** if one screen takes more than 2 hours (except P2.S1 and P2.S4), simplify it and move on. Done and consistent beats perfect and missing.

## Scoreboard

| Day | Sombath | Phaleap | Cumulative |
|-----|---------|---------|------------|
| Mon | Kit + P2.S2 | Kit + P3.S3 | Component kit + 2 screens |
| Tue | P2.S1 · P2.S3 | P3.S1 · S2 · S4 | 7 screens |
| Wed | P2.S4 (3 frames) | P3.S5 · S6 · P4.S1 | 11 screens |
| Thu | P2.S5 · S6 · P5.S1 · S2 | P4.S3 · S2 · S4 · S5 | 20 screens |
| Fri | Prototype + polish | P4.S6–S9 → polish | All 24 + clickable demo |
