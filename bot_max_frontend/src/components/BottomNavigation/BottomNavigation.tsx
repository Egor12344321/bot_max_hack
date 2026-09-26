import { NavLink } from "react-router-dom";

import { useTranslation } from "react-i18next";

import styles from "./BottomNavigation.module.css";

export function BottomNavigation() {
  const { t } = useTranslation();

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
            isActive ? `${styles.item} ${styles.itemActive}` : styles.item
          }
        >
          <span className={styles.icon}>🏛️</span>

          <span className={styles.label}>{t("universities.title")}</span>
        </NavLink>

        <NavLink
          to="/priorities"
          className={({ isActive }) =>
            isActive ? `${styles.item} ${styles.itemActive}` : styles.item
          }
        >
          <span className={styles.icon}>📊</span>

          <span className={styles.label}>{t("priorities.title")}</span>
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
