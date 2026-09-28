import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Pencil, Plus, Trash2, X } from "lucide-react";
import { type ReactNode, useEffect, useState } from "react";
import { Link } from "react-router";
import { toast } from "sonner";
import { apiFetch } from "../../lib/api";
import type { Announcement, AppSettings, ChatUsage } from "../../lib/types";
import {
  EmptyState,
  errorMessage,
  formatDate,
  fromLocalInput,
  PageHeader,
  StatusBadge,
  toLocalInput,
} from "../../components/shared";
import { Badge } from "../../components/ui/badge";
import { Button } from "../../components/ui/button";
import {
  Dialog,
  DialogContent,
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
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../../components/ui/tabs";
import { Textarea } from "../../components/ui/textarea";

type SettingKey = keyof AppSettings;

/** Loads all settings once; each section edits a local copy of its key. */
const useSetting = <K extends SettingKey>(key: K) => {
  const qc = useQueryClient();
  const settings = useQuery({
    queryKey: ["settings"],
    queryFn: () => apiFetch<AppSettings>("/admin/settings"),
  });
  const saved = settings.data?.[key];
  const [draft, setDraft] = useState<AppSettings[K] | undefined>(saved);
  useEffect(() => setDraft(saved), [saved]);
  const save = useMutation({
    mutationFn: (value: AppSettings[K]) =>
      apiFetch(`/admin/settings/${key}`, { method: "PUT", body: value }),
    onSuccess: () => {
      toast.success("Saved");
      qc.invalidateQueries({ queryKey: ["settings"] });
    },
    onError: (e) => toast.error(errorMessage(e)),
  });
  const dirty = JSON.stringify(draft) !== JSON.stringify(saved);
  const reset = () => setDraft(saved);
  return { loading: settings.isLoading, draft, setDraft, save, dirty, reset };
};

export const Platform = () => (
  <div>
    <PageHeader
      title="Platform"
      description="App status, announcements, AI tutor and content filter"
    />
    <Tabs defaultValue="app">
      <TabsList>
        <TabsTrigger value="app">App</TabsTrigger>
        <TabsTrigger value="announcements">Announcements</TabsTrigger>
        <TabsTrigger value="ai">AI tutor</TabsTrigger>
        <TabsTrigger value="words">Banned words</TabsTrigger>
      </TabsList>
      <TabsContent value="app">
        <AppFlags />
      </TabsContent>
      <TabsContent value="announcements">
        <Announcements />
      </TabsContent>
      <TabsContent value="ai">
        <AiChat />
      </TabsContent>
      <TabsContent value="words">
        <BannedWords />
      </TabsContent>
    </Tabs>
  </div>
);

const Section = ({
  title,
  description,
  children,
  footer,
}: {
  title: string;
  description?: string;
  children: ReactNode;
  footer?: ReactNode;
}) => (
  <div className="space-y-4 rounded-md border bg-card p-5">
    <div>
      <h3 className="font-medium">{title}</h3>
      {description && <p className="text-sm text-muted-foreground">{description}</p>}
    </div>
    {children}
    {footer && <div className="flex justify-end gap-2">{footer}</div>}
  </div>
);

const SaveButtons = ({
  dirty,
  pending,
  onReset,
  onSave,
}: {
  dirty: boolean;
  pending: boolean;
  onReset: () => void;
  onSave: () => void;
}) => (
  <>
    {dirty && (
      <Button variant="ghost" onClick={onReset}>
        Discard
      </Button>
    )}
    <Button disabled={!dirty || pending} onClick={onSave}>
      {pending ? "Saving..." : "Save"}
    </Button>
  </>
);

// ── App flags ──

const AppFlags = () => {
  const { loading, draft, setDraft, save, dirty, reset } = useSetting("app.flags");
  if (loading || !draft) return <Skeleton className="h-48 w-full" />;
  const set = (patch: Partial<typeof draft>) => setDraft({ ...draft, ...patch });
  return (
    <Section
      title="App status"
      description="The mobile app reads these on launch from GET /api/v1/app/config (cached up to a minute)."
      footer={
        <SaveButtons
          dirty={dirty}
          pending={save.isPending}
          onReset={reset}
          onSave={() => save.mutate(draft)}
        />
      }
    >
      <label
        className={`flex items-start gap-3 rounded-md border p-3 ${
          draft.maintenanceMode ? "border-destructive bg-destructive/5" : ""
        }`}
      >
        <input
          type="checkbox"
          className="mt-1 h-4 w-4"
          checked={draft.maintenanceMode}
          onChange={(e) => set({ maintenanceMode: e.target.checked })}
        />
        <span>
          <span className="block text-sm font-medium">Maintenance mode</span>
          <span className="block text-xs text-muted-foreground">
            The app shows the message below instead of its normal screens.
          </span>
        </span>
      </label>
      <div className="space-y-1">
        <Label>Maintenance message</Label>
        <Textarea
          rows={2}
          value={draft.maintenanceMessage}
          placeholder="We're upgrading Riksa. Back soon!"
          onChange={(e) => set({ maintenanceMessage: e.target.value })}
        />
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div className="space-y-1">
          <Label>Minimum app version</Label>
          <Input
            placeholder="e.g. 1.4.0"
            value={draft.minAppVersion}
            onChange={(e) => set({ minAppVersion: e.target.value })}
          />
          <p className="text-xs text-muted-foreground">Older versions must update to continue.</p>
        </div>
        <div className="space-y-1">
          <Label>Latest app version</Label>
          <Input
            placeholder="e.g. 1.6.2"
            value={draft.latestAppVersion}
            onChange={(e) => set({ latestAppVersion: e.target.value })}
          />
          <p className="text-xs text-muted-foreground">Older versions see an optional update prompt.</p>
        </div>
      </div>
    </Section>
  );
};

// ── Announcements ──

type AnnouncementDraft = { id: string | null; title: string; body: string; startsAt: string; endsAt: string };

const Announcements = () => {
  const qc = useQueryClient();
  const [editing, setEditing] = useState<AnnouncementDraft | null>(null);
  const list = useQuery({
    queryKey: ["announcements"],
    queryFn: () => apiFetch<Announcement[]>("/admin/announcements"),
  });
  const save = useMutation({
    mutationFn: ({ id, title, body, startsAt, endsAt }: AnnouncementDraft) =>
      apiFetch(id ? `/admin/announcements/${id}` : "/admin/announcements", {
        method: id ? "PUT" : "POST",
        body: { title, body, startsAt: fromLocalInput(startsAt), endsAt: fromLocalInput(endsAt) },
      }),
    onSuccess: () => {
      toast.success("Announcement saved");
      setEditing(null);
      qc.invalidateQueries({ queryKey: ["announcements"] });
    },
    onError: (e) => toast.error(errorMessage(e)),
  });
  const remove = useMutation({
    mutationFn: (id: string) => apiFetch(`/admin/announcements/${id}`, { method: "DELETE" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["announcements"] }),
    onError: (e) => toast.error(errorMessage(e)),
  });

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <p className="text-sm text-muted-foreground">
          In-app banners shown to everyone during their window. Push notifications
          aren't set up in the API yet.
        </p>
        <Button
          onClick={() =>
            setEditing({
              id: null,
              title: "",
              body: "",
              startsAt: toLocalInput(new Date().toISOString()),
              endsAt: "",
            })
          }
        >
          <Plus className="mr-1 h-4 w-4" /> New announcement
        </Button>
      </div>
      {list.isLoading && <Skeleton className="h-32 w-full" />}
      {list.data?.length === 0 && <EmptyState>No announcements</EmptyState>}
      <div className="space-y-2">
        {(list.data ?? []).map((a) => (
          <div key={a.id} className="flex items-start justify-between gap-3 rounded-md border bg-card p-4">
            <div className="min-w-0">
              <div className="flex items-center gap-2">
                <p className="font-medium">{a.title}</p>
                <StatusBadge value={a.status} />
              </div>
              <p className="mt-1 whitespace-pre-wrap text-sm">{a.body}</p>
              <p className="mt-1 text-xs text-muted-foreground">
                {formatDate(a.startsAt)} → {a.endsAt ? formatDate(a.endsAt) : "no end"}
              </p>
            </div>
            <div className="flex shrink-0">
              <Button
                size="icon"
                variant="ghost"
                onClick={() =>
                  setEditing({
                    id: a.id,
                    title: a.title,
                    body: a.body,
                    startsAt: toLocalInput(a.startsAt),
                    endsAt: toLocalInput(a.endsAt),
                  })
                }
              >
                <Pencil className="h-4 w-4" />
              </Button>
              <Button
                size="icon"
                variant="ghost"
                disabled={remove.isPending}
                onClick={() => remove.mutate(a.id)}
              >
                <Trash2 className="h-4 w-4 text-destructive" />
              </Button>
            </div>
          </div>
        ))}
      </div>
      <Dialog open={!!editing} onOpenChange={(o) => !o && setEditing(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{editing?.id ? "Edit announcement" : "New announcement"}</DialogTitle>
          </DialogHeader>
          {editing && (
            <div className="grid gap-3">
              <div className="space-y-1">
                <Label>Title *</Label>
                <Input
                  value={editing.title}
                  onChange={(e) => setEditing({ ...editing, title: e.target.value })}
                />
              </div>
              <div className="space-y-1">
                <Label>Message *</Label>
                <Textarea
                  rows={4}
                  value={editing.body}
                  onChange={(e) => setEditing({ ...editing, body: e.target.value })}
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1">
                  <Label>Show from *</Label>
                  <Input
                    type="datetime-local"
                    value={editing.startsAt}
                    onChange={(e) => setEditing({ ...editing, startsAt: e.target.value })}
                  />
                </div>
                <div className="space-y-1">
                  <Label>Until</Label>
                  <Input
                    type="datetime-local"
                    value={editing.endsAt}
                    onChange={(e) => setEditing({ ...editing, endsAt: e.target.value })}
                  />
                </div>
              </div>
            </div>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setEditing(null)}>
              Cancel
            </Button>
            <Button
              disabled={
                save.isPending || !editing?.title.trim() || !editing?.body.trim() || !editing?.startsAt
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

// ── AI tutor ──

const AiChat = () => {
  const { loading, draft, setDraft, save, dirty, reset } = useSetting("ai.chat");
  const [days, setDays] = useState(1);
  const usage = useQuery({
    queryKey: ["ai-chat", "usage", days],
    queryFn: () => apiFetch<ChatUsage>("/admin/ai-chat/usage", { params: { days } }),
  });

  return (
    <div className="space-y-6">
      {loading || !draft ? (
        <Skeleton className="h-48 w-full" />
      ) : (
        <Section
          title="AI tutor"
          description="Every learner message is a paid model call, so the daily limit is your cost control."
          footer={
            <SaveButtons
              dirty={dirty}
              pending={save.isPending}
              onReset={reset}
              onSave={() => save.mutate(draft)}
            />
          }
        >
          <label className="flex items-center gap-3 text-sm">
            <input
              type="checkbox"
              className="h-4 w-4"
              checked={draft.enabled}
              onChange={(e) => setDraft({ ...draft, enabled: e.target.checked })}
            />
            AI tutor is {draft.enabled ? "on" : "off"} for learners
          </label>
          <div className="space-y-1">
            <Label>Messages per learner per day</Label>
            <Input
              type="number"
              min={0}
              className="w-40"
              value={draft.dailyMessageLimit}
              onChange={(e) =>
                setDraft({ ...draft, dailyMessageLimit: Math.max(0, Math.floor(Number(e.target.value))) })
              }
            />
            <p className="text-xs text-muted-foreground">0 = unlimited. Resets at midnight Cambodia time.</p>
          </div>
          <div className="space-y-1">
            <Label>System prompt</Label>
            <Textarea
              rows={6}
              className="font-mono text-xs"
              placeholder="Empty = the built-in Riksa tutor prompt"
              value={draft.systemPrompt}
              onChange={(e) => setDraft({ ...draft, systemPrompt: e.target.value })}
            />
          </div>
        </Section>
      )}

      <Section title="Usage">
        <div className="flex items-center gap-3">
          <Select value={String(days)} onValueChange={(v) => setDays(Number(v))}>
            <SelectTrigger className="w-40">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="1">Last 24 hours</SelectItem>
              <SelectItem value="7">Last 7 days</SelectItem>
              <SelectItem value="30">Last 30 days</SelectItem>
            </SelectContent>
          </Select>
          {usage.data && (
            <p className="text-sm text-muted-foreground">
              {usage.data.totalMessages.toLocaleString()} learner messages from{" "}
              {usage.data.activeUsers} users
            </p>
          )}
        </div>
        {usage.isLoading && <Skeleton className="h-24 w-full" />}
        {usage.data?.topUsers.length === 0 && <EmptyState>No AI tutor use in this window</EmptyState>}
        {!!usage.data?.topUsers.length && (
          <div className="divide-y rounded-md border">
            {usage.data.topUsers.map((u) => (
              <div key={u.userId} className="flex items-center justify-between p-3 text-sm">
                <Link className="font-medium hover:underline" to={`/users/${u.userId}`}>
                  {u.username}
                </Link>
                <span className="text-muted-foreground">
                  {u.messages} messages · {u.sessions} chats · last {formatDate(u.lastMessageAt)}
                </span>
              </div>
            ))}
          </div>
        )}
        <p className="text-xs text-muted-foreground">
          To read a conversation, open the user and use the AI chat tab. Each
          opened transcript is recorded in the audit log.
        </p>
      </Section>
    </div>
  );
};

// ── Banned words ──

const BannedWords = () => {
  const { loading, draft, setDraft, save, dirty, reset } = useSetting("moderation.bannedWords");
  const [input, setInput] = useState("");
  if (loading || !draft) return <Skeleton className="h-48 w-full" />;

  const add = () => {
    const words = input
      .split(/[,\n]/)
      .map((w) => w.trim().toLowerCase())
      .filter((w) => w && !draft.words.includes(w));
    if (words.length) setDraft({ words: [...draft.words, ...words] });
    setInput("");
  };

  return (
    <Section
      title="Banned words"
      description="Posts, comments and blog posts containing these are rejected. English words match whole words only; Khmer matches anywhere in the text."
      footer={
        <SaveButtons
          dirty={dirty}
          pending={save.isPending}
          onReset={reset}
          onSave={() => save.mutate(draft)}
        />
      }
    >
      <div className="flex gap-2">
        <Input
          placeholder="Add words, separated by commas"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              e.preventDefault();
              add();
            }
          }}
        />
        <Button variant="outline" onClick={add} disabled={!input.trim()}>
          Add
        </Button>
      </div>
      {draft.words.length === 0 ? (
        <EmptyState>No banned words</EmptyState>
      ) : (
        <div className="flex flex-wrap gap-2">
          {draft.words.map((w) => (
            <Badge key={w} variant="secondary" className="gap-1 pr-1">
              {w}
              <button
                type="button"
                aria-label={`Remove ${w}`}
                className="rounded p-0.5 hover:bg-background"
                onClick={() => setDraft({ words: draft.words.filter((x) => x !== w) })}
              >
                <X className="h-3 w-3" />
              </button>
            </Badge>
          ))}
        </div>
      )}
    </Section>
  );
};
