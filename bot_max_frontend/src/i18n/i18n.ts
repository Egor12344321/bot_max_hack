import i18n from "i18next";
import { initReactI18next } from "react-i18next";

import { ru } from "@/i18n/locales/ru";
import { kk } from "@/i18n/locales/kk";
import { ky } from "@/i18n/locales/ky";

i18n.use(initReactI18next).init({
  resources: {
    ru: {
      translation: ru,
    },
    kk: {
      translation: kk,
    },
    ky: {
      translation: ky,
    },
  },

  lng: "ru",
  fallbackLng: "ru",

  interpolation: {
    escapeValue: false,
  },
});

export default i18n;
