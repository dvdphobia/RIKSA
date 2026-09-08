import { type HttpError, useNavigation } from "@refinedev/core";
import { useForm } from "@refinedev/react-hook-form";
import { useQuery } from "@tanstack/react-query";
import { toast } from "sonner";
import { apiFetch } from "../../lib/api";
import type { Category, DifficultyLevel, Lesson } from "../../lib/types";
import { Button } from "../../components/ui/button";
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
import { Skeleton } from "../../components/ui/skeleton";
import { ChevronLeft } from "lucide-react";
import { lazy, Suspense, useEffect, useState } from "react";
import {
  ExerciseEditor,
  type ExerciseItem,
  emptyExerciseItem,
} from "../../components/lessons/exercise-editor";

// The rich text editor pulls in ProseMirror and the markdown converters; keep
// them out of the main bundle so only the lesson form pays for them.
const LessonBodyEditor = lazy(() =>
  import("../../components/editor/lesson-body-editor").then((m) => ({
    default: m.LessonBodyEditor,
  })),
);

const DIFFICULTIES: DifficultyLevel[] = ["EASY", "MEDIUM", "HARD", "DIFFICULT"];

interface LessonFormValues {
  title: string;
  description?: string;
  difficulty: DifficultyLevel;
  baseXp: number;
  position: number;
  categoryId?: string;
  reflectionPrompt?: string;
}

