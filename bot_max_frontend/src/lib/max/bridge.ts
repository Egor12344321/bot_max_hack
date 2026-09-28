import type { MaxBridge } from "@/lib/max/types";

import { mockBridge } from "@/lib/max/mockBridge";
import { AppError } from "@/utils/appError";

const isMock = import.meta.env.VITE_MAX_MODE === "mock";

const realBridge: MaxBridge = {
  async getLaunchParams() {
    const webApp = window.WebApp;

    if (!webApp) {
      throw new AppError("MAX_SDK_UNAVAILABLE");
    }

    return webApp.initData || "";
  },
};

export const maxBridge = isMock ? mockBridge : realBridge;
