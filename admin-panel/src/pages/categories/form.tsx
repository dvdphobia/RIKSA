import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { useNavigate, useParams } from "react-router";
import { toast } from "sonner";
import { apiFetch } from "../../lib/api";
import type { Category } from "../../lib/types";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { Label } from "../../components/ui/label";
import { Textarea } from "../../components/ui/textarea";
import { ChevronLeft } from "lucide-react";
import { Alert, AlertDescription } from "../../components/ui/alert";
import { Skeleton } from "../../components/ui/skeleton";

interface CategoryForm {
  name: string;
  slug: string;
  description?: string;
  iconUrl?: string;
}

export const CategoryForm = ({ mode }: { mode: "create" | "edit" }) => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { id } = useParams();
  const isEdit = mode === "edit";
  const toList = () => navigate("/categories");
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CategoryForm>();

  const record = useQuery({
    queryKey: ["categories", "one", id],
    queryFn: () => apiFetch<Category>(`/admin/categories/${id}`),
    enabled: isEdit && !!id,
  });

  useEffect(() => {
    if (record.data) {
      reset({
        name: record.data.name,
        slug: record.data.slug,
        description: record.data.description ?? "",
        iconUrl: record.data.iconUrl ?? "",
      });
    }
  }, [record.data, reset]);

  const save = useMutation({
    mutationFn: (body: CategoryForm) =>
      apiFetch<Category>(isEdit ? `/admin/categories/${id}` : "/admin/categories", {
        method: isEdit ? "PUT" : "POST",
        body,
      }),
    onSuccess: () => {
      toast.success(isEdit ? "Category saved" : "Category created");
      queryClient.invalidateQueries({ queryKey: ["categories"] });
      toList();
    },
    onError: (e) => toast.error(e.message || "Save failed"),
  });

  const submit = (values: CategoryForm) => {
    if (isEdit && !record.data) {
      toast.error("Category has not loaded; changes were not saved");
      return;
    }
    save.mutate({
      name: values.name,
      slug: values.slug,
      description: values.description ?? "",
      iconUrl: values.iconUrl ?? "",
    });
  };

  if (isEdit && !id) {
    return (
      <div className="mx-auto max-w-2xl space-y-4">
        <Alert variant="destructive">
          <AlertDescription>Category ID is missing. No changes can be saved.</AlertDescription>
        </Alert>
        <Button variant="outline" onClick={toList}>Back to categories</Button>
      </div>
    );
  }

  if (isEdit && record.isLoading) {
    return (
      <div className="mx-auto max-w-2xl space-y-4">
        <Skeleton className="h-10 w-48" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  if (isEdit && (record.isError || !record.data)) {
    return (
      <div className="mx-auto max-w-2xl space-y-4">
        <Alert variant="destructive">
          <AlertDescription>
            Could not load this category. The edit form is unavailable so defaults cannot overwrite it.
          </AlertDescription>
        </Alert>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => record.refetch()}>Retry</Button>
          <Button variant="outline" onClick={toList}>Back to categories</Button>
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-2xl">
      <div className="mb-6 flex items-center gap-2">
        <Button variant="ghost" size="icon" onClick={toList}>
          <ChevronLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-2xl font-semibold">
          {isEdit ? "Edit category" : "New category"}
        </h1>
      </div>
      <form
        onSubmit={handleSubmit(submit)}
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
        <Button type="submit" disabled={save.isPending || (isEdit && !record.data)}>
          {save.isPending ? "Saving..." : "Save"}
        </Button>
      </form>
    </div>
  );
};

export const CategoryCreate = () => <CategoryForm mode="create" />;
export const CategoryEdit = () => <CategoryForm mode="edit" />;
