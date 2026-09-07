import { type HttpError, useNavigation } from "@refinedev/core";
import { useForm } from "@refinedev/react-hook-form";
import { useQuery } from "@tanstack/react-query";
import { useFieldArray } from "react-hook-form";
import { toast } from "sonner";
import { apiFetch } from "../../lib/api";
import type { Category, DifficultyLevel, ExerciseType } from "../../lib/types";
import { StatusBadge } from "../../components/shared";
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
import { ChevronLeft, Plus, Trash2 } from "lucide-react";
import { lazy, Suspense, useState } from "react";

// The rich text editor pulls in ProseMirror and the markdown converters; keep
// them out of the main bundle so only the lesson form pays for them.
const LessonBodyEditor = lazy(() =>
  import("../../components/editor/lesson-body-editor").then((m) => ({
    default: m.LessonBodyEditor,
  })),
);

const DIFFICULTIES: DifficultyLevel[] = ["EASY", "MEDIUM", "HARD", "DIFFICULT"];

interface ExerciseForm {
  question: string;
  answerKey: string;
  difficulty: DifficultyLevel;
  points: number;
  type: ExerciseType;
  optionsText: string;
  hint?: string;
  explanation?: string;
}

interface LessonFormValues {
  title: string;
  description?: string;
  difficulty: DifficultyLevel;
  baseXp: number;
  position: number;
  categoryId?: string;
  reflectionPrompt?: string;
  exercises: ExerciseForm[];
}

const emptyExercise = (): ExerciseForm => ({
  question: "",
  answerKey: "",
  difficulty: "EASY",
  points: 5,
  type: "MULTIPLE_CHOICE",
  optionsText: "",
});

