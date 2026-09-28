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
  "USER_ROLE_CHANGE",
  "USER_SUSPEND",
  "USER_UNSUSPEND",
  "REPORT_RESOLVED",
  "REPORT_DISMISSED",
  "POST_HIDE",
  "POST_RESTORE",
  "COMMENT_HIDE",
  "COMMENT_RESTORE",
];

const TARGET_TYPES = ["USER", "REPORT", "POST", "COMMENT", "BLOG"];

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
              {row.original.targetId.slice(0, 8)}...
            </code>
          </span>
        ),
      },
      {
        id: "actorId",
        header: "Actor",
        cell: ({ row }) => (
          <code className="text-xs">{row.original.actorId.slice(0, 8)}...</code>
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
    filters: { targetType, action, actorId, targetId },
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
      <div className="rounded-md border bg-card">
        <DataTable tableProps={table} />
      </div>
    </div>
  );
};
