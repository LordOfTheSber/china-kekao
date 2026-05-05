import { Link } from "react-router-dom";

import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

export function RegisterPage() {
  return (
    <div className="max-w-md mx-auto">
      <Card>
        <CardHeader>
          <CardTitle>Create an account</CardTitle>
          <CardDescription>
            Form will be wired up in TASK-018.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">
            Already registered?{" "}
            <Link to="/login" className="text-primary underline">
              Sign in
            </Link>
            .
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