export const LessonForm = ({ mode }: { mode: "create" | "edit" }) => {
  const { list } = useNavigation();
  const isEdit = mode === "edit";
  const [replaceExercises, setReplaceExercises] = useState(false);

  const categories = useQuery({
    queryKey: ["categories-all"],
    queryFn: () => apiFetch<Category[]>("/admin/categories"),
  });

  const {
    refineCore: { onFinish, query: recordQuery },
    register,
    control,
    handleSubmit,
    setValue,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<LessonFormValues, HttpError, LessonFormValues>({
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
      exercises: [emptyExercise()],
    },
  });

  const { fields, append, remove } = useFieldArray({ control, name: "exercises" });
  const difficulty = watch("difficulty");
  const existingExercises = (recordQuery?.data?.data?.exercises ?? []) as {
    id?: string;
    question: string;
    difficulty: DifficultyLevel;
    points: number;
  }[];

  const submit = (values: LessonFormValues) => {
    const payload: Record<string, unknown> = {
      title: values.title,
      description: values.description || null,
      difficulty: values.difficulty,
      baseXp: Number(values.baseXp) || 0,
      position: Number(values.position) || 1,
      categoryId: values.categoryId || null,
      reflectionPrompt: values.reflectionPrompt || null,
    };
    if (!isEdit || replaceExercises) {
      payload.exercises = values.exercises.map((e) => ({
        question: e.question,
        answerKey: e.answerKey,
        difficulty: e.difficulty,
        points: Number(e.points) || 5,
        type: e.type,
        options: e.type === "TRUE_FALSE"
          ? ["True", "False"]
          : e.optionsText.split("\n").map((option) => option.trim()).filter(Boolean),
        hint: e.hint || null,
        explanation: e.explanation || null,
      }));
    }
    onFinish(payload as unknown as LessonFormValues);
  };

  return (
    <div className="mx-auto max-w-3xl">
      <div className="mb-6 flex items-center gap-2">
        <Button variant="ghost" size="icon" onClick={() => list("lessons")}>
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
          <div className="grid grid-cols-4 gap-4">
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
              <Input type="number" min={1} {...register("position", { required: "Position is required" })} />
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
            <Label>Reflection prompt</Label>
            <Textarea
              rows={2}
              placeholder="A question the learner answers at the end, e.g. “What surprised you?”"
              {...register("reflectionPrompt")}
            />
          </div>
        </div>

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
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-medium">Exercises *</h2>
            {isEdit && (
              <Button
                type="button"
                variant={replaceExercises ? "default" : "outline"}
                size="sm"
                onClick={() => setReplaceExercises((v) => !v)}
              >
                {replaceExercises ? "Editing exercises" : "Replace exercises"}
              </Button>
            )}
          </div>

          {isEdit && !replaceExercises && (
            <>
              <Alert>
                <AlertDescription>
                  These {existingExercises.length} exercise(s) stay exactly as
                  they are when you save. Answer keys can't be shown again for
                  security, so changing any exercise means rewriting all of them
                  — click <strong>Replace exercises</strong> to do that.
                </AlertDescription>
              </Alert>
              <div className="space-y-2">
                {existingExercises.map((e, i) => (
                  <div
                    key={e.id ?? i}
                    className="flex items-center justify-between rounded-md border p-3 text-sm"
                  >
                    <span className="max-w-lg truncate">{e.question}</span>
                    <span className="flex items-center gap-2">
                      <StatusBadge value={e.difficulty} />
                      <span className="text-muted-foreground">
                        {e.points} pts
                      </span>
                    </span>
                  </div>
                ))}
              </div>
            </>
          )}

          {(!isEdit || replaceExercises) && (
            <>
              {fields.map((field, index) => (
                <div
                  key={(field as { id?: string }).id ?? index}
                  className="space-y-3 rounded-md border p-4"
                >
                  <div className="flex items-center justify-between">
                    <p className="text-sm font-medium">Exercise {index + 1}</p>
                    {fields.length > 1 && (
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        onClick={() => remove(index)}
                      >
                        <Trash2 className="h-4 w-4 text-destructive" />
                      </Button>
                    )}
                  </div>
                  <Input
                    placeholder="Question"
                    {...register(`exercises.${index}.question`, {
                      required: "Question is required",
                    })}
                  />
                  {errors.exercises?.[index]?.question && (
                    <p className="text-sm text-destructive">
                      {errors.exercises[index]?.question?.message}
                    </p>
                  )}
                  <Input
                    placeholder="Answer key"
                    {...register(`exercises.${index}.answerKey`, {
                      required: "Answer key is required",
                    })}
                  />
                  {errors.exercises?.[index]?.answerKey && (
                    <p className="text-sm text-destructive">
                      {errors.exercises[index]?.answerKey?.message}
                    </p>
                  )}
                  <div className="grid grid-cols-3 gap-4">
                    <Select
                      value={watch(`exercises.${index}.type`)}
                      onValueChange={(v) => {
                        const type = v as ExerciseType;
                        setValue(`exercises.${index}.type`, type);
                        if (type === "TRUE_FALSE") {
                          setValue(`exercises.${index}.optionsText`, "True\nFalse");
                        }
                      }}
                    >
                      <SelectTrigger><SelectValue /></SelectTrigger>
                      <SelectContent>
                        <SelectItem value="MULTIPLE_CHOICE">Multiple choice (QCM)</SelectItem>
                        <SelectItem value="TRUE_FALSE">True / False</SelectItem>
                        <SelectItem value="TEXT">Short text</SelectItem>
                      </SelectContent>
                    </Select>
                    <Select
                      value={watch(`exercises.${index}.difficulty`)}
                      onValueChange={(v) =>
                        setValue(
                          `exercises.${index}.difficulty`,
                          v as DifficultyLevel,
                        )
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
                    <Input
                      type="number"
                      min={1}
                      placeholder="Points"
                      {...register(`exercises.${index}.points`, {
                        valueAsNumber: true,
                      })}
                    />
                  </div>
                  {watch(`exercises.${index}.type`) !== "TEXT" && (
                    <Textarea
                      rows={3}
                      placeholder={watch(`exercises.${index}.type`) === "TRUE_FALSE" ? "True\nFalse" : "One option per line"}
                      disabled={watch(`exercises.${index}.type`) === "TRUE_FALSE"}
                      {...register(`exercises.${index}.optionsText`)}
                    />
                  )}
                  <Input placeholder="Hint (optional)" {...register(`exercises.${index}.hint`)} />
                  <Textarea rows={3} placeholder="Explanation shown after submission (optional)" {...register(`exercises.${index}.explanation`)} />
                </div>
              ))}
              <Button
                type="button"
                variant="outline"
                onClick={() => append(emptyExercise())}
              >
                <Plus className="mr-1 h-4 w-4" /> Add exercise
              </Button>
            </>
          )}
        </div>

        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Saving..." : "Save lesson"}
        </Button>
      </form>
    </div>
  );
};

export const LessonCreate = () => <LessonForm mode="create" />;
export const LessonEdit = () => <LessonForm mode="edit" />;
