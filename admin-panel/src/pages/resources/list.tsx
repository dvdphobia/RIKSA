import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { Eye, EyeOff, Trash2 } from "lucide-react";
import { Link } from "react-router";
import { useMemo, useState } from "react";
import { toast } from "sonner";
import { ReasonDialog } from "../../components/reason-dialog";
import { EmptyState, errorMessage, formatDate, PageHeader, StatusBadge } from "../../components/shared";
import { DataTable } from "../../components/table/data-table";
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../../components/ui/select";
import { apiFetch } from "../../lib/api";
import type {
  AdminLessonResource,
  AdminLessonResourceStatusRequest,
  SpringPage,
} from "../../lib/types";
import { useServerTable } from "../../lib/use-server-table";

interface LessonOption {
  id: string;
  title: string;
}

const fetchLessons = async () => {
  const pageSize = 100;
  const first = await apiFetch<SpringPage<LessonOption>>("/admin/lessons", {
    params: { page: 0, size: pageSize },
  });
  const lessons = [...first.content];
  for (let page = 1; page < first.totalPages; page += 1) {
    const next = await apiFetch<SpringPage<LessonOption>>("/admin/lessons", {
      params: { page, size: pageSize },
    });
    lessons.push(...next.content);
  }
  return lessons;
};

const safeExternalUrl = (value: string) => {
  try {
    const url = new URL(value);
    return url.protocol === "https:" || url.protocol === "http:" ? url.toString() : null;
  } catch {
    return null;
  }
};

