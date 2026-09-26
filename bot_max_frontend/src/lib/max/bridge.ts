import type { MaxBridge } from "@/lib/max/types";

import { mockBridge } from "@/lib/max/mockBridge";

const isMock = import.meta.env.VITE_MAX_MODE === "mock";

const realBridge: MaxBridge = {
  async getLaunchParams() {
    const webApp = window.WebApp;

    if (!webApp) {
      throw new Error("MAX WebApp is not available");
    }

    return webApp.initData || "";
  },
};

export const maxBridge = isMock ? mockBridge : realBridge;
