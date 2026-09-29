import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";

import { InterestsPage } from "@/pages/InterestsPage/InterestsPage";

import { AchievementsPage } from "@/pages/AchievementsPage/AchievementsPage";

import { PrivilegesPage } from "@/pages/PrivilegesPage/PrivilegesPage";

import { ProfilePage } from "@/pages/ProfilePage/ProfilePage";
import { EgeScoresPage } from "@/pages/EgeScoresPage/EgeScoresPage";

import { PlanningPage } from "@/pages/PlanningPage/PlanningPage";
import { OlympiadsPage } from "@/pages/OlympiadsPage/OlympiadsPage";

import { CalendarPage } from "@/pages/CalendarPage/CalendarPage";


export function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route
          path="/"
          element={<Navigate to="/onboarding/interests" replace />}
        />

        <Route path="/onboarding/interests" element={<InterestsPage />} />
        <Route path="/onboarding/olympiads" element={<OlympiadsPage />} />

        <Route path="/onboarding/achievements" element={<AchievementsPage />} />

        <Route path="/onboarding/privileges" element={<PrivilegesPage />} />

        <Route path="/profile" element={<ProfilePage />} />
        <Route path="/profile/ege" element={<EgeScoresPage />} />

        <Route path="/universities" element={<PlanningPage />} />
        <Route path="/universities/search" element={<Navigate to="/universities" replace />} />

        <Route
          path="/universities/:universityId"
          element={<Navigate to="/universities" replace />}
        />

        <Route path="/priorities" element={<Navigate to="/universities?view=plan" replace />} />

        <Route path="/calendar" element={<CalendarPage />} />

        <Route path="/strategy/report" element={<Navigate to="/universities?view=plan" replace />} />

        <Route
          path="*"
          element={<Navigate to="/onboarding/interests" replace />}
        />
      </Routes>
    </BrowserRouter>
  );
}
