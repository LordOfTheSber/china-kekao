import { useParams } from "react-router-dom";

import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

export function HanziDetailPage() {
  const { id } = useParams<{ id: string }>();
  return (
    <Card>
      <CardHeader>
        <CardTitle>Hanzi #{id ?? "?"}</CardTitle>
        <CardDescription>
          Detail view will be implemented in TASK-027.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <p className="text-sm text-muted-foreground">
          Stroke order, pinyin, meanings, examples, study state.
        </p>
      </CardContent>
    </Card>
  );
}
