# RIKSA — Screen Design & API Mapping

> **Audience:** Figma designers, Flutter devs, backend devs, and competition judges.
> **Source of truth for API:** Spring Boot controllers under `src/main/java/com/riksa/**/controller/*`.
> **Design system:** X.com + Cambodian identity + Reddit voting — see `docs/PROJECT_CONTEXT_PROMPT.md`.
>
> Organized **phase by phase following the user journey**. For every screen you'll see: the UI elements it contains, the API it consumes (verb, path, request, response, auth), and demo notes.

---

## How to read this doc

**Screen ID format:** `P{phase}.S{screen}` (e.g. `P2.S4` = Phase 2, screen 4).

**Transport:** REST → JSON over HTTP. GraphQL → `social.graphqls` schema, sent as `POST /graphql` with `{ "query": "...", "variables": {...} }`.

**Auth:** 🌐 public · 🔐 Bearer JWT (current user) · 🛡️ MODERATOR role · 👑 ADMIN role.

---

# Phase 1 — Onboarding & Auth

> **12 screens.** First-time student enters the app, picks a province + topics, registers, and lands on Home. The first 15 seconds of the demo.

## P1.S1 — Splash
**Elements:**
- Full-screen black background
- RIKSA logo: gold gradient square with Khmer "រ" character
- "RIKSA" wordmark below logo
- Bilingual tagline: "Learn. Rise. Lead." / "សិក្សា។ លើកស្ទួយ។ ដឹកនាំ។"
- "Get Started" gold pill button at bottom
- Language toggle (English / ភាសាខ្មែរ) in corner

**API:** none (static screen).

## P1.S2 — Onboarding: Province Selector
**Elements:**
- Sticky header: back arrow + "Choose your province" title + step indicator (1/3)
- 6 tappable province cards (2-column grid):
  - Phnom Penh, Siem Reap, Battambang, Kampong Cham, Sihanoukville, Mondulkiri
  - Each card: province crest icon, province name (English + Khmer), short tagline
  - Selected card: gold border + checkmark
- "Continue" gold pill button at bottom (disabled until a province is selected)

**API:** none (state held in app, sent with register).

## P1.S3 — Onboarding: Topic Selection
**Elements:**
- Sticky header: back arrow + "What do you want to learn?" + step indicator (2/3)
- 3 topic cards (vertical stack):
  - STEM — icon, "120 lessons", checkmark circle
  - Technology — icon, "85 lessons", checkmark circle
  - History — icon, "60 lessons", checkmark circle
- Multi-select: tapped card flips checkmark on/off
- "Continue" gold pill button at bottom

**API:** none at this step. Topic selection maps to category memberships (see Phase 4 categories) after registration.

## P1.S4 — Register
**Elements:**
- RIKSA mini logo at top
- "Create your account" heading
- Form fields:
  - Full Name (text input)
  - Email (text input, email keyboard)
  - Password (secure input, show/hide toggle)
  - Province (readonly text reflecting P1.S2 choice, with "Change" link)
- Gold "Create Account" pill button
- "or" divider line
- "Sign in with Google" outline pill button with Google logo
- Language toggle (English / ភាសាខ្មែរ) at bottom
- "Already have an account? Log in" link

**API:**
| Verb | Path | Request | Response | Auth |
|------|------|---------|----------|------|
| POST | `/api/v1/auth/register` | `{fullName, email, password, provinceId}` | `{accessToken, refreshToken, user:{...}}` | 🌐 |
| POST | `/api/v1/auth/google` | `{googleIdToken}` | `{accessToken, refreshToken, user}` | 🌐 |

## P1.S5 — Login
**Elements:**
- RIKSA mini logo
- "Welcome back" heading
- Email input
- Password input with show/hide
- "Forgot password?" link (right-aligned)
- Gold "Log in" pill button
- "or" divider
- "Sign in with Google" outline button
- "Don't have an account? Sign up" link
- Language toggle

**API:**
| Verb | Path | Request | Response | Auth |
|------|------|---------|----------|------|
| POST | `/api/v1/auth/login` | `{email, password}` | `{accessToken, refreshToken, user}` | 🌐 |

## P1.S6 — Forgot Password
**Elements:**
- Back arrow + "Forgot password" title
- Instruction text: "Enter your email and we'll send you a verification code"
- Email input
- Gold "Send code" pill button
- Note: "You'll receive a Telegram OTP" (channel is TELEGRAM)

**API:**
| Verb | Path | Request | Response | Auth |
|------|------|---------|----------|------|
| POST | `/api/v1/auth/forgot-password` | `{email}` | `{message}` | 🌐 |

## P1.S7 — Reset Password
**Elements:**
- Back arrow + "Reset password" title
- Instruction: "Enter the code we sent to your Telegram"
- 6-digit OTP code input (segmented boxes)
- New password input (secure, with strength indicator)
- Confirm new password input
- Gold "Reset password" pill button

**API:**
| Verb | Path | Request | Response | Auth |
|------|------|---------|----------|------|
| POST | `/api/v1/auth/reset-password` | `{email, otpCode, newPassword}` | `{message}` | 🌐 |

## P1.S8 — Verify Email
**Elements:**
- Back arrow + "Verify your email" title
- Instruction: "We sent a verification link to {email}"
- Token input (paste field or segmented boxes)
- Gold "Verify" pill button
- "Resend email" link

**API:**
| Verb | Path | Request | Response | Auth |
|------|------|---------|----------|------|
| POST | `/api/v1/auth/verify-email` | `{token}` | `{verified, message}` | 🌐 |

## P1.S9 — Telegram OTP Send
**Elements:**
- Back arrow + "Phone verification" title
- Instruction: "Enter your phone number to receive a Telegram code"
- Phone number input (country code prefix +668 / +855)
- Gold "Send code" pill button

**API:**
| Verb | Path | Request | Response | Auth |
|------|------|---------|----------|------|
| POST | `/api/v1/auth/otp/send` | `{phoneNumber}` | `{message, channel:"TELEGRAM"}` | 🌐 |

## P1.S10 — Telegram OTP Verify
**Elements:**
- Back arrow + "Enter code" title
- "Code sent to {phone} via Telegram" subtitle
- 6-digit segmented code input
- "Resend code in 0:30" countdown timer
- Gold "Verify" pill button (disabled until 6 digits entered)