export const LessonForm = ({ mode }: { mode: "create" | "edit" }) => {
  const { list } = useNavigation();
  const isEdit = mode === "edit";
  const [exercises, setExercises] = useState<ExerciseItem[]>([emptyExerciseItem()]);

  const categories = useQuery({
    queryKey: ["categories-all"],
    queryFn: () => apiFetch<Category[]>("/admin/categories"),
  });

  const {
    refineCore: { onFinish, query: recordQuery },
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<Lesson, HttpError, LessonFormValues>({
    refineCoreProps: {
      resource: "lessons",
      action: mode,
      onMutationSuccess: () => {
        toast.success(isEdit ? "Lesson saved" : "Lesson created");
        list("lessons");
      },
      onMutationError: (e) => toast.error(e.message || "Save failed"),
    },
    defaultValues: {
      title: "",
      difficulty: "EASY",
      baseXp: 10,
      position: 1,
    },
  });

  const difficulty = watch("difficulty");

  // Load existing exercises into state when editing an existing lesson
  useEffect(() => {
    if (recordQuery?.data?.data) {
      const lesson = recordQuery.data.data;
      if (Array.isArray(lesson.exercises) && lesson.exercises.length > 0) {
        setExercises(
          lesson.exercises.map((e: any) => ({
            id: e.id,
            question: e.question || "",
            answerKey: e.answerKey || "",
            difficulty: (e.difficulty as DifficultyLevel) || "EASY",
            points: Number(e.points) || 5,
            type: e.type || "MULTIPLE_CHOICE",
            options:
              Array.isArray(e.options) && e.options.length > 0
                ? e.options
                : ["", "", "", ""],
            hint: e.hint || "",
            explanation: e.explanation || "",
          })),
        );
      }
    }
  }, [recordQuery?.data?.data]);

  const submit = (values: LessonFormValues) => {
    // Client-side exercise validation
    if (exercises.length === 0) {
      toast.error("Please add at least one exercise");
      return;
    }

    for (let i = 0; i < exercises.length; i++) {
      const ex = exercises[i];
      if (!ex.question.trim()) {
        toast.error(`Exercise #${i + 1}: Please provide a question prompt`);
        return;
      }

      if (ex.type === "MULTIPLE_CHOICE") {
        const cleanOpts = ex.options.map((o) => o.trim()).filter(Boolean);
        if (cleanOpts.length < 2) {
          toast.error(`Exercise #${i + 1}: Needs at least 2 options`);
          return;
        }
        if (!ex.answerKey || !cleanOpts.includes(ex.answerKey.trim())) {
          toast.error(
            `Exercise #${i + 1}: Please select a valid correct answer from the options`,
          );
          return;
        }
      } else if (ex.type === "TRUE_FALSE") {
        if (!["True", "False"].includes(ex.answerKey)) {
          toast.error(`Exercise #${i + 1}: Please select True or False`);
          return;
        }
      } else if (ex.type === "TEXT") {
        if (!ex.answerKey.trim()) {
          toast.error(`Exercise #${i + 1}: Please provide an expected answer key`);
          return;
        }
      }
    }

    const payload: Record<string, unknown> = {
      title: values.title.trim(),
      description: values.description || null,
      difficulty: values.difficulty,
      baseXp: Number(values.baseXp) || 0,
      position: Number(values.position) || 1,
      categoryId: values.categoryId || null,
      reflectionPrompt: values.reflectionPrompt?.trim() || null,
      exercises: exercises.map((e) => ({
        question: e.question.trim(),
        answerKey: e.answerKey.trim(),
        difficulty: e.difficulty,
        points: Number(e.points) || 5,
        type: e.type,
        options:
          e.type === "TRUE_FALSE"
            ? ["True", "False"]
            : e.type === "MULTIPLE_CHOICE"
            ? e.options.map((option) => option.trim()).filter(Boolean)
            : [],
        hint: e.hint?.trim() || null,
        explanation: e.explanation?.trim() || null,
      })),
    };

    onFinish(payload as unknown as LessonFormValues);
  };

  return (
    <div className="mx-auto max-w-4xl pb-16">
      <div className="mb-6 flex items-center gap-2">
        <Button variant="ghost" size="icon" onClick={() => list("lessons")}>
          <ChevronLeft className="h-4 w-4" />
        </Button>
        <div>
          <h1 className="text-2xl font-semibold">
            {isEdit ? "Edit lesson" : "New lesson"}
          </h1>
          <p className="text-xs text-muted-foreground">
            Configure lesson details, learning content, and interactive QCM exercises.
          </p>
        </div>
      </div>

      <form onSubmit={handleSubmit(submit)} className="space-y-6">
        {/* Basic Settings */}
        <div className="space-y-4 rounded-md border bg-card p-6">
          <div className="space-y-2">
            <Label>Title *</Label>
            <Input {...register("title", { required: "Title is required" })} />
            {errors.title && (
              <p className="text-sm text-destructive">{errors.title.message}</p>
            )}
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
            <div className="space-y-2">
              <Label>Difficulty *</Label>
              <Select
                value={difficulty}
                onValueChange={(v) =>
                  setValue("difficulty", v as DifficultyLevel)
                }
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {DIFFICULTIES.map((d) => (
                    <SelectItem key={d} value={d}>
                      {d}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label>Base XP</Label>
              <Input
                type="number"
                min={0}
                {...register("baseXp", { valueAsNumber: true })}
              />
            </div>
            <div className="space-y-2">
              <Label>Position *</Label>
              <Input
                type="number"
                min={1}
                {...register("position", { required: "Position is required" })}
              />
            </div>
            <div className="space-y-2">
              <Label>Category</Label>
              <Select
                value={watch("categoryId") ?? "none"}
                onValueChange={(v) =>
                  setValue("categoryId", v === "none" ? undefined : v)
                }
              >
                <SelectTrigger>
                  <SelectValue placeholder="None" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">None</SelectItem>
                  {(categories.data ?? []).map((c) => (
                    <SelectItem key={c.id} value={c.id}>
                      {c.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
          <div className="space-y-2">
            <Label>Reflection prompt (Optional)</Label>
            <Textarea
              rows={2}
              placeholder="e.g. “What was your favorite phrase from today's lesson?”"
              {...register("reflectionPrompt")}
            />
          </div>
        </div>

        {/* Lesson Markdown Content */}
        <div className="space-y-4 rounded-md border bg-card p-6">
          <div>
            <h2 className="text-lg font-medium">Lesson Content & Dialogue</h2>
            <p className="text-sm text-muted-foreground">
              Write the lesson explanation, cultural notes, or vocabulary dialogue.
              Each section corresponds to a step in the learner's journey.
            </p>
          </div>
          <Suspense fallback={<Skeleton className="h-[420px] w-full" />}>
            <LessonBodyEditor
              value={watch("description") ?? ""}
              onChange={(markdown) =>
                setValue("description", markdown, { shouldDirty: true })
              }
            />
          </Suspense>
        </div>

        {/* Interactive QCM Exercises Builder */}
        <div className="space-y-4 rounded-md border bg-card p-6">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-lg font-medium">
                Lesson Exercises ({exercises.length})
              </h2>
              <p className="text-sm text-muted-foreground">
                Build multiple-choice (QCM) questions with 4 options, hints, and explanations.
              </p>
            </div>
          </div>

          <ExerciseEditor exercises={exercises} onChange={setExercises} />
        </div>

        <div className="flex items-center justify-end gap-3 pt-2">
          <Button
            type="button"
            variant="outline"
            onClick={() => list("lessons")}
          >
            Cancel
          </Button>
          <Button type="submit" disabled={isSubmitting}>
            {isSubmitting ? "Saving..." : isEdit ? "Update Lesson" : "Create Lesson"}
          </Button>
        </div>
      </form>
    </div>
  );
};

export const LessonCreate = () => <LessonForm mode="create" />;
export const LessonEdit = () => <LessonForm mode="edit" />;
