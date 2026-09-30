import { StrictMode } from "react";
import { createRoot } from "react-dom/client";

import "@maxhub/max-ui/dist/styles.css";

import "@/styles/reset.css";
import "@/styles/variables.css";
import "@/styles/globals.css";



import { App } from "@/app/App";
import { AppProviders } from "@/app/providers";

const root = document.getElementById("root");

if (!root) {
  throw new Error("Root element was not found");
}

createRoot(root).render(
  <StrictMode>
    <AppProviders>
      <App />
    </AppProviders>
  </StrictMode>,
);
