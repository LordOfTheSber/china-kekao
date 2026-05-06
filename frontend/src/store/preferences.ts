import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";

export type HelpLevel = "STRICT" | "NORMAL" | "EASY";
export type ProductionMode = "DRAWING" | "CHOICE";

export interface PreferencesValues {
  newPerDay: number;
  maxReviewsPerDay: number;
  requestRetention: number;
  productionMode: ProductionMode;
  helpLevel: HelpLevel;
  withTones: boolean;
}

interface PreferencesState extends PreferencesValues {
  hydrated: boolean;
  setProductionMode: (value: ProductionMode) => void;
  setHelpLevel: (value: HelpLevel) => void;
  setWithTones: (value: boolean) => void;
  hydrate: (values: Partial<PreferencesValues>) => void;
}

export const DEFAULT_PREFERENCES: PreferencesValues = {
  newPerDay: 20,
  maxReviewsPerDay: 200,
  requestRetention: 0.9,
  productionMode: "DRAWING",
  helpLevel: "NORMAL",
  withTones: true,
};

export const usePreferencesStore = create<PreferencesState>()(
  persist(
    (set) => ({
      ...DEFAULT_PREFERENCES,
      hydrated: false,
      setProductionMode: (productionMode) => set({ productionMode }),
      setHelpLevel: (helpLevel) => set({ helpLevel }),
      setWithTones: (withTones) => set({ withTones }),
      hydrate: (values) =>
        set((prev) => ({
          ...prev,
          ...values,
          hydrated: true,
        })),
    }),
    {
      name: "kekao.preferences",
      storage: createJSONStorage(() => localStorage),
    },
  ),
);