export const ResourceList = () => {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState("");
  const [lessonId, setLessonId] = useState<string | undefined>();
  const [hidden, setHidden] = useState<boolean | undefined>();
  const [statusTarget, setStatusTarget] = useState<{
    resource: AdminLessonResource;
    hidden: boolean;
  } | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<AdminLessonResource | null>(null);

  const lessons = useQuery({
    queryKey: ["resources", "lesson-options"],
    queryFn: fetchLessons,
  });

  const columns = useMemo<ColumnDef<AdminLessonResource>[]>(
    () => [
      {
        id: "lessonTitle",
        header: "Lesson",
        cell: ({ row }) => (
          <Link className="font-medium hover:underline" to={`/lessons/edit/${row.original.lessonId}`}>
            {row.original.lessonTitle}
          </Link>
        ),
      },
      { id: "title", accessorKey: "title", header: "Title" },
      {
        id: "url",
        header: "URL",
        cell: ({ row }) => {
          const url = safeExternalUrl(row.original.url);
          return url ? (
            <a className="block max-w-xs truncate text-primary underline" href={url} target="_blank" rel="noopener noreferrer" title={row.original.url}>
              {row.original.url}
            </a>
          ) : (
            <span className="block max-w-xs truncate text-sm text-muted-foreground" title={row.original.url}>
              {row.original.url}
            </span>
          );
        },
      },
      { id: "kind", accessorKey: "kind", header: "Kind", cell: ({ row }) => <StatusBadge value={row.original.kind} /> },
      {
        id: "author",
        header: "Author",
        cell: ({ row }) => (
          <span className="text-sm" title={row.original.authorId}>
            {row.original.authorUsername}
          </span>
        ),
      },
      {
        id: "hidden",
        header: "Visibility",
        cell: ({ row }) => (
          <div>
            <StatusBadge value={row.original.hidden ? "HIDDEN" : "VISIBLE"} />
            {row.original.hiddenAt && <p className="mt-1 text-xs text-muted-foreground">{formatDate(row.original.hiddenAt)}</p>}
          </div>
        ),
      },
      { id: "createdAt", header: "Created", cell: ({ row }) => <span className="whitespace-nowrap text-sm">{formatDate(row.original.createdAt)}</span> },
      {
        id: "actions",
        header: "",
        cell: ({ row }) => (
          <div className="flex justify-end gap-1">
            <Button size="sm" variant="outline" onClick={() => setStatusTarget({ resource: row.original, hidden: !row.original.hidden })}>
              {row.original.hidden ? <Eye className="mr-1 h-4 w-4" /> : <EyeOff className="mr-1 h-4 w-4" />}
              {row.original.hidden ? "Unhide" : "Hide"}
            </Button>
            <Button size="icon" variant="ghost" title="Delete resource" onClick={() => setDeleteTarget(row.original)}>
              <Trash2 className="h-4 w-4 text-destructive" />
            </Button>
          </div>
        ),
      },
    ],
    [],
  );

  const table = useServerTable<AdminLessonResource>({
    resource: "resources",
    columns,
    pageSize: 20,
    filters: { lessonId, hidden, query: search },
  });

  const updateStatus = useMutation({
    mutationFn: ({ id, request }: { id: string; request: AdminLessonResourceStatusRequest }) =>
      apiFetch<AdminLessonResource>(`/admin/resources/${id}/status`, {
        method: "PATCH",
        body: request,
      }),
    onSuccess: (resource) => {
      toast.success(resource.hidden ? "Resource hidden" : "Resource unhidden");
      setStatusTarget(null);
      queryClient.invalidateQueries({ queryKey: ["resources"] });
    },
    onError: (error) => toast.error(errorMessage(error)),
  });
  const remove = useMutation({
    mutationFn: (id: string) => apiFetch<void>(`/admin/resources/${id}`, { method: "DELETE" }),
    onSuccess: () => {
      toast.success("Resource deleted");
      setDeleteTarget(null);
      queryClient.invalidateQueries({ queryKey: ["resources"] });
    },
    onError: (error) => toast.error(errorMessage(error)),
  });

  return (
    <div>
      <PageHeader title="Lesson resources" description="Review learner-submitted links, PDFs, and videos" />
      <div className="mb-4 flex flex-wrap gap-2">
        <Input className="max-w-sm" placeholder="Search titles or authors…" value={search} onChange={(event) => setSearch(event.target.value)} />
        <Select value={lessonId ?? "all"} onValueChange={(value) => setLessonId(value === "all" ? undefined : value)}>
          <SelectTrigger className="w-64"><SelectValue placeholder="All lessons" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All lessons</SelectItem>
            {(lessons.data ?? []).map((lesson) => <SelectItem key={lesson.id} value={lesson.id}>{lesson.title}</SelectItem>)}
          </SelectContent>
        </Select>
        <Select value={hidden === undefined ? "all" : String(hidden)} onValueChange={(value) => setHidden(value === "all" ? undefined : value === "true")}>
          <SelectTrigger className="w-40"><SelectValue placeholder="Visibility" /></SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All resources</SelectItem>
            <SelectItem value="false">Visible</SelectItem>
            <SelectItem value="true">Hidden</SelectItem>
          </SelectContent>
        </Select>
      </div>
      {lessons.isError && <p className="mb-3 text-sm text-destructive">Lesson filter options could not be loaded.</p>}
      {table.query.isLoading ? (
        <p className="py-12 text-center text-sm text-muted-foreground">Loading resources…</p>
      ) : table.query.isError ? (
        <EmptyState>{errorMessage(table.query.error, "Could not load resources")}</EmptyState>
      ) : table.query.data?.rows.length === 0 ? (
        <EmptyState>No resources match these filters.</EmptyState>
      ) : (
        <div className="rounded-md border bg-card"><DataTable tableProps={table} /></div>
      )}

      <ReasonDialog
        open={!!statusTarget}
        onOpenChange={(open) => !open && setStatusTarget(null)}
        title={statusTarget?.hidden ? "Hide resource" : "Unhide resource"}
        description={statusTarget ? `Change visibility for “${statusTarget.resource.title}”.` : undefined}
        confirmLabel={statusTarget?.hidden ? "Hide resource" : "Unhide resource"}
        destructive={!!statusTarget?.hidden}
        pending={updateStatus.isPending}
        reasonRequired={false}
        reasonPlaceholder="Optional moderation note"
        onConfirm={(reason) => {
          if (!statusTarget) return;
          updateStatus.mutate({
            id: statusTarget.resource.id,
            request: { hidden: statusTarget.hidden, ...(reason ? { reason } : {}) },
          });
        }}
      />

      <Dialog open={!!deleteTarget} onOpenChange={(open) => !open && setDeleteTarget(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Delete “{deleteTarget?.title}”?</DialogTitle>
            <DialogDescription>
              This permanently deletes the resource and its saved-resource and reward rows.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteTarget(null)}>Cancel</Button>
            <Button variant="destructive" disabled={remove.isPending} onClick={() => deleteTarget && remove.mutate(deleteTarget.id)}>
              {remove.isPending ? "Deleting…" : "Delete permanently"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};
