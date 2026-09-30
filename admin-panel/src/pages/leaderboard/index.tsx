import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { RefreshCw } from "lucide-react";
import { useState } from "react";
import { Link } from "react-router";
import { toast } from "sonner";
import { apiFetch } from "../../lib/api";
import type { LeaderboardBoard, LeaderboardMeta } from "../../lib/types";
import { EmptyState, errorMessage, PageHeader } from "../../components/shared";
import { Button } from "../../components/ui/button";
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

const PAGE_SIZE = 50;

export const Leaderboard = () => {
  const qc = useQueryClient();
  const [date, setDate] = useState<string | undefined>();
  const [scopeKey, setScopeKey] = useState("COUNTRY:CAMBODIA");
  const [page, setPage] = useState(0);
  const [scope, scopeId] = scopeKey.split(":");

  const meta = useQuery({
    queryKey: ["leaderboard", "meta"],
    queryFn: () => apiFetch<LeaderboardMeta>("/admin/leaderboard/meta"),
  });
  const board = useQuery({
    queryKey: ["leaderboard", "board", date, scope, scopeId, page],
    queryFn: () =>
      apiFetch<LeaderboardBoard>("/admin/leaderboard", {
        params: { date, scope, scopeId, page, size: PAGE_SIZE },
      }),
  });

  const refresh = useMutation({
    mutationFn: () =>
      apiFetch<{ countryRows: number; provinceRows: number; redisRebuilt: boolean }>(
        "/admin/leaderboard/refresh",
        { method: "POST" },
      ),
    onSuccess: (r) => {
      toast.success(`Rebuilt: ${r.countryRows} ranked users`);
      if (!r.redisRebuilt) toast.warning("Live (Redis) ranking wasn't rebuilt — check the API logs");
      qc.invalidateQueries({ queryKey: ["leaderboard"] });
    },
    onError: (e) => toast.error(errorMessage(e)),
  });

  const b = board.data;
  const pages = b ? Math.max(1, Math.ceil(b.total / PAGE_SIZE)) : 1;

  return (
    <div>
      <PageHeader
        title="Leaderboard"
        description="Daily ranking snapshots. Suspended and hidden users are left out."
        actions={
          <Button disabled={refresh.isPending} onClick={() => refresh.mutate()}>
            <RefreshCw className={`mr-1 h-4 w-4 ${refresh.isPending ? "animate-spin" : ""}`} />
            Rebuild now
          </Button>
        }
      />
      <div className="mb-4 flex flex-wrap gap-2">
        <Select
          value={date ?? "latest"}
          onValueChange={(v) => {
            setDate(v === "latest" ? undefined : v);
            setPage(0);
          }}
        >
          <SelectTrigger className="w-44">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="latest">Latest snapshot</SelectItem>
            {(meta.data?.dates ?? []).map((d) => (
              <SelectItem key={d} value={d}>
                {d}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Select
          value={scopeKey}
          onValueChange={(v) => {
            setScopeKey(v);
            setPage(0);
          }}
        >
          <SelectTrigger className="w-60">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="COUNTRY:CAMBODIA">Cambodia (national)</SelectItem>
            {(meta.data?.scopes ?? [])
              .filter((s) => s.scope === "PROVINCE")
              .map((s) => (
                <SelectItem key={s.scopeId} value={`PROVINCE:${s.scopeId}`}>
                  {s.label} ({s.users})
                </SelectItem>
              ))}
          </SelectContent>
        </Select>
      </div>

      {board.isLoading && <Skeleton className="h-64 w-full" />}
      {b && b.rows.length === 0 && (
        <EmptyState>No snapshot for this day yet. Use “Rebuild now”.</EmptyState>
      )}
      {b && b.rows.length > 0 && (
        <>
          <p className="mb-2 text-sm text-muted-foreground">
            Snapshot of {b.date} · {b.total.toLocaleString()} ranked users · score = XP + 5 ×
            streak
          </p>
          <div className="rounded-md border bg-card">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-16">#</TableHead>
                  <TableHead>User</TableHead>
                  <TableHead>Tier</TableHead>
                  <TableHead className="text-right">Score</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {b.rows.map((r) => (
                  <TableRow key={r.userId}>
                    <TableCell className="font-mono">{r.rank}</TableCell>
                    <TableCell>
                      <Link className="font-medium hover:underline" to={`/users/${r.userId}`}>
                        {r.username}
                      </Link>
                      {r.displayName && (
                        <span className="ml-2 text-xs text-muted-foreground">{r.displayName}</span>
                      )}
                    </TableCell>
                    <TableCell className="text-sm">{r.tier}</TableCell>
                    <TableCell className="text-right font-mono">{r.score.toLocaleString()}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
          <div className="mt-3 flex items-center justify-end gap-2 text-sm">
            <Button size="sm" variant="outline" disabled={page === 0} onClick={() => setPage(page - 1)}>
              Previous
            </Button>
            <span>
              Page {page + 1} of {pages}
            </span>
            <Button
              size="sm"
              variant="outline"
              disabled={page + 1 >= pages}
              onClick={() => setPage(page + 1)}
            >
              Next
            </Button>
          </div>
        </>
      )}
    </div>
  );
};
