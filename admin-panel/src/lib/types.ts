export type Role = "USER" | "MODERATOR" | "ADMIN";
export type DifficultyLevel = "EASY" | "MEDIUM" | "HARD" | "DIFFICULT";
export type ContentStatus = "VISIBLE" | "HIDDEN";
export type BlogStatus = "PENDING" | "APPROVED" | "REJECTED" | "HIDDEN";
export type ReportStatus = "OPEN" | "RESOLVED" | "DISMISSED";
export type ReportAction = "NONE" | "HIDE";
export type ReportTargetType = "POST" | "COMMENT" | "BLOG" | "RESOURCE";
export type ExerciseType = "TEXT" | "MULTIPLE_CHOICE" | "TRUE_FALSE";
export type ReportReason =
  | "SPAM"
  | "HARASSMENT"
  | "NSFW"
  | "MISINFORMATION"
  | "OTHER";

export interface SpringPage<T> {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface CursorPage<T> {
  edges: T[];
  hasNextPage: boolean;
  endCursor?: string | null;
}

export interface BlogPage<T = BlogPost> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AuthSessionResponse {
  userId: string;
  accessToken: string;
  refreshToken: string;
  tokenType?: string;
  username: string;
  role: Role;
}

export interface TelegramBotInfo {
  available: boolean;
  botUsername: string | null;
  botUrl: string | null;
  botAppUrl: string | null;
  codeLength: number;
}

export interface TelegramAuthStartResponse {
  requestToken: string;
  botUrl: string;
  botAppUrl: string;
  botUsername: string;
  expiresAt: string;
  expiresInSeconds: number;
  pollIntervalSeconds: number;
}

export type TelegramAuthStatus =
  | "PENDING"
  | "AUTHENTICATED"
  | "PROFILE_REQUIRED"
  | "DENIED"
  | "EXPIRED";

export interface TelegramAuthResult {
  status: TelegramAuthStatus;
  auth?: AuthSessionResponse;
  signupToken?: string;
  telegram?: {
    displayName: string | null;
    username: string | null;
  };
  expiresAt?: string;
}

export interface AuthUser {
  userId: string;
  username: string;
  role: Role;
}

export interface AdminUser {
  id: string;
  username: string;
  displayName: string | null;
  email: string | null;
  emailVerified: boolean | null;
  avatarUrl: string | null;
  role: Role;
  suspendedUntil: string | null;
  suspensionReason: string | null;
  lockedUntil: string | null;
  createdAt: string;
}

export interface Exercise {
  id: string;
  question: string;
  answerKey: string;
  difficulty: DifficultyLevel;
  points: number;
  sectionIndex: number;
  type: ExerciseType;
  options: string[];
  hint: string | null;
  explanation: string | null;
}

/** Built-in lesson-page illustrations shipped in the student app. */
export type LessonBackgroundKey =
  | "math"
  | "physics"
  | "chemistry"
  | "biology"
  | "computer_science"
  | "none";

export const LESSON_BACKGROUND_KEYS: { key: LessonBackgroundKey; label: string }[] = [
  { key: "math", label: "Math" },
  { key: "physics", label: "Physics" },
  { key: "chemistry", label: "Chemistry" },
  { key: "biology", label: "Biology" },
  { key: "computer_science", label: "Computer science" },
  { key: "none", label: "None" },
];

export interface Lesson {
  id: string;
  title: string;
  description: string | null;
  difficulty: DifficultyLevel;
  baseXp: number;
  categoryId: string | null;
  reflectionPrompt: string | null;
  position: number;
  roadmapUnitId: string | null;
  estimatedMinutes: number;
  backgroundKey: LessonBackgroundKey | null;
  backgroundImageUrl: string | null;
  exercises: Exercise[];
  createdAt: string;
  updatedAt: string;
}

export interface Category {
  id: string;
  name: string;
  slug: string;
  description: string | null;
  iconUrl: string | null;
  backgroundKey: LessonBackgroundKey | null;
  backgroundImageUrl: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Report {
  id: string;
  reporterId: string;
  targetType: ReportTargetType;
  targetId: string;
  reason: ReportReason;
  details: string | null;
  status: ReportStatus;
  resolvedBy: string | null;
  resolvedAt: string | null;
  createdAt: string;
}

export interface BlogPost {
  id: string;
  authorId: string;
  title: string;
  content: string;
  coverUrl: string | null;
  status: BlogStatus;
  rejectionReason: string | null;
  hiddenReason: string | null;
  viewCount: number;
  likeCount: number;
  categoryId: string | null;
  liked: boolean;
  createdAt: string;
  updatedAt: string;
  moderationScore: number | null;
  moderationReasons: string | null;
}

export type AdminMarketSlot = "HAT" | "EYEWEAR";
export type AdminMarketRarity = "COMMON" | "RARE" | "EPIC";

export interface AdminMarketItem {
  id: string;
  slot: AdminMarketSlot;
  name: string;
  nameKm: string;
  price: number;
  rarity: AdminMarketRarity;
  assetKey: string;
  speciesMask: string | null;
  minTier: string | null;
  availableFrom: string | null;
  availableUntil: string | null;
  sortOrder: number;
  active: boolean;
  ownedCount: number;
  equippedCount: number;
}

export interface AdminMarketItemRequest {
  slot: AdminMarketSlot;
  name: string;
  nameKm: string;
  price: number;
  rarity: AdminMarketRarity;
  assetKey: string;
  speciesMask?: string | null;
  minTier?: string | null;
  availableFrom?: string | null;
  availableUntil?: string | null;
  sortOrder: number;
  active: boolean;
}

export type AdminResourceKind = "LINK" | "PDF" | "VIDEO";

export interface AdminLessonResource {
  id: string;
  lessonId: string;
  lessonTitle: string;
  authorId: string;
  authorUsername: string;
  title: string;
  url: string;
  kind: AdminResourceKind;
  hidden: boolean;
  hiddenAt: string | null;
  hiddenBy: string | null;
  saveCount: number;
  createdAt: string;
}

export interface AdminLessonResourceStatusRequest {
  hidden: boolean;
  reason?: string;
}

export interface PostModeration {
  id: string;
  title: string;
  mediaUrl: string | null;
  authorId: string;
  categoryId: string | null;
  status: ContentStatus;
  hiddenReason: string | null;
  hiddenBy: string | null;
  hiddenAt: string | null;
  createdAt: string;
}

export interface CommentModeration {
  id: string;
  content: string | null;
  mediaUrl: string | null;
  postId: string;
  authorId: string;
  status: ContentStatus;
  hiddenReason: string | null;
  hiddenBy: string | null;
  hiddenAt: string | null;
  createdAt: string;
}

export interface AuditLog {
  id: string;
  actorId: string | null;
  action: string;
  targetType: string;
  targetId: string | null;
  details: Record<string, unknown> | null;
  createdAt: string;
}

export interface StatsOverview {
  signups24h: number;
  signups7d: number;
  posts24h: number;
  posts7d: number;
  comments24h: number;
  comments7d: number;
  lessonCompletions24h: number;
  lessonCompletions7d: number;
  pendingBlogs: number;
  openReports: number;
}

export interface ExerciseStats {
  exerciseId: string;
  question: string | null;
  difficulty: DifficultyLevel | null;
  attempts: number;
  correctCount: number;
  correctRate: number;
  failureRate: number;
}

export interface GamificationStats {
  xpByDay: {
    date: string;
    sourceType: string;
    totalXp: number;
    transactionCount: number;
  }[];
  streakHistogram: { bucket: string; userCount: number }[];
}

export interface SecurityStats {
  lockedAccounts: number;
  suspendedAccounts: number;
  totalFailedLoginAttempts: number;
}

// ── User detail ──

export interface UserOverview {
  user: AdminUser;
  failedLoginAttempts: number;
  leaderboardExcluded: boolean;
  gamification: {
    totalXp: number;
    currentStreak: number;
    highestStreak: number;
    lastCompletedAt: string | null;
    tier: string | null;
    tierIconUrl: string | null;
  } | null;
  achievements: {
    id: string;
    name: string;
    iconUrl: string;
    rarity: AchievementRarity;
    xpReward: number;
    active: boolean;
    earned: boolean;
    unlockedAt: string | null;
  }[];
  lessonsCompleted: number;
  recentCompletions: {
    lessonId: string;
    lessonTitle: string | null;
    xpEarned: number;
    score: number;
    completedAt: string;
  }[];
  reportsFiled: number;
  reportsAgainst: {
    id: string;
    targetType: ReportTargetType;
    targetId: string;
    reason: ReportReason;
    details: string | null;
    status: ReportStatus;
    createdAt: string;
  }[];
}

export interface XpTransaction {
  id: string;
  amount: number;
  sourceType: string;
  sourceId: string | null;
  runningBalance: number;
  createdAt: string;
}

export interface XpAuditRow {
  userId: string;
  username: string;
  displayName: string | null;
  xpGained: number;
  transactionCount: number;
  lessonCompletions: number;
  totalXp: number;
  firstAt: string;
  lastAt: string;
  suspended: boolean;
  leaderboardExcluded: boolean;
}

// ── Gamification config ──

export type AchievementRarity = "COMMON" | "RARE" | "EPIC" | "LEGENDARY";

export const ACHIEVEMENT_CRITERIA = [
  "COMPLETE_COURSE",
  "COMPLETE_N_COURSES",
  "COMPLETE_COURSE_IN_EVERY_CATEGORY",
  "COMPLETE_ALL_IN_CATEGORY",
  "COMPLETE_ALL_IN_EVERY_CATEGORY",
  "COMPLETE_COURSE_WITH_DIFFICULTY",
  "EARN_XP",
  "MAINTAIN_STREAK",
  "REACH_RANK",
  "CREATE_POST",
  "RECEIVE_REACTION",
  "WRITE_BLOG_POST",
  "RECEIVE_BLOG_LIKE",
] as const;
export type AchievementCriteriaType = (typeof ACHIEVEMENT_CRITERIA)[number];

export interface AchievementConfig {
  id: string;
  name: string;
  description: string;
  criteriaType: AchievementCriteriaType;
  criteriaValue: number | null;
  requiresAdvanced: boolean;
  iconUrl: string;
  categoryId: string | null;
  rarity: AchievementRarity;
  xpReward: number;
  active: boolean;
  earnedCount: number;
  createdAt: string;
}

export interface RankTierConfig {
  id: string;
  tierName: string;
  minXp: number;
  iconUrl: string;
  userCount: number;
}

export interface StreakMilestoneConfig {
  id: string;
  minStreak: number;
  streakName: string;
  iconUrl: string;
}

export interface XpBoost {
  id: string;
  name: string;
  multiplier: number;
  startsAt: string;
  endsAt: string;
  status: "SCHEDULED" | "ACTIVE" | "ENDED";
  createdBy: string | null;
}

// ── Leaderboard ──

export interface LeaderboardBoard {
  date: string;
  scope: "COUNTRY" | "PROVINCE";
  scopeId: string;
  total: number;
  page: number;
  size: number;
  rows: {
    rank: number;
    userId: string;
    username: string;
    displayName: string | null;
    score: number;
    tier: string;
  }[];
}

export interface LeaderboardMeta {
  dates: string[];
  scopes: { scope: "COUNTRY" | "PROVINCE"; scopeId: string; label: string; users: number }[];
}

// ── Roadmap ──

export interface RoadmapUnit {
  id: string;
  categoryId: string;
  title: string;
  description: string | null;
  position: number;
  lessonCount: number;
  updatedAt: string;
}

// ── Platform ──

export interface AppSettings {
  "xp.rules": { dailyLessonXpCap: number };
  "app.flags": {
    maintenanceMode: boolean;
    maintenanceMessage: string;
    minAppVersion: string;
    latestAppVersion: string;
  };
  "ai.chat": { enabled: boolean; dailyMessageLimit: number; systemPrompt: string };
  "moderation.bannedWords": { words: string[] };
}

export interface MediaUploadResponse {
  id: string;
  url: string;
  width: number | null;
  height: number | null;
}

export interface Announcement {
  id: string;
  title: string;
  body: string;
  startsAt: string;
  endsAt: string | null;
  status: "SCHEDULED" | "LIVE" | "ENDED";
  updatedAt: string;
}

export interface ChatUsage {
  days: number;
  totalMessages: number;
  activeUsers: number;
  dailyLimit: number;
  enabled: boolean;
  topUsers: {
    userId: string;
    username: string;
    displayName: string | null;
    messages: number;
    sessions: number;
    lastMessageAt: string;
  }[];
}

export interface ChatSessionSummary {
  id: string;
  userId: string;
  contextSummary: string | null;
  messageCount: number;
  startedAt: string;
  updatedAt: string;
}

export interface ChatSessionDetail {
  session: ChatSessionSummary;
  messages: { id: string; role: "user" | "assistant"; content: string; createdAt: string }[];
}
