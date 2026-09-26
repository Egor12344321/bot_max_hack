import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";

import { InterestsPage } from "@/pages/InterestsPage/InterestsPage";

import { AchievementsPage } from "@/pages/AchievementsPage/AchievementsPage";

import { PrivilegesPage } from "@/pages/PrivilegesPage/PrivilegesPage";

import { ProfilePage } from "@/pages/ProfilePage/ProfilePage";

import { UniversitiesPage } from "@/pages/UniversitiesPage/UniversitiesPage";
import { UniversitySearchPage } from "@/pages/UniversitySearchPage/UniversitySearchPage";

import { PrioritiesPage } from "@/pages/PrioritiesPage/PrioritiesPage";

import { CalendarPage } from "@/pages/CalendarPage/CalendarPage";

import { UniversityDetailPage } from "@/pages/UniversityDetailPage/UniversityDetailPage";

import { StrategyReportPage } from "@/pages/StrategyReportPage/StrategyReportPage";

export function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route
          path="/"
          element={<Navigate to="/onboarding/interests" replace />}
        />

        <Route path="/onboarding/interests" element={<InterestsPage />} />

        <Route path="/onboarding/achievements" element={<AchievementsPage />} />

        <Route path="/onboarding/privileges" element={<PrivilegesPage />} />

        <Route path="/profile" element={<ProfilePage />} />

        <Route path="/universities" element={<UniversitiesPage />} />
        <Route path="/universities/search" element={<UniversitySearchPage />} />

        <Route
          path="/universities/:universityId"
          element={<UniversityDetailPage />}
        />

        <Route path="/priorities" element={<PrioritiesPage />} />

        <Route path="/calendar" element={<CalendarPage />} />

        <Route path="/strategy/report" element={<StrategyReportPage />} />

        <Route
          path="*"
          element={<Navigate to="/onboarding/interests" replace />}
        />
      </Routes>
    </BrowserRouter>
  );
}
