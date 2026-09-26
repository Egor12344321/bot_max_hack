import itIcon from "@/assets/it.svg";
import medicineIcon from "@/assets/medicine.svg";
import biologyIcon from "@/assets/biology.svg";
import economicsIcon from "@/assets/economy.svg";
import engineeringIcon from "@/assets/engineering.svg";
import lawIcon from "@/assets/law.svg";
import languagesIcon from "@/assets/languages.svg";
import designIcon from "@/assets/design.svg";
import physicsIcon from "@/assets/physics.svg";

const interestIcons: Record<string, string> = {
  it: itIcon,
  medicine: medicineIcon,
  biology: biologyIcon,
  economics: economicsIcon,
  engineering: engineeringIcon,
  law: lawIcon,
  languages: languagesIcon,
  design: designIcon,
  physics: physicsIcon,
};

export function getInterestIcon(id: string) {
  return interestIcons[id];
}
