import { useMutation, useQuery } from "@tanstack/react-query";
import type { ColumnDef } from "@tanstack/react-table";
import { useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router";
import { toast } from "sonner";
import { apiFetch } from "../../lib/api";
import type { Category, Lesson } from "../../lib/types";
import { formatDate, PageHeader, StatusBadge } from "../../components/shared";
import { useServerTable } from "../../lib/use-server-table";
import { DataTable } from "../../components/table/data-table";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../../components/ui/select";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "../../components/ui/dialog";
import { Textarea } from "../../components/ui/textarea";
import { Badge } from "../../components/ui/badge";
import { BookOpen, CheckCircle2, Pencil, Plus, Trash2, Upload } from "lucide-react";

interface BulkResult {
  successCount: number;
  failureCount: number;
  errors: string[];
}

const stripPublished = (input: unknown) =>
  Array.isArray(input)
    ? input.map((lesson) => {
        if (!lesson || typeof lesson !== "object" || Array.isArray(lesson)) return lesson;
        const request = { ...(lesson as Record<string, unknown>) };
        delete request.published;
        return request;
      })
    : input;

export const LessonList = () => {
  const navigate = useNavigate();
  const [deleteTarget, setDeleteTarget] = useState<Lesson | null>(null);
  const [previewLesson, setPreviewLesson] = useState<Lesson | null>(null);
  const [bulkOpen, setBulkOpen] = useState(false);
  const [bulkJson, setBulkJson] = useState("");
  const [search, setSearch] = useState("");
  const [difficulty, setDifficulty] = useState<string | undefined>();
  const [categoryId, setCategoryId] = useState<string | undefined>();
  const fileInput = useRef<HTMLInputElement>(null);

  const categoriesQuery = useQuery({
    queryKey: ["categories", "all"],
    queryFn: () => apiFetch<Category[]>("/admin/categories"),
  });
  const categories = categoriesQuery.data ?? [];

  const columns = useMemo<ColumnDef<Lesson>[]>(
    () => [
      {
        id: "title",
        accessorKey: "title",
        header: "Title",
        cell: ({ row }) => (
          <div>
            <p className="font-medium">{row.original.title}</p>
            {row.original.reflectionPrompt && (
              <p className="max-w-md truncate text-xs text-muted-foreground">
                Reflection: {row.original.reflectionPrompt}
              </p>
            )}
          </div>
        ),
      },
      {
        id: "difficulty",
        header: "Difficulty",
        cell: ({ row }) => <StatusBadge value={row.original.difficulty} />,
      },
      { id: "baseXp", accessorKey: "baseXp", header: "XP" },
      {
        id: "exercises",
        header: "Exercises",
        cell: ({ row }) => (
          <Button
            variant="outline"
            size="sm"
            className="h-7 gap-1.5 text-xs font-medium"
            onClick={() => setPreviewLesson(row.original)}
            title="Click to preview exercises"
          >
            <CheckCircle2 className="h-3.5 w-3.5 text-emerald-600" />
            <span>{row.original.exercises.length} QCM</span>
          </Button>
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
              onClick={() => navigate(`/lessons/edit/${row.original.id}`)}
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

  const table = useServerTable<Lesson>({
    resource: "lessons",
    columns,
    pageSize: 20,
    filters: { query: search, difficulty, categoryId },
  });

  const refetch = () => table.query.refetch();

  const deleteLesson = useMutation({
    mutationFn: (id: string) =>
      apiFetch(`/admin/lessons/${id}`, { method: "DELETE" }),
    onSuccess: () => {
      toast.success("Lesson deleted");
      setDeleteTarget(null);
      refetch();
    },
    onError: (e) => toast.error(e.message || "Delete failed"),
  });

  const bulkImport = useMutation({
    mutationFn: (items: unknown) =>
      apiFetch<BulkResult>("/admin/lessons/bulk", {
        method: "POST",
        body: stripPublished(items),
      }),
    onSuccess: (data) => {
      toast.success(
        `Imported ${data.successCount} lesson(s)` +
          (data.failureCount ? `, ${data.failureCount} failed` : ""),
      );
      data.errors.forEach((e) => toast.error(e));
      setBulkOpen(false);
      setBulkJson("");
      refetch();
    },
    onError: (e) =>
      toast.error(e instanceof Error ? e.message : "Import failed"),
  });

  const upload = useMutation({
    mutationFn: async (files: File[]) => {
      if (files.length === 1) {
        const fd = new FormData();
        fd.append("file", files[0]);
        await apiFetch("/admin/lessons/upload", { method: "POST", formData: fd });
        return { successCount: 1, failureCount: 0, errors: [] };
      }
      const fd = new FormData();
      for (const f of files) fd.append("files", f);
      return apiFetch<BulkResult>("/admin/lessons/upload/bulk", {
        method: "POST",
        formData: fd,
      });
    },
    onSuccess: (data) => {
      toast.success(
        `Uploaded ${data.successCount} lesson(s)` +
          (data.failureCount ? `, ${data.failureCount} failed` : ""),
      );
      data.errors.forEach((e) => toast.error(e));
      refetch();
    },
    onError: (e) =>
      toast.error(e instanceof Error ? e.message : "Upload failed"),
  });

  return (
    <div>
      <PageHeader
        title="Lessons"
        description="Manage lessons and exercises"
        actions={
          <>
            <input
              ref={fileInput}
              type="file"
              accept=".md,.markdown"
              multiple
              className="hidden"
              onChange={(e) => {
                const selected = Array.from(e.target.files ?? []);
                e.target.value = "";
                if (selected.length) upload.mutate(selected);
              }}
            />
            <Button
              variant="outline"
              onClick={() => fileInput.current?.click()}
              disabled={upload.isPending}
            >
              <Upload className="mr-1 h-4 w-4" /> Upload .md
            </Button>
            <Button variant="outline" onClick={() => setBulkOpen(true)}>
              Bulk JSON
            </Button>
            <Button onClick={() => navigate("/lessons/create")}>
              <Plus className="mr-1 h-4 w-4" /> New lesson
            </Button>
          </>
        }
      />
      <div className="mb-4 flex flex-wrap gap-2">
        <Input
          placeholder="Search lessons by title..."
          className="max-w-sm"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <Select
          value={difficulty ?? "all"}
          onValueChange={(v) => setDifficulty(v === "all" ? undefined : v)}
        >
          <SelectTrigger className="w-40">
            <SelectValue placeholder="Difficulty" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All difficulties</SelectItem>
            <SelectItem value="EASY">EASY</SelectItem>
            <SelectItem value="MEDIUM">MEDIUM</SelectItem>
            <SelectItem value="HARD">HARD</SelectItem>
            <SelectItem value="DIFFICULT">DIFFICULT</SelectItem>
          </SelectContent>
        </Select>
        <Select
          value={categoryId ?? "all"}
          onValueChange={(v) => setCategoryId(v === "all" ? undefined : v)}
        >
          <SelectTrigger className="w-48">
            <SelectValue placeholder="Category" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All categories</SelectItem>
            {categories.map((c) => (
              <SelectItem key={c.id} value={c.id}>
                {c.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
      <div className="rounded-md border bg-card">
        <DataTable tableProps={table} />
      </div>

      <Dialog
        open={!!deleteTarget}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Delete "{deleteTarget?.title}"?</DialogTitle>
            <DialogDescription>
              This hard-deletes the lesson, its exercises, and erases all user
              completion records for it. This cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteTarget(null)}>
              Cancel
            </Button>
            <Button
              variant="destructive"
              disabled={deleteLesson.isPending}
              onClick={() => deleteTarget && deleteLesson.mutate(deleteTarget.id)}
            >
              Delete permanently
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={bulkOpen} onOpenChange={setBulkOpen}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>Bulk import lessons (JSON)</DialogTitle>
            <DialogDescription>
              Paste a JSON array of lesson objects (same shape as the create
              endpoint).
            </DialogDescription>
          </DialogHeader>
          <Textarea
            rows={12}
            className="font-mono text-xs"
            placeholder='[{ "title": "...", "difficulty": "EASY", "exercises": [{ "question": "...", "answerKey": "...", "difficulty": "EASY" }] }]'
            value={bulkJson}
            onChange={(e) => setBulkJson(e.target.value)}
          />
          <DialogFooter>
            <Button variant="outline" onClick={() => setBulkOpen(false)}>
              Cancel
            </Button>
            <Button
              disabled={bulkImport.isPending}
              onClick={() => {
                try {
                  bulkImport.mutate(JSON.parse(bulkJson));
                } catch {
                  toast.error("Invalid JSON");
                }
              }}
            >
              {bulkImport.isPending ? "Importing..." : "Import"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Exercise Preview Modal */}
      <Dialog
        open={Boolean(previewLesson)}
        onOpenChange={(open) => !open && setPreviewLesson(null)}
      >
        <DialogContent className="max-w-2xl max-h-[85vh] overflow-y-auto">
          <DialogHeader>
            <div className="flex items-center gap-2">
              <DialogTitle>
                {previewLesson?.title}
              </DialogTitle>
              {previewLesson && <StatusBadge value={previewLesson.difficulty} />}
            </div>
            <DialogDescription>
              {previewLesson?.exercises.length} Exercise(s) • {previewLesson?.baseXp} Base XP
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4 py-2">
            {previewLesson?.exercises.map((ex, i) => (
              <div key={ex.id ?? i} className="rounded-lg border p-4 space-y-2.5 bg-muted/20">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold text-primary">
                    Question #{i + 1}
                  </span>
                  <div className="flex items-center gap-2">
                    <span className="text-xs text-muted-foreground">{ex.points} pts</span>
                    <Badge variant="outline" className="text-[11px]">{ex.type}</Badge>
                  </div>
                </div>
                <p className="font-medium text-sm">{ex.question}</p>

                {ex.options && ex.options.length > 0 && (
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-1">
                    {ex.options.map((opt, optIndex) => {
                      const isCorrect = opt.trim() === ex.answerKey.trim();
                      const letter = String.fromCharCode(65 + optIndex);
                      return (
                        <div
                          key={optIndex}
                          className={`flex items-center gap-2 rounded-md border p-2 text-xs ${
                            isCorrect
                              ? "border-emerald-500 bg-emerald-50 text-emerald-900 font-semibold dark:bg-emerald-950/40 dark:text-emerald-200"
                              : "bg-background text-muted-foreground"
                          }`}
                        >
                          <span className={`flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-[10px] font-bold ${
                            isCorrect ? "bg-emerald-600 text-white" : "bg-muted text-muted-foreground"
                          }`}>
                            {letter}
                          </span>
                          <span className="truncate">{opt}</span>
                          {isCorrect && <CheckCircle2 className="ml-auto h-3.5 w-3.5 shrink-0 text-emerald-600" />}
                        </div>
                      );
                    })}
                  </div>
                )}

                {ex.explanation && (
                  <p className="text-xs text-muted-foreground italic pt-1 border-t">
                    Explanation: {ex.explanation}
                  </p>
                )}
              </div>
            ))}
          </div>

          <DialogFooter className="gap-2 sm:gap-0">
            <Button variant="outline" onClick={() => setPreviewLesson(null)}>
              Close
            </Button>
            {previewLesson && (
              <Button
                onClick={() => {
                  const id = previewLesson.id;
                  setPreviewLesson(null);
                  navigate(`/lessons/edit/${id}`);
                }}
              >
                <Pencil className="mr-1.5 h-4 w-4" /> Edit Exercises
              </Button>
            )}
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};
