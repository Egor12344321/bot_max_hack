import { NavLink, useLocation } from "react-router-dom";

import { useTranslation } from "react-i18next";

import styles from "./BottomNavigation.module.css";

export function BottomNavigation() {
  const { t } = useTranslation();
  const location = useLocation();
  const planActive =
    location.pathname === "/universities" &&
    new URLSearchParams(location.search).get("view") === "plan";

  return (
    <nav className={styles.navigation}>
      <div className={styles.navigationInner}>
        <NavLink
          to="/profile"
          className={({ isActive }) =>
            isActive ? `${styles.item} ${styles.itemActive}` : styles.item
          }
        >
          <span className={styles.icon}>👤</span>

          <span className={styles.label}>{t("profile.title")}</span>
        </NavLink>

        <NavLink
          to="/universities"
          className={({ isActive }) =>
            isActive && !planActive
              ? `${styles.item} ${styles.itemActive}`
              : styles.item
          }
        >
          <span className={styles.icon}>🏛️</span>

          <span className={styles.label}>Результаты</span>
        </NavLink>

        <NavLink
          to="/universities?view=plan"
          className={() =>
            planActive ? `${styles.item} ${styles.itemActive}` : styles.item
          }
        >
          <span className={styles.icon}>📊</span>

          <span className={styles.label}>План 5×5</span>
        </NavLink>

        <NavLink
          to="/calendar"
          className={({ isActive }) =>
            isActive ? `${styles.item} ${styles.itemActive}` : styles.item
          }
        >
          <span className={styles.icon}>📅</span>

          <span className={styles.label}>{t("calendar.title")}</span>
        </NavLink>
      </div>
    </nav>
  );
}
