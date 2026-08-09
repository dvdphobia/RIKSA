import type { ReactNode } from "react";
import { Badge } from "./ui/badge";

const STATUS_VARIANTS: Record<
  string,
  "default" | "secondary" | "destructive" | "outline"
> = {
  OPEN: "destructive",
  RESOLVED: "default",
  DISMISSED: "secondary",
  PENDING: "outline",
  APPROVED: "default",
  REJECTED: "destructive",
  HIDDEN: "destructive",
  VISIBLE: "default",
  EASY: "default",
  MEDIUM: "secondary",
  HARD: "outline",
  DIFFICULT: "destructive",
  ADMIN: "destructive",
  MODERATOR: "secondary",
  USER: "outline",
};

export const StatusBadge = ({ value }: { value: string }) => (
  <Badge variant={STATUS_VARIANTS[value] ?? "outline"}>{value}</Badge>
);

export const PageHeader = ({
  title,
  description,
  actions,
}: {
  title: string;
  description?: string;
  actions?: ReactNode;
}) => (
  <div className="mb-6 flex items-start justify-between">
    <div>
      <h1 className="text-2xl font-semibold tracking-tight">{title}</h1>
      {description && (
        <p className="text-sm text-muted-foreground">{description}</p>
      )}
    </div>
    {actions && <div className="flex gap-2">{actions}</div>}
  </div>
);

export const formatDate = (value: string | null | undefined) =>
  value ? new Date(value).toLocaleString() : "—";

export const isSuspended = (suspendedUntil: string | null) =>
  !!suspendedUntil && new Date(suspendedUntil).getTime() > Date.now();
