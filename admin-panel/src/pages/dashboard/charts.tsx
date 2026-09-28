import { ArrowDownRight, ArrowUpRight } from "lucide-react";
import { type ReactNode, useMemo, useState } from "react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import type { GamificationStats } from "../../lib/types";
import { Card, CardContent } from "../../components/ui/card";
import { Skeleton } from "../../components/ui/skeleton";

// One ink for every bar, one accent for the bar you're looking at, and a
// hatch for bars that are "not the real thing yet" (today, inactive users).
const INK = "hsl(var(--primary))";
const ACCENT = "var(--chart-accent)";
const HATCH_ID = "chart-hatch";

const SOURCE_LABELS: Record<string, string> = {
  LESSON_COMPLETION: "Lessons",
  ACHIEVEMENT_UNLOCK: "Achievements",
  ACHIEVEMENT_REWARD: "Achievements",
  DAILY_PRACTICE_STREAK: "Streak bonus",
  REACH_RANK: "Rank ups",
  ADMIN_ADJUSTMENT: "Staff adjustments",
};
const sourceLabel = (s: string) =>
  SOURCE_LABELS[s] ?? s.charAt(0) + s.slice(1).toLowerCase().replace(/_/g, " ");

const STREAK_LABELS: Record<string, string> = {
  "0": "None",
  "1-2": "1–2 d",
  "3-6": "3–6 d",
  "7-13": "1–2 wk",
  "14-29": "2–4 wk",
  "30+": "30+ d",
};

const fmt = (n: number) => n.toLocaleString();
const dayKey = (d: Date) => d.toISOString().slice(0, 10);
const shortDate = (key: string) =>
  new Date(`${key}T00:00:00`).toLocaleDateString(undefined, {
    month: "short",
    day: "numeric",
  });

const HatchDefs = () => (
  <defs>
    <pattern
      id={HATCH_ID}
      width="6"
      height="6"
      patternUnits="userSpaceOnUse"
      patternTransform="rotate(45)"
    >
      <rect width="6" height="6" fill="hsl(var(--muted))" />
      <line
        x1="0"
        y1="0"
        x2="0"
        y2="6"
        stroke="hsl(var(--muted-foreground))"
        strokeWidth="1.5"
        strokeOpacity="0.45"
      />
    </pattern>
  </defs>
);

const ChangeBadge = ({
  pct,
  suffix,
}: {
  pct: number | null;
  suffix: string;
}) => {
  if (pct === null)
    return <span className="text-sm text-muted-foreground">{suffix}</span>;
  const up = pct >= 0;
  return (
    <span className="flex items-center gap-2 text-sm text-muted-foreground">
      <span
        className={`inline-flex items-center gap-0.5 rounded-full px-2 py-0.5 text-xs font-semibold text-white ${
          up ? "bg-[var(--chart-accent)]" : "bg-destructive"
        }`}
      >
        {up ? (
          <ArrowUpRight className="h-3 w-3" />
        ) : (
          <ArrowDownRight className="h-3 w-3" />
        )}
        {Math.abs(pct).toFixed(1)}%
      </span>
      {suffix}
    </span>
  );
};

const ChartCard = ({
  title,
  hero,
  badge,
  children,
  footer,
  action,
}: {
  title: string;
  hero: ReactNode;
  badge: ReactNode;
  children: ReactNode;
  footer?: ReactNode;
  action?: ReactNode;
}) => (
  <Card className="rounded-2xl">
    <CardContent className="p-6">
      <div className="flex items-start justify-between gap-2">
        <p className="text-sm font-medium text-muted-foreground">{title}</p>
        {action}
      </div>
      <p className="mt-1 text-4xl font-semibold tracking-tight">{hero}</p>
      <div className="mt-2">{badge}</div>
      <div className="mt-6 h-52">{children}</div>
      {footer && <div className="mt-4">{footer}</div>}
    </CardContent>
  </Card>
);

const TooltipBox = ({ children }: { children: ReactNode }) => (
  <div className="rounded-lg border bg-popover px-3 py-2 text-sm text-popover-foreground shadow-md">
    {children}
  </div>
);

const axisProps = {
  axisLine: false,
  tickLine: false,
  fontSize: 11,
  tick: { fill: "hsl(var(--muted-foreground))" },
} as const;

// ── XP earned ──

/** The stats API returns the last 30 days. */
const API_DAYS = 30;
const RANGES = [14, 30] as const;
type Range = (typeof RANGES)[number];

interface XpDay {
  key: string;
  label: string;
  total: number;
  bySource: [string, number][];
  partial: boolean;
}

