import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AlertTriangle, Pencil, Plus, Trash2 } from "lucide-react";
import { useEffect, useState } from "react";
import { Link } from "react-router";
import { toast } from "sonner";
import { apiFetch } from "../../lib/api";
import {
  ACHIEVEMENT_CRITERIA,
  type AchievementConfig,
  type AchievementCriteriaType,
  type AchievementRarity,
  type AppSettings,
  type Category,
  type RankTierConfig,
  type StreakMilestoneConfig,
  type XpAuditRow,
  type XpBoost,
} from "../../lib/types";
import {
  EmptyState,
  errorMessage,
  formatDate,
  fromLocalInput,
  PageHeader,
  StatusBadge,
  toLocalInput,
} from "../../components/shared";
import { Button } from "../../components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "../../components/ui/dialog";
import { Input } from "../../components/ui/input";
import { Label } from "../../components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../../components/ui/select";
import { Skeleton } from "../../components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "../../components/ui/table";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../../components/ui/tabs";
import { Textarea } from "../../components/ui/textarea";

const RARITIES: AchievementRarity[] = ["COMMON", "RARE", "EPIC", "LEGENDARY"];

/** Plain-language meaning of each criteria type and what its number means. */
const CRITERIA_HELP: Record<AchievementCriteriaType, { label: string; value?: string }> = {
  COMPLETE_COURSE: { label: "Complete any lesson" },
  COMPLETE_N_COURSES: { label: "Complete N lessons", value: "Lessons" },
  COMPLETE_COURSE_IN_EVERY_CATEGORY: { label: "A lesson in every category" },
  COMPLETE_ALL_IN_CATEGORY: { label: "Every lesson in one category" },
  COMPLETE_ALL_IN_EVERY_CATEGORY: { label: "Every lesson everywhere" },
  COMPLETE_COURSE_WITH_DIFFICULTY: { label: "Complete a lesson of a difficulty" },
  EARN_XP: { label: "Reach total XP", value: "XP" },
  MAINTAIN_STREAK: { label: "Reach a streak", value: "Days" },
  REACH_RANK: { label: "Reach a rank tier" },
  CREATE_POST: { label: "Create N posts", value: "Posts" },
  RECEIVE_REACTION: { label: "Receive N reactions", value: "Reactions" },
  WRITE_BLOG_POST: { label: "Publish N blog posts", value: "Posts" },
  RECEIVE_BLOG_LIKE: { label: "Receive N blog likes", value: "Likes" },
};

const invalidate = (qc: ReturnType<typeof useQueryClient>, key: string) =>
  qc.invalidateQueries({ queryKey: ["gamification", key] });

export const Gamification = () => (
  <div>
    <PageHeader
      title="Gamification"
      description="The XP economy: badges, ranks, streaks, XP rules and farming checks"
    />
    <Tabs defaultValue="achievements">
      <TabsList>
        <TabsTrigger value="achievements">Achievements</TabsTrigger>
        <TabsTrigger value="ranks">Rank tiers</TabsTrigger>
        <TabsTrigger value="streaks">Streak milestones</TabsTrigger>
        <TabsTrigger value="xp">XP rules & boosts</TabsTrigger>
        <TabsTrigger value="audit">XP audit</TabsTrigger>
      </TabsList>
      <TabsContent value="achievements">
        <Achievements />
      </TabsContent>
      <TabsContent value="ranks">
        <RankTiers />
      </TabsContent>
      <TabsContent value="streaks">
        <StreakMilestones />
      </TabsContent>
      <TabsContent value="xp">
        <XpRules />
        <Boosts />
      </TabsContent>
      <TabsContent value="audit">
        <XpAudit />
      </TabsContent>
    </Tabs>
  </div>
);

// ── Achievements ──

type AchievementDraft = Omit<AchievementConfig, "id" | "earnedCount" | "createdAt">;

const emptyAchievement = (): AchievementDraft => ({
  name: "",
  description: "",
  criteriaType: "COMPLETE_N_COURSES",
  criteriaValue: 1,
  requiresAdvanced: false,
  iconUrl: "",
  categoryId: null,
  rarity: "COMMON",
  xpReward: 0,
  active: true,
});

