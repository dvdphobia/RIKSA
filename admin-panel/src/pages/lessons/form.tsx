import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  FormProvider,
  useFieldArray,
  useForm,
  useFormContext,
  useWatch,
} from "react-hook-form";
import { useNavigate, useParams } from "react-router";
import { toast } from "sonner";
import { apiFetch } from "../../lib/api";
import type {
  Category,
  DifficultyLevel,
  Exercise,
  ExerciseType,
  Lesson,
  LessonBackgroundKey,
  RoadmapUnit,
} from "../../lib/types";
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
import { Alert, AlertDescription } from "../../components/ui/alert";
import { ChevronLeft, Plus, Trash2, X } from "lucide-react";
import { lazy, Suspense, useEffect } from "react";
import { LessonBackgroundSection } from "../../components/lesson-background-section";

// The rich text editor pulls in ProseMirror and the markdown converters; keep
// them out of the main bundle so only the lesson form pays for them.
const LessonBodyEditor = lazy(() =>
  import("../../components/editor/lesson-body-editor").then((m) => ({
    default: m.LessonBodyEditor,
  })),
);

const DIFFICULTIES: DifficultyLevel[] = ["EASY", "MEDIUM", "HARD", "DIFFICULT"];

const EXERCISE_TYPES: { value: ExerciseType; label: string }[] = [
  { value: "MULTIPLE_CHOICE", label: "Multiple choice" },
  { value: "TRUE_FALSE", label: "True / false" },
  { value: "TEXT", label: "Typed answer" },
];

interface ExerciseForm {
  type: ExerciseType;
  question: string;
  answerKey: string;
  options: string[];
  difficulty: DifficultyLevel;
  points: number;
  sectionIndex: number;
  hint: string;
  explanation: string;
}

interface LessonFormValues {
  title: string;
  description?: string;
  difficulty: DifficultyLevel;
  baseXp: number;
  estimatedMinutes: number;
  categoryId?: string;
  roadmapUnitId?: string;
  reflectionPrompt?: string;
  backgroundKey: LessonBackgroundKey | null;
  backgroundImageUrl: string | null;
  exercises: ExerciseForm[];
}

const emptyExercise = (sectionIndex = 1): ExerciseForm => ({
  type: "MULTIPLE_CHOICE",
  question: "",
  answerKey: "",
  options: ["", ""],
  difficulty: "EASY",
  points: 5,
  sectionIndex,
  hint: "",
  explanation: "",
});

const fromExercise = (e: Exercise): ExerciseForm => ({
  type: e.type ?? "TEXT",
  question: e.question,
  answerKey: e.answerKey ?? "",
  options:
    e.type === "MULTIPLE_CHOICE"
      ? e.options?.length
        ? e.options
        : ["", ""]
      : [],
  difficulty: e.difficulty,
  points: e.points,
  sectionIndex: e.sectionIndex ?? 1,
  hint: e.hint ?? "",
  explanation: e.explanation ?? "",
});

/** Mirrors the API's validation so mistakes show up before saving. */
const validateExercise = (e: ExerciseForm): string | null => {
  if (!e.question.trim()) return "Question is required";
  if (e.type === "MULTIPLE_CHOICE") {
    const opts = e.options.map((o) => o.trim()).filter(Boolean);
    if (opts.length < 2) return "Add at least two options";
    if (new Set(opts).size !== opts.length) return "Options must be different";
    if (!opts.includes(e.answerKey.trim())) return "Pick the correct option";
  } else if (e.type === "TRUE_FALSE") {
    if (e.answerKey !== "True" && e.answerKey !== "False")
      return "Pick True or False";
  } else if (!e.answerKey.trim()) {
    return "Answer is required";
  }
  return null;
};

