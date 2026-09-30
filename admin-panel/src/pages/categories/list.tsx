import { useMutation } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { useMemo, useState } from "react";
import { useNavigate } from "react-router";
import { toast } from "sonner";
import { apiFetch } from "../../lib/api";
import type { Category } from "../../lib/types";
import { formatDate, PageHeader } from "../../components/shared";
import { useServerTable } from "../../lib/use-server-table";
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
import { Pencil, Plus, Trash2 } from "lucide-react";

export const CategoryList = () => {
  const navigate = useNavigate();
  const [deleteTarget, setDeleteTarget] = useState<Category | null>(null);

  const columns = useMemo<ColumnDef<Category>[]>(
    () => [
      { id: "name", accessorKey: "name", header: "Name" },
      {
        id: "slug",
        accessorKey: "slug",
        header: "Slug",
        cell: ({ getValue }) => (
          <code className="text-xs">{getValue<string>()}</code>
        ),
      },
      {
        id: "description",
        accessorKey: "description",
        header: "Description",
        cell: ({ getValue }) => (
          <span className="max-w-md truncate text-muted-foreground">
            {getValue<string>() ?? "—"}
          </span>
        ),
      },
      {
        id: "updatedAt",
        header: "Updated",
        cell: ({ row }) => (
          <span className="text-sm">{formatDate(row.original.updatedAt)}</span>
        ),
      },
      {
        id: "actions",
        header: () => <div className="text-right">Actions</div>,
        cell: ({ row }) => (
          <div className="flex justify-end gap-1">
            <Button
              variant="ghost"
              size="icon"
              onClick={() => navigate(`/categories/edit/${row.original.id}`)}
            >
              <Pencil className="h-4 w-4" />
            </Button>
            <Button
              variant="ghost"
              size="icon"
              onClick={() => setDeleteTarget(row.original)}
            >
              <Trash2 className="h-4 w-4 text-destructive" />
            </Button>
          </div>
        ),
      },
    ],
    [navigate],
  );

  const table = useServerTable<Category>({
    resource: "categories",
    columns,
    pageSize: 50,
    paginated: false,
  });

  const deleteCategory = useMutation({
    mutationFn: (id: string) =>
      apiFetch(`/admin/categories/${id}`, { method: "DELETE" }),
    onSuccess: () => {
      toast.success("Category deleted");
      setDeleteTarget(null);
      table.query.refetch();
    },
    onError: (e) => toast.error(e.message || "Delete failed"),
  });

  return (
    <div>
      <PageHeader
        title="Categories"
        description="Lesson categories"
        actions={
          <Button onClick={() => navigate("/categories/create")}>
            <Plus className="mr-1 h-4 w-4" /> New category
          </Button>
        }
      />
      <div className="rounded-md border bg-card">
        <DataTable tableProps={table} />
      </div>

      <Dialog
        open={!!deleteTarget}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Delete "{deleteTarget?.name}"?</DialogTitle>
            <DialogDescription>
              Lessons in this category will not be deleted, but their category
              will be unset. This cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteTarget(null)}>
              Cancel
            </Button>
            <Button
              variant="destructive"
              disabled={deleteCategory.isPending}
              onClick={() => deleteTarget && deleteCategory.mutate(deleteTarget.id)}
            >
              Delete
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};
