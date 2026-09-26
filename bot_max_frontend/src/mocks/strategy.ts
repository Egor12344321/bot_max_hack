import type { StrategyFinalizeResponse } from "@/api/types/strategy";

import { getMockPriorities } from "@/mocks/mockApi";
import { getMockStorage, saveMockStorage } from "@/mocks/mockStorage";

function delay(ms = 300) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

export async function finalizeMockStrategy(): Promise<StrategyFinalizeResponse> {
  await delay();

  const priorities = await getMockPriorities();

  const report: StrategyFinalizeResponse = {
    status: "saved",
    summary: priorities,
    reportUrl: null,
  };
  const storage = getMockStorage();
  storage.strategyReport = report;
  saveMockStorage(storage);
  return report;
}

export async function getMockStrategyReport(): Promise<StrategyFinalizeResponse> {
  await delay();

  const report = getMockStorage().strategyReport;
  if (!report) throw new Error("Strategy has not been finalized");
  return report;
}
