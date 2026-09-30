import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router";
import { apiFetch } from "../../lib/api";
import { StreakChart, XpEarnedChart } from "./charts";
import type {
  ExerciseStats,
  GamificationStats,
  SecurityStats,
  StatsOverview,
} from "../../lib/types";
import { PageHeader, StatusBadge } from "../../components/shared";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "../../components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "../../components/ui/table";
import { Skeleton } from "../../components/ui/skeleton";

const StatCard = ({
  title,
  last24h,
  last7d,
}: {
  title: string;
  last24h: number;
  last7d: number;
}) => (
  <Card>
    <CardHeader className="pb-2">
      <CardDescription>{title}</CardDescription>
      <CardTitle className="text-3xl">{last24h}</CardTitle>
    </CardHeader>
    <CardContent>
      <p className="text-xs text-muted-foreground">last 24h · {last7d} last 7d</p>
    </CardContent>
  </Card>
);

export const Dashboard = () => {
  const overview = useQuery({
    queryKey: ["stats-overview"],
    queryFn: () => apiFetch<StatsOverview>("/admin/stats/overview"),
  });
  const exercises = useQuery({
    queryKey: ["stats-exercises"],
    queryFn: () =>
      apiFetch<ExerciseStats[]>("/admin/stats/exercises", {
        params: { minAttempts: 10 },
      }),
  });
  const gamification = useQuery({
    queryKey: ["stats-gamification"],
    queryFn: () => apiFetch<GamificationStats>("/admin/stats/gamification"),
  });
  const security = useQuery({
    queryKey: ["stats-security"],
    queryFn: () => apiFetch<SecurityStats>("/admin/stats/security"),
  });



  return (
    <div>
      <PageHeader title="Dashboard" description="Platform overview" />

      {overview.isLoading && <Skeleton className="mb-4 h-24 w-full" />}
      {overview.data && (
        <>
          <div className="mb-4 flex gap-3">
            {overview.data.pendingBlogs > 0 && (
              <Link
                to="/blog"
                className="rounded-md border border-amber-300 bg-amber-50 px-4 py-2 text-sm font-medium text-amber-900 hover:bg-amber-100"
              >
                {overview.data.pendingBlogs} blog(s) awaiting review
              </Link>
            )}
            {overview.data.openReports > 0 && (
              <Link
                to="/reports"
                className="rounded-md border border-red-300 bg-red-50 px-4 py-2 text-sm font-medium text-red-900 hover:bg-red-100"
              >
                {overview.data.openReports} open report(s)
              </Link>
            )}
          </div>
          <div className="mb-6 grid grid-cols-2 gap-4 lg:grid-cols-4">
            <StatCard
              title="Signups"
              last24h={overview.data.signups24h}
              last7d={overview.data.signups7d}
            />
            <StatCard
              title="Posts"
              last24h={overview.data.posts24h}
              last7d={overview.data.posts7d}
            />
            <StatCard
              title="Comments"
              last24h={overview.data.comments24h}
              last7d={overview.data.comments7d}
            />
            <StatCard
              title="Lesson completions"
              last24h={overview.data.lessonCompletions24h}
              last7d={overview.data.lessonCompletions7d}
            />
          </div>
        </>
      )}

      <div className="mb-6 grid gap-4 lg:grid-cols-2">
        <XpEarnedChart data={gamification.data} loading={gamification.isLoading} />
        <StreakChart data={gamification.data} loading={gamification.isLoading} />
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>Hardest exercises</CardTitle>
            <CardDescription>
              Lowest correct rate (min. 10 attempts)
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Question</TableHead>
                  <TableHead>Difficulty</TableHead>
                  <TableHead className="text-right">Attempts</TableHead>
                  <TableHead className="text-right">Correct %</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {(exercises.data ?? []).slice(0, 10).map((e) => (
                  <TableRow key={e.exerciseId}>
                    <TableCell className="max-w-md truncate">
                      {e.question ?? "(deleted)"}
                    </TableCell>
                    <TableCell>
                      {e.difficulty ? <StatusBadge value={e.difficulty} /> : "—"}
                    </TableCell>
                    <TableCell className="text-right">{e.attempts}</TableCell>
                    <TableCell className="text-right">
                      {e.correctRate.toFixed(1)}%
                    </TableCell>
                  </TableRow>
                ))}
                {exercises.data?.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={4} className="text-center text-muted-foreground">
                      No exercise attempts yet
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle>Security</CardTitle>
            <CardDescription>Account protection status</CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            {[
              ["Locked accounts", security.data?.lockedAccounts],
              ["Suspended accounts", security.data?.suspendedAccounts],
              ["Failed login attempts", security.data?.totalFailedLoginAttempts],
            ].map(([label, value]) => (
              <div key={label as string} className="flex items-center justify-between">
                <span className="text-sm text-muted-foreground">{label}</span>
                <span className="text-lg font-semibold">
                  {value ?? <Skeleton className="h-6 w-8" />}
                </span>
              </div>
            ))}
          </CardContent>
        </Card>
      </div>
    </div>
  );
};
