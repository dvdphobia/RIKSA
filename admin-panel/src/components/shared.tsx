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
  SCHEDULED: "outline",
  ACTIVE: "default",
  LIVE: "default",
  ENDED: "secondary",
  SUSPENDED: "destructive",
  COMMON: "secondary",
  RARE: "default",
  EPIC: "default",
  LEGENDARY: "destructive",
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

export const errorMessage = (e: unknown, fallback = "Something went wrong") =>
  e instanceof Error && e.message ? e.message : fallback;

/** ISO string → value for <input type="datetime-local"> in the viewer's timezone. */
export const toLocalInput = (iso: string | null | undefined) => {
  if (!iso) return "";
  const d = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
};

/** <input type="datetime-local"> value → ISO string (or null when empty). */
export const fromLocalInput = (value: string) =>
  value ? new Date(value).toISOString() : null;

export const EmptyState = ({ children }: { children: ReactNode }) => (
  <p className="py-12 text-center text-sm text-muted-foreground">{children}</p>
);
