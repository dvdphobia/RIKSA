import {
  BookOpen,
  FileText,
  Flag,
  LayoutDashboard,
  ListOrdered,
  Medal,
  ScrollText,
  Settings2,
  ShoppingBag,
  Shield,
  Tags,
  Trophy,
  Users,
} from "lucide-react";
import type { ReactNode } from "react";

export interface NavItem {
  path: string;
  label: string;
  icon: ReactNode;
  section: string;
  adminOnly?: boolean;
}

// Admin-only: the XP economy and people/platform controls. Moderators handle content.
export const NAV_ITEMS: NavItem[] = [
  { section: "", path: "/", label: "Dashboard", icon: <LayoutDashboard size={16} /> },
  { section: "Learning", path: "/lessons", label: "Lessons", icon: <BookOpen size={16} /> },
  { section: "Learning", path: "/resources", label: "Resources", icon: <FileText size={16} /> },
  { section: "Learning", path: "/roadmap", label: "Roadmap", icon: <ListOrdered size={16} /> },
  { section: "Learning", path: "/categories", label: "Categories", icon: <Tags size={16} /> },
  { section: "Community", path: "/reports", label: "Reports", icon: <Flag size={16} /> },
  { section: "Community", path: "/blog", label: "Blog queue", icon: <FileText size={16} /> },
  { section: "Community", path: "/moderation", label: "Content", icon: <Shield size={16} /> },
  { section: "Economy", path: "/gamification", label: "Gamification", icon: <Medal size={16} />, adminOnly: true },
  { section: "Economy", path: "/leaderboard", label: "Leaderboard", icon: <Trophy size={16} />, adminOnly: true },
  { section: "Economy", path: "/market", label: "Market items", icon: <ShoppingBag size={16} />, adminOnly: true },
  { section: "Admin", path: "/users", label: "Users", icon: <Users size={16} />, adminOnly: true },
  { section: "Admin", path: "/platform", label: "Platform", icon: <Settings2 size={16} />, adminOnly: true },
  { section: "Admin", path: "/audit-logs", label: "Audit logs", icon: <ScrollText size={16} />, adminOnly: true },
];
