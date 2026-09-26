import type { ReactNode } from "react";

import { BottomNavigation } from "@/components/BottomNavigation/BottomNavigation";

import styles from "./AppLayout.module.css";

interface AppLayoutProps {
  children: ReactNode;
}

export function AppLayout({ children }: AppLayoutProps) {
  return (
    <div className={styles.app}>
      <main className={styles.content}>{children}</main>

      <BottomNavigation />
    </div>
  );
}
