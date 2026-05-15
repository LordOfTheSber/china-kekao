import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import {
  fetchUserSettings,
  updateUserSettings,
  type UserSettings,
  type UserSettingsPatch,
} from "@/api/me";
import { extractErrorMessage } from "@/api/auth";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { HanziLoader } from "@/components/HanziLoader";
import { AppearanceCard } from "@/components/AppearanceCard";
import { toast } from "@/components/Toaster";
import { cn } from "@/lib/utils";
import {
  usePreferencesStore,
  type HelpLevel,
  type ProductionMode,
  type PromptMode,
} from "@/store/preferences";

const PROMPT_OPTIONS: Array<{ value: PromptMode; label: string; description: string }> = [
  { value: "BOTH", label: "Both", description: "Pinyin and meaning together (default)." },
  { value: "PINYIN_ONLY", label: "Pinyin only", description: "Skip the meaning, focus on reading." },
  { value: "MEANING_ONLY", label: "Meaning only", description: "Skip the pinyin, focus on the gloss." },
];

const PRODUCTION_OPTIONS: Array<{ value: ProductionMode; label: string; description: string }> = [
  { value: "DRAWING", label: "Drawing", description: "Draw the character stroke by stroke." },
  { value: "CHOICE", label: "Choice", description: "Pick the correct character from six options." },
];

const HELP_OPTIONS: Array<{ value: HelpLevel; label: string; description: string }> = [
  { value: "STRICT", label: "Strict", description: "No outline, no hints. Stricter stroke matching." },
  { value: "NORMAL", label: "Normal", description: "Outline visible, hint after 3 misses on a stroke." },
  { value: "EASY", label: "Easy", description: "Outline visible, hint after 1 miss, looser matching." },
];

const RETENTION_PRESETS: Array<{ label: string; value: number; description: string }> = [
  { label: "Casual", value: 0.85, description: "Fewer reviews, lower retention." },
  { label: "Normal", value: 0.9, description: "FSRS default." },
  { label: "Intensive", value: 0.95, description: "More reviews, higher retention." },
];

export function SettingsPage() {
  const queryClient = useQueryClient();
  const hydrate = usePreferencesStore((s) => s.hydrate);

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ["user-settings"],
    queryFn: fetchUserSettings,
  });

  const mutation = useMutation({
    mutationFn: updateUserSettings,
    onSuccess: (settings) => {
      queryClient.setQueryData(["user-settings"], settings);
      hydrate({
        newPerDay: settings.newPerDay,
        maxReviewsPerDay: settings.maxReviewsPerDay,
        requestRetention: settings.requestRetention,
        productionMode: settings.productionMode,
        helpLevel: settings.drawingHelpLevel,
        withTones: settings.withTones,
      });
      toast({ title: "Settings saved" });
    },
    onError: (error) => {
      toast({
        title: "Could not save settings",
        description: extractErrorMessage(error, "Try again"),
        variant: "destructive",
      });
    },
  });

  if (isLoading) {
    return (
      <div className="min-h-[40vh] flex items-center justify-center">
        <HanziLoader size={96} label="Loading preferences…" />
      </div>
    );
  }
  if (isError || !data) {
    return (
      <Card className="max-w-2xl mx-auto">
        <CardContent className="pt-6 flex items-center justify-between">
          <p className="text-sm text-destructive">Could not load your settings.</p>
          <Button size="sm" variant="outline" onClick={() => refetch()}>Retry</Button>
        </CardContent>
      </Card>
    );
  }

  return (
    <SettingsForm
      initial={data}
      onSubmit={(patch) => mutation.mutate(patch)}
      saving={mutation.isPending}
    />
  );
}

