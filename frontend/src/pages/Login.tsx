import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useMutation } from "@tanstack/react-query";
import { z } from "zod";

import { login, extractErrorMessage } from "@/api/auth";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { toast } from "@/components/Toaster";
import { decodeAccessToken } from "@/lib/jwt";
import { useAuthStore } from "@/store/auth";

const schema = z.object({
  email: z.string().email("Enter a valid email"),
  password: z.string().min(1, "Password is required"),
});

type FormValues = z.infer<typeof schema>;

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const setSession = useAuthStore((s) => s.setSession);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: "", password: "" },
  });

  const mutation = useMutation({
    mutationFn: login,
    onSuccess: (tokens) => {
      const user = decodeAccessToken(tokens.accessToken);
      if (!user) {
        toast({ title: "Sign-in failed", description: "Invalid token received", variant: "destructive" });
        return;
      }
      setSession({ user, tokens });
      const from = (location.state as { from?: string } | null)?.from ?? "/";
      navigate(from, { replace: true });
    },
    onError: (error) => {
      toast({
        title: "Sign-in failed",
        description: extractErrorMessage(error, "Check your email and password"),
        variant: "destructive",
      });
    },
  });

  return (
    <div className="max-w-md mx-auto">
      <Card>
        <CardHeader>
          <CardTitle>Sign in</CardTitle>
          <CardDescription>Welcome back to china-kekao.</CardDescription>
        </CardHeader>
        <CardContent>
          <form
            className="flex flex-col gap-4"
            onSubmit={handleSubmit((values) => mutation.mutate(values))}
            noValidate
          >
            <div className="flex flex-col gap-1">
              <label htmlFor="email" className="text-sm font-medium">Email</label>
              <Input id="email" type="email" autoComplete="email" {...register("email")} />
              {errors.email ? (
                <p className="text-xs text-destructive">{errors.email.message}</p>
              ) : null}
            </div>
            <div className="flex flex-col gap-1">
              <label htmlFor="password" className="text-sm font-medium">Password</label>
              <Input id="password" type="password" autoComplete="current-password" {...register("password")} />
              {errors.password ? (
                <p className="text-xs text-destructive">{errors.password.message}</p>
              ) : null}
            </div>
            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending ? "Signing in…" : "Sign in"}
            </Button>
            <p className="text-sm text-muted-foreground">
              Don&apos;t have an account?{" "}
              <Link to="/register" className="text-primary underline">
                Register
              </Link>
              .
            </p>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
