import { create } from "zustand";
import { persist, createJSONStorage } from "zustand/middleware";

export type HelpLevel = "STRICT" | "NORMAL" | "EASY";
export type ProductionMode = "DRAWING" | "CHOICE";

interface PreferencesState {
  productionMode: ProductionMode;
  helpLevel: HelpLevel;
  setProductionMode: (value: ProductionMode) => void;
  setHelpLevel: (value: HelpLevel) => void;
}

export const usePreferencesStore = create<PreferencesState>()(
  persist(
    (set) => ({
      productionMode: "DRAWING",
      helpLevel: "NORMAL",
      setProductionMode: (productionMode) => set({ productionMode }),
      setHelpLevel: (helpLevel) => set({ helpLevel }),
    }),
    {
      name: "kekao.preferences",
      storage: createJSONStorage(() => localStorage),
    },
  ),
);
