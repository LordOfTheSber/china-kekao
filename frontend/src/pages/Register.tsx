import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Link, useNavigate } from "react-router-dom";
import { useMutation } from "@tanstack/react-query";
import { z } from "zod";

import { register as registerUser, extractErrorMessage } from "@/api/auth";
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

const schema = z
  .object({
    email: z.string().email("Enter a valid email"),
    password: z
      .string()
      .min(8, "Password must be at least 8 characters")
      .max(200, "Password is too long"),
    confirm: z.string(),
  })
  .refine((data) => data.password === data.confirm, {
    path: ["confirm"],
    message: "Passwords do not match",
  });

type FormValues = z.infer<typeof schema>;

export function RegisterPage() {
  const navigate = useNavigate();
  const setSession = useAuthStore((s) => s.setSession);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: "", password: "", confirm: "" },
  });

  const mutation = useMutation({
    mutationFn: (values: FormValues) =>
      registerUser({ email: values.email, password: values.password }),
    onSuccess: (tokens) => {
      const user = decodeAccessToken(tokens.accessToken);
      if (!user) {
        toast({ title: "Registration failed", description: "Invalid token received", variant: "destructive" });
        return;
      }
      setSession({ user, tokens });
      toast({ title: "Welcome!", description: "Account created", variant: "success" });
      navigate("/", { replace: true });
    },
    onError: (error) => {
      toast({
        title: "Registration failed",
        description: extractErrorMessage(error, "Could not create account"),
        variant: "destructive",
      });
    },
  });

  return (
    <div className="max-w-md mx-auto">
      <Card>
        <CardHeader>
          <CardTitle>Create an account</CardTitle>
          <CardDescription>Start learning Chinese characters today.</CardDescription>
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
              <Input id="password" type="password" autoComplete="new-password" {...register("password")} />
              {errors.password ? (
                <p className="text-xs text-destructive">{errors.password.message}</p>
              ) : null}
            </div>
            <div className="flex flex-col gap-1">
              <label htmlFor="confirm" className="text-sm font-medium">Confirm password</label>
              <Input id="confirm" type="password" autoComplete="new-password" {...register("confirm")} />
              {errors.confirm ? (
                <p className="text-xs text-destructive">{errors.confirm.message}</p>
              ) : null}
            </div>
            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending ? "Creating account…" : "Create account"}
            </Button>
            <p className="text-sm text-muted-foreground">
              Already registered?{" "}
              <Link to="/login" className="text-primary underline">
                Sign in
              </Link>
              .
            </p>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
