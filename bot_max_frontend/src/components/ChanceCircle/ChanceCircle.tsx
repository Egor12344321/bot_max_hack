import styles from "./ChanceCircle.module.css";

interface ChanceCircleProps {
  percent: number;
}

export function ChanceCircle({ percent }: ChanceCircleProps) {
  const radius = 47;

  const circumference = 2 * Math.PI * radius;

  const safePercent = Math.min(Math.max(percent, 0), 100);

  const offset = circumference - (circumference * safePercent) / 100;

  return (
    <div className={styles.wrapper}>
      <svg width="110" height="110" viewBox="0 0 110 110">
        <circle cx="55" cy="55" r={radius} className={styles.background} />

        <circle
          cx="55"
          cy="55"
          r={radius}
          className={styles.progress}
          strokeDasharray={circumference}
          strokeDashoffset={offset}
        />
      </svg>

      <div className={styles.text}>
        <span className={styles.percent}>{safePercent}%</span>

        <span className={styles.label}>шанс</span>
      </div>
    </div>
  );
}