const Achievements = () => {
  const qc = useQueryClient();
  const [editing, setEditing] = useState<{ id: string | null; draft: AchievementDraft } | null>(
    null,
  );
  const [deleting, setDeleting] = useState<AchievementConfig | null>(null);

  const list = useQuery({
    queryKey: ["gamification", "achievements"],
    queryFn: () => apiFetch<AchievementConfig[]>("/admin/gamification/achievements"),
  });
  const categories = useQuery({
    queryKey: ["categories", "all"],
    queryFn: () => apiFetch<Category[]>("/admin/categories"),
  });

  const save = useMutation({
    mutationFn: ({ id, draft }: { id: string | null; draft: AchievementDraft }) =>
      apiFetch(id ? `/admin/gamification/achievements/${id}` : "/admin/gamification/achievements", {
        method: id ? "PUT" : "POST",
        body: draft,
      }),
    onSuccess: () => {
      toast.success("Achievement saved");
      setEditing(null);
      invalidate(qc, "achievements");
    },
    onError: (e) => toast.error(errorMessage(e)),
  });

  const toggle = useMutation({
    mutationFn: (a: AchievementConfig) =>
      apiFetch(`/admin/gamification/achievements/${a.id}`, {
        method: "PUT",
        body: { ...a, active: !a.active },
      }),
    onSuccess: () => invalidate(qc, "achievements"),
    onError: (e) => toast.error(errorMessage(e)),
  });

  const remove = useMutation({
    mutationFn: (id: string) =>
      apiFetch(`/admin/gamification/achievements/${id}`, { method: "DELETE" }),
    onSuccess: () => {
      toast.success("Achievement deleted");
      setDeleting(null);
      invalidate(qc, "achievements");
    },
    onError: (e) => toast.error(errorMessage(e)),
  });

  const draft = editing?.draft;
  const set = (patch: Partial<AchievementDraft>) =>
    setEditing((e) => (e ? { ...e, draft: { ...e.draft, ...patch } } : e));
  const valueLabel = draft ? CRITERIA_HELP[draft.criteriaType].value : undefined;

  return (
    <div className="space-y-3">
      <div className="flex justify-end">
        <Button onClick={() => setEditing({ id: null, draft: emptyAchievement() })}>
          <Plus className="mr-1 h-4 w-4" /> New achievement
        </Button>
      </div>
      {list.isLoading && <Skeleton className="h-40 w-full" />}
      <div className="rounded-md border bg-card">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Badge</TableHead>
              <TableHead>Earned by</TableHead>
              <TableHead>Reward</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {(list.data ?? []).map((a) => (
              <TableRow key={a.id} className={a.active ? "" : "opacity-60"}>
                <TableCell>
                  <div className="flex items-center gap-3">
                    <img src={a.iconUrl} alt="" className="h-9 w-9 rounded" />
                    <div>
                      <p className="font-medium">{a.name}</p>
                      <p className="text-xs text-muted-foreground">
                        {CRITERIA_HELP[a.criteriaType]?.label ?? a.criteriaType}
                        {a.criteriaValue != null && CRITERIA_HELP[a.criteriaType]?.value
                          ? ` · ${a.criteriaValue.toLocaleString()} ${CRITERIA_HELP[a.criteriaType].value?.toLowerCase()}`
                          : ""}
                      </p>
                    </div>
                  </div>
                </TableCell>
                <TableCell className="text-sm">{a.earnedCount.toLocaleString()} users</TableCell>
                <TableCell>
                  <div className="flex items-center gap-2 text-sm">
                    <StatusBadge value={a.rarity} />
                    {a.xpReward} XP
                  </div>
                </TableCell>
                <TableCell>
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={toggle.isPending}
                    onClick={() => toggle.mutate(a)}
                  >
                    {a.active ? "On" : "Off"}
                  </Button>
                </TableCell>
                <TableCell>
                  <div className="flex justify-end gap-1">
                    <Button
                      size="icon"
                      variant="ghost"
                      onClick={() => {
                        const { id, earnedCount: _e, createdAt: _c, ...rest } = a;
                        setEditing({ id, draft: rest });
                      }}
                    >
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button
                      size="icon"
                      variant="ghost"
                      disabled={a.earnedCount > 0}
                      title={
                        a.earnedCount > 0
                          ? "Already earned — turn it off instead"
                          : "Delete"
                      }
                      onClick={() => setDeleting(a)}
                    >
                      <Trash2 className="h-4 w-4 text-destructive" />
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>

      <Dialog open={!!editing} onOpenChange={(o) => !o && setEditing(null)}>
        <DialogContent className="max-w-xl">
          <DialogHeader>
            <DialogTitle>{editing?.id ? "Edit achievement" : "New achievement"}</DialogTitle>
          </DialogHeader>
          {draft && (
            <div className="grid gap-3">
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1">
                  <Label>Name *</Label>
                  <Input value={draft.name} onChange={(e) => set({ name: e.target.value })} />
                </div>
                <div className="space-y-1">
                  <Label>Icon URL *</Label>
                  <Input value={draft.iconUrl} onChange={(e) => set({ iconUrl: e.target.value })} />
                </div>
              </div>
              <div className="space-y-1">
                <Label>Description *</Label>
                <Textarea
                  rows={2}
                  value={draft.description}
                  onChange={(e) => set({ description: e.target.value })}
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1">
                  <Label>Earned when</Label>
                  <Select
                    value={draft.criteriaType}
                    onValueChange={(v) =>
                      set({ criteriaType: v as AchievementCriteriaType })
                    }
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {ACHIEVEMENT_CRITERIA.map((c) => (
                        <SelectItem key={c} value={c}>
                          {CRITERIA_HELP[c].label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                {valueLabel ? (
                  <div className="space-y-1">
                    <Label>{valueLabel} needed</Label>
                    <Input
                      type="number"
                      min={1}
                      value={draft.criteriaValue ?? ""}
                      onChange={(e) =>
                        set({ criteriaValue: e.target.value ? Number(e.target.value) : null })
                      }
                    />
                  </div>
                ) : draft.criteriaType === "COMPLETE_ALL_IN_CATEGORY" ? (
                  <div className="space-y-1">
                    <Label>Category</Label>
                    <Select
                      value={draft.categoryId ?? ""}
                      onValueChange={(v) => set({ categoryId: v })}
                    >
                      <SelectTrigger>
                        <SelectValue placeholder="Pick a category" />
                      </SelectTrigger>
                      <SelectContent>
                        {(categories.data ?? []).map((c) => (
                          <SelectItem key={c.id} value={c.id}>
                            {c.name}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                ) : (
                  <div className="space-y-1">
                    <Label>Criteria value</Label>
                    <Input
                      type="number"
                      min={0}
                      placeholder="Usually empty"
                      value={draft.criteriaValue ?? ""}
                      onChange={(e) =>
                        set({ criteriaValue: e.target.value ? Number(e.target.value) : null })
                      }
                    />
                  </div>
                )}
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1">
                  <Label>Rarity</Label>
                  <Select
                    value={draft.rarity}
                    onValueChange={(v) => set({ rarity: v as AchievementRarity })}
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {RARITIES.map((r) => (
                        <SelectItem key={r} value={r}>
                          {r}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-1">
                  <Label>XP reward</Label>
                  <Input
                    type="number"
                    min={0}
                    value={draft.xpReward}
                    onChange={(e) => set({ xpReward: Math.max(0, Number(e.target.value)) })}
                  />
                </div>
              </div>
              <label className="flex items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  checked={draft.active}
                  onChange={(e) => set({ active: e.target.checked })}
                />
                Active — can be earned and shows in the app
              </label>
            </div>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setEditing(null)}>
              Cancel
            </Button>
            <Button
              disabled={
                save.isPending ||
                !draft?.name.trim() ||
                !draft?.description.trim() ||
                !draft?.iconUrl.trim()
              }
              onClick={() => editing && save.mutate(editing)}
            >
              {save.isPending ? "Saving..." : "Save"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={!!deleting} onOpenChange={(o) => !o && setDeleting(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Delete "{deleting?.name}"?</DialogTitle>
            <DialogDescription>Nobody has earned it yet. This can't be undone.</DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleting(null)}>
              Cancel
            </Button>
            <Button
              variant="destructive"
              disabled={remove.isPending}
              onClick={() => deleting && remove.mutate(deleting.id)}
            >
              Delete
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};

// ── Rank tiers ──

const RankTiers = () => {
  const qc = useQueryClient();
  const [edits, setEdits] = useState<Record<string, { minXp: number; iconUrl: string }>>({});
  const tiers = useQuery({
    queryKey: ["gamification", "rank-tiers"],
    queryFn: () => apiFetch<RankTierConfig[]>("/admin/gamification/rank-tiers"),
  });
  const save = useMutation({
    mutationFn: ({ id, ...body }: { id: string; minXp: number; iconUrl: string }) =>
      apiFetch<RankTierConfig[]>(`/admin/gamification/rank-tiers/${id}`, { method: "PUT", body }),
    onSuccess: (data, vars) => {
      toast.success("Tier saved");
      qc.setQueryData(["gamification", "rank-tiers"], data);
      setEdits(({ [vars.id]: _, ...rest }) => rest);
    },
    onError: (e) => toast.error(errorMessage(e)),
  });

  if (tiers.isLoading) return <Skeleton className="h-40 w-full" />;
  return (
    <div className="space-y-3">
      <p className="text-sm text-muted-foreground">
        Tier names are fixed. Thresholds must rise tier by tier and the first
        tier starts at 0 XP. Changes apply to everyone immediately.
      </p>
      <div className="rounded-md border bg-card">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Tier</TableHead>
              <TableHead>Starts at XP</TableHead>
              <TableHead>Icon URL</TableHead>
              <TableHead>Users now</TableHead>
              <TableHead />
            </TableRow>
          </TableHeader>
          <TableBody>
            {(tiers.data ?? []).map((t) => {
              const e = edits[t.id] ?? { minXp: t.minXp, iconUrl: t.iconUrl };
              const changed = e.minXp !== t.minXp || e.iconUrl !== t.iconUrl;
              return (
                <TableRow key={t.id}>
                  <TableCell>
                    <div className="flex items-center gap-2 font-medium">
                      <img src={t.iconUrl} alt="" className="h-7 w-7" />
                      {t.tierName}
                    </div>
                  </TableCell>
                  <TableCell>
                    <Input
                      type="number"
                      min={0}
                      className="w-32"
                      value={e.minXp}
                      onChange={(ev) =>
                        setEdits((s) => ({ ...s, [t.id]: { ...e, minXp: Number(ev.target.value) } }))
                      }
                    />
                  </TableCell>
                  <TableCell>
                    <Input
                      value={e.iconUrl}
                      onChange={(ev) =>
                        setEdits((s) => ({ ...s, [t.id]: { ...e, iconUrl: ev.target.value } }))
                      }
                    />
                  </TableCell>
                  <TableCell className="text-sm">{t.userCount.toLocaleString()}</TableCell>
                  <TableCell>
                    <Button
                      size="sm"
                      disabled={!changed || save.isPending}
                      onClick={() => save.mutate({ id: t.id, ...e })}
                    >
                      Save
                    </Button>
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </div>
    </div>
  );
};

// ── Streak milestones ──

const StreakMilestones = () => {
  const qc = useQueryClient();
  const [editing, setEditing] = useState<
    { id: string | null; minStreak: number; streakName: string; iconUrl: string } | null
  >(null);
  const list = useQuery({
    queryKey: ["gamification", "streak-milestones"],
    queryFn: () => apiFetch<StreakMilestoneConfig[]>("/admin/gamification/streak-milestones"),
  });
  const onSaved = (data: StreakMilestoneConfig[]) => {
    qc.setQueryData(["gamification", "streak-milestones"], data);
    setEditing(null);
  };
  const save = useMutation({
    mutationFn: ({ id, ...body }: NonNullable<typeof editing>) =>
      apiFetch<StreakMilestoneConfig[]>(
        id ? `/admin/gamification/streak-milestones/${id}` : "/admin/gamification/streak-milestones",
        { method: id ? "PUT" : "POST", body },
      ),
    onSuccess: (data) => {
      toast.success("Milestone saved");
      onSaved(data);
    },
    onError: (e) => toast.error(errorMessage(e)),
  });
  const remove = useMutation({
    mutationFn: (id: string) =>
      apiFetch<StreakMilestoneConfig[]>(`/admin/gamification/streak-milestones/${id}`, {
        method: "DELETE",
      }),
    onSuccess: (data) => {
      toast.success("Milestone deleted");
      onSaved(data);
    },
    onError: (e) => toast.error(errorMessage(e)),
  });

  return (
    <div className="space-y-3">
      <div className="flex justify-end">
        <Button onClick={() => setEditing({ id: null, minStreak: 1, streakName: "", iconUrl: "" })}>
          <Plus className="mr-1 h-4 w-4" /> New milestone
        </Button>
      </div>
      {list.isLoading && <Skeleton className="h-32 w-full" />}
      <div className="rounded-md border bg-card">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>From day</TableHead>
              <TableHead>Name</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {(list.data ?? []).map((m) => (
              <TableRow key={m.id}>
                <TableCell className="font-mono">{m.minStreak}</TableCell>
                <TableCell>
                  <div className="flex items-center gap-2">
                    <img src={m.iconUrl} alt="" className="h-6 w-6" />
                    {m.streakName}
                  </div>
                </TableCell>
                <TableCell>
                  <div className="flex justify-end gap-1">
                    <Button size="icon" variant="ghost" onClick={() => setEditing({ ...m })}>
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button
                      size="icon"
                      variant="ghost"
                      disabled={remove.isPending}
                      onClick={() => remove.mutate(m.id)}
                    >
                      <Trash2 className="h-4 w-4 text-destructive" />
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
      <Dialog open={!!editing} onOpenChange={(o) => !o && setEditing(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{editing?.id ? "Edit milestone" : "New milestone"}</DialogTitle>
          </DialogHeader>
          {editing && (
            <div className="grid gap-3">
              <div className="space-y-1">
                <Label>Streak days</Label>
                <Input
                  type="number"
                  min={1}
                  value={editing.minStreak}
                  onChange={(e) => setEditing({ ...editing, minStreak: Number(e.target.value) })}
                />
              </div>
              <div className="space-y-1">
                <Label>Name</Label>
                <Input
                  value={editing.streakName}
                  onChange={(e) => setEditing({ ...editing, streakName: e.target.value })}
                />
              </div>
              <div className="space-y-1">
                <Label>Icon URL</Label>
                <Input
                  value={editing.iconUrl}
                  onChange={(e) => setEditing({ ...editing, iconUrl: e.target.value })}
                />
              </div>
            </div>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setEditing(null)}>
              Cancel
            </Button>
            <Button
              disabled={
                save.isPending ||
                !editing?.streakName.trim() ||
                !editing?.iconUrl.trim() ||
                (editing?.minStreak ?? 0) < 1
              }
              onClick={() => editing && save.mutate(editing)}
            >
              Save
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};

// ── XP rules (settings) ──

const XpRules = () => {
  const qc = useQueryClient();
  const settings = useQuery({
    queryKey: ["settings"],
    queryFn: () => apiFetch<AppSettings>("/admin/settings"),
  });
  const [cap, setCap] = useState(0);
  const current = settings.data?.["xp.rules"].dailyLessonXpCap ?? 0;
  useEffect(() => setCap(current), [current]);

  const save = useMutation({
    mutationFn: () =>
      apiFetch("/admin/settings/xp.rules", { method: "PUT", body: { dailyLessonXpCap: cap } }),
    onSuccess: () => {
      toast.success("XP rules saved");
      qc.invalidateQueries({ queryKey: ["settings"] });
    },
    onError: (e) => toast.error(errorMessage(e)),
  });

  return (
    <div className="mb-6 rounded-md border bg-card p-4">
      <h3 className="font-medium">XP rules</h3>
      <p className="mb-3 text-sm text-muted-foreground">
        Lesson XP comes from each lesson's base XP. Posts, comments and likes
        don't award XP today. Achievement rewards are set per badge.
      </p>
      <div className="flex items-end gap-3">
        <div className="space-y-1">
          <Label>Daily lesson XP cap per user</Label>
          <Input
            type="number"
            min={0}
            className="w-40"
            value={cap}
            onChange={(e) => setCap(Math.max(0, Math.floor(Number(e.target.value))))}
          />
        </div>
        <Button disabled={cap === current || save.isPending} onClick={() => save.mutate()}>
          Save
        </Button>
        <p className="pb-2 text-xs text-muted-foreground">
          0 = no cap. Applied after boosts; Cambodia time.
        </p>
      </div>
    </div>
  );
};

// ── Boost events ──

const Boosts = () => {
  const qc = useQueryClient();
  const [creating, setCreating] = useState<{
    name: string;
    multiplier: number;
    startsAt: string;
    endsAt: string;
  } | null>(null);
  const list = useQuery({
    queryKey: ["gamification", "boosts"],
    queryFn: () => apiFetch<XpBoost[]>("/admin/gamification/xp-boosts"),
  });
  const create = useMutation({
    mutationFn: (b: NonNullable<typeof creating>) =>
      apiFetch("/admin/gamification/xp-boosts", {
        method: "POST",
        body: {
          name: b.name,
          multiplier: b.multiplier,
          startsAt: fromLocalInput(b.startsAt),
          endsAt: fromLocalInput(b.endsAt),
        },
      }),
    onSuccess: () => {
      toast.success("Boost scheduled");
      setCreating(null);
      invalidate(qc, "boosts");
    },
    onError: (e) => toast.error(errorMessage(e)),
  });
  const end = useMutation({
    mutationFn: (id: string) => apiFetch(`/admin/gamification/xp-boosts/${id}`, { method: "DELETE" }),
    onSuccess: () => invalidate(qc, "boosts"),
    onError: (e) => toast.error(errorMessage(e)),
  });

  const weekend = () => {
    const now = new Date();
    const start = new Date(now);
    start.setDate(now.getDate() + ((6 - now.getDay() + 7) % 7));
    start.setHours(0, 0, 0, 0);
    const endAt = new Date(start);
    endAt.setDate(start.getDate() + 2);
    return {
      startsAt: toLocalInput(start.toISOString()),
      endsAt: toLocalInput(endAt.toISOString()),
    };
  };

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <div>
          <h3 className="font-medium">XP boost events</h3>
          <p className="text-sm text-muted-foreground">
            Multiplies lesson XP while active. The app shows the active boost.
          </p>
        </div>
        <Button
          onClick={() => setCreating({ name: "2× XP weekend", multiplier: 2, ...weekend() })}
        >
          <Plus className="mr-1 h-4 w-4" /> New boost
        </Button>
      </div>
      {list.data?.length === 0 && <EmptyState>No boosts yet</EmptyState>}
      {!!list.data?.length && (
        <div className="rounded-md border bg-card">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Name</TableHead>
                <TableHead>Multiplier</TableHead>
                <TableHead>Window</TableHead>
                <TableHead>Status</TableHead>
                <TableHead />
              </TableRow>
            </TableHeader>
            <TableBody>
              {list.data.map((b) => (
                <TableRow key={b.id}>
                  <TableCell className="font-medium">{b.name}</TableCell>
                  <TableCell>×{Number(b.multiplier)}</TableCell>
                  <TableCell className="text-sm">
                    {formatDate(b.startsAt)} → {formatDate(b.endsAt)}
                  </TableCell>
                  <TableCell>
                    <StatusBadge value={b.status} />
                  </TableCell>
                  <TableCell className="text-right">
                    {b.status !== "ENDED" && (
                      <Button
                        size="sm"
                        variant="outline"
                        disabled={end.isPending}
                        onClick={() => end.mutate(b.id)}
                      >
                        {b.status === "ACTIVE" ? "End now" : "Cancel"}
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
      <Dialog open={!!creating} onOpenChange={(o) => !o && setCreating(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>New XP boost</DialogTitle>
          </DialogHeader>
          {creating && (
            <div className="grid gap-3">
              <div className="space-y-1">
                <Label>Name</Label>
                <Input
                  value={creating.name}
                  onChange={(e) => setCreating({ ...creating, name: e.target.value })}
                />
              </div>
              <div className="space-y-1">
                <Label>Multiplier</Label>
                <Input
                  type="number"
                  min={1}
                  max={10}
                  step={0.25}
                  value={creating.multiplier}
                  onChange={(e) => setCreating({ ...creating, multiplier: Number(e.target.value) })}
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1">
                  <Label>Starts</Label>
                  <Input
                    type="datetime-local"
                    value={creating.startsAt}
                    onChange={(e) => setCreating({ ...creating, startsAt: e.target.value })}
                  />
                </div>
                <div className="space-y-1">
                  <Label>Ends</Label>
                  <Input
                    type="datetime-local"
                    value={creating.endsAt}
                    onChange={(e) => setCreating({ ...creating, endsAt: e.target.value })}
                  />
                </div>
              </div>
            </div>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setCreating(null)}>
              Cancel
            </Button>
            <Button
              disabled={
                create.isPending ||
                !creating?.name.trim() ||
                !creating?.startsAt ||
                !creating?.endsAt ||
                (creating?.multiplier ?? 0) < 1
              }
              onClick={() => creating && create.mutate(creating)}
            >
              Schedule
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};

// ── XP audit ──

const XpAudit = () => {
  const [hours, setHours] = useState(24);
  const [minXp, setMinXp] = useState(0);
  const audit = useQuery({
    queryKey: ["gamification", "xp-audit", hours, minXp],
    queryFn: () =>
      apiFetch<XpAuditRow[]>("/admin/gamification/xp-audit", {
        params: { hours, minXp, limit: 100 },
      }),
  });
  const rows = audit.data ?? [];
  // Flag anyone far above the typical gainer in this window.
  const sorted = [...rows].map((r) => r.xpGained).sort((a, b) => a - b);
  const median = sorted.length ? sorted[Math.floor(sorted.length / 2)] : 0;
  const suspicious = (r: XpAuditRow) => median > 0 && r.xpGained >= median * 5 && rows.length >= 5;

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-end gap-3">
        <div className="space-y-1">
          <Label>Window</Label>
          <Select value={String(hours)} onValueChange={(v) => setHours(Number(v))}>
            <SelectTrigger className="w-40">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="1">Last hour</SelectItem>
              <SelectItem value="24">Last 24 hours</SelectItem>
              <SelectItem value="168">Last 7 days</SelectItem>
              <SelectItem value="720">Last 30 days</SelectItem>
            </SelectContent>
          </Select>
        </div>
        <div className="space-y-1">
          <Label>Min XP gained</Label>
          <Input
            type="number"
            min={0}
            className="w-32"
            value={minXp}
            onChange={(e) => setMinXp(Math.max(0, Number(e.target.value)))}
          />
        </div>
        <p className="pb-2 text-xs text-muted-foreground">
          Staff adjustments are excluded. <AlertTriangle className="inline h-3 w-3" /> marks
          users gaining 5× the median.
        </p>
      </div>
      {audit.isLoading && <Skeleton className="h-40 w-full" />}
      {!audit.isLoading && rows.length === 0 && <EmptyState>No XP gained in this window</EmptyState>}
      {rows.length > 0 && (
        <div className="rounded-md border bg-card">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>User</TableHead>
                <TableHead className="text-right">XP gained</TableHead>
                <TableHead className="text-right">Lessons</TableHead>
                <TableHead className="text-right">Events</TableHead>
                <TableHead>Active between</TableHead>
                <TableHead className="text-right">Total XP</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((r) => (
                <TableRow key={r.userId}>
                  <TableCell>
                    <div className="flex items-center gap-2">
                      {suspicious(r) && <AlertTriangle className="h-4 w-4 text-destructive" />}
                      <Link className="font-medium hover:underline" to={`/users/${r.userId}`}>
                        {r.username}
                      </Link>
                      {r.suspended && <StatusBadge value="SUSPENDED" />}
                      {r.leaderboardExcluded && (
                        <span className="text-xs text-muted-foreground">hidden</span>
                      )}
                    </div>
                  </TableCell>
                  <TableCell className="text-right font-mono">{r.xpGained.toLocaleString()}</TableCell>
                  <TableCell className="text-right font-mono">{r.lessonCompletions}</TableCell>
                  <TableCell className="text-right font-mono">{r.transactionCount}</TableCell>
                  <TableCell className="text-xs text-muted-foreground">
                    {formatDate(r.firstAt)} → {formatDate(r.lastAt)}
                  </TableCell>
                  <TableCell className="text-right font-mono">{r.totalXp.toLocaleString()}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </div>
  );
};