**API:**
| Verb | Path | Request | Response | Auth |
|------|------|---------|----------|------|
| POST | `/api/v1/auth/otp/verify` | `{phoneNumber, code}` | `{verified, message}` | 🌐 |

## P1.S11 — Google OAuth Redirect
**Elements:**
- Loading spinner with RIKSA logo
- "Connecting to Google..." text
- Auto-transitions on success/failure

**API:** handled by P1.S4 `POST /api/v1/auth/google` after the Google SDK returns an ID token.

## P1.S12 — Edit Profile
**Elements:**
- Sticky header: back arrow + "Edit profile" + save action
- Avatar: large circle with "Change" overlay + camera icon
- Display Name input
- Bio textarea (multiline, character counter e.g. 0/160)
- Gender selector (segmented: Male / Female / Other / Prefer not to say)
- Province dropdown (editable — overrides onboarding choice)
- "Save changes" gold pill button at bottom

**API:**
| Verb | Path | Request | Response | Auth |
|------|------|---------|----------|------|
| GET | `/api/v1/auth/me` | — | `UserProfileResponse` | 🔐 |
| PUT | `/api/v1/auth/me` | `{displayName, bio, gender, provinceId}` | `UserProfileResponse` | 🔐 |

## App Bootstrap (not a screen, called on app launch when a token exists)
| Verb | Path | Request | Response | Auth |
|------|------|---------|----------|------|
| GET | `/api/v1/auth/me` | — | `UserProfileResponse` | 🔐 |
| POST | `/api/v1/auth/refresh` | `{refreshToken}` | `{accessToken, refreshToken, user}` | 🌐 |
| POST | `/api/v1/auth/logout` | `{refreshToken}` | `{message}` | 🔐 |

## Demo highlight
Splash → province card → topic cards → register form → land on Home. Show the bilingual toggle.

## Notes for the Figma team
- Province + topic selection happen before registration in the flow, but province is required at `register` time. Topic persistence needs a join-category step after registration (see Phase 4 categories).
- No avatar/banner upload endpoint exists yet — design the avatar picker as a "choose from preset" or placeholder for now.
- The Profile screen design includes a banner image; no `banner_url` column exists yet. Use a default gradient with province icon overlay.

---

# Phase 2 — Core Learning Loop

> **6 screens.** Student browses lessons, completes exercises, hits 70% to pass, earns XP, sees streak/rank update. The 45-second core of the demo.

## P2.S1 — Home / Dashboard
**Elements:**
- Sticky header: avatar (tappable → profile), "Home" title, "For you" subtitle
- Tab bar: For You | Lessons | Community (gold underline on active)
- Gamification banner (indigo gradient card):
  - Avatar + display name + rank name
  - XP number (large, gold)
  - Progress bar to next rank + "X XP to {nextRank}"
  - Streak badge: 🔥 + "13 day streak"
- "Today's Challenge" section:
  - Card with challenge icon, title, progress (1 of 2 done), +50 XP reward badge
