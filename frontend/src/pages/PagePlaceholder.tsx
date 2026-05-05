import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

interface PagePlaceholderProps {
  title: string;
  description: string;
  taskId: string;
}

export function PagePlaceholder({ title, description, taskId }: PagePlaceholderProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
        <CardDescription>{description}</CardDescription>
      </CardHeader>
      <CardContent>
        <p className="text-sm text-muted-foreground">
          Implementation tracked under <code className="font-mono">{taskId}</code>.
        </p>
      </CardContent>
    </Card>
  );
}
