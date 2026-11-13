import type { ColumnDef } from "@tanstack/react-table";
import { useMemo, useState } from "react";
import type { AuditLog } from "../../lib/types";
import { formatDate, PageHeader } from "../../components/shared";
import { useServerTable } from "../../lib/use-server-table";
import { DataTable } from "../../components/table/data-table";
import { Badge } from "../../components/ui/badge";
import { Input } from "../../components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../../components/ui/select";

const ACTIONS = [
  "AI_CHAT_TRANSCRIPT_VIEW",
  "ACHIEVEMENT_CREATE",
  "ACHIEVEMENT_DELETE",
  "ACHIEVEMENT_UPDATE",
  "APP_SETTING_UPDATE",
  "COMMENT_HIDE",
  "COMMENT_MEDIA_REMOVE",
  "COMMENT_RESTORE",
  "LEADERBOARD_REFRESH",
  "LESSON_CREATE",
  "LESSON_DELETE",
  "LESSON_REORDER",
  "LESSON_UPDATE",
  "MARKET_ITEM_CREATE",
  "MARKET_ITEM_DEACTIVATE",
  "MARKET_ITEM_DELETE",
  "MARKET_ITEM_UPDATE",
  "POST_HIDE",
  "POST_MEDIA_REMOVE",
  "POST_RESTORE",
  "RANK_TIER_UPDATE",
  "REPORT_DISMISSED",
  "REPORT_RESOLVED",
  "RESOURCE_DELETE",
  "RESOURCE_HIDE",
  "RESOURCE_UNHIDE",
  "ROADMAP_UNIT_CREATE",
  "ROADMAP_UNIT_DELETE",
  "ROADMAP_UNIT_REORDER",
  "ROADMAP_UNIT_UPDATE",
  "STREAK_MILESTONE_CREATE",
  "STREAK_MILESTONE_DELETE",
  "STREAK_MILESTONE_UPDATE",
  "USER_ACHIEVEMENT_GRANT",
  "USER_ACHIEVEMENT_REVOKE",
  "USER_LEADERBOARD_EXCLUSION",
  "USER_ROLE_CHANGE",
  "USER_STREAK_ADJUST",
  "USER_SUSPEND",
  "USER_UNSUSPEND",
  "USER_UNLOCK",
  "USER_XP_ADJUST",
  "XP_BOOST_CANCEL",
  "XP_BOOST_CREATE",
];

const TARGET_TYPES = [
  "ACHIEVEMENT",
  "APP_SETTING",
  "CATEGORY",
  "CHAT_SESSION",
  "COMMENT",
  "LEADERBOARD",
  "LESSON",
  "LESSON_RESOURCE",
  "MARKET_ITEM",
  "POST",
  "RANK_TIER",
  "REPORT",
  "ROADMAP_UNIT",
  "STREAK_MILESTONE",
  "USER",
  "XP_BOOST",
];
const isUuid = (value: string) =>
  /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value);

export const AuditLogList = () => {
  const [targetType, setTargetType] = useState<string | undefined>();
  const [action, setAction] = useState<string | undefined>();
  const [actorId, setActorId] = useState("");
  const [targetId, setTargetId] = useState("");

  const columns = useMemo<ColumnDef<AuditLog>[]>(
    () => [
      {
        id: "createdAt",
        header: "Time",
        cell: ({ row }) => (
          <span className="whitespace-nowrap text-sm">
            {formatDate(row.original.createdAt)}
          </span>
        ),
      },
      {
        id: "action",
        header: "Action",
        cell: ({ row }) => <Badge variant="outline">{row.original.action}</Badge>,
      },
      {
        id: "target",
        header: "Target",
        cell: ({ row }) => (
          <span className="text-sm">
            {row.original.targetType}{" "}
              <code className="text-xs text-muted-foreground">
              {row.original.targetId ? `${row.original.targetId.slice(0, 8)}...` : "—"}
            </code>
          </span>
        ),
      },
      {
        id: "actorId",
        header: "Actor",
        cell: ({ row }) => (
          <code className="text-xs">
            {row.original.actorId ? `${row.original.actorId.slice(0, 8)}...` : "Deleted account"}
          </code>
        ),
      },
      {
        id: "details",
        header: "Details",
        cell: ({ row }) => (
          <span className="max-w-md truncate font-mono text-xs text-muted-foreground">
            {row.original.details ? JSON.stringify(row.original.details) : "—"}
          </span>
        ),
      },
    ],
    [],
  );

  const table = useServerTable<AuditLog>({
    resource: "audit-logs",
    columns,
    pageSize: 20,
    filters: {
      targetType,
      action,
      actorId: isUuid(actorId) ? actorId : undefined,
      targetId: isUuid(targetId) ? targetId : undefined,
    },
  });

  return (
    <div>
      <PageHeader title="Audit logs" description="Every admin action is recorded" />
      <div className="mb-4 flex flex-wrap gap-2">
        <Select
          value={targetType ?? "all"}
          onValueChange={(v) => setTargetType(v === "all" ? undefined : v)}
        >
          <SelectTrigger className="w-40">
            <SelectValue placeholder="Target type" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All targets</SelectItem>
            {TARGET_TYPES.map((t) => (
              <SelectItem key={t} value={t}>
                {t}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Select
          value={action ?? "all"}
          onValueChange={(v) => setAction(v === "all" ? undefined : v)}
        >
          <SelectTrigger className="w-52">
            <SelectValue placeholder="Action" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All actions</SelectItem>
            {ACTIONS.map((a) => (
              <SelectItem key={a} value={a}>
                {a}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Input
          className="max-w-64"
          placeholder="Actor ID"
          value={actorId}
          onChange={(e) => setActorId(e.target.value)}
        />
        <Input
          className="max-w-64"
          placeholder="Target ID"
          value={targetId}
          onChange={(e) => setTargetId(e.target.value)}
        />
      </div>
      <p className="mb-3 text-xs text-muted-foreground">
        Actor and target filters apply after you enter a complete UUID.
      </p>
      <div className="rounded-md border bg-card">
        <DataTable tableProps={table} />
      </div>
    </div>
  );
};
