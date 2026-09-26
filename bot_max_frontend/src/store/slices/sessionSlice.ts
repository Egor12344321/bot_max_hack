import { createSlice, type PayloadAction } from "@reduxjs/toolkit";

import type { LanguageCode } from "@/api/types/localization";
import type { EgeScoreResult } from "@/api/types/session";
import { getEgeScoreSummary } from "@/utils/egeScore";

interface SessionState {
  sessionId: string | null;
  accessToken: string | null;

  language: LanguageCode;

  isInitialized: boolean;
  initializationError: boolean;
  isCompleteFromBot: boolean;
  egeScores: EgeScoreResult[];
}

const initialState: SessionState = {
  sessionId: null,
  accessToken: null,

  language: "ru",

  isInitialized: false,
  initializationError: false,
  isCompleteFromBot: false,
  egeScores: [],
};

interface SetSessionPayload {
  sessionId: string;
  accessToken: string;
  language: LanguageCode;
  isCompleteFromBot: boolean;
  egeScores: EgeScoreResult[];
}

const sessionSlice = createSlice({
  name: "session",

  initialState,

  reducers: {
    setSession(state, action: PayloadAction<SetSessionPayload>) {
      state.sessionId = action.payload.sessionId;
      state.accessToken = action.payload.accessToken;
      state.language = action.payload.language;
      state.isCompleteFromBot = action.payload.isCompleteFromBot;
      state.egeScores = action.payload.egeScores;
      state.initializationError = false;
    },

    setLanguage(state, action: PayloadAction<LanguageCode>) {
      state.language = action.payload;
    },

    setInitialized(state, action: PayloadAction<boolean>) {
      state.isInitialized = action.payload;
    },

    initializationFailed(state) {
      state.initializationError = true;
      state.isInitialized = false;
    },

    clearSession() {
      return initialState;
    },
  },
});

export const { setSession, setLanguage, setInitialized, clearSession, initializationFailed } =
  sessionSlice.actions;

export default sessionSlice.reducer;

export const selectEgeScores = (state: { session: SessionState }) => state.session.egeScores;
export const selectEgeTotal = (state: { session: SessionState }) =>
  getEgeScoreSummary(state.session.egeScores).total;
