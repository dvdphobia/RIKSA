import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import {
  ChevronLeft,
  Flame,
  Unlock,
  MessageSquare,
  Minus,
  Plus,
  Trophy,
  Zap,
} from "lucide-react";
import { type ReactNode, useMemo, useState } from "react";
import { Link, useParams } from "react-router";
import { toast } from "sonner";
import { apiFetch } from "../../lib/api";
import type {
  ChatSessionDetail,
  ChatSessionSummary,
  UserOverview,
  XpTransaction,
} from "../../lib/types";
import { useServerTable } from "../../lib/use-server-table";
import {
  EmptyState,
  errorMessage,
  formatDate,
  isSuspended,
  StatusBadge,
} from "../../components/shared";
import { ReasonDialog } from "../../components/reason-dialog";
import { DataTable } from "../../components/table/data-table";
import { Badge } from "../../components/ui/badge";
import { Button } from "../../components/ui/button";
import { Card, CardContent } from "../../components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "../../components/ui/dialog";
import { Input } from "../../components/ui/input";
import { Label } from "../../components/ui/label";
import { Skeleton } from "../../components/ui/skeleton";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../../components/ui/tabs";

type Dialogs =
  | { kind: "xp" }
  | { kind: "streak" }
  | { kind: "grant"; achievementId: string; name: string }
  | { kind: "revoke"; achievementId: string; name: string }
  | { kind: "leaderboard"; exclude: boolean }
  | { kind: "chat"; sessionId: string }
  | null;