function SettingsForm({
  initial,
  onSubmit,
  saving,
}: {
  initial: UserSettings;
  onSubmit: (patch: UserSettingsPatch) => void;
  saving: boolean;
}) {
  const [newPerDay, setNewPerDay] = useState(initial.newPerDay);
  const [maxReviewsPerDay, setMaxReviewsPerDay] = useState(initial.maxReviewsPerDay);
  const [requestRetention, setRequestRetention] = useState(initial.requestRetention);
  const [productionMode, setProductionMode] = useState<ProductionMode>(initial.productionMode);
  const [drawingHelpLevel, setDrawingHelpLevel] = useState<HelpLevel>(initial.drawingHelpLevel);
  const [withTones, setWithTones] = useState<boolean>(initial.withTones);

  useEffect(() => {
    setNewPerDay(initial.newPerDay);
    setMaxReviewsPerDay(initial.maxReviewsPerDay);
    setRequestRetention(initial.requestRetention);
    setProductionMode(initial.productionMode);
    setDrawingHelpLevel(initial.drawingHelpLevel);
    setWithTones(initial.withTones);
  }, [initial]);

  const dirty =
    newPerDay !== initial.newPerDay ||
    maxReviewsPerDay !== initial.maxReviewsPerDay ||
    requestRetention !== initial.requestRetention ||
    productionMode !== initial.productionMode ||
    drawingHelpLevel !== initial.drawingHelpLevel ||
    withTones !== initial.withTones;

  function handleSave() {
    onSubmit({
      newPerDay,
      maxReviewsPerDay,
      requestRetention,
      productionMode,
      drawingHelpLevel,
      withTones,
    });
  }

  return (
    <div className="max-w-2xl mx-auto w-full flex flex-col gap-6">
      <div>
        <h1 className="font-hanzi text-3xl sm:text-4xl font-bold tracking-tight text-ink">
          设置 <span className="text-2xl sm:text-3xl">Settings</span>
        </h1>
        <p className="text-sm text-ink-soft mt-1">
          Saved on the server and synced across your devices.
        </p>
      </div>

      <AppearanceCard />

      <Card>
        <CardHeader>
          <CardTitle>Daily limits</CardTitle>
          <CardDescription>
            Caps for the daily study queue.
          </CardDescription>
        </CardHeader>
        <CardContent className="grid sm:grid-cols-2 gap-4">
          <NumberField
            id="newPerDay"
            label="New cards per day"
            value={newPerDay}
            min={0}
            max={200}
            onChange={setNewPerDay}
          />
          <NumberField
            id="maxReviewsPerDay"
            label="Max reviews per day"
            value={maxReviewsPerDay}
            min={0}
            max={2000}
            onChange={setMaxReviewsPerDay}
          />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Target retention</CardTitle>
          <CardDescription>
            Higher retention means more reviews. FSRS schedules around this target.
          </CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-3">
          <div className="flex items-center gap-3">
            <input
              type="range"
              min={0.8}
              max={0.97}
              step={0.01}
              value={requestRetention}
              onChange={(event) => setRequestRetention(Number(event.target.value))}
              className="flex-1"
            />
            <span className="font-mono text-sm w-12 text-right">
              {Math.round(requestRetention * 100)}%
            </span>
          </div>
          <div className="flex flex-wrap gap-2">
            {RETENTION_PRESETS.map((preset) => (
              <Button
                key={preset.label}
                size="sm"
                type="button"
                variant={Math.abs(requestRetention - preset.value) < 0.005 ? "default" : "outline"}
                onClick={() => setRequestRetention(preset.value)}
                title={preset.description}
              >
                {preset.label} ({Math.round(preset.value * 100)}%)
              </Button>
            ))}
          </div>
        </CardContent>
      </Card>

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
              selected={drawingHelpLevel === opt.value}
              label={opt.label}
              description={opt.description}
              onClick={() => setDrawingHelpLevel(opt.value)}
              disabled={productionMode !== "DRAWING"}
            />
          ))}
        </CardContent>
      </Card>

      <PromptModeCard
        title="Recognition prompt"
        description="When you see the character, what should you type?"
        prefKey="recognitionPrompt"
      />

      <PromptModeCard
        title="Production prompt"
        description="When you need to recall the character, what hint do you see?"
        prefKey="productionPrompt"
      />

      <Card>
        <CardHeader>
          <CardTitle>Pinyin tone checking</CardTitle>
          <CardDescription>
            Whether tones are required when checking pinyin during recognition.
          </CardDescription>
        </CardHeader>
        <CardContent className="grid sm:grid-cols-2 gap-2">
          <OptionButton
            selected={withTones}
            label="Strict (with tones)"
            description="ni3 ≠ ni4. Required for correct."
            onClick={() => setWithTones(true)}
          />
          <OptionButton
            selected={!withTones}
            label="Lenient (ignore tones)"
            description="Any tone counts as long as the syllable matches."
            onClick={() => setWithTones(false)}
          />
        </CardContent>
      </Card>

      <div className="sticky bottom-2 z-10 flex justify-end">
        <Button
          size="lg"
          disabled={!dirty || saving}
          onClick={handleSave}
          className="w-full sm:w-auto shadow-card-hover"
        >
          {saving ? "Saving…" : "Save changes"}
        </Button>
      </div>
    </div>
  );
}

function PromptModeCard({
  title,
  description,
  prefKey,
}: {
  title: string;
  description: string;
  prefKey: "recognitionPrompt" | "productionPrompt";
}) {
  const value = usePreferencesStore((s) => s[prefKey]);
  const setRecognition = usePreferencesStore((s) => s.setRecognitionPrompt);
  const setProduction = usePreferencesStore((s) => s.setProductionPrompt);
  const setter = prefKey === "recognitionPrompt" ? setRecognition : setProduction;
  return (
    <Card>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
        <CardDescription>{description}</CardDescription>
      </CardHeader>
      <CardContent className="grid sm:grid-cols-3 gap-2">
        {PROMPT_OPTIONS.map((opt) => (
          <OptionButton
            key={opt.value}
            selected={value === opt.value}
            label={opt.label}
            description={opt.description}
            onClick={() => setter(opt.value)}
          />
        ))}
      </CardContent>
    </Card>
  );
}

function NumberField({
  id,
  label,
  value,
  min,
  max,
  onChange,
}: {
  id: string;
  label: string;
  value: number;
  min: number;
  max: number;
  onChange: (value: number) => void;
}) {
  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={id} className="text-sm font-medium">{label}</label>
      <Input
        id={id}
        type="number"
        min={min}
        max={max}
        value={value}
        onChange={(event) => {
          const next = Number(event.target.value);
          if (Number.isFinite(next)) onChange(Math.max(min, Math.min(max, Math.floor(next))));
        }}
      />
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
        "h-auto flex flex-col items-start gap-1 p-4 text-left whitespace-normal w-full min-w-0",
        selected && "ring-2 ring-primary border-primary bg-primary/5",
      )}
    >
      <span className="text-sm font-semibold">{label}</span>
      <span className="text-xs text-muted-foreground">{description}</span>
    </Button>
  );
}
