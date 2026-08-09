import { useTable } from "@refinedev/react-table";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { ColumnDef } from "@tanstack/react-table";
import { useMemo, useState } from "react";
import { toast } from "sonner";
import { apiFetch, ApiError } from "../../lib/api";
import type { AdminUser, Role } from "../../lib/types";
import {
  formatDate,
  isSuspended,
  PageHeader,
  StatusBadge,
} from "../../components/shared";
import { DataTable } from "../../components/table/data-table";
import { Button } from "../../components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "../../components/ui/dialog";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "../../components/ui/dropdown-menu";
import { Input } from "../../components/ui/input";
import { Label } from "../../components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../../components/ui/select";
import { Textarea } from "../../components/ui/textarea";
import { MoreHorizontal } from "lucide-react";

const errMessage = (e: unknown) =>
  e instanceof ApiError ? e.message : "Something went wrong";

export const UserList = () => {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState("");
  const [role, setRole] = useState<string | undefined>();
  const [suspended, setSuspended] = useState<string | undefined>();
  const [suspendTarget, setSuspendTarget] = useState<AdminUser | null>(null);
  const [reason, setReason] = useState("");
  const [until, setUntil] = useState("");

  const columns = useMemo<ColumnDef<AdminUser>[]>(
    () => [
      {
        id: "username",
        header: "User",
        cell: ({ row }) => (
          <div>
            <p className="font-medium">{row.original.username}</p>
            <p className="text-xs text-muted-foreground">
              {row.original.displayName ?? "—"} ·{" "}
              {row.original.email ?? "no email"}
            </p>
          </div>
        ),
      },
      {
        id: "role",
        header: "Role",
        cell: ({ row }) => <StatusBadge value={row.original.role} />,
      },
      {
        id: "status",
        header: "Status",
        cell: ({ row }) => {
          const u = row.original;
          if (isSuspended(u.suspendedUntil)) {
            return (
              <div className="text-sm">
                <StatusBadge value="SUSPENDED" />
                <p className="mt-1 text-xs text-muted-foreground">
                  until {formatDate(u.suspendedUntil)}
                  {u.suspensionReason ? ` · ${u.suspensionReason}` : ""}
                </p>
              </div>
            );
          }
          if (u.lockedUntil) {
            return (
              <span className="text-sm text-muted-foreground">
                Locked until {formatDate(u.lockedUntil)}
              </span>
            );
          }
          return <span className="text-sm text-muted-foreground">Active</span>;
        },
      },
      {
        id: "createdAt",
        header: "Joined",
        cell: ({ row }) => (
          <span className="text-sm">{formatDate(row.original.createdAt)}</span>
        ),
      },
      {
        id: "actions",
        header: "",
        cell: ({ row }) => <RowActions user={row.original} />,
      },
    ],
    [],
  );

  const table = useTable<AdminUser>({
    columns,
    refineCoreProps: {
      resource: "users",
      pagination: { pageSize: 20 },
      filters: {
        permanent: [
          { field: "query", operator: "contains", value: search || undefined },
          { field: "role", operator: "eq", value: role },
          { field: "suspended", operator: "eq", value: suspended },
        ],
      },
    },
  });

  const invalidate = () =>
    queryClient.invalidateQueries({ queryKey: ["users"] });

  const promote = useMutation({
    mutationFn: ({ id, role }: { id: string; role: Role }) =>
      apiFetch(`/admin/users/${id}/role`, { method: "POST", body: { role } }),
    onSuccess: () => {
      toast.success("Role updated");
      invalidate();
    },
    onError: (e) => toast.error(errMessage(e)),
  });

  const unsuspend = useMutation({
    mutationFn: (id: string) =>
      apiFetch(`/admin/users/${id}/suspension`, { method: "DELETE" }),
    onSuccess: () => {
      toast.success("Suspension lifted");
      invalidate();
    },
    onError: (e) => toast.error(errMessage(e)),
  });

  const suspend = useMutation({
    mutationFn: ({ id, reason, until }: { id: string; reason: string; until: string }) =>
      apiFetch(`/admin/users/${id}/suspension`, {
        method: "POST",
        body: { reason, until: new Date(until).toISOString() },
      }),
    onSuccess: () => {
      toast.success("User suspended");
      setSuspendTarget(null);
      setReason("");
      setUntil("");
      invalidate();
    },
    onError: (e) => toast.error(errMessage(e)),
  });

  const RowActions = ({ user }: { user: AdminUser }) => {
    const suspended = isSuspended(user.suspendedUntil);
    const isStaff = user.role !== "USER";
    return (
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="ghost" size="icon">
            <MoreHorizontal className="h-4 w-4" />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          {user.role !== "ADMIN" && (
            <DropdownMenuItem
              onClick={() =>
                promote.mutate({
                  id: user.id,
                  role: user.role === "USER" ? "MODERATOR" : "ADMIN",
                })
              }
            >
              Promote to {user.role === "USER" ? "MODERATOR" : "ADMIN"}
            </DropdownMenuItem>
          )}
          {!isStaff && !suspended && (
            <DropdownMenuItem onClick={() => setSuspendTarget(user)}>
              Suspend...
            </DropdownMenuItem>
          )}
          {suspended && (
            <DropdownMenuItem onClick={() => unsuspend.mutate(user.id)}>
              Lift suspension
            </DropdownMenuItem>
          )}
        </DropdownMenuContent>
      </DropdownMenu>
    );
  };

  return (
    <div>
      <PageHeader
        title="Users"
        description="Search, promote, suspend and manage accounts"
      />
      <div className="mb-4 flex flex-wrap gap-2">
        <Input
          placeholder="Search username, email or display name..."
          className="max-w-sm"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <Select
          value={role ?? "all"}
          onValueChange={(v) => setRole(v === "all" ? undefined : v)}
        >
          <SelectTrigger className="w-40">
            <SelectValue placeholder="Role" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All roles</SelectItem>
            <SelectItem value="USER">USER</SelectItem>
            <SelectItem value="MODERATOR">MODERATOR</SelectItem>
            <SelectItem value="ADMIN">ADMIN</SelectItem>
          </SelectContent>
        </Select>
        <Select
          value={suspended ?? "all"}
          onValueChange={(v) => setSuspended(v === "all" ? undefined : v)}
        >
          <SelectTrigger className="w-44">
            <SelectValue placeholder="Suspension" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">Any status</SelectItem>
            <SelectItem value="true">Suspended</SelectItem>
            <SelectItem value="false">Not suspended</SelectItem>
          </SelectContent>
        </Select>
      </div>
      <div className="rounded-md border bg-card">
        <DataTable tableProps={table} />
      </div>

      <Dialog
        open={!!suspendTarget}
        onOpenChange={(open) => !open && setSuspendTarget(null)}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Suspend {suspendTarget?.username}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label>Reason</Label>
              <Textarea
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                placeholder="Reason for suspension"
              />
            </div>
            <div className="space-y-2">
              <Label>Suspended until</Label>
              <Input
                type="datetime-local"
                value={until}
                onChange={(e) => setUntil(e.target.value)}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setSuspendTarget(null)}>
              Cancel
            </Button>
            <Button
              variant="destructive"
              disabled={!reason.trim() || !until || suspend.isPending}
              onClick={() =>
                suspendTarget &&
                suspend.mutate({ id: suspendTarget.id, reason, until })
              }
            >
              {suspend.isPending ? "Suspending..." : "Suspend"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};

