import type { MaxBridge } from "@/lib/max/types";

export const mockBridge: MaxBridge = {
  async getLaunchParams() {
    return "mock-launch-params";
  },
};
