import { useTable } from "@refinedev/react-table";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { useMemo, useState } from "react";
import { toast } from "sonner";
import { apiFetch, ApiError } from "../../lib/api";
import type { CommentModeration, ContentStatus, PostModeration } from "../../lib/types";
import { formatDate, PageHeader, StatusBadge } from "../../components/shared";
import { DataTable } from "../../components/table/data-table";
import { Button } from "../../components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "../../components/ui/dialog";
import { Label } from "../../components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../../components/ui/select";
import { Tabs, TabsList, TabsTrigger } from "../../components/ui/tabs";
import { Textarea } from "../../components/ui/textarea";
import { Eye, EyeOff } from "lucide-react";

type Target = { kind: "posts" } | { kind: "comments" };

export const ModerationList = () => {
  const queryClient = useQueryClient();
  const [target, setTarget] = useState<Target>({ kind: "posts" });
  const [status, setStatus] = useState<ContentStatus>("HIDDEN");
  const [hideTarget, setHideTarget] = useState<{ id: string; label: string } | null>(null);
  const [hideReason, setHideReason] = useState("");

  const setStatusMutation = useMutation({
    mutationFn: ({
      kind,
      id,
      status,
      reason,
    }: {
      kind: "posts" | "comments";
      id: string;
      status: ContentStatus;
      reason?: string;
    }) =>
      apiFetch(`/admin/${kind}/${id}/status`, {
        method: "PATCH",
        body: reason ? { status, reason } : { status },
      }),
    onSuccess: () => {
      toast.success("Content updated");
      setHideTarget(null);
      setHideReason("");
      queryClient.invalidateQueries({ queryKey: [target.kind] });
    },
    onError: (e) =>
      toast.error(e instanceof ApiError ? e.message : "Update failed"),
  });

  const postColumns = useMemo<ColumnDef<PostModeration>[]>(
    () => [
      {
        id: "title",
        accessorKey: "title",
        header: "Post",
        cell: ({ row }) => (
          <div>
            <p className="font-medium">{row.original.title}</p>
            <p className="text-xs text-muted-foreground">
              by {row.original.authorId.slice(0, 8)}...
            </p>
          </div>
        ),
      },
      {
        id: "hiddenReason",
        header: "Hidden reason",
        cell: ({ row }) => (
          <span className="max-w-sm truncate text-sm text-muted-foreground">
            {row.original.hiddenReason ?? "—"}
          </span>
        ),
      },
      {
        id: "status",
        header: "Status",
        cell: ({ row }) => <StatusBadge value={row.original.status} />,
      },
      {
        id: "createdAt",
        header: "Created",
        cell: ({ row }) => (
          <span className="text-sm">{formatDate(row.original.createdAt)}</span>
        ),
      },
      {
        id: "actions",
        header: () => <div className="text-right">Actions</div>,
        cell: ({ row }) => {
          const p = row.original;
          return (
            <div className="flex justify-end gap-1">
              {p.status === "HIDDEN" ? (
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() =>
                    setStatusMutation.mutate({ kind: "posts", id: p.id, status: "VISIBLE" })
                  }
                >
                  <Eye className="mr-1 h-4 w-4" /> Restore
                </Button>
              ) : (
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() => setHideTarget({ id: p.id, label: p.title })}
                >
                  <EyeOff className="mr-1 h-4 w-4" /> Hide
                </Button>
              )}
            </div>
          );
        },
      },
    ],
    [setStatusMutation],
  );

  const commentColumns = useMemo<ColumnDef<CommentModeration>[]>(
    () => [
      {
        id: "content",
        accessorKey: "content",
        header: "Comment",
        cell: ({ row }) => (
          <div>
            <p className="max-w-md truncate">{row.original.content}</p>
            <p className="text-xs text-muted-foreground">
              on post {row.original.postId.slice(0, 8)}... · by{" "}
              {row.original.authorId.slice(0, 8)}...
            </p>
          </div>
        ),
      },
      {
        id: "hiddenReason",
        header: "Hidden reason",
        cell: ({ row }) => (
          <span className="max-w-sm truncate text-sm text-muted-foreground">
            {row.original.hiddenReason ?? "—"}
          </span>
        ),
      },
      {
        id: "status",
        header: "Status",
        cell: ({ row }) => <StatusBadge value={row.original.status} />,
      },
      {
        id: "createdAt",
        header: "Created",
        cell: ({ row }) => (
          <span className="text-sm">{formatDate(row.original.createdAt)}</span>
        ),
      },
      {
        id: "actions",
        header: () => <div className="text-right">Actions</div>,
        cell: ({ row }) => {
          const c = row.original;
          return (
            <div className="flex justify-end gap-1">
              {c.status === "HIDDEN" ? (
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() =>
                    setStatusMutation.mutate({ kind: "comments", id: c.id, status: "VISIBLE" })
                  }
                >
                  <Eye className="mr-1 h-4 w-4" /> Restore
                </Button>
              ) : (
                <Button
                  size="sm"
                  variant="outline"
                  onClick={() =>
                    setHideTarget({ id: c.id, label: c.content.slice(0, 40) })
                  }
                >
                  <EyeOff className="mr-1 h-4 w-4" /> Hide
                </Button>
              )}
            </div>
          );
        },
      },
    ],
    [setStatusMutation],
  );

  const postsTable = useTable<PostModeration>({
    columns: postColumns,
    refineCoreProps: {
      resource: "posts",
      filters: {
        permanent: [{ field: "status", operator: "eq", value: status }],
      },
    },
  });

  const commentsTable = useTable<CommentModeration>({
    columns: commentColumns,
    refineCoreProps: {
      resource: "comments",
      filters: {
        permanent: [{ field: "status", operator: "eq", value: status }],
      },
    },
  });

  return (
    <div>
      <PageHeader
        title="Hidden content"
        description="Restore or hide posts and comments"
      />
      <div className="mb-4 flex items-center gap-3">
        <Tabs
          value={target.kind}
          onValueChange={(v) => setTarget({ kind: v as "posts" | "comments" })}
        >
          <TabsList>
            <TabsTrigger value="posts">Posts</TabsTrigger>
            <TabsTrigger value="comments">Comments</TabsTrigger>
          </TabsList>
        </Tabs>
        <Select value={status} onValueChange={(v) => setStatus(v as ContentStatus)}>
          <SelectTrigger className="w-40">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="HIDDEN">Hidden (queue)</SelectItem>
            <SelectItem value="VISIBLE">Visible</SelectItem>
          </SelectContent>
        </Select>
      </div>
      <div className="rounded-md border bg-card">
        {target.kind === "posts" ? (
          <DataTable tableProps={postsTable} />
        ) : (
          <DataTable tableProps={commentsTable} />
        )}
      </div>

      <Dialog open={!!hideTarget} onOpenChange={(o) => !o && setHideTarget(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Hide "{hideTarget?.label}"</DialogTitle>
          </DialogHeader>
          <div className="space-y-2">
            <Label>Reason *</Label>
            <Textarea
              value={hideReason}
              onChange={(e) => setHideReason(e.target.value)}
              placeholder="Why is this content being hidden?"
            />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setHideTarget(null)}>
              Cancel
            </Button>
            <Button
              variant="destructive"
              disabled={!hideReason.trim() || setStatusMutation.isPending}
              onClick={() =>
                hideTarget &&
                setStatusMutation.mutate({
                  kind: target.kind,
                  id: hideTarget.id,
                  status: "HIDDEN",
                  reason: hideReason,
                })
              }
            >
              Hide content
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};
