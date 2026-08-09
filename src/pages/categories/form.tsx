import { type HttpError, useNavigation } from "@refinedev/core";
import { useForm } from "@refinedev/react-hook-form";
import { toast } from "sonner";
import type { Category } from "../../lib/types";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { Label } from "../../components/ui/label";
import { Textarea } from "../../components/ui/textarea";
import { ChevronLeft } from "lucide-react";

interface CategoryForm {
  name: string;
  slug: string;
  description?: string;
  iconUrl?: string;
}

export const CategoryForm = ({ mode }: { mode: "create" | "edit" }) => {
  const { list } = useNavigation();
  const {
    refineCore: { onFinish },
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<CategoryForm, HttpError, CategoryForm>({
    refineCoreProps: {
      resource: "categories",
      action: mode,
      onMutationSuccess: () => {
        toast.success(mode === "create" ? "Category created" : "Category saved");
        list("categories");
      },
      onMutationError: (e) =>
        toast.error(e.message || "Save failed"),
    },
  });

  return (
    <div className="mx-auto max-w-2xl">
      <div className="mb-6 flex items-center gap-2">
        <Button variant="ghost" size="icon" onClick={() => list("categories")}>
          <ChevronLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-2xl font-semibold">
          {mode === "create" ? "New category" : "Edit category"}
        </h1>
      </div>
      <form
        onSubmit={handleSubmit((values) =>
          onFinish({
            name: values.name,
            slug: values.slug,
            description: values.description ?? "",
            iconUrl: values.iconUrl ?? "",
          }),
        )}
        className="space-y-4 rounded-md border bg-card p-6"
      >
        <div className="space-y-2">
          <Label>Name *</Label>
          <Input {...register("name", { required: "Name is required" })} />
          {errors.name && (
            <p className="text-sm text-destructive">{errors.name.message}</p>
          )}
        </div>
        <div className="space-y-2">
          <Label>Slug *</Label>
          <Input
            placeholder="e.g. programming"
            {...register("slug", { required: "Slug is required" })}
          />
          {errors.slug && (
            <p className="text-sm text-destructive">{errors.slug.message}</p>
          )}
        </div>
        <div className="space-y-2">
          <Label>Description</Label>
          <Textarea {...register("description")} />
        </div>
        <div className="space-y-2">
          <Label>Icon URL</Label>
          <Input {...register("iconUrl")} />
        </div>
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Saving..." : "Save"}
        </Button>
      </form>
    </div>
  );
};

export const CategoryCreate = () => <CategoryForm mode="create" />;
export const CategoryEdit = () => <CategoryForm mode="edit" />;