export const UserDetail = () => {
  const { id = "" } = useParams();
  const queryClient = useQueryClient();
  const [dialog, setDialog] = useState<Dialogs>(null);
  const [xpAmount, setXpAmount] = useState(0);
  const [xpSign, setXpSign] = useState<1 | -1>(1);
  const [streak, setStreak] = useState(0);
  const [transcript, setTranscript] = useState<ChatSessionDetail | null>(null);

  const overview = useQuery({
    queryKey: ["users", "overview", id],
    queryFn: () => apiFetch<UserOverview>(`/admin/users/${id}/overview`),
  });

  // Every write returns a fresh overview; also refresh the XP ledger.
  const onWrite = (message: string) => (data: UserOverview) => {
    toast.success(message);
    queryClient.setQueryData(["users", "overview", id], data);
    queryClient.invalidateQueries({ queryKey: ["user-xp"] });
    queryClient.invalidateQueries({ queryKey: ["users", "list"] });
    setDialog(null);
  };
  const onError = (e: Error) => toast.error(errorMessage(e));

  const adjustXp = useMutation({
    mutationFn: (body: { amount: number; reason: string }) =>
      apiFetch<UserOverview>(`/admin/users/${id}/xp`, { method: "POST", body }),
    onSuccess: onWrite("XP updated"),
    onError,
  });
  const restoreStreak = useMutation({
    mutationFn: (body: { currentStreak: number; reason: string }) =>
      apiFetch<UserOverview>(`/admin/users/${id}/streak`, { method: "POST", body }),
    onSuccess: onWrite("Streak updated"),
    onError,
  });
  const grant = useMutation({
    mutationFn: ({ achievementId, reason }: { achievementId: string; reason: string }) =>
      apiFetch<UserOverview>(`/admin/users/${id}/achievements/${achievementId}`, {
        method: "POST",
        body: { reason },
      }),
    onSuccess: onWrite("Achievement granted"),
    onError,
  });
  const revoke = useMutation({
    mutationFn: ({ achievementId, reason }: { achievementId: string; reason: string }) =>
      apiFetch<UserOverview>(`/admin/users/${id}/achievements/${achievementId}`, {
        method: "DELETE",
        params: { reason },
      }),
    onSuccess: onWrite("Achievement revoked"),
    onError,
  });
  const unlock = useMutation({
    mutationFn: () =>
      apiFetch<UserOverview>(`/admin/users/${id}/lock`, { method: "DELETE" }),
    onSuccess: onWrite("Account unlocked"),
    onError,
  });
  const leaderboard = useMutation({
    mutationFn: ({ excluded, reason }: { excluded: boolean; reason: string }) =>
      apiFetch<UserOverview>(`/admin/users/${id}/leaderboard-exclusion`, {
        method: "PUT",
        params: { excluded, reason },
      }),
    onSuccess: onWrite("Leaderboard setting updated"),
    onError,
  });
  const openTranscript = useMutation({
    mutationFn: ({ sessionId, reason }: { sessionId: string; reason: string }) =>
      apiFetch<ChatSessionDetail>(`/admin/ai-chat/sessions/${sessionId}`, {
        params: { reason },
      }),
    onSuccess: (data) => {
      setDialog(null);
      setTranscript(data);
    },
    onError,
  });

  if (overview.isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-12 w-72" />
        <Skeleton className="h-28 w-full" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }
  if (overview.isError || !overview.data) {
    return <EmptyState>{errorMessage(overview.error, "User not found")}</EmptyState>;
  }

  const o = overview.data;
  const u = o.user;
  const g = o.gamification;
  const suspended = isSuspended(u.suspendedUntil);
  const locked =
    (!!u.lockedUntil && new Date(u.lockedUntil).getTime() > Date.now()) ||
    o.failedLoginAttempts > 0;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="flex items-start gap-3">
          <Button variant="ghost" size="icon" asChild>
            <Link to="/users" aria-label="Back to users">
              <ChevronLeft className="h-4 w-4" />
            </Link>
          </Button>
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <h1 className="text-2xl font-semibold tracking-tight">{u.username}</h1>
              <StatusBadge value={u.role} />
              {suspended && <StatusBadge value="SUSPENDED" />}
              {o.leaderboardExcluded && (
                <Badge variant="outline">Hidden from leaderboard</Badge>
              )}
            </div>
            <p className="text-sm text-muted-foreground">
              {u.displayName ?? "—"} · {u.email ?? "no email"} · joined{" "}
              {formatDate(u.createdAt)}
            </p>
            {suspended && (
              <p className="mt-1 text-sm text-destructive">
                Suspended until {formatDate(u.suspendedUntil)}
                {u.suspensionReason ? ` — ${u.suspensionReason}` : ""}
              </p>
            )}
            {locked && (
              <p className="mt-1 text-sm text-muted-foreground">
                {o.failedLoginAttempts} failed sign-in attempt(s)
                {u.lockedUntil ? ` · locked until ${formatDate(u.lockedUntil)}` : ""}
              </p>
            )}
          </div>
        </div>
        <div className="flex flex-wrap gap-2">
          {locked && (
            <Button variant="outline" disabled={unlock.isPending} onClick={() => unlock.mutate()}>
              <Unlock className="mr-1 h-4 w-4" /> Unlock sign-in
            </Button>
          )}
          <Button
            variant="outline"
            onClick={() => setDialog({ kind: "leaderboard", exclude: !o.leaderboardExcluded })}
          >
            {o.leaderboardExcluded ? "Show on leaderboard" : "Hide from leaderboard"}
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-3 md:grid-cols-3 xl:grid-cols-5">
        <Stat
          icon={<Zap className="h-4 w-4" />}
          label="Total XP"
          value={g ? g.totalXp.toLocaleString() : "—"}
          action={
            g && (
              <Button
                size="sm"
                variant="ghost"
                onClick={() => {
                  setXpAmount(0);
                  setXpSign(1);
                  setDialog({ kind: "xp" });
                }}
              >
                Adjust
              </Button>
            )
          }
        />
        <Stat
          icon={<Flame className="h-4 w-4" />}
          label="Streak"
          value={g ? `${g.currentStreak} days` : "—"}
          sub={g ? `best ${g.highestStreak}` : undefined}
          action={
            g && (
              <Button
                size="sm"
                variant="ghost"
                onClick={() => {
                  setStreak(Math.max(g.currentStreak, 1));
                  setDialog({ kind: "streak" });
                }}
              >
                Set
              </Button>
            )
          }
        />
        <Stat
          icon={<Trophy className="h-4 w-4" />}
          label="Rank tier"
          value={g?.tier ?? "—"}
          sub={`${o.achievements.filter((a) => a.earned).length} badges`}
        />
        <Stat label="Lessons completed" value={o.lessonsCompleted} />
        <Stat
          label="Reports"
          value={o.reportsAgainst.length}
          sub={`against · ${o.reportsFiled} filed`}
        />
      </div>

      <Tabs defaultValue="xp">
        <TabsList>
          <TabsTrigger value="xp">XP history</TabsTrigger>
          <TabsTrigger value="achievements">Achievements</TabsTrigger>
          <TabsTrigger value="lessons">Lessons</TabsTrigger>
          <TabsTrigger value="reports">Reports</TabsTrigger>
          <TabsTrigger value="chat">AI chat</TabsTrigger>
        </TabsList>
        <TabsContent value="xp">
          <XpHistory userId={id} />
        </TabsContent>
        <TabsContent value="achievements">
          <div className="grid gap-2 md:grid-cols-2">
            {o.achievements.map((a) => (
              <div
                key={a.id}
                className="flex items-center justify-between gap-3 rounded-md border bg-card p-3"
              >
                <div className="flex min-w-0 items-center gap-3">
                  <img
                    src={a.iconUrl}
                    alt=""
                    className={`h-9 w-9 rounded ${a.earned ? "" : "opacity-30 grayscale"}`}
                  />
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium">
                      {a.name}
                      {!a.active && (
                        <span className="ml-2 text-xs text-muted-foreground">(off)</span>
                      )}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {a.rarity} · {a.xpReward} XP
                      {a.earned ? ` · earned ${formatDate(a.unlockedAt)}` : ""}
                    </p>
                  </div>
                </div>
                {a.earned ? (
                  <Button
                    size="sm"
                    variant="ghost"
                    onClick={() =>
                      setDialog({ kind: "revoke", achievementId: a.id, name: a.name })
                    }
                  >
                    Revoke
                  </Button>
                ) : (
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() =>
                      setDialog({ kind: "grant", achievementId: a.id, name: a.name })
                    }
                  >
                    Grant
                  </Button>
                )}
              </div>
            ))}
          </div>
        </TabsContent>
        <TabsContent value="lessons">
          {o.recentCompletions.length === 0 ? (
            <EmptyState>No lessons completed yet</EmptyState>
          ) : (
            <div className="divide-y rounded-md border bg-card">
              {o.recentCompletions.map((c) => (
                <div key={`${c.lessonId}-${c.completedAt}`} className="flex justify-between p-3 text-sm">
                  <Link className="hover:underline" to={`/lessons/edit/${c.lessonId}`}>
                    {c.lessonTitle ?? "Deleted lesson"}
                  </Link>
                  <span className="text-muted-foreground">
                    +{c.xpEarned} XP · score {c.score} · {formatDate(c.completedAt)}
                  </span>
                </div>
              ))}
            </div>
          )}
        </TabsContent>
        <TabsContent value="reports">
          {o.reportsAgainst.length === 0 ? (
            <EmptyState>No reports against this user or their content</EmptyState>
          ) : (
            <div className="divide-y rounded-md border bg-card">
              {o.reportsAgainst.map((r) => (
                <div key={r.id} className="flex items-start justify-between gap-3 p-3 text-sm">
                  <div>
                    <div className="flex items-center gap-2">
                      <StatusBadge value={r.reason} />
                      <span className="text-muted-foreground">on their {r.targetType.toLowerCase()}</span>
                    </div>
                    {r.details && <p className="mt-1">{r.details}</p>}
                  </div>
                  <div className="text-right">
                    <StatusBadge value={r.status} />
                    <p className="mt-1 text-xs text-muted-foreground">{formatDate(r.createdAt)}</p>
                  </div>
                </div>
              ))}
            </div>
          )}
        </TabsContent>
        <TabsContent value="chat">
          <ChatSessions
            userId={id}
            onOpen={(sessionId) => setDialog({ kind: "chat", sessionId })}
          />
        </TabsContent>
      </Tabs>

      <ReasonDialog
        open={dialog?.kind === "xp"}
        onOpenChange={(open) => !open && setDialog(null)}
        title="Adjust XP"
        description="Adds a ledger entry. Removals can't take XP below zero. Ranks and achievements update automatically."
        confirmLabel={`${xpSign > 0 ? "Grant" : "Remove"} ${xpAmount || 0} XP`}
        destructive={xpSign < 0}
        canConfirm={xpAmount > 0}
        pending={adjustXp.isPending}
        onConfirm={(reason) => adjustXp.mutate({ amount: xpSign * xpAmount, reason })}
      >
        <div className="flex items-end gap-2">
          <div className="flex rounded-md border">
            <Button
              type="button"
              size="icon"
              variant={xpSign > 0 ? "default" : "ghost"}
              onClick={() => setXpSign(1)}
              aria-label="Grant"
            >
              <Plus className="h-4 w-4" />
            </Button>
            <Button
              type="button"
              size="icon"
              variant={xpSign < 0 ? "destructive" : "ghost"}
              onClick={() => setXpSign(-1)}
              aria-label="Remove"
            >
              <Minus className="h-4 w-4" />
            </Button>
          </div>
          <div className="flex-1 space-y-2">
            <Label>Amount</Label>
            <Input
              type="number"
              min={1}
              value={xpAmount || ""}
              onChange={(e) => setXpAmount(Math.max(0, Math.floor(Number(e.target.value))))}
            />
          </div>
        </div>
      </ReasonDialog>

      <ReasonDialog
        open={dialog?.kind === "streak"}
        onOpenChange={(open) => !open && setDialog(null)}
        title="Set streak"
        description="Use after an outage. The user keeps the streak by finishing a lesson today."
        confirmLabel="Save streak"
        pending={restoreStreak.isPending}
        onConfirm={(reason) => restoreStreak.mutate({ currentStreak: streak, reason })}
      >
        <div className="space-y-2">
          <Label>Current streak (days)</Label>
          <Input
            type="number"
            min={0}
            value={streak}
            onChange={(e) => setStreak(Math.max(0, Math.floor(Number(e.target.value))))}
          />
        </div>
      </ReasonDialog>

      <ReasonDialog
        open={dialog?.kind === "grant"}
        onOpenChange={(open) => !open && setDialog(null)}
        title={`Grant "${dialog?.kind === "grant" ? dialog.name : ""}"`}
        description="The badge is added without its XP reward. Use Adjust XP if they should get that too."
        confirmLabel="Grant"
        pending={grant.isPending}
        onConfirm={(reason) =>
          dialog?.kind === "grant" && grant.mutate({ achievementId: dialog.achievementId, reason })
        }
      />

      <ReasonDialog
        open={dialog?.kind === "revoke"}
        onOpenChange={(open) => !open && setDialog(null)}
        title={`Revoke "${dialog?.kind === "revoke" ? dialog.name : ""}"`}
        description="Removes the badge only. XP it rewarded stays unless you adjust it."
        confirmLabel="Revoke"
        destructive
        pending={revoke.isPending}
        onConfirm={(reason) =>
          dialog?.kind === "revoke" && revoke.mutate({ achievementId: dialog.achievementId, reason })
        }
      />

      <ReasonDialog
        open={dialog?.kind === "leaderboard"}
        onOpenChange={(open) => !open && setDialog(null)}
        title={
          dialog?.kind === "leaderboard" && dialog.exclude
            ? "Hide from leaderboard"
            : "Show on leaderboard"
        }
        description="Takes effect in the live ranking now and in snapshots from the next refresh."
        confirmLabel="Confirm"
        pending={leaderboard.isPending}
        onConfirm={(reason) =>
          dialog?.kind === "leaderboard" &&
          leaderboard.mutate({ excluded: dialog.exclude, reason })
        }
      />

      <ReasonDialog
        open={dialog?.kind === "chat"}
        onOpenChange={(open) => !open && setDialog(null)}
        title="Open AI chat transcript"
        description="Private conversation. Opening it is recorded in the audit log with your reason."
        confirmLabel="Open transcript"
        reasonPlaceholder="e.g. Investigating report about abusive use"
        pending={openTranscript.isPending}
        onConfirm={(reason) =>
          dialog?.kind === "chat" &&
          openTranscript.mutate({ sessionId: dialog.sessionId, reason })
        }
      />

      <Dialog open={!!transcript} onOpenChange={(open) => !open && setTranscript(null)}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>AI chat transcript</DialogTitle>
          </DialogHeader>
          <div className="max-h-[60vh] space-y-3 overflow-y-auto">
            {transcript?.messages.map((m) => (
              <div
                key={m.id}
                className={`rounded-md p-3 text-sm ${
                  m.role === "user" ? "bg-muted" : "border"
                }`}
              >
                <p className="mb-1 text-xs text-muted-foreground">
                  {m.role === "user" ? "Learner" : "Tutor"} · {formatDate(m.createdAt)}
                </p>
                <p className="whitespace-pre-wrap">{m.content}</p>
              </div>
            ))}
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
};