const toPayload = (e: ExerciseForm) => ({
  type: e.type,
  question: e.question.trim(),
  answerKey: e.answerKey.trim(),
  options:
    e.type === "MULTIPLE_CHOICE"
      ? e.options.map((o) => o.trim()).filter(Boolean)
      : e.type === "TRUE_FALSE"
        ? ["True", "False"]
        : [],
  difficulty: e.difficulty,
  points: Number(e.points) || 5,
  sectionIndex: Number(e.sectionIndex) || 1,
  hint: e.hint.trim() || null,
  explanation: e.explanation.trim() || null,
});

export const LessonForm = ({ mode }: { mode: "create" | "edit" }) => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { id } = useParams();
  const isEdit = mode === "edit";
  const toList = () => navigate("/lessons");

  const categories = useQuery({
    queryKey: ["categories", "all"],
    queryFn: () => apiFetch<Category[]>("/admin/categories"),
  });

  const record = useQuery({
    queryKey: ["lessons", "one", id],
    queryFn: () => apiFetch<Lesson>(`/admin/lessons/${id}`),
    enabled: isEdit && !!id,
  });

  const form = useForm<LessonFormValues>({
    defaultValues: {
      title: "",
      difficulty: "EASY",
      baseXp: 10,
      estimatedMinutes: 8,
      backgroundKey: null,
      backgroundImageUrl: null,
      exercises: [emptyExercise()],
    },
  });
  const {
    register,
    control,
    handleSubmit,
    reset,
    setValue,
    watch,
    formState: { errors, dirtyFields },
  } = form;

  useEffect(() => {
    if (!record.data) return;
    const l = record.data;
    reset({
      title: l.title,
      description: l.description ?? "",
      difficulty: l.difficulty,
      baseXp: l.baseXp,
      estimatedMinutes: l.estimatedMinutes ?? 8,
      categoryId: l.categoryId ?? undefined,
      roadmapUnitId: l.roadmapUnitId ?? undefined,
      reflectionPrompt: l.reflectionPrompt ?? "",
      backgroundKey: l.backgroundKey,
      backgroundImageUrl: l.backgroundImageUrl,
      exercises: l.exercises.length
        ? [...l.exercises]
            .sort((a, b) => (a.sectionIndex ?? 1) - (b.sectionIndex ?? 1))
            .map(fromExercise)
        : [emptyExercise()],
    });
  }, [record.data, reset]);

  const categoryId = watch("categoryId");
  const units = useQuery({
    queryKey: ["roadmap-units", categoryId],
    queryFn: () =>
      apiFetch<RoadmapUnit[]>("/admin/roadmap-units", { params: { categoryId } }),
    enabled: !!categoryId,
  });

  // Exercises are replaced wholesale on the server, which deletes learners'
  // saved attempts for this lesson. Only send them when something changed.
  const exercisesChanged = isEdit && !!dirtyFields.exercises;

  const save = useMutation({
    mutationFn: (payload: Record<string, unknown>) =>
      apiFetch<Lesson>(isEdit ? `/admin/lessons/${id}` : "/admin/lessons", {
        method: isEdit ? "PUT" : "POST",
        body: payload,
      }),
    onSuccess: () => {
      toast.success(isEdit ? "Lesson saved" : "Lesson created");
      queryClient.invalidateQueries({ queryKey: ["lessons"] });
      toList();
    },
    onError: (e) => toast.error(e.message || "Save failed"),
  });

  const { fields, append, remove } = useFieldArray({ control, name: "exercises" });
  const difficulty = watch("difficulty");

  const submit = (values: LessonFormValues) => {
    if (isEdit && !record.data) {
      toast.error("Lesson has not loaded; changes were not saved");
      return;
    }
    const sendExercises = !isEdit || exercisesChanged;
    if (sendExercises) {
      for (const [i, e] of values.exercises.entries()) {
        const problem = validateExercise(e);
        if (problem) {
          toast.error(`Exercise ${i + 1}: ${problem}`);
          return;
        }
      }
    }
    const payload: Record<string, unknown> = {
      title: values.title,
      description: values.description || null,
      difficulty: values.difficulty,
      baseXp: Number(values.baseXp) || 0,
      estimatedMinutes: Number(values.estimatedMinutes) || 8,
      categoryId: values.categoryId || null,
      roadmapUnitId: values.categoryId ? values.roadmapUnitId || null : null,
      reflectionPrompt: values.reflectionPrompt || null,
      backgroundKey: values.backgroundKey,
      backgroundImageUrl: values.backgroundImageUrl,
    };
    if (sendExercises) payload.exercises = values.exercises.map(toPayload);
    save.mutate(payload);
  };

  if (isEdit && !id) {
    return (
      <div className="mx-auto max-w-3xl space-y-4">
        <Alert variant="destructive">
          <AlertDescription>Lesson ID is missing. No changes can be saved.</AlertDescription>
        </Alert>
        <Button variant="outline" onClick={toList}>Back to lessons</Button>
      </div>
    );
  }

  if (isEdit && record.isLoading) {
    return (
      <div className="mx-auto max-w-3xl space-y-4">
        <Skeleton className="h-10 w-48" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  if (isEdit && (record.isError || !record.data)) {
    return (
      <div className="mx-auto max-w-3xl space-y-4">
        <Alert variant="destructive">
          <AlertDescription>
            Could not load this lesson. The edit form is unavailable so defaults cannot overwrite it.
          </AlertDescription>
        </Alert>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => record.refetch()}>Retry</Button>
          <Button variant="outline" onClick={toList}>Back to lessons</Button>
        </div>
      </div>
    );
  }

  return (
    <FormProvider {...form}>
      <div className="mx-auto max-w-3xl">
        <div className="mb-6 flex items-center gap-2">
          <Button variant="ghost" size="icon" onClick={toList}>
            <ChevronLeft className="h-4 w-4" />
          </Button>
          <h1 className="text-2xl font-semibold">
            {isEdit ? "Edit lesson" : "New lesson"}
          </h1>
        </div>

        <form onSubmit={handleSubmit(submit)} className="space-y-6">
          <div className="space-y-4 rounded-md border bg-card p-6">
            <div className="space-y-2">
              <Label>Title *</Label>
              <Input {...register("title", { required: "Title is required" })} />
              {errors.title && (
                <p className="text-sm text-destructive">{errors.title.message}</p>
              )}
            </div>
            <div className="grid grid-cols-3 gap-4">
              <div className="space-y-2">
                <Label>Difficulty *</Label>
                <Select
                  value={difficulty}
                  onValueChange={(v) =>
                    setValue("difficulty", v as DifficultyLevel, { shouldDirty: true })
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
                <Label>Estimated minutes</Label>
                <Input
                  type="number"
                  min={1}
                  {...register("estimatedMinutes", { valueAsNumber: true })}
                />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>Category</Label>
                <Select
                  value={categoryId ?? "none"}
                  onValueChange={(v) => {
                    // Radix emits "" while the options are still loading; that isn't a user choice.
                    if (!v || v === (categoryId ?? "none")) return;
                    setValue("categoryId", v === "none" ? undefined : v, {
                      shouldDirty: true,
                    });
                    // Units belong to one category.
                    setValue("roadmapUnitId", undefined, { shouldDirty: true });
                  }}
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
              <div className="space-y-2">
                <Label>Roadmap unit</Label>
                <Select
                  disabled={!categoryId}
                  value={watch("roadmapUnitId") ?? "none"}
                  onValueChange={(v) =>
                    v &&
                    setValue("roadmapUnitId", v === "none" ? undefined : v, {
                      shouldDirty: true,
                    })
                  }
                >
                  <SelectTrigger>
                    <SelectValue placeholder={categoryId ? "None" : "Pick a category first"} />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="none">None</SelectItem>
                    {(units.data ?? []).map((u) => (
                      <SelectItem key={u.id} value={u.id}>
                        {u.position}. {u.title}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>
            <div className="space-y-2">
              <Label>Reflection prompt</Label>
              <Textarea
                rows={2}
                placeholder="A question the learner answers at the end, e.g. “What surprised you?”"
                {...register("reflectionPrompt")}
              />
            </div>
          </div>

          <LessonBackgroundSection
            inheritSubject
            backgroundKey={watch("backgroundKey")}
            backgroundImageUrl={watch("backgroundImageUrl")}
            subjectKey={
              (categories.data ?? []).find((category) => category.id === categoryId)
                ?.backgroundKey ?? null
            }
            subjectImageUrl={
              (categories.data ?? []).find((category) => category.id === categoryId)
                ?.backgroundImageUrl ?? null
            }
            onBackgroundKey={(key) => setValue("backgroundKey", key, { shouldDirty: true })}
            onBackgroundImageUrl={(url) =>
              setValue("backgroundImageUrl", url, { shouldDirty: true })
            }
          />

          <div className="space-y-4 rounded-md border bg-card p-6">
            <div>
              <h2 className="text-lg font-medium">Lesson content</h2>
              <p className="text-sm text-muted-foreground">
                Write it like a document — the toolbar handles the formatting.
                Each “New section” starts a new screen for the learner.
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

          <div className="space-y-4 rounded-md border bg-card p-6">
            <div>
              <h2 className="text-lg font-medium">Exercises *</h2>
              <p className="text-sm text-muted-foreground">
                The section number places the exercise after that section of the
                lesson content.
              </p>
            </div>

            {exercisesChanged && (
              <Alert>
                <AlertDescription>
                  You changed exercises. Saving replaces all of this lesson's
                  exercises and clears learners' saved answers for them. Lesson
                  completions and XP are kept.
                </AlertDescription>
              </Alert>
            )}

            {fields.map((field, index) => (
              <ExerciseEditor
                key={field.id}
                index={index}
                canRemove={fields.length > 1}
                onRemove={() => remove(index)}
              />
            ))}
            <Button
              type="button"
              variant="outline"
              onClick={() => {
                const last = form.getValues("exercises").at(-1);
                append(emptyExercise(last?.sectionIndex ?? 1));
              }}
            >
              <Plus className="mr-1 h-4 w-4" /> Add exercise
            </Button>
          </div>

          <div className="flex gap-2">
            <Button type="submit" disabled={save.isPending}>
              {save.isPending ? "Saving..." : "Save lesson"}
            </Button>
            <Button type="button" variant="outline" onClick={toList}>
              Cancel
            </Button>
          </div>
        </form>
      </div>
    </FormProvider>
  );
};

const ExerciseEditor = ({
  index,
  canRemove,
  onRemove,
}: {
  index: number;
  canRemove: boolean;
  onRemove: () => void;
}) => {
  const { register, setValue, control } = useFormContext<LessonFormValues>();
  const base = `exercises.${index}` as const;
  const type = useWatch({ control, name: `${base}.type` });
  const answerKey = useWatch({ control, name: `${base}.answerKey` });
  const options = useWatch({ control, name: `${base}.options` }) ?? [];
  const difficulty = useWatch({ control, name: `${base}.difficulty` });
  const dirty = { shouldDirty: true } as const;

  const changeType = (next: ExerciseType) => {
    setValue(`${base}.type`, next, dirty);
    if (next === "MULTIPLE_CHOICE") {
      setValue(`${base}.options`, options.length >= 2 ? options : ["", ""], dirty);
      setValue(`${base}.answerKey`, "", dirty);
    } else if (next === "TRUE_FALSE") {
      setValue(`${base}.options`, [], dirty);
      setValue(`${base}.answerKey`, "True", dirty);
    } else {
      setValue(`${base}.options`, [], dirty);
    }
  };

  const setOption = (i: number, value: string) => {
    const next = [...options];
    const wasCorrect = options[i] === answerKey && answerKey !== "";
    next[i] = value;
    setValue(`${base}.options`, next, dirty);
    // Keep the correct-answer mark on the option being edited.
    if (wasCorrect) setValue(`${base}.answerKey`, value, dirty);
  };

  const removeOption = (i: number) => {
    if (options[i] === answerKey) setValue(`${base}.answerKey`, "", dirty);
    setValue(
      `${base}.options`,
      options.filter((_, j) => j !== i),
      dirty,
    );
  };

  return (
    <div className="space-y-3 rounded-md border p-4">
      <div className="flex items-center justify-between gap-2">
        <p className="text-sm font-medium">Exercise {index + 1}</p>
        <div className="flex items-center gap-2">
          <Select value={type} onValueChange={(v) => changeType(v as ExerciseType)}>
            <SelectTrigger className="h-8 w-40">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {EXERCISE_TYPES.map((t) => (
                <SelectItem key={t.value} value={t.value}>
                  {t.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          {canRemove && (
            <Button type="button" variant="ghost" size="icon" onClick={onRemove}>
              <Trash2 className="h-4 w-4 text-destructive" />
            </Button>
          )}
        </div>
      </div>

      <Textarea rows={2} placeholder="Question" {...register(`${base}.question`)} />

      {type === "MULTIPLE_CHOICE" && (
        <div className="space-y-2">
          <Label className="text-xs text-muted-foreground">
            Options — select the correct one
          </Label>
          {options.map((opt, i) => (
            <div key={i} className="flex items-center gap-2">
              <input
                type="radio"
                className="h-4 w-4"
                name={`${base}-correct`}
                checked={opt !== "" && opt === answerKey}
                disabled={!opt.trim()}
                onChange={() => setValue(`${base}.answerKey`, opt, dirty)}
                aria-label={`Option ${i + 1} is correct`}
              />
              <Input
                value={opt}
                placeholder={`Option ${i + 1}`}
                onChange={(e) => setOption(i, e.target.value)}
              />
              {options.length > 2 && (
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  onClick={() => removeOption(i)}
                >
                  <X className="h-4 w-4" />
                </Button>
              )}
            </div>
          ))}
          {options.length < 8 && (
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => setValue(`${base}.options`, [...options, ""], dirty)}
            >
              <Plus className="mr-1 h-3 w-3" /> Add option
            </Button>
          )}
        </div>
      )}

      {type === "TRUE_FALSE" && (
        <div className="flex gap-4">
          {["True", "False"].map((v) => (
            <label key={v} className="flex items-center gap-2 text-sm">
              <input
                type="radio"
                className="h-4 w-4"
                name={`${base}-tf`}
                checked={answerKey === v}
                onChange={() => setValue(`${base}.answerKey`, v, dirty)}
              />
              {v}
            </label>
          ))}
        </div>
      )}

      {type === "TEXT" && (
        <Input placeholder="Expected answer" {...register(`${base}.answerKey`)} />
      )}

      <div className="grid grid-cols-3 gap-3">
        <div className="space-y-1">
          <Label className="text-xs text-muted-foreground">Difficulty</Label>
          <Select
            value={difficulty}
            onValueChange={(v) => setValue(`${base}.difficulty`, v as DifficultyLevel, dirty)}
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
        <div className="space-y-1">
          <Label className="text-xs text-muted-foreground">Points</Label>
          <Input
            type="number"
            min={1}
            {...register(`${base}.points`, { valueAsNumber: true })}
          />
        </div>
        <div className="space-y-1">
          <Label className="text-xs text-muted-foreground">After section</Label>
          <Input
            type="number"
            min={1}
            {...register(`${base}.sectionIndex`, { valueAsNumber: true })}
          />
        </div>
      </div>
      <div className="grid grid-cols-2 gap-3">
        <Input placeholder="Hint (optional)" {...register(`${base}.hint`)} />
        <Input
          placeholder="Explanation after answering (optional)"
          {...register(`${base}.explanation`)}
        />
      </div>
    </div>
  );
};

export const LessonCreate = () => <LessonForm mode="create" />;
export const LessonEdit = () => <LessonForm mode="edit" />;
