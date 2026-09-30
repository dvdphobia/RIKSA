import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Pencil, Plus, Trash2 } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router";
import { toast } from "sonner";
import { apiFetch } from "../../lib/api";
import type { Category, Lesson, RoadmapUnit, SpringPage } from "../../lib/types";
import { EmptyState, errorMessage, PageHeader, StatusBadge } from "../../components/shared";
import { SortableList } from "../../components/sortable-list";
import { Button } from "../../components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "../../components/ui/dialog";
import { Input } from "../../components/ui/input";
import { Label } from "../../components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../../components/ui/select";
import { Skeleton } from "../../components/ui/skeleton";
import { Textarea } from "../../components/ui/textarea";

/** Lessons above this position are standalone and not part of the locked track. */
const TRACK_MAX = 50;

const sameOrder = (a: { id: string }[], b: { id: string }[]) =>
  a.length === b.length && a.every((x, i) => x.id === b[i].id);

export const Roadmap = () => {
  const categories = useQuery({
    queryKey: ["categories", "all"],
    queryFn: () => apiFetch<Category[]>("/admin/categories"),
  });
  const [categoryId, setCategoryId] = useState<string>();
  useEffect(() => {
    if (!categoryId && categories.data?.length) setCategoryId(categories.data[0].id);
  }, [categories.data, categoryId]);

  return (
    <div>
      <PageHeader
        title="Roadmap"
        description="Group a category's lessons into units and set the order learners unlock them"
      />
      <div className="mb-4">
        <Select value={categoryId} onValueChange={setCategoryId}>
          <SelectTrigger className="w-64">
            <SelectValue placeholder="Pick a category" />
          </SelectTrigger>
          <SelectContent>
            {(categories.data ?? []).map((c) => (
              <SelectItem key={c.id} value={c.id}>
                {c.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
      {categoryId ? (
        <div className="grid gap-6 lg:grid-cols-2">
          <Units categoryId={categoryId} />
          <LessonOrder categoryId={categoryId} />
        </div>
      ) : (
        !categories.isLoading && <EmptyState>Create a category first</EmptyState>
      )}
    </div>
  );
};

const Units = ({ categoryId }: { categoryId: string }) => {
  const qc = useQueryClient();
  const key = ["roadmap-units", categoryId];
  const units = useQuery({
    queryKey: key,
    queryFn: () => apiFetch<RoadmapUnit[]>("/admin/roadmap-units", { params: { categoryId } }),
  });
  const [order, setOrder] = useState<RoadmapUnit[]>([]);
  useEffect(() => setOrder(units.data ?? []), [units.data]);
  const [editing, setEditing] = useState<{ id: string | null; title: string; description: string } | null>(
    null,
  );
  const [deleting, setDeleting] = useState<RoadmapUnit | null>(null);
  const refresh = () => {
    qc.invalidateQueries({ queryKey: ["roadmap-units"] });
    qc.invalidateQueries({ queryKey: ["lessons"] });
  };

  const save = useMutation({
    mutationFn: ({ id, title, description }: NonNullable<typeof editing>) =>
      apiFetch(id ? `/admin/roadmap-units/${id}` : "/admin/roadmap-units", {
        method: id ? "PUT" : "POST",
        body: { categoryId, title, description },
      }),
    onSuccess: () => {
      toast.success("Unit saved");
      setEditing(null);
      refresh();
    },
    onError: (e) => toast.error(errorMessage(e)),
  });
  const remove = useMutation({
    mutationFn: (id: string) => apiFetch(`/admin/roadmap-units/${id}`, { method: "DELETE" }),
    onSuccess: () => {
      toast.success("Unit deleted");
      setDeleting(null);
      refresh();
    },
    onError: (e) => toast.error(errorMessage(e)),
  });
  const reorder = useMutation({
    mutationFn: () =>
      apiFetch<RoadmapUnit[]>("/admin/roadmap-units/reorder", {
        method: "PUT",
        body: { categoryId, ids: order.map((u) => u.id) },
      }),
    onSuccess: (data) => {
      toast.success("Unit order saved");
      qc.setQueryData(key, data);
    },
    onError: (e) => toast.error(errorMessage(e)),
  });
  const dirty = !sameOrder(order, units.data ?? []);

  return (
    <section className="space-y-3">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-medium">Units</h2>
        <div className="flex gap-2">
          {dirty && (
            <>
              <Button size="sm" variant="ghost" onClick={() => setOrder(units.data ?? [])}>
                Undo
              </Button>
              <Button size="sm" disabled={reorder.isPending} onClick={() => reorder.mutate()}>
                Save order
              </Button>
            </>
          )}
          <Button
            size="sm"
            variant="outline"
            onClick={() => setEditing({ id: null, title: "", description: "" })}
          >
            <Plus className="mr-1 h-4 w-4" /> Unit
          </Button>
        </div>
      </div>
      {units.isLoading && <Skeleton className="h-32 w-full" />}
      {!units.isLoading && order.length === 0 && (
        <EmptyState>No units — lessons show as one list</EmptyState>
      )}
      {order.length > 0 && (
        <SortableList
          items={order}
          getId={(u) => u.id}
          onChange={setOrder}
          renderItem={(u) => (
            <div className="flex items-center justify-between gap-2">
              <div className="min-w-0">
                <p className="truncate text-sm font-medium">{u.title}</p>
                <p className="text-xs text-muted-foreground">{u.lessonCount} lessons</p>
              </div>
              <div className="flex shrink-0">
                <Button
                  size="icon"
                  variant="ghost"
                  className="h-7 w-7"
                  onClick={() =>
                    setEditing({ id: u.id, title: u.title, description: u.description ?? "" })
                  }
                >
                  <Pencil className="h-3 w-3" />
                </Button>
                <Button
                  size="icon"
                  variant="ghost"
                  className="h-7 w-7"
                  onClick={() => setDeleting(u)}
                >
                  <Trash2 className="h-3 w-3 text-destructive" />
                </Button>
              </div>
            </div>
          )}
        />
      )}

      <Dialog open={!!editing} onOpenChange={(o) => !o && setEditing(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{editing?.id ? "Edit unit" : "New unit"}</DialogTitle>
          </DialogHeader>
          {editing && (
            <div className="grid gap-3">
              <div className="space-y-1">
                <Label>Title *</Label>
                <Input
                  value={editing.title}
                  onChange={(e) => setEditing({ ...editing, title: e.target.value })}
                />
              </div>
              <div className="space-y-1">
                <Label>Description</Label>
                <Textarea
                  rows={3}
                  value={editing.description}
                  onChange={(e) => setEditing({ ...editing, description: e.target.value })}
                />
              </div>
            </div>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setEditing(null)}>
              Cancel
            </Button>
            <Button
              disabled={!editing?.title.trim() || save.isPending}
              onClick={() => editing && save.mutate(editing)}
            >
              Save
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={!!deleting} onOpenChange={(o) => !o && setDeleting(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Delete unit "{deleting?.title}"?</DialogTitle>
            <DialogDescription>
              Its {deleting?.lessonCount ?? 0} lesson(s) are kept and become unassigned.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleting(null)}>
              Cancel
            </Button>
            <Button
              variant="destructive"
              disabled={remove.isPending}
              onClick={() => deleting && remove.mutate(deleting.id)}
            >
              Delete unit
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </section>
  );
};

const LessonOrder = ({ categoryId }: { categoryId: string }) => {
  const qc = useQueryClient();
  const lessons = useQuery({
    queryKey: ["lessons", "track", categoryId],
    queryFn: () =>
      apiFetch<SpringPage<Lesson>>("/admin/lessons", {
        params: { categoryId, page: 0, size: 100 },
      }),
  });
  const units = useQuery({
    queryKey: ["roadmap-units", categoryId],
    queryFn: () => apiFetch<RoadmapUnit[]>("/admin/roadmap-units", { params: { categoryId } }),
  });
  const unitName = useMemo(
    () => new Map((units.data ?? []).map((u) => [u.id, u.title])),
    [units.data],
  );

  const track = useMemo(
    () =>
      (lessons.data?.content ?? [])
        .filter((l) => l.position <= TRACK_MAX)
        .sort((a, b) => a.position - b.position),
    [lessons.data],
  );
  const standalone = (lessons.data?.content ?? []).filter((l) => l.position > TRACK_MAX);
  const [order, setOrder] = useState<Lesson[]>([]);
  useEffect(() => setOrder(track), [track]);
  const dirty = !sameOrder(order, track);

  const save = useMutation({
    mutationFn: () =>
      apiFetch("/admin/lessons/reorder", {
        method: "PUT",
        body: { categoryId, ids: order.map((l) => l.id) },
      }),
    onSuccess: () => {
      toast.success("Lesson order saved");
      qc.invalidateQueries({ queryKey: ["lessons"] });
    },
    onError: (e) => toast.error(errorMessage(e)),
  });

  return (
    <section className="space-y-3">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-medium">Lesson order</h2>
          <p className="text-xs text-muted-foreground">
            Learners unlock these one after another. Within the app, lessons
            are grouped under their unit.
          </p>
        </div>
        {dirty && (
          <div className="flex gap-2">
            <Button size="sm" variant="ghost" onClick={() => setOrder(track)}>
              Undo
            </Button>
            <Button size="sm" disabled={save.isPending} onClick={() => save.mutate()}>
              Save order
            </Button>
          </div>
        )}
      </div>
      {lessons.isLoading && <Skeleton className="h-48 w-full" />}
      {!lessons.isLoading && order.length === 0 && (
        <EmptyState>No lessons in this category's track</EmptyState>
      )}
      {order.length > 0 && (
        <SortableList
          items={order}
          getId={(l) => l.id}
          onChange={setOrder}
          renderItem={(l) => (
            <div className="flex items-center justify-between gap-2">
              <div className="min-w-0">
                <Link
                  to={`/lessons/edit/${l.id}`}
                  className="block truncate text-sm font-medium hover:underline"
                >
                  {l.title}
                </Link>
                <p className="truncate text-xs text-muted-foreground">
                  {l.roadmapUnitId ? unitName.get(l.roadmapUnitId) ?? "Unit" : "No unit"}
                </p>
              </div>
            </div>
          )}
        />
      )}
      {standalone.length > 0 && (
        <p className="text-xs text-muted-foreground">
          {standalone.length} standalone lesson(s) (position above {TRACK_MAX}) are
          always available and not reordered here.
        </p>
      )}
    </section>
  );
};
