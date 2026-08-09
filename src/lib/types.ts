export type Role = "USER" | "MODERATOR" | "ADMIN";
export type DifficultyLevel = "EASY" | "MEDIUM" | "HARD" | "DIFFICULT";
export type ContentStatus = "VISIBLE" | "HIDDEN";
export type BlogStatus = "PENDING" | "APPROVED" | "REJECTED";
export type ReportStatus = "OPEN" | "RESOLVED" | "DISMISSED";
export type ReportAction = "NONE" | "HIDE";
export type ReportTargetType = "POST" | "COMMENT" | "BLOG";
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
  endCursor?: string;
}

export interface BlogPage {
  content: BlogPost[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
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
  emailVerified: boolean;
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
  difficulty: DifficultyLevel;
  points: number;
}

export interface Lesson {
  id: string;
  title: string;
  description: string | null;
  difficulty: DifficultyLevel;
  baseXp: number;
  categoryId: string | null;
  reflectionPrompt: string | null;
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
  viewCount: number;
  likeCount: number;
  categoryId: string | null;
  liked: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PostModeration {
  id: string;
  title: string;
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
  content: string;
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
  actorId: string;
  action: string;
  targetType: string;
  targetId: string;
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
