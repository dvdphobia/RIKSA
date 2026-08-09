import { type HttpError, useLogin } from "@refinedev/core";
import { useForm } from "@refinedev/react-hook-form";
import { AlertCircle } from "lucide-react";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { Label } from "../../components/ui/label";
import { Alert, AlertDescription } from "../../components/ui/alert";

interface LoginForm {
  username: string;
  password: string;
}

export const Login = () => {
  const { mutate, isPending } = useLogin<LoginForm>();
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<LoginForm, HttpError, LoginForm>({
    defaultValues: { username: "", password: "" },
  });

  const onSubmit = (values: LoginForm) => {
    mutate(values, {
      onError: (err) =>
        setError("root", {
          message: err.message || "Login failed",
        }),
    });
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-muted/30">
      <div className="w-full max-w-sm space-y-6 rounded-lg border bg-card p-8 shadow-sm">
        <div className="space-y-1 text-center">
          <div className="mx-auto flex h-10 w-10 items-center justify-center rounded-md bg-primary font-bold text-primary-foreground">
            R
          </div>
          <h1 className="text-xl font-semibold">Riksa Admin</h1>
          <p className="text-sm text-muted-foreground">
            Sign in with an admin or moderator account
          </p>
        </div>
        {errors.root?.message && (
          <Alert variant="destructive">
            <AlertCircle className="h-4 w-4" />
            <AlertDescription>{errors.root.message}</AlertDescription>
          </Alert>
        )}
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="username">Username</Label>
            <Input
              id="username"
              autoComplete="username"
              {...register("username", { required: "Username is required" })}
            />
            {errors.username && (
              <p className="text-sm text-destructive">
                {errors.username.message}
              </p>
            )}
          </div>
          <div className="space-y-2">
            <Label htmlFor="password">Password</Label>
            <Input
              id="password"
              type="password"
              autoComplete="current-password"
              {...register("password", { required: "Password is required" })}
            />
            {errors.password && (
              <p className="text-sm text-destructive">
                {errors.password.message}
              </p>
            )}
          </div>
          <Button type="submit" className="w-full" disabled={isPending}>
            {isPending ? "Signing in..." : "Sign in"}
          </Button>
        </form>
      </div>
    </div>
  );
};
