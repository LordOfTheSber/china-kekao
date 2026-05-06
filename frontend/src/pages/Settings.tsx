import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { cn } from "@/lib/utils";
import {
  usePreferencesStore,
  type HelpLevel,
  type ProductionMode,
} from "@/store/preferences";

const PRODUCTION_OPTIONS: Array<{ value: ProductionMode; label: string; description: string }> = [
  {
    value: "DRAWING",
    label: "Drawing",
    description: "Draw the character stroke by stroke.",
  },
  {
    value: "CHOICE",
    label: "Choice",
    description: "Pick the correct character from six options.",
  },
];

const HELP_OPTIONS: Array<{ value: HelpLevel; label: string; description: string }> = [
  {
    value: "STRICT",
    label: "Strict",
    description: "No outline, no hints. Stricter stroke matching.",
  },
  {
    value: "NORMAL",
    label: "Normal",
    description: "Outline visible, hint after 3 misses on a stroke.",
  },
  {
    value: "EASY",
    label: "Easy",
    description: "Outline visible, hint after 1 miss, looser matching.",
  },
];

export function SettingsPage() {
  const productionMode = usePreferencesStore((s) => s.productionMode);
  const helpLevel = usePreferencesStore((s) => s.helpLevel);
  const setProductionMode = usePreferencesStore((s) => s.setProductionMode);
  const setHelpLevel = usePreferencesStore((s) => s.setHelpLevel);

  return (
    <div className="max-w-2xl mx-auto flex flex-col gap-6">
      <div>
        <h1 className="text-2xl font-semibold">Settings</h1>
        <p className="text-sm text-muted-foreground">
          Preferences are stored locally on this device.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Production mode</CardTitle>
          <CardDescription>
            How recall (English → character) cards are tested.
          </CardDescription>
        </CardHeader>
        <CardContent className="grid sm:grid-cols-2 gap-2">
          {PRODUCTION_OPTIONS.map((opt) => (
            <OptionButton
              key={opt.value}
              selected={productionMode === opt.value}
              label={opt.label}
              description={opt.description}
              onClick={() => setProductionMode(opt.value)}
            />
          ))}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Drawing help level</CardTitle>
          <CardDescription>
            Applies when production mode is set to Drawing.
          </CardDescription>
        </CardHeader>
        <CardContent className="grid sm:grid-cols-3 gap-2">
          {HELP_OPTIONS.map((opt) => (
            <OptionButton
              key={opt.value}
              selected={helpLevel === opt.value}
              label={opt.label}
              description={opt.description}
              onClick={() => setHelpLevel(opt.value)}
              disabled={productionMode !== "DRAWING"}
            />
          ))}
        </CardContent>
      </Card>
    </div>
  );
}

function OptionButton({
  selected,
  label,
  description,
  onClick,
  disabled,
}: {
  selected: boolean;
  label: string;
  description: string;
  onClick: () => void;
  disabled?: boolean;
}) {
  return (
    <Button
      type="button"
      variant="outline"
      onClick={onClick}
      disabled={disabled}
      className={cn(
        "h-auto flex flex-col items-start gap-1 p-4 text-left whitespace-normal",
        selected && "ring-2 ring-foreground border-foreground",
      )}
    >
      <span className="text-sm font-semibold">{label}</span>
      <span className="text-xs text-muted-foreground">{description}</span>
    </Button>
  );
}
