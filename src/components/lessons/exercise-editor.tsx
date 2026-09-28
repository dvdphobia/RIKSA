import type { DifficultyLevel, ExerciseType } from "../../lib/types";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Label } from "../ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "../ui/select";
import { Badge } from "../ui/badge";
import {
  ArrowDown,
  ArrowUp,
  CheckCircle2,
  Copy,
  Plus,
  Trash2,
  AlertCircle,
  HelpCircle,
  Lightbulb,
} from "lucide-react";

export interface ExerciseItem {
  id?: string;
  question: string;
  answerKey: string;
  difficulty: DifficultyLevel;
  points: number;
  type: ExerciseType;
  options: string[];
  hint?: string;
  explanation?: string;
}

const DIFFICULTIES: DifficultyLevel[] = ["EASY", "MEDIUM", "HARD", "DIFFICULT"];

export const emptyExerciseItem = (): ExerciseItem => ({
  question: "",
  answerKey: "",
  difficulty: "EASY",
  points: 5,
  type: "MULTIPLE_CHOICE",
  options: ["", "", "", ""],
  hint: "",
  explanation: "",
});

interface ExerciseEditorProps {
  exercises: ExerciseItem[];
  onChange: (exercises: ExerciseItem[]) => void;
}

export const ExerciseEditor = ({ exercises, onChange }: ExerciseEditorProps) => {
  const updateExercise = (index: number, updates: Partial<ExerciseItem>) => {
    const next = [...exercises];
    next[index] = { ...next[index], ...updates };
    onChange(next);
  };

  const handleTypeChange = (index: number, newType: ExerciseType) => {
    const current = exercises[index];
    let newOptions = current.options;
    let newAnswerKey = current.answerKey;

    if (newType === "MULTIPLE_CHOICE") {
      newOptions = current.options.length >= 2 ? current.options : ["", "", "", ""];
      newAnswerKey = newOptions.includes(current.answerKey) ? current.answerKey : "";
    } else if (newType === "TRUE_FALSE") {
      newOptions = ["True", "False"];
      newAnswerKey = ["True", "False"].includes(current.answerKey) ? current.answerKey : "True";
    } else if (newType === "TEXT") {
      newOptions = [];
    }

    updateExercise(index, {
      type: newType,
      options: newOptions,
      answerKey: newAnswerKey,
    });
  };

  const handleAddOption = (exerciseIndex: number) => {
    const current = exercises[exerciseIndex];
    const newOptions = [...current.options, ""];
    updateExercise(exerciseIndex, { options: newOptions });
  };

  const handleRemoveOption = (exerciseIndex: number, optionIndex: number) => {
    const current = exercises[exerciseIndex];
    const removedVal = current.options[optionIndex];
    const newOptions = current.options.filter((_, i) => i !== optionIndex);
    const newAnswerKey = current.answerKey === removedVal ? "" : current.answerKey;
    updateExercise(exerciseIndex, {
      options: newOptions,
      answerKey: newAnswerKey,
    });
  };

  const handleOptionChange = (exerciseIndex: number, optionIndex: number, value: string) => {
    const current = exercises[exerciseIndex];
    const oldVal = current.options[optionIndex];
    const newOptions = [...current.options];
    newOptions[optionIndex] = value;

    // If this option was selected as the correct answer, keep answerKey in sync
    const newAnswerKey = current.answerKey === oldVal ? value : current.answerKey;
    updateExercise(exerciseIndex, {
      options: newOptions,
      answerKey: newAnswerKey,
    });
  };

  const handleSelectAnswer = (exerciseIndex: number, selectedAnswer: string) => {
    updateExercise(exerciseIndex, { answerKey: selectedAnswer });
  };

  const moveExercise = (index: number, direction: "up" | "down") => {
    const target = direction === "up" ? index - 1 : index + 1;
    if (target < 0 || target >= exercises.length) return;
    const next = [...exercises];
    const temp = next[index];
    next[index] = next[target];
    next[target] = temp;
    onChange(next);
  };

  const duplicateExercise = (index: number) => {
    const current = exercises[index];
    const clone: ExerciseItem = {
      ...current,
      id: undefined,
      question: `${current.question} (Copy)`,
      options: [...current.options],
    };
    const next = [...exercises];
    next.splice(index + 1, 0, clone);
    onChange(next);
  };

  const deleteExercise = (index: number) => {
    if (exercises.length <= 1) return;
    const next = exercises.filter((_, i) => i !== index);
    onChange(next);
  };

  const addExercise = () => {
    onChange([...exercises, emptyExerciseItem()]);
  };

  return (
    <div className="space-y-6">
      {exercises.map((exercise, index) => {
        const isQCM = exercise.type === "MULTIPLE_CHOICE";
        const isTF = exercise.type === "TRUE_FALSE";
        const isText = exercise.type === "TEXT";

        const validOptions = exercise.options.map((o) => o.trim()).filter(Boolean);
        const hasEnoughOptions = validOptions.length >= 2;
        const answerSelected = Boolean(
          exercise.answerKey &&
            (isQCM ? validOptions.includes(exercise.answerKey.trim()) : true)
        );

        return (
          <div
            key={exercise.id ?? `ex-${index}`}
            className="rounded-lg border bg-card p-5 shadow-sm space-y-4 transition-colors"
          >
            {/* Header: Title, Type badge, Actions */}
            <div className="flex flex-wrap items-center justify-between gap-2 border-b pb-3">
              <div className="flex items-center gap-2">
                <span className="flex h-6 w-6 items-center justify-center rounded-full bg-primary/10 text-xs font-semibold text-primary">
                  {index + 1}
                </span>
                <span className="font-semibold text-sm">
                  Exercise #{index + 1}
                </span>
                <Badge variant={isQCM ? "default" : isTF ? "secondary" : "outline"}>
                  {isQCM ? "Multiple Choice (QCM)" : isTF ? "True / False" : "Short Text"}
                </Badge>
                {!answerSelected && (
                  <Badge variant="destructive" className="flex items-center gap-1 text-[11px]">
                    <AlertCircle className="h-3 w-3" /> Needs correct answer
                  </Badge>
                )}
                {isQCM && !hasEnoughOptions && (
                  <Badge variant="destructive" className="flex items-center gap-1 text-[11px]">
                    <AlertCircle className="h-3 w-3" /> Needs ≥ 2 options
                  </Badge>
                )}
              </div>

              <div className="flex items-center gap-1">
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="h-8 w-8"
                  disabled={index === 0}
                  onClick={() => moveExercise(index, "up")}
                  title="Move Up"
                >
                  <ArrowUp className="h-4 w-4" />
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="h-8 w-8"
                  disabled={index === exercises.length - 1}
                  onClick={() => moveExercise(index, "down")}
                  title="Move Down"
                >
                  <ArrowDown className="h-4 w-4" />
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="h-8 w-8"
                  onClick={() => duplicateExercise(index)}
                  title="Duplicate Exercise"
                >
                  <Copy className="h-4 w-4" />
                </Button>
                {exercises.length > 1 && (
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    className="h-8 w-8 text-destructive hover:bg-destructive/10"
                    onClick={() => deleteExercise(index)}
                    title="Delete Exercise"
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                )}
              </div>
            </div>

            {/* Question Input */}
            <div className="space-y-1.5">
              <Label className="text-xs font-medium text-muted-foreground">
                Question Prompt *
              </Label>
              <Input
                placeholder="e.g. How do you say 'Hello' politely in Khmer?"
                value={exercise.question}
                onChange={(e) => updateExercise(index, { question: e.target.value })}
                required
              />
            </div>

            {/* Settings Row: Type, Difficulty, Points */}
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
              <div className="space-y-1.5">
                <Label className="text-xs font-medium text-muted-foreground">
                  Exercise Type
                </Label>
                <Select
                  value={exercise.type}
                  onValueChange={(v) => handleTypeChange(index, v as ExerciseType)}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="MULTIPLE_CHOICE">Multiple Choice (QCM)</SelectItem>
                    <SelectItem value="TRUE_FALSE">True / False</SelectItem>
                    <SelectItem value="TEXT">Short Text</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-1.5">
                <Label className="text-xs font-medium text-muted-foreground">
                  Difficulty
                </Label>
                <Select
                  value={exercise.difficulty}
                  onValueChange={(v) => updateExercise(index, { difficulty: v as DifficultyLevel })}
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

              <div className="space-y-1.5">
                <Label className="text-xs font-medium text-muted-foreground">
                  XP Points
                </Label>
                <Input
                  type="number"
                  min={1}
                  max={50}
                  value={exercise.points}
                  onChange={(e) =>
                    updateExercise(index, { points: Number(e.target.value) || 5 })
                  }
                />
              </div>
            </div>

            {/* QCM Options Section */}
            {isQCM && (
              <div className="space-y-3 rounded-md border bg-muted/20 p-4">
                <div className="flex items-center justify-between">
                  <div className="space-y-0.5">
                    <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                      Multiple Choice Options
                    </p>
                    <p className="text-xs text-muted-foreground">
                      Click the circle next to an option to mark it as the <strong>correct answer</strong>.
                    </p>
                  </div>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="h-8 text-xs"
                    onClick={() => handleAddOption(index)}
                  >
                    <Plus className="mr-1 h-3.5 w-3.5" /> Add Option
                  </Button>
                </div>

                <div className="space-y-2">
                  {exercise.options.map((opt, optIndex) => {
                    const isSelected = Boolean(
                      exercise.answerKey &&
                        opt.trim() !== "" &&
                        exercise.answerKey.trim() === opt.trim()
                    );
                    const letter = String.fromCharCode(65 + optIndex);

                    return (
                      <div
                        key={optIndex}
                        className={`flex items-center gap-2 rounded-md border p-2 transition-colors ${
                          isSelected
                            ? "border-emerald-500/50 bg-emerald-50/40 dark:bg-emerald-950/20"
                            : "bg-background"
                        }`}
                      >
                        {/* Radio selector for correct answer */}
                        <button
                          type="button"
                          onClick={() => {
                            if (opt.trim()) {
                              handleSelectAnswer(index, opt.trim());
                            }
                          }}
                          className={`flex h-7 w-7 shrink-0 items-center justify-center rounded-full border text-xs font-bold transition-all ${
                            isSelected
                              ? "border-emerald-600 bg-emerald-600 text-white shadow-sm"
                              : "border-muted-foreground/30 text-muted-foreground hover:border-emerald-500 hover:text-emerald-600"
                          }`}
                          title={
                            opt.trim()
                              ? "Click to set as correct answer"
                              : "Type option text first"
                          }
                        >
                          {isSelected ? (
                            <CheckCircle2 className="h-4 w-4" />
                          ) : (
                            letter
                          )}
                        </button>

                        {/* Option text input */}
                        <Input
                          placeholder={`Option ${letter} text...`}
                          value={opt}
                          onChange={(e) =>
                            handleOptionChange(index, optIndex, e.target.value)
                          }
                          className="flex-1 bg-transparent border-none shadow-none focus-visible:ring-1"
                        />

                        {/* Correct badge */}
                        {isSelected && (
                          <span className="hidden sm:inline-flex items-center rounded-full bg-emerald-100 px-2 py-0.5 text-[11px] font-semibold text-emerald-800 dark:bg-emerald-900/50 dark:text-emerald-300">
                            Correct Answer
                          </span>
                        )}

                        {/* Delete option */}
                        {exercise.options.length > 2 && (
                          <Button
                            type="button"
                            variant="ghost"
                            size="icon"
                            className="h-7 w-7 text-muted-foreground hover:text-destructive"
                            onClick={() => handleRemoveOption(index, optIndex)}
                            title="Remove this option"
                          >
                            <Trash2 className="h-3.5 w-3.5" />
                          </Button>
                        )}
                      </div>
                    );
                  })}
                </div>

                {!answerSelected && (
                  <div className="flex items-center gap-1.5 text-xs text-destructive">
                    <AlertCircle className="h-3.5 w-3.5" />
                    <span>
                      Please select which option is the correct answer by clicking its circle.
                    </span>
                  </div>
                )}
              </div>
            )}

            {/* True / False Section */}
            {isTF && (
              <div className="space-y-2 rounded-md border bg-muted/20 p-4">
                <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                  Select Correct Answer
                </p>
                <div className="flex gap-4">
                  {["True", "False"].map((choice) => {
                    const isSelected = exercise.answerKey.toLowerCase() === choice.toLowerCase();
                    return (
                      <button
                        key={choice}
                        type="button"
                        onClick={() => handleSelectAnswer(index, choice)}
                        className={`flex flex-1 items-center justify-center gap-2 rounded-md border py-2.5 font-medium text-sm transition-all ${
                          isSelected
                            ? "border-emerald-600 bg-emerald-600 text-white shadow-sm"
                            : "bg-background hover:border-muted-foreground/50"
                        }`}
                      >
                        {isSelected && <CheckCircle2 className="h-4 w-4" />}
                        {choice}
                      </button>
                    );
                  })}
                </div>
              </div>
            )}

            {/* Short Text Section */}
            {isText && (
              <div className="space-y-1.5 rounded-md border bg-muted/20 p-4">
                <Label className="text-xs font-medium text-muted-foreground">
                  Expected Answer Key *
                </Label>
                <Input
                  placeholder="Expected answer key..."
                  value={exercise.answerKey}
                  onChange={(e) => updateExercise(index, { answerKey: e.target.value })}
                  required
                />
              </div>
            )}

            {/* Hint & Explanation */}
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 pt-1">
              <div className="space-y-1.5">
                <div className="flex items-center gap-1">
                  <HelpCircle className="h-3.5 w-3.5 text-muted-foreground" />
                  <Label className="text-xs font-medium text-muted-foreground">
                    Hint (Optional)
                  </Label>
                </div>
                <Input
                  placeholder="Helpful clue shown if learner is stuck..."
                  value={exercise.hint ?? ""}
                  onChange={(e) => updateExercise(index, { hint: e.target.value })}
                />
              </div>

              <div className="space-y-1.5">
                <div className="flex items-center gap-1">
                  <Lightbulb className="h-3.5 w-3.5 text-muted-foreground" />
                  <Label className="text-xs font-medium text-muted-foreground">
                    Explanation (Optional)
                  </Label>
                </div>
                <Input
                  placeholder="Explanation displayed after answer verification..."
                  value={exercise.explanation ?? ""}
                  onChange={(e) => updateExercise(index, { explanation: e.target.value })}
                />
              </div>
            </div>
          </div>
        );
      })}

      {/* Add Exercise button */}
      <Button
        type="button"
        variant="outline"
        className="w-full border-dashed py-5 text-sm"
        onClick={addExercise}
      >
        <Plus className="mr-1.5 h-4 w-4" /> Add Another Exercise
      </Button>
    </div>
  );
};
