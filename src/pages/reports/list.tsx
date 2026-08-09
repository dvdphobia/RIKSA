import { useInfiniteQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { toast } from "sonner";
import { apiFetch, ApiError } from "../../lib/api";
import type {
  CursorPage,
  Report,
  ReportAction,
  ReportStatus,
  ReportTargetType,
} from "../../lib/types";
import { formatDate, PageHeader, StatusBadge } from "../../components/shared";
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
import { Skeleton } from "../../components/ui/skeleton";

const PAGE_SIZE = 20;

export const ReportList = () => {
  const queryClient = useQueryClient();
  const [status, setStatus] = useState<ReportStatus>("OPEN");
  const [targetType, setTargetType] = useState<ReportTargetType | undefined>();
  const [resolveTarget, setResolveTarget] = useState<Report | null>(null);
  const [resolveStatus, setResolveStatus] = useState<ReportStatus>("RESOLVED");
  const [action, setAction] = useState<ReportAction>("NONE");

  const query = useInfiniteQuery({
    queryKey: ["reports", status, targetType],
    queryFn: ({ pageParam }) =>
      apiFetch<CursorPage<Report>>("/admin/reports", {
        params: { status, targetType, cursor: pageParam, limit: PAGE_SIZE },
      }),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (last) => (last.hasNextPage ? last.endCursor : undefined),
  });

  const reports = query.data?.pages.flatMap((p) => p.edges) ?? [];

  const resolve = useMutation({
    mutationFn: ({ id, status, action }: { id: string; status: ReportStatus; action: ReportAction }) =>
      apiFetch(`/admin/reports/${id}`, {
        method: "PATCH",
        body: { status, action },
      }),
    onSuccess: () => {
      toast.success("Report updated");
      setResolveTarget(null);
      queryClient.invalidateQueries({ queryKey: ["reports"] });
    },
    onError: (e) =>
      toast.error(e instanceof ApiError ? e.message : "Update failed"),
  });

  const openResolve = (report: Report) => {
    setResolveTarget(report);
    setResolveStatus("RESOLVED");
    setAction("NONE");
  };

  return (
    <div>
      <PageHeader
        title="Reports"
        description="Moderate user-submitted reports"
      />
      <div className="mb-4 flex items-center gap-3">
        <Tabs
          value={status}
          onValueChange={(v) => setStatus(v as ReportStatus)}
        >
          <TabsList>
            <TabsTrigger value="OPEN">Open</TabsTrigger>
            <TabsTrigger value="RESOLVED">Resolved</TabsTrigger>
            <TabsTrigger value="DISMISSED">Dismissed</TabsTrigger>
          </TabsList>
        </Tabs>
        <Select
          value={targetType ?? "all"}
          onValueChange={(v) =>
            setTargetType(v === "all" ? undefined : (v as ReportTargetType))
          }
        >
          <SelectTrigger className="w-40">
            <SelectValue placeholder="Target type" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All targets</SelectItem>
            <SelectItem value="POST">POST</SelectItem>
            <SelectItem value="COMMENT">COMMENT</SelectItem>
            <SelectItem value="BLOG">BLOG</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <div className="space-y-3">
        {query.isLoading &&
          Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-20 w-full" />
          ))}
        {reports.map((r) => (
          <div
            key={r.id}
            className="flex items-start justify-between rounded-md border bg-card p-4"
          >
            <div className="space-y-1">
              <div className="flex items-center gap-2">
                <StatusBadge value={r.reason} />
                <span className="text-sm text-muted-foreground">
                  {r.targetType} · {r.targetId.slice(0, 8)}...
                </span>
              </div>
              {r.details && <p className="text-sm">{r.details}</p>}
              <p className="text-xs text-muted-foreground">
                Reported {formatDate(r.createdAt)}
                {r.resolvedAt ? ` · closed ${formatDate(r.resolvedAt)}` : ""}
              </p>
            </div>
            <div className="flex items-center gap-2">
              <StatusBadge value={r.status} />
              {r.status === "OPEN" && (
                <Button size="sm" onClick={() => openResolve(r)}>
                  Resolve
                </Button>
              )}
            </div>
          </div>
        ))}
        {!query.isLoading && reports.length === 0 && (
          <p className="py-12 text-center text-muted-foreground">
            No {status.toLowerCase()} reports
          </p>
        )}
        {query.hasNextPage && (
          <Button
            variant="outline"
            className="w-full"
            disabled={query.isFetchingNextPage}
            onClick={() => query.fetchNextPage()}
          >
            {query.isFetchingNextPage ? "Loading..." : "Load more"}
          </Button>
        )}
      </div>

      <Dialog
        open={!!resolveTarget}
        onOpenChange={(open) => !open && setResolveTarget(null)}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Resolve report</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label>Outcome</Label>
              <Select
                value={resolveStatus}
                onValueChange={(v) => setResolveStatus(v as ReportStatus)}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="RESOLVED">RESOLVED — take action</SelectItem>
                  <SelectItem value="DISMISSED">DISMISSED — no violation</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>Action</Label>
              <Select
                value={action}
                onValueChange={(v) => setAction(v as ReportAction)}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="NONE">NONE — no content change</SelectItem>
                  <SelectItem
                    value="HIDE"
                    disabled={resolveTarget?.targetType === "BLOG"}
                  >
                    HIDE — hide the reported content
                  </SelectItem>
                </SelectContent>
              </Select>
              {resolveTarget?.targetType === "BLOG" && (
                <p className="text-xs text-muted-foreground">
                  Blogs use the approve/reject flow instead.
                </p>
              )}
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setResolveTarget(null)}>
              Cancel
            </Button>
            <Button
              disabled={resolve.isPending}
              onClick={() =>
                resolveTarget &&
                resolve.mutate({
                  id: resolveTarget.id,
                  status: resolveStatus,
                  action,
                })
              }
            >
              {resolve.isPending ? "Saving..." : "Confirm"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};