const Stat = ({
  icon,
  label,
  value,
  sub,
  action,
}: {
  icon?: ReactNode;
  label: string;
  value: ReactNode;
  sub?: string;
  action?: ReactNode;
}) => (
  <Card>
    <CardContent className="p-4">
      <div className="flex items-center justify-between">
        <p className="flex items-center gap-1 text-xs text-muted-foreground">
          {icon}
          {label}
        </p>
        {action}
      </div>
      <p className="mt-1 text-xl font-semibold">{value}</p>
      {sub && <p className="text-xs text-muted-foreground">{sub}</p>}
    </CardContent>
  </Card>
);

const XpHistory = ({ userId }: { userId: string }) => {
  const columns = useMemo<ColumnDef<XpTransaction>[]>(
    () => [
      {
        id: "createdAt",
        header: "When",
        cell: ({ row }) => <span className="text-sm">{formatDate(row.original.createdAt)}</span>,
      },
      {
        id: "source",
        header: "Source",
        cell: ({ row }) => <Badge variant="outline">{row.original.sourceType}</Badge>,
      },
      {
        id: "amount",
        header: () => <div className="text-right">Amount</div>,
        cell: ({ row }) => (
          <div
            className={`text-right font-mono text-sm ${
              row.original.amount < 0 ? "text-destructive" : ""
            }`}
          >
            {row.original.amount > 0 ? "+" : ""}
            {row.original.amount}
          </div>
        ),
      },
      {
        id: "balance",
        header: () => <div className="text-right">Balance</div>,
        cell: ({ row }) => (
          <div className="text-right font-mono text-sm">
            {row.original.runningBalance.toLocaleString()}
          </div>
        ),
      },
    ],
    [],
  );
  const table = useServerTable<XpTransaction>({
    resource: "user-xp",
    path: `/admin/users/${userId}/xp`,
    columns,
    pageSize: 20,
  });
  return (
    <div className="rounded-md border bg-card">
      <DataTable tableProps={table} />
    </div>
  );
};

const ChatSessions = ({
  userId,
  onOpen,
}: {
  userId: string;
  onOpen: (sessionId: string) => void;
}) => {
  const sessions = useQuery({
    queryKey: ["ai-chat", "sessions", userId],
    queryFn: () =>
      apiFetch<ChatSessionSummary[]>(`/admin/ai-chat/users/${userId}/sessions`),
  });
  if (sessions.isLoading) return <Skeleton className="h-24 w-full" />;
  if (!sessions.data?.length) return <EmptyState>No AI chat sessions</EmptyState>;
  return (
    <div className="divide-y rounded-md border bg-card">
      {sessions.data.map((s) => (
        <div key={s.id} className="flex items-center justify-between gap-3 p-3 text-sm">
          <div className="min-w-0">
            <p className="truncate">{s.contextSummary ?? "General tutoring"}</p>
            <p className="text-xs text-muted-foreground">
              {s.messageCount} messages · last active {formatDate(s.updatedAt)}
            </p>
          </div>
          <Button size="sm" variant="outline" onClick={() => onOpen(s.id)}>
            <MessageSquare className="mr-1 h-4 w-4" /> Open
          </Button>
        </div>
      ))}
    </div>
  );
};