export const XpEarnedChart = ({
  data,
  loading,
}: {
  data: GamificationStats | undefined;
  loading: boolean;
}) => {
  const [hover, setHover] = useState<number | null>(null);
  const [picked, setPicked] = useState<Range | null>(null);

  const all = useMemo(() => {
    // Continuous, oldest → newest, with empty days as 0 so gaps are visible.
    const rows = data?.xpByDay ?? [];
    const byDay = new Map<string, Map<string, number>>();
    for (const r of rows) {
      // Staff corrections aren't XP anyone earned.
      if (r.sourceType === "ADMIN_ADJUSTMENT") continue;
      const m = byDay.get(r.date) ?? new Map<string, number>();
      const label = sourceLabel(r.sourceType);
      m.set(label, (m.get(label) ?? 0) + r.totalXp);
      byDay.set(r.date, m);
    }
    const today = new Date();
    const all: XpDay[] = [];
    for (let i = API_DAYS - 1; i >= 0; i--) {
      const d = new Date(today);
      d.setDate(today.getDate() - i);
      const key = dayKey(d);
      const m = byDay.get(key) ?? new Map<string, number>();
      const bySource = [...m.entries()].sort((a, b) => b[1] - a[1]);
      all.push({
        key,
        label: shortDate(key),
        total: bySource.reduce((s, [, v]) => s + v, 0),
        bySource,
        partial: i === 0,
      });
    }
    return all;
  }, [data]);

  const sum = (list: XpDay[]) => list.reduce((s, d) => s + d.total, 0);
  // Start on 14 days, unless it's empty and the full month isn't.
  const range: Range =
    picked ?? (sum(all.slice(-14)) > 0 || sum(all) === 0 ? 14 : 30);

  const { days, total, change, sources, peak } = useMemo(() => {
    const days = all.slice(-range);
    // Only 14 days has a full previous window inside the 30 the API returns.
    const previous =
      range * 2 <= API_DAYS ? sum(all.slice(-range * 2, -range)) : 0;
    const totals = new Map<string, number>();
    for (const d of days)
      for (const [s, v] of d.bySource) totals.set(s, (totals.get(s) ?? 0) + v);
    const peak = days.reduce(
      (best, d, i) => (d.total > days[best].total ? i : best),
      0,
    );
    return {
      days,
      total: sum(days),
      change: previous > 0 ? ((sum(days) - previous) / previous) * 100 : null,
      sources: [...totals.entries()].sort((a, b) => b[1] - a[1]),
      peak,
    };
  }, [all, range]);

  if (loading) return <Skeleton className="h-[420px] w-full rounded-2xl" />;

  const highlighted = hover ?? (days[peak]?.total > 0 ? peak : null);

  return (
    <ChartCard
      title={`XP earned by all learners · last ${range} days`}
      hero={fmt(total)}
      action={
        <div className="flex rounded-full bg-muted p-0.5 text-xs font-medium">
          {RANGES.map((r) => (
            <button
              key={r}
              type="button"
              onClick={() => {
                setPicked(r);
                setHover(null);
              }}
              className={`rounded-full px-2.5 py-1 transition-colors ${
                r === range
                  ? "bg-background text-foreground shadow-sm"
                  : "text-muted-foreground"
              }`}
            >
              {r}d
            </button>
          ))}
        </div>
      }
      badge={
        range * 2 > API_DAYS ? (
          <span className="text-sm text-muted-foreground">
            about{" "}
            <span className="font-medium text-foreground">
              {fmt(Math.round(total / range))}
            </span>{" "}
            XP a day
          </span>
        ) : (
          <ChangeBadge
            pct={change}
            suffix={
              change === null
                ? `No XP in the ${range} days before to compare`
                : `vs the previous ${range} days`
            }
          />
        )
      }
      footer={
        sources.length > 0 && (
          <div className="flex flex-wrap gap-x-5 gap-y-1 text-sm">
            {sources.map(([name, value]) => (
              <span key={name} className="text-muted-foreground">
                {name}{" "}
                <span className="font-medium text-foreground">
                  {Math.round((value / Math.max(total, 1)) * 100)}%
                </span>
              </span>
            ))}
          </div>
        )
      }
    >
      {total === 0 ? (
        <div className="flex h-full items-center justify-center rounded-xl border border-dashed text-sm text-muted-foreground">
          No XP earned in the last {range} days
        </div>
      ) : (
        <ResponsiveContainer width="100%" height="100%">
          <BarChart
            data={days}
            margin={{ top: 4, right: 8, bottom: 0, left: -12 }}
            barCategoryGap="22%"
            onMouseMove={(s) =>
              setHover(
                typeof s.activeTooltipIndex === "number"
                  ? s.activeTooltipIndex
                  : null,
              )
            }
            onMouseLeave={() => setHover(null)}
          >
            <HatchDefs />
            <CartesianGrid vertical={false} stroke="hsl(var(--border))" />
            <XAxis
              dataKey="label"
              {...axisProps}
              interval={range === 14 ? 2 : 6}
              tickMargin={8}
            />
            <YAxis
              {...axisProps}
              tickCount={4}
              width={48}
              tickFormatter={(v) => (v >= 1000 ? `${v / 1000}k` : v)}
            />
            <Tooltip
              cursor={false}
              content={({ active, payload }) => {
                const d = payload?.[0]?.payload as XpDay | undefined;
                if (!active || !d) return null;
                return (
                  <TooltipBox>
                    <p className="font-medium">
                      {d.label}
                      {d.partial && (
                        <span className="font-normal text-muted-foreground">
                          {" "}
                          · today so far
                        </span>
                      )}
                    </p>
                    <p className="text-lg font-semibold">{fmt(d.total)} XP</p>
                    {d.bySource.map(([s, v]) => (
                      <p
                        key={s}
                        className="flex justify-between gap-6 text-muted-foreground"
                      >
                        <span>{s}</span>
                        <span className="text-foreground">{fmt(v)}</span>
                      </p>
                    ))}
                  </TooltipBox>
                );
              }}
            />
            <Bar
              dataKey="total"
              radius={[6, 6, 6, 6]}
              minPointSize={2}
              isAnimationActive={false}
            >
              {days.map((d, i) => (
                <Cell
                  key={d.key}
                  fill={
                    i === highlighted
                      ? ACCENT
                      : d.partial
                        ? `url(#${HATCH_ID})`
                        : INK
                  }
                />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      )}
    </ChartCard>
  );
};

// ── Streaks ──

interface StreakBucket {
  bucket: string;
  label: string;
  users: number;
  share: number;
}

export const StreakChart = ({
  data,
  loading,
}: {
  data: GamificationStats | undefined;
  loading: boolean;
}) => {
  const [hover, setHover] = useState<number | null>(null);

  const { buckets, active, learners, top } = useMemo(() => {
    const rows = data?.streakHistogram ?? [];
    const learners = rows.reduce((s, r) => s + r.userCount, 0);
    const buckets: StreakBucket[] = rows.map((r) => ({
      bucket: r.bucket,
      label: STREAK_LABELS[r.bucket] ?? r.bucket,
      users: r.userCount,
      share: learners ? (r.userCount / learners) * 100 : 0,
    }));
    const active = buckets
      .filter((b) => b.bucket !== "0")
      .reduce((s, b) => s + b.users, 0);
    // Default highlight: the biggest group among learners who do have a streak.
    const top = buckets.reduce(
      (best, b, i) =>
        b.bucket !== "0" && (best < 0 || b.users > buckets[best].users)
          ? i
          : best,
      -1,
    );
    return { buckets, active, learners, top };
  }, [data]);

  if (loading) return <Skeleton className="h-[420px] w-full rounded-2xl" />;

  const highlighted =
    hover ?? (top >= 0 && buckets[top].users > 0 ? top : null);
  const activePct = learners ? (active / learners) * 100 : 0;

  return (
    <ChartCard
      title="Learners on a streak"
      hero={
        <>
          {fmt(active)}
          <span className="ml-2 text-lg font-normal text-muted-foreground">
            of {fmt(learners)}
          </span>
        </>
      }
      badge={
        <span className="text-sm text-muted-foreground">
          <span className="font-medium text-foreground">
            {activePct.toFixed(0)}%
          </span>{" "}
          practised yesterday or today · striped bar = no current streak
        </span>
      }
    >
      <ResponsiveContainer width="100%" height="100%">
        <BarChart
          data={buckets}
          margin={{ top: 4, right: 0, bottom: 0, left: -12 }}
          barCategoryGap="18%"
          onMouseMove={(s) =>
            setHover(
              typeof s.activeTooltipIndex === "number"
                ? s.activeTooltipIndex
                : null,
            )
          }
          onMouseLeave={() => setHover(null)}
        >
          <HatchDefs />
          <CartesianGrid vertical={false} stroke="hsl(var(--border))" />
          <XAxis dataKey="label" {...axisProps} tickMargin={8} />
          <YAxis
            {...axisProps}
            tickCount={4}
            width={48}
            allowDecimals={false}
          />
          <Tooltip
            cursor={false}
            content={({ active: isActive, payload }) => {
              const b = payload?.[0]?.payload as StreakBucket | undefined;
              if (!isActive || !b) return null;
              return (
                <TooltipBox>
                  <p className="font-medium">
                    {b.bucket === "0"
                      ? "No current streak"
                      : `Streak of ${b.label.replace(" d", " days").replace(" wk", " weeks")}`}
                  </p>
                  <p className="text-lg font-semibold">
                    {fmt(b.users)} learners
                  </p>
                  <p className="text-muted-foreground">
                    {b.share.toFixed(1)}% of all learners
                  </p>
                </TooltipBox>
              );
            }}
          />
          <Bar
            dataKey="users"
            radius={[6, 6, 6, 6]}
            minPointSize={2}
            isAnimationActive={false}
          >
            {buckets.map((b, i) => (
              <Cell
                key={b.bucket}
                fill={
                  i === highlighted
                    ? ACCENT
                    : b.bucket === "0"
                      ? `url(#${HATCH_ID})`
                      : INK
                }
              />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </ChartCard>
  );
};
