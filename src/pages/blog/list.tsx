import { useTable } from "@refinedev/react-table";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { useMemo, useState } from "react";
import { toast } from "sonner";
import { apiFetch, ApiError } from "../../lib/api";
import type { BlogPost, BlogStatus } from "../../lib/types";
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
import { Tabs, TabsList, TabsTrigger } from "../../components/ui/tabs";
import { Textarea } from "../../components/ui/textarea";
import { Check, Eye, X } from "lucide-react";

export const BlogList = () => {
  const queryClient = useQueryClient();
  const [status, setStatus] = useState<BlogStatus>("PENDING");
  const [viewTarget, setViewTarget] = useState<BlogPost | null>(null);
  const [rejectTarget, setRejectTarget] = useState<BlogPost | null>(null);
  const [rejectReason, setRejectReason] = useState("");

  const moderate = useMutation({
    mutationFn: ({
      id,
      status,
      reason,
    }: {
      id: string;
      status: BlogStatus;
      reason?: string;
    }) =>
      apiFetch(`/admin/blog/${id}/status`, {
        method: "PATCH",
        body: reason ? { status, reason } : { status },
      }),
    onSuccess: () => {
      toast.success("Blog post updated");
      setRejectTarget(null);
      setRejectReason("");
      queryClient.invalidateQueries({ queryKey: ["blog"] });
    },
    onError: (e) =>
      toast.error(e instanceof ApiError ? e.message : "Update failed"),
  });

  const columns = useMemo<ColumnDef<BlogPost>[]>(
    () => [
      {
        id: "title",
        accessorKey: "title",
        header: "Title",
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
        id: "stats",
        header: "Views / Likes",
        cell: ({ row }) => (
          <span className="text-sm">
            {row.original.viewCount} / {row.original.likeCount}
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
        header: "Submitted",
        cell: ({ row }) => (
          <span className="text-sm">{formatDate(row.original.createdAt)}</span>
        ),
      },
      {
        id: "actions",
        header: () => <div className="text-right">Actions</div>,
        cell: ({ row }) => {
          const post = row.original;
          return (
            <div className="flex justify-end gap-1">
              <Button
                variant="ghost"
                size="icon"
                onClick={() => setViewTarget(post)}
              >
                <Eye className="h-4 w-4" />
              </Button>
              {post.status === "PENDING" && (
                <>
                  <Button
                    variant="ghost"
                    size="icon"
                    title="Approve"
                    onClick={() => moderate.mutate({ id: post.id, status: "APPROVED" })}
                  >
                    <Check className="h-4 w-4 text-green-600" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    title="Reject"
                    onClick={() => setRejectTarget(post)}
                  >
                    <X className="h-4 w-4 text-destructive" />
                  </Button>
                </>
              )}
            </div>
          );
        },
      },
    ],
    [moderate],
  );

  const table = useTable<BlogPost>({
    columns,
    refineCoreProps: {
      resource: "blog",
      pagination: { pageSize: 20 },
      filters: {
        permanent: [{ field: "status", operator: "eq", value: status }],
      },
    },
  });

  return (
    <div>
      <PageHeader title="Blog moderation" description="Approve or reject user blog posts" />
      <div className="mb-4">
        <Tabs value={status} onValueChange={(v) => setStatus(v as BlogStatus)}>
          <TabsList>
            <TabsTrigger value="PENDING">Pending</TabsTrigger>
            <TabsTrigger value="APPROVED">Approved</TabsTrigger>
            <TabsTrigger value="REJECTED">Rejected</TabsTrigger>
          </TabsList>
        </Tabs>
      </div>
      <div className="rounded-md border bg-card">
        <DataTable tableProps={table} />
      </div>

      <Dialog open={!!viewTarget} onOpenChange={(o) => !o && setViewTarget(null)}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>{viewTarget?.title}</DialogTitle>
          </DialogHeader>
          {viewTarget?.coverUrl && (
            <img
              src={viewTarget.coverUrl}
              alt=""
              className="max-h-48 w-full rounded-md object-cover"
            />
          )}
          <pre className="max-h-96 overflow-y-auto whitespace-pre-wrap rounded-md bg-muted p-4 text-sm">
            {viewTarget?.content}
          </pre>
          {viewTarget?.status === "PENDING" && (
            <DialogFooter>
              <Button
                variant="outline"
                onClick={() => {
                  setRejectTarget(viewTarget);
                  setViewTarget(null);
                }}
              >
                Reject...
              </Button>
              <Button
                onClick={() =>
                  moderate.mutate({ id: viewTarget.id, status: "APPROVED" })
                }
              >
                Approve
              </Button>
            </DialogFooter>
          )}
        </DialogContent>
      </Dialog>

      <Dialog
        open={!!rejectTarget}
        onOpenChange={(o) => !o && setRejectTarget(null)}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Reject "{rejectTarget?.title}"</DialogTitle>
          </DialogHeader>
          <div className="space-y-2">
            <Label>Reason (shown to the author)</Label>
            <Textarea
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              placeholder="Why is this post being rejected?"
            />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setRejectTarget(null)}>
              Cancel
            </Button>
            <Button
              variant="destructive"
              disabled={!rejectReason.trim() || moderate.isPending}
              onClick={() =>
                rejectTarget &&
                moderate.mutate({
                  id: rejectTarget.id,
                  status: "REJECTED",
                  reason: rejectReason,
                })
              }
            >
              Reject post
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};