- "Quick Actions" horizontal scroll cards:
  - Continue (Fractions 101 · 40% progress)
  - Leaderboard (#15)
  - Achievements (3/6 earned)
- "Recent Activity" section: one community post card (see post card format in Phase 4)
- Bottom navigation bar: Home (active) | Learn | Compete | Community | Chat

**API:**
| Verb | Path | Purpose | Auth |
|------|------|---------|------|
| GET | `/api/v1/gamification/me` | Banner XP, rank, streak | 🔐 |
| GET | `/api/v1/learning/me/in-progress` | Continue card | 🔐 |
| GET | `/api/v1/leaderboard/me` | Quick action rank | 🔐 |
| GET | `/api/v1/gamification/achievements` | Quick action count | 🔐 |
| GraphQL | `feed(limit:1)` | Recent activity post | 🔐 |

## P2.S2 — Learn: Lesson Catalog
**Elements:**
- Sticky header: "Lessons" title + tab bar (All | STEM | Tech | History)
- Difficulty pill filters: All Levels | Easy | Medium | Hard
- "Continue where you left off" card (indigo gradient):
  - Lesson title, progress bar, "2 of 5 exercises", "+80 XP" badge
- Lesson list (vertical scroll), each lesson card:
  - Lesson icon (category-colored)
  - Title (bold)
  - Meta row: category dot · difficulty stars · exercise count
  - Right side: XP badge (+60) or ✓ checkmark (green) if completed
  - Hairline bottom border

**API:**
| Verb | Path | Request | Response | Auth |
|------|------|---------|----------|------|
| GET | `/api/v1/learning/lessons` | query: `categoryId`, `difficulty`, `cursor`, `limit` | `CursorPage<LessonSummary>` | 🔐 |
| GET | `/api/v1/learning/me/in-progress` | — | `List<LessonProgress>` | 🔐 |

## P2.S3 — Lesson Detail
**Elements:**
- Sticky header: back arrow + lesson title + bookmark/share icons
- Hero card (indigo gradient, slimmer than v1):
  - Category badge pill
  - Lesson title (large)
  - Short one-line teaser (full body lives inside the Read stage now)
  - Meta row: difficulty stars · exercise count · est. reading time · base XP badge
- "What you'll do" section — 3-step preview of the loop (matches the in-loop stepper):
  - ① Read — "{wordCount} words · ~{minutes} min"
  - ② Practice — "{n} free-response exercises"
  - ③ Reflect — "Write a teach-back explanation"
- "Exercises" section header + list of exercise preview rows (truncated question, points badge, no locked icons — order is student's choice once Practice is unlocked)
- "Start lesson" gold pill button (sticky bottom)
- If already in progress: "Resume" button + stage indicator ("Reading · 60%" / "Practice · 2/5" / "Reflect — pending")

**API:**
| Verb | Path | Request | Response | Auth |
|------|------|---------|----------|------|
| GET | `/api/v1/learning/lessons/{id}` | — | `LessonDetail` (summary, body, wordCount, keyTerms[], reflectionPrompt, exercises overview without answerKey) | 🔐 |
| GET | `/api/v1/learning/lessons/{id}/exercises` | — | `List<ExerciseResponse>` (id, question, difficulty, points, relatedParagraphIndex — NO answerKey) | 🔐 |

> **Design note:** the lesson body (full text) is NOT shown on this screen. It only lives inside the Read stage of P2.S4. The hero here is a teaser, not the content.

## P2.S4 — Read → Practice → Reflect (the unique core loop)
> **One screen, three stages.** Top of screen: a 3-segment stepper (① Read · ② Practice · ③ Reflect) with the active segment filled gold. The student cannot skip forward — each stage unlocks the next only when its gate is satisfied. Backward navigation is always allowed. Visual language = book/journal (paper-feel cards, margin notes, pen icon), not quiz cards.

### Stage 1 — Read
**Goal:** force actual reading, not scroll-through. Three anti-skim gates stack here.

**Elements:**
- Sticky header: back arrow + 3-segment stepper (① highlighted) + reading-progress ring tied to scroll position
- Lesson body as a readable "page" — paragraph cards with comfortable line height, key-term highlights inline
- **Key-term tap interaction:** 3–5 terms are highlighted in the text. Tapping one opens a small definition popover. A tiny counter "0/4 key terms opened" sits under the progress ring; all must be opened to unlock Continue.
- **Reading-pace gate:** the "Continue →" button stays disabled (grey, with a small ⏳) until `readingTimeMs >= wordCount * 240` (≈250 wpm). Scrolling to the bottom in 3 seconds does not satisfy this.
- **Highlight-the-thesis interaction:** at the end of the page, a prompt "Long-press the sentence that captures the core idea." Student selects one sentence; stored as `highlightedSentence`. Required to unlock Continue.
- "Continue →" gold pill (sticky bottom, unlocks when all 3 gates pass)

**API:**
| Verb | Path | Request | Response | Auth |
|------|------|---------|----------|------|
| GET | `/api/v1/learning/lessons/{id}` | — | `LessonDetail` with `body`, `wordCount`, `keyTerms[]` | 🔐 |

**Client-side gates (validated again server-side at complete):** `readingTimeMs`, `keyTermsOpened[]`, `highlightedSentence`.

### Stage 2 — Practice (free-recall, not QCM)
**Goal:** production, not recognition. No options, no answer reveal, wrong = re-read.

**Elements:**
- Stepper ② highlighted; progress bar shows Practice segment filling as exercises are solved
- One exercise at a time: question text (large) + free-text input ("Type your answer") + "Check" button
- **Confidence row** above the keyboard: 3 dots · "Not sure / Somewhat / Sure" — must tap one before "Check" enables. Stored as `confidence: 1|2|3` on the attempt.
- Feedback states:
  - Correct → green check + "✓ +{points} XP preview" + auto-advance after 1.2s
  - Incorrect → soft red + "Not quite — re-read this part, then try again" + **"Re-read paragraph" button** that scrolls Read back to `relatedParagraphIndex` with that paragraph highlighted. The correct answer is NEVER revealed.
  - Retry unlimited (matches `exercise_attempts` upsert)
- After all exercises correct → stepper ③ unlocks, "Reflect →" button appears

**API:**
| Verb | Path | Request | Response | Auth |
|------|------|---------|----------|------|
| POST | `/api/v1/lessons/{id}/exercises/{exerciseId}/attempt` | `{answer, confidence}` | `{exerciseId, correct, pointsIfCorrect}` | 🔐 |

> **Why this isn't QCM:** there is nothing to tap at random. The student must produce the answer from memory. Wrong answers route them back to the text, not to the solution.

### Stage 3 — Reflect (teach-back)
**Goal:** prove understanding in the student's own words. The artifact no QCM app produces.

**Elements:**
- Stepper ③ highlighted
- Prompt card at top: the lesson's `reflectionPrompt`, reframed as **"Explain this to a friend who missed class today."** (UI copy; backend stores the raw prompt.)
- Multi-line writing area — journal feel, pen icon in corner, live char counter "0 / 20 min"
- "Submit reflection" gold pill — disabled until ≥20 chars (matches `SimpleReflectionValidator`; V2 `AiReflectionValidator` will judge whether the reflection actually addresses the prompt)
- On submit → calls `POST /api/v1/lessons/{id}/complete` with the full payload (see P2.S5)

**API:**
| Verb | Path | Request | Response | Auth |
|------|------|---------|----------|------|
| POST | `/api/v1/lessons/{id}/complete` | see P2.S5 | see P2.S5 | 🔐 |

## P2.S5 — Completion: The Payoff
**Elements:**
- 🎉 celebration emoji at top
- "Lesson Complete!" heading
- **"Your reflection" card** (new, journal-styled): shows the student's own `reflectionText` quoted back, with their `highlightedSentence` from the Read stage as a margin note. This is the personal artifact — saved to their lesson journal on the profile.
- Status line: "All exercises solved · Reflection submitted" (NOT a percentage — mastery, not partial credit)
- XP earned badge: "🪙 +{xpEarned} XP" (gold pill) — XP = `baseXp + sum(correct exercise points)`
- Rank-up card (gold gradient border, only if `rankUp=true`):
  - "🎖️ RANK UP!" label
  - "{oldRank} → {newRank}" with arrow
- Streak update card (red tint):
  - "🔥 Streak: {streakBefore} → {streakAfter} days"
- Two buttons at bottom:
  - "Back to Lessons" (outline)
  - "Next Lesson →" (gold)

**API:**
| Verb | Path | Request | Response | Auth |
|------|------|---------|----------|------|
| POST | `/api/v1/lessons/{id}/complete` | `{exerciseAnswers:[{exerciseId, answer, confidence}], reflectionText, highlightedSentence, keyTermsOpened:[{termId, openedAt}], clozeAnswer, readingTimeMs}` | `{completionId, lessonId, xpEarned, score, reflectionText, highlightedSentence, rankUp, newRank, streakBefore, streakAfter}` | 🔐 |

> **Server-side validation in `LessonCompletionService.completeLesson()`** (in addition to existing checks):
> - `readingTimeMs >= wordCount * 240` — rejects speed-scrollers
> - all `keyTerms` for the lesson appear in `keyTermsOpened` — rejects term-skippers
> - `highlightedSentence` is non-empty and matches a sentence in `body` — rejects no-thesis
> - `clozeAnswer` matches the cloze pulled from `body` — rejects non-readers
> - all exercises have a correct attempt — rejects incomplete practice
> - `reflectionText` passes `ReflectionValidator` — rejects empty reflections
> A failed validation returns 400 with a `gate` field naming which gate failed, so the app can route the student back to the right stage.

## P2.S6 — Resume / In-Progress
**Elements:** (appears as a card in Home and Learn, or as a deep-linked screen)
- Lesson title
- Progress bar with "X of Y exercises" label
- XP remaining badge
- "Resume" gold pill button

**API:**
| Verb | Path | Response | Auth |
|------|------|----------|------|
| GET | `/api/v1/learning/me/in-progress` | `List<LessonProgress>` (lessonId, title, completedExercises, totalExercises) | 🔐 |

## Demo highlight
Open Fractions 101 → Read stage: scroll the lesson, tap 4 key-term definitions, wait out the reading-pace timer, long-press the thesis sentence → Practice stage: type free-text answers, tap confidence, wrong answer bounces back to the relevant paragraph → Reflect stage: write a 25-char teach-back explanation → Completion: 🎉 + 🪙 +{xp} XP + the student's own reflection quoted back + 🎖️ RANK UP + 🔥 streak 12→13. The wow moment is the reflection card — a personal artifact, not a score.

## Notes for the Figma team
- **This is NOT a QCM app.** No A/B/C/D option cards anywhere in Phase 2. Practice is free-text input only. The backend's `exercises.answer_key` is free-text and that's the contract — there is no `exercise_options` table and none will be added.
- **The Read stage has 3 stacked anti-skim gates:** key-term tap counter, reading-pace timer (`readingTimeMs >= wordCount * 240`), and highlight-the-thesis. All three must be satisfied before the Practice stage unlocks. Design each gate's locked/unlocked state clearly — locked = grey + ⏳ + reason ("Read for 30 more seconds" / "Open 2 more key terms" / "Highlight the core idea").
- **Wrong answers never reveal the correct answer.** The incorrect state shows "Re-read this part" and routes back to `relatedParagraphIndex` in the Read stage. Design the scroll-back-and-return transition.
- **Confidence row (3 dots: Not sure / Somewhat / Sure)** must be tapped before the "Check" button enables. This is a deliberate pause, not decoration — keep it small but obviously required.
- **Cloze-from-text checkpoint:** between Read and Practice, one fill-in-the-blank lifted verbatim from the lesson body. Wrong → back to that paragraph. (If the cloze is deferred for V1, leave the slot in the stepper but mark it "optional".)
- **Completion shows the reflection, not a percentage.** No "80%" / "Pass = 70%" copy. Status line is "All exercises solved · Reflection submitted". The reflection card is the hero of the completion screen.
- "Today's Challenge" card on Home has no backing table yet — design it, backend will add `daily_challenges`.
- "Continue where you left off" needs partial-progress tracking; backend will add this. The Resume state must include which stage the student is on (Read / Practice / Reflect), not just "X of Y exercises".
- The Learning student endpoints listed above don't exist yet — design against this contract.

---

# Phase 3 — Gamification & Compete

> **6 screens.** Student sees XP, streak, rank, achievements, climbs the provincial + national leaderboard. The social-proof segment.

## P3.S1 — Gamification Profile
**Elements:**
- Sticky header: back arrow + "Gamification" title + "Your progress" subtitle
- Indigo gradient header card:
  - Large circular avatar (gold gradient ring)
  - Display name
  - Province name
  - Rank badge: "⭐ Rising Scholar · Tier 3"
- Stats grid (2×2, divider lines between cells):
  - Total XP (5,550, gold)
  - Current Streak (13, red)
  - Highest Streak (21)
  - Lessons Done (42)
- Tab bar: XP History | Achievements | Rank History (gold underline)

**API:**
| Verb | Path | Response | Auth |
|------|------|----------|------|
| GET | `/api/v1/gamification/me` | `GamificationProfileResponse` (totalXp, currentStreak, highestStreak, rank, tier) | 🔐 |
| GET | `/api/v1/gamification/streak` | `StreakResponse` (current, highest, lastCompletedAt) | 🔐 |

## P3.S2 — XP History (tab inside P3.S1)
**Elements:**
- Tab active: "XP History"
- Vertical list of XP transaction rows (X-style):
  - 🪙 icon (left)
  - Source name (e.g. "Lesson completed: Fractions 101") + timestamp
  - Right: +amount (gold) and running balance (secondary)
- Cursor-paginated: "Load more" at bottom or infinite scroll
- Each row has hairline bottom border

**API:**
| Verb | Path | Request | Response | Auth |
|------|------|---------|----------|------|
| GET | `/api/v1/gamification/xp` | query: `cursor`, `limit=20` | `CursorPage<XpTransactionResponse>` (amount, sourceType, sourceId, runningBalance, createdAt) | 🔐 |

## P3.S3 — Achievements (tab inside P3.S1)
**Elements:**
- Tab active: "Achievements"
- Grid of achievement badges (2 or 3 columns):
  - Earned: full-color icon + name + unlocked date
  - Locked: grayscale icon + name + "Critiera: {description}"
- 6 achievements seeded (e.g. First Lesson, 7-Day Streak, Scholar Rank, etc.)
- Each badge card: rounded, hairline border, icon at top, name below

**API:**
| Verb | Path | Response | Auth |
|------|------|----------|------|
| GET | `/api/v1/gamification/achievements` | `List<AchievementResponse>` (id, name, description, criteriaType, criteriaValue, iconUrl, unlocked, unlockedAt) | 🔐 |

## P3.S4 — Rank History (tab inside P3.S1)
**Elements:**
- Tab active: "Rank History"
- Vertical timeline of rank transitions:
  - Date + time
  - Scope badge (PROVINCE or COUNTRY)
  - "{oldRank} → {newRank}" with rank icons
  - Vertical line connecting entries
- Cursor-paginated

**API:**
| Verb | Path | Request | Response | Auth |
|------|------|---------|----------|------|
| GET | `/api/v1/gamification/rank-history` | query: `cursor`, `limit=20` | `CursorPage<RankHistoryResponse>` (scope, oldRank, newRank, changedAt) | 🔐 |

## P3.S5 — Leaderboard
**Elements:**
- Sticky header: back arrow + "Leaderboard" title + "Updated 12:05 AM today" subtitle
- Scope toggle (pill segment): 🇰🇭 Cambodia | 🏙️ Phnom Penh (active)
- Top 3 podium (visual blocks):
  - 1st place: gold, center, 72px avatar
  - 2nd place: silver, left, 54px avatar
  - 3rd place: bronze, right, 40px avatar
  - Each: avatar, name, XP score
- Ranked list (X-style rows) below podium:
  - Rank number (left)
  - Avatar
  - Name + province
  - XP score (gold, right)
  - Trend arrow (📈 up / 📉 down / — same)
  - User's own row highlighted with gold tint + gold avatar ring
- Sticky bottom banner: "#15 in Phnom Penh" + "5,550 XP · Rising Scholar" + trend

**API:**
| Verb | Path | Request | Response | Auth |
|------|------|---------|----------|------|
| GET | `/api/v1/leaderboard` | query: `scope` (COUNTRY/PROVINCE), `scopeId`, `date` | `List<LeaderboardRowResponse>` (rank, userId, displayName, avatarUrl, score, trend) | 🔐 |
| GET | `/api/v1/leaderboard/me` | query: `scopeId` | `MyRankResponse` (countryRank, provinceRank, score, rankingTier) | 🔐 |

## P3.S6 — User Profile (X-style)
**Elements:**
- Sticky header: back arrow + "Sok Pisey" title + "42 posts" subtitle + ⋯ menu
- Banner image area (120px, indigo gradient with 🏔️ province icon)
- Large circular avatar (88px, gold gradient ring, overlapping banner)
- "Edit Profile" button (pill outline, right side)
- Profile info:
  - Display name (bold)
  - @handle (secondary)
  - Bio: "📚 Learning STEM & Tech | 🏙️ Phnom Penh | 🔥 13-day streak | Goal: Top 10"
  - Location + join date row
  - Following / Followers counts (tappable)
- Gamification stats bar (4 columns with divider lines):
  - Total XP (5,550, gold) | Streak (13, red) | Lessons (42) | Province Rank (#15)
- Rank progression card (indigo gradient):
  - ⭐ rank badge + rank name
  - "X XP to {nextRank}"
  - Progress bar (gold fill)
- Achievement showcase (horizontal scroll):
  - 6 badges (earned full color, locked grayscale)
  - Each: icon + name
- Tab bar: Posts | Replies | Lessons | XP History
- Posts tab: X-style post cards with Reddit-style vote bars (see Phase 4 post card)

**API:**
| Verb | Path | Purpose | Auth |
|------|------|---------|------|
| GET | `/api/v1/auth/me` | Profile info (own profile) | 🔐 |
| GET | `/api/v1/gamification/me` | Stats bar + rank progression | 🔐 |
| GET | `/api/v1/gamification/achievements` | Achievement showcase | 🔐 |
| GET | `/api/v1/leaderboard/me` | Province rank stat | 🔐 |
| GraphQL | `feed` filtered by author | Posts tab | 🔐 |

## Demo highlight
After Phase 2's rank-up, jump to Leaderboard → toggle 🇰🇭 ↔ 🏙️ Phnom Penh → user's row highlighted in gold with 📈 trend. End on gamification dashboard showing XP history ledger.

## Notes for the Figma team
- Score = `total_xp + (current_streak * 5)`.
- Rank tiers seeded: BRONZE 0 / SILVER 100 / GOLD 500 / PLATINUM 1000 / DIAMOND 2500 / LEGEND 5000.
- Streak milestones seeded at 1/3/7/14/30/60/90/365 days — these drive flame animation intensity.
- Achievement `iconUrl` is null in seed data — use placeholder badge art (gold for unlocked, grayscale for locked).
- XP grant happens server-side when a lesson is completed (Phase 2's `POST /lessons/{id}/complete`). No direct "award XP" endpoint exists.

---

# Phase 4 — Social & Blog

> **9 screens.** Student joins communities, posts about what they learned, upvotes peers, reads/writes blog posts. The social-engagement segment.

## P4.S1 — Social Feed (X-style timeline)
**Elements:**
- Sticky header: avatar (tappable → profile) + "Community" title
- Tab bar: For You | Following | STEM | Tech | History (gold underline)
- Compose box (top of feed):
  - Avatar + text input "Share something about your learning..."
  - Media icons row: 📷 photo · 🔗 link · 🏷️ tag
  - "Post" gold pill button (disabled until text entered)
- Post cards (vertical scroll) — see post card format below
- Floating action button (gold circle, ✏️) bottom-right (alternate compose entry)
- Bottom nav: Community active

**Post card format (used in feed, profile, home):**
```
[Avatar] [Display Name] [@handle · time] [Category Badge]
         [Post title (if any)]
         [Post content text]
         [Optional: media image area]
         ▲  [score]  ▼  |  💬 [comments]  📤
```
- Avatar: 44px circle, initials, indigo gradient (gold for current user)
- Category badge: colored pill (STEM=blue, Tech=green, History=amber)
- Vote bar: ▲ (green when active) + score (green if positive, red if negative) + ▼ (red when active) + divider + 💬 comment count + 📤 share

**API:**
| Operation | GraphQL | Args | Auth |
|-----------|---------|------|------|
| Query | `feed(categoryId, cursor, limit)` | UUID, UUID, Int | 🔐 |

## P4.S2 — Community Categories
**Elements:**
- Sticky header: back arrow + "Communities" title
- Search bar (optional)
- Category list (vertical), each card:
  - Category icon (rounded square)
  - Category name + slug
  - Description (one line)
  - Member count
  - "Join" / "Joined" toggle pill button (right side)
- Used during onboarding to persist topic selection (STEM/Tech/History categories)

**API:**
| Operation | GraphQL | Args | Response | Auth |
|-----------|---------|------|----------|------|
| Query | `categories` | — | `List<CategoryResponse>` (id, name, slug, iconUrl, memberCount, joined) | 🔐 |
| Mutation | `joinCategory(categoryId)` | UUID | `CategoryResponse` | 🔐 |
| Mutation | `leaveCategory(categoryId)` | UUID | `CategoryResponse` | 🔐 |

## P4.S3 — Post Detail + Comments
**Elements:**
- Sticky header: back arrow + "Post" title + ⋯ menu
- Full post card (same format as feed, expanded):
  - Avatar, name, handle, time, category badge
  - Title (if any)
  - Full content text
  - Media image (if any)
  - Vote bar (▲ score ▼ | 💬 📤)
  - Author info row below: "Posted in {category}" + join-category shortcut
- Comments section:
  - "Comments" header with count
  - Comment input bar at top: avatar + text field + send button
  - Nested comments (indented):
    - Avatar, name, handle, time
    - Comment text
    - Mini vote bar (▲ score ▼) + "Reply" link
    - Nested replies (further indented, max depth visual indicator)
- Cursor-paginated comments ("Load more replies")

**API:**
| Operation | GraphQL | Args | Auth |
|-----------|---------|------|------|
| Query | `post(id)` | UUID | 🔐 |
| Query | `comments(postId, cursor, limit)` | UUID, UUID, Int | 🔐 (pending resolver) |
| Mutation | `createComment(input)` | `{postId, content, parentId}` | 🔐 |
| Mutation | `updateComment(input)` | `{id, content}` | 🔐 |
| Mutation | `deleteComment(id)` | UUID | 🔐 |
| Mutation | `react(input)` | `{postId, commentId, type}` | 🔐 |
| Mutation | `removeReaction(postId, commentId)` | UUID, UUID | 🔐 |

## P4.S4 — Compose Post
**Elements:**
- Sticky header: back arrow + "New Post" + "Post" gold action button
- Author row: avatar + name + visibility selector (Public / Category)
- Title input (optional, single line)
- Content textarea (multiline, character counter)
- Media row: 📷 attach image (preview thumbnail) · 🔗 attach link · 🏷️ category picker
- Category picker (dropdown or bottom sheet): STEM / Tech / History + subcategories
- Character counter bottom-right (e.g. 0/280)

**API:**
| Operation | GraphQL | Args | Auth |
|-----------|---------|------|------|
| Mutation | `createPost(input)` | `{title, content, mediaUrl, categoryId}` | 🔐 |
| Mutation | `updatePost(input)` | `{id, title, content}` | 🔐 |

## P4.S5 — Profile: Posts & Replies Tabs
**Elements:** (tabs inside P3.S6 User Profile)
- "Posts" tab: grid or list of user's post cards
- "Replies" tab: list of user's comments (compact rows: parent post title + comment text + vote bar)
- "Lessons" tab: list of completed lessons with date + XP earned
- "XP History" tab: same as P3.S2

**API:**
| Verb / Op | Path / GraphQL | Purpose | Auth |
|-----------|----------------|---------|------|
| GraphQL | `feed` filtered by authorId | Posts tab | 🔐 |
| GraphQL | `comments` filtered by authorId | Replies tab | 🔐 |
| GET | `/api/v1/learning/me/completions` | Lessons tab | 🔐 |
| GET | `/api/v1/gamification/xp` | XP History tab | 🔐 |

## P4.S6 — Blog List
**Elements:**
- Sticky header: back arrow + "Blog" title + tab bar (All | Approved | My Posts)
- Featured blog card (large, rounded, top):
  - Cover image area (indigo gradient with icon if no image)
  - Title (large)
  - Meta row: author avatar + name · 👁 views · ❤ likes
- 2-column grid of blog mini cards:
  - Cover thumbnail
  - Title (2 lines max)
  - Meta: author · views · likes
- "Write" floating action button (gold circle, ✏️) bottom-right

**API:**
| Verb | Path | Request | Response | Auth |
|------|------|---------|----------|------|
| GET | `/api/v1/blog` | query: `status` (default APPROVED), `categoryId`, `page=0`, `size=20` | `BlogListResponse` | 🔐 |

## P4.S7 — Blog Detail
**Elements:**
- Sticky header: back arrow + ⋯ menu (share, report)
- Cover image (full-width, 200px height)
- Blog title (large, bold)
- Author row: avatar + name + "Follow" button + publish date
- Meta row: 👁 view count · ❤ like count · category badge
- Blog content (rich text, scrollable)
- About section (if `about` field present): italic blockquote
- Bottom action bar (sticky):
  - ❤ Like button (toggle, fill when liked)
  - 💬 Comment count (tappable → comments)
  - 📤 Share button
- "Related posts" section at bottom (horizontal scroll of mini cards)

**API:**
| Verb | Path | Request | Response | Auth |
|------|------|---------|----------|------|
| GET | `/api/v1/blog/{id}` | — | `BlogPostResponse` (tracks a view) | 🔐 |
| POST | `/api/v1/blog/{id}/like` | — | `BlogPostResponse` | 🔐 |
| POST | `/api/v1/blog/{id}/view` | — | `Void` | 🌐 |
| GraphQL | `comments(postId)` | UUID | 🔐 (if blog has comments) |

## P4.S8 — Blog Compose
**Elements:**
- Sticky header: back arrow + "New Blog Post" + "Submit" gold action
- Cover image picker (dashed border placeholder + 📷 icon)
- Title input (large, single line)
- About input (italic, single line, "A short teaser")
- Content textarea (multiline, rich-text toolbar: B / I / U / list / quote / link)
- Category picker (dropdown: STEM / Tech / History)
- Character counter
- Note text: "Your post will be reviewed by a moderator before publishing."

**API:**
| Verb | Path | Request | Response | Auth |
|------|------|---------|----------|------|
| POST | `/api/v1/blog` | `BlogPostRequest` (title, content, coverUrl, categoryId, about) | `BlogPostResponse` (status=PENDING) | 🔐 |

## P4.S9 — Blog: My Posts
**Elements:** (tab inside P4.S6 Blog List)
- Tab active: "My Posts"
- Status filter pills: All | Pending | Approved | Rejected
- List of user's blog posts, each card:
  - Cover thumbnail
  - Title
  - Status badge: PENDING (amber) / APPROVED (green) / REJECTED (red)
  - If rejected: rejection reason text below title
  - Views · likes meta
  - "Edit" / "Delete" actions

**API:**
| Verb | Path | Request | Response | Auth |
|------|------|---------|----------|------|
| GET | `/api/v1/blog` | query: `status=PENDING/APPROVED/REJECTED`, `page`, `size` | `BlogListResponse` (author-scoped) | 🔐 |

## Demo highlight
After completing a lesson, open Community → tap compose → post "Just finished Fractions 101! 🔥" → peer upvotes → score turns green. Switch to Blog → read an approved featured post → like it.

## Notes for the Figma team
- Reactions enum in schema: UPVOTE/DOWNVOTE/HAHA/LOVE. The design uses Reddit-style ▲ ▼ only — 2 buttons, backend supports all 4.
- Blog posts start PENDING and need moderator approval (Phase 6) before others see them. The "My Posts" tab shows all 3 statuses with rejection reason.
- No `post.view_count` column — design shows shares/comments only, or use share count from `shares` table.
- No `user_follows` table — design the following/followers counts on Profile as placeholder, or remove them.
- Blog cover images: `BlogPostRequest` has `coverUrl` but no upload endpoint. Design cover picker as "paste URL" or placeholder for now.

---

# Phase 5 — AI Tutor

> **3 screens.** Personalized AI tutor that knows the student's rank, streak, lessons, and weak areas. The technical-wow segment.

## P5.S1 — Chat Session List
**Elements:**
- Sticky header: back arrow + "Krama Tutor" title + "New chat" icon (top-right)
- Subtitle: "Your personalized AI tutor"
- List of past chat sessions (vertical), each row:
  - 🦉 owl icon (Krama Tutor mascot with krama scarf)
  - Session title (derived from first message or context summary)
  - Last message preview (one line, truncated)
  - Timestamp (e.g. "2h ago")
  - Hairline bottom border
- Empty state: mascot illustration + "Ask Krama Tutor anything about your learning"

**API:**
| Verb | Path | Response | Auth |
|------|------|----------|------|
| GET | `/api/v1/chat/sessions` | `List<ChatSessionSummary>` (id, contextSummary, updatedAt) | 🔐 |
| POST | `/api/v1/chat/sessions` | (creates new session) | 🔐 |

## P5.S2 — Chat Conversation
**Elements:**
- Chat header (sticky):
  - Back arrow
  - 🦉 Krama Tutor owl mascot (with krama scarf)
  - "Krama Tutor" name + "● Online · Knows your progress" subtitle
  - ⋯ menu (clear chat, delete session)
- Context card (dashed border, top of conversation):
  - "📋 I know about you:" label
  - Tag chips: "Rank: Rising Scholar" · "🔥 13-day streak" · "42 lessons done" · "Struggled with fractions"
- AI message bubble (left, secondary bg):
  - Personalized greeting referencing recent lesson completion
  - Suggested prompts below bubble (gold outline pills):
    - "Yes, explain it"
    - "Give me a practice problem"
    - "What should I learn next?"
- User message bubble (right, indigo or black bg)
- Typing indicator (3 animated dots) when AI is responding
- Chat input bar (sticky bottom):
  - Text field ("Message Krama Tutor...")
  - Gold send button (➤) — circular, disabled when empty
  - Microphone icon (optional, voice input)

**API:**
| Verb | Path | Request | Response | Auth |
|------|------|---------|----------|------|
| GET | `/api/v1/chat/sessions/{id}/messages` | query: `cursor`, `limit` | `CursorPage<ChatMessageResponse>` (role, content, createdAt) | 🔐 |
| POST | `/api/v1/chat/sessions/{id}/messages` | `{content}` | `ChatMessageResponse` (assistant reply) | 🔐 |

## P5.S3 — Context Card (inline in P5.S2)
**Elements:**
- Dashed-border card
- Header: "📋 I know about you:"
- Tag chips (horizontal wrap):
  - Rank + tier name
  - 🔥 streak count
  - Lessons completed count
  - Weakest topic (derived from low-score lessons)
  - Province
  - Current rank position

**API:** context is built server-side from `user_activity_summary` VIEW + recent `xp_transactions` + `lesson_completions`. Returned as part of session creation response or a `contextSummary` field.

## Demo highlight
Open Krama Tutor → context card shows "Rank: Rising Scholar · 🔥 13-day streak · 42 lessons · Struggled with fractions" → ask "What should I learn next?" → AI suggests the next lesson based on weak areas.

## Notes for the Figma team
- LLM client is currently a stub — backend must wire a real provider (Claude/OpenAI) before this phase is demoable.
- Decide streaming (SSE, typing indicator animates) vs. buffered (single response, simpler). Buffered is easier for the demo.
- "Struggled with fractions" context needs a definition — likely: lessons completed with score <80% in the last N attempts, grouped by category.
- Suggested prompt pills send their text directly as a new message.

---

# Phase 6 — Admin Panel

> **6 screens.** Moderator/Admin manage lessons, categories, blog moderation, and user roles. Not part of the student demo flow, but judges will ask "how is content moderated?"

> 📖 Reference: `docs/ADMIN_PANEL_GUIDE (2).md` — read before designing admin screens.

## P6.S1 — Admin Dashboard
**Elements:**
- Sticky header: "Admin" title + admin avatar + role badge (MODERATOR / ADMIN)
- Stat cards grid (2×2 or 2×3):
  - Pending blog posts (count, tappable → P6.S5)
  - Total lessons (count, tappable → P6.S2)
  - Total categories (count, tappable → P6.S4)
  - Total users (count, tappable → P6.S6) — ADMIN only
  - Active streaks / leaderboard snapshot count (optional)
- "Moderation queue" preview (top 5 pending blog posts, tappable row → P6.S5)
- "Recent lessons" preview (top 5 lessons, tappable row → P6.S2)
- Quick action buttons: "New lesson" / "New category" / "Moderate blog"

**API:** no dedicated stats endpoint yet. Counts derived from list endpoints:
| Verb | Path | Purpose | Auth |
|------|------|---------|------|
| GET | `/api/v1/blog?status=PENDING&size=1` | Pending count (from page total) | 🛡️ |
| GET | `/api/v1/admin/lessons?size=1` | Lesson count | 🛡️ |
| GET | `/api/v1/admin/categories` | Category count | 🛡️ |

## P6.S2 — Lesson Management
**Elements:**
- Sticky header: back arrow + "Lessons" title + "+ New lesson" gold button
- Search bar (by title)
- Filter row: category dropdown · difficulty dropdown · status (active/all)
- Lesson list (table-like rows), each row:
  - Lesson icon
  - Title + category badge + difficulty stars
  - Exercise count + XP badge
  - "Edit" / "Delete" actions (or swipe-to-delete on mobile)
- "Bulk import" button (opens file picker or JSON paste)

**API:**
| Verb | Path | Request | Response | Auth |
|------|------|---------|----------|------|
| POST | `/api/v1/admin/lessons` | `LessonCreateRequest` | `LessonResponse` (201) | 🛡️ |
| PUT | `/api/v1/admin/lessons/{id}` | `LessonUpdateRequest` | `LessonResponse` | 🛡️ |
| DELETE | `/api/v1/admin/lessons/{id}` | — | 204 | 🛡️ |
| POST | `/api/v1/admin/lessons/bulk` | `List<LessonCreateRequest>` | `BulkLessonResult` | 🛡️ |

## P6.S3 — Lesson Editor
**Elements:**
- Sticky header: back arrow + "New Lesson" / "Edit Lesson" + "Save" gold action
- Lesson metadata form:
  - Title input
  - Description textarea
  - Difficulty selector (segmented: Easy | Medium | Hard | Difficult)
  - Base XP input (number)
  - Category dropdown
- "Exercises" section:
  - List of exercise cards (drag to reorder):
    - Question textarea
    - Answer key input
    - Difficulty selector
    - Points input
    - (If multiple-choice) 4 option inputs with "correct" radio
    - "Delete exercise" button per card
  - "+ Add exercise" button
- "Preview as student" link (optional)
- "Save lesson" gold pill button (sticky bottom)

**API:**
| Verb | Path | Request | Auth |
|------|------|---------|------|
| POST | `/api/v1/admin/lessons` | `LessonCreateRequest` (title, description, difficulty, baseXp, categoryId, exercises[]) | 🛡️ |
| PUT | `/api/v1/admin/lessons/{id}` | `LessonUpdateRequest` | 🛡️ |

## P6.S4 — Category Management
**Elements:**
- Sticky header: back arrow + "Categories" title + "+ New category" gold button
- Category list (rows), each row:
  - Category icon (editable)
  - Name + slug
  - Description
  - Member count
  - "Edit" / "Delete" actions
- New/Edit category form (bottom sheet or modal):
  - Name input
  - Slug input (auto-generated from name, editable)
  - Description textarea
  - Icon URL input (or icon picker)
  - "Save" button

**API:**
| Verb | Path | Request | Response | Auth |
|------|------|---------|----------|------|
| POST | `/api/v1/admin/categories` | `CategoryRequest` (name, slug, description, iconUrl) | `CategoryResponse` (201) | 🛡️ |
| PUT | `/api/v1/admin/categories/{id}` | `CategoryRequest` | `CategoryResponse` | 🛡️ |
| DELETE | `/api/v1/admin/categories/{id}` | — | 204 | 🛡️ |

## P6.S5 — Blog Moderation Queue
**Elements:**
- Sticky header: back arrow + "Moderation" title + tab bar (Pending | Approved | Rejected)
- Pending tab active by default
- List of blog posts, each card:
  - Cover thumbnail
  - Title + author name + submitted date
  - Content preview (3 lines truncated)
  - "Preview" button (opens full post)
  - "Approve" green pill button + "Reject" red outline button
- Reject flow: bottom sheet with reason input + "Confirm reject" button
- Filter by category (optional)

**API:**
| Verb | Path | Request | Response | Auth |
|------|------|---------|----------|------|
| GET | `/api/v1/blog?status=PENDING` | query: `page`, `size` | `BlogListResponse` | 🛡️ |
| PATCH | `/api/v1/admin/blog/{id}/status` | `ModerationRequest` (status, reason) | `BlogPostResponse` | 🛡️ |

## P6.S6 — User Management
**Elements:**
- Sticky header: back arrow + "Users" title (ADMIN only)
- Search bar (by name / email / handle)
- User list (rows), each row:
  - Avatar + display name + @handle
  - Email + province
  - Current role badge (USER / MODERATOR / ADMIN)
  - "Change role" button → bottom sheet with role selector
  - Join date
- Role change bottom sheet:
  - Current role display
  - Role selector (segmented: USER | MODERATOR | ADMIN)
  - "Confirm" gold button
  - Warning text if promoting to ADMIN

**API:**
| Verb | Path | Request | Response | Auth |
|------|------|---------|----------|------|
| POST | `/api/v1/admin/users/{id}/role` | `RoleUpdateRequest` (role) | `UserProfileResponse` | 👑 |

## Demo highlight (optional, if judges ask)
Open admin → approve a pending blog post → create a new lesson with 3 exercises → promote a user to MODERATOR. Shows content governance is real.

## Notes for the Figma team
- Admin panel is a separate Flutter route, gated by role check on the client (`role == MODERATOR || ADMIN`). Unauthenticated users redirect to login.
- ADMIN role can do everything MODERATOR can, plus user role management (P6.S6 is ADMIN-only).
- Exercises are nested inside `LessonCreateRequest.exercises[]` — no separate exercise endpoint. The lesson editor (P6.S3) handles exercises inline.
- No dedicated stats endpoint for P6.S1 — counts are derived from list endpoint page totals, or design the dashboard without live counts.

---

# Cross-phase — Demo Flow (3 minutes)

| Time | Step | Screen | API call |
|------|------|--------|----------|
| 0:00–0:15 | Splash → province → topics → register | P1.S1 → P1.S2 → P1.S3 → P1.S4 | `POST /auth/register` |
| 0:15–1:00 | Open Fraction 101, answer 4/5, pass | P2.S1 → P2.S2 → P2.S3 → P2.S4 | `GET /learning/lessons/{id}`, `GET /learning/lessons/{id}/exercises`, `POST /learning/lessons/{id}/complete` |
| 1:00–1:15 | Rank-up celebration animation | P2.S5 | (response from `complete`: `rankUp:true`, `newRank`) |
| 1:15–1:30 | Leaderboard, see self climb | P3.S5 | `GET /leaderboard?scope=PROVINCE&scopeId=PP`, `GET /leaderboard/me` |
| 1:30–1:50 | Ask Krama Tutor for help | P5.S2 | `POST /chat/sessions/{id}/messages` |
| 1:50–2:05 | Post in community, get upvotes | P4.S1 → P4.S4 | GraphQL `createPost`, `react` |
| 2:05–2:50 | End on gamification dashboard | P3.S1 → P3.S2 → P3.S3 | `GET /gamification/me`, `/gamification/xp`, `/gamification/achievements` |

---

# Screen count summary

| Phase | Screens | Backend status |
|-------|---------|----------------|
| 1 — Onboarding & Auth | 12 | ✅ implemented |
| 2 — Core Learning Loop | 6 | 🔴 endpoints pending |
| 3 — Gamification & Compete | 6 | 🟡 read ✅ / write 🔴 |
| 4 — Social & Blog | 9 | ✅ implemented (2 GraphQL ops pending) |
| 5 — AI Tutor | 3 | 🔴 schema only |
| 6 — Admin Panel | 6 | ✅ implemented, role-gated |
| **Total** | **42 screens** | |

---

# Next steps

1. **Figma team:** Design screens phase by phase, in phase order. Each screen's element list above is your component checklist. Follow the design system in `docs/PROJECT_CONTEXT_PROMPT.md`.
2. **Backend team:** Pick up the endpoints marked 🔴 — Phase 2 (Learning student endpoints) and Phase 3 (XP grant event chain) are on the demo critical path.
3. **This doc:** Update as screens get designed and APIs get implemented. The `screenDetail/index.html` navigation hub links into this doc by phase.