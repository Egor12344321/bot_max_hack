import type { PropsWithChildren } from "react";

import { MaxUI, useAppearance } from "@maxhub/max-ui";
import { Provider } from "react-redux";

import { store } from "@/store/store";

function AppTheme({ children }: PropsWithChildren) {
  const { colorScheme } = useAppearance();

  return (
    <div className="app-theme" data-theme={colorScheme}>
      {children}
    </div>
  );
}

export function AppProviders({ children }: PropsWithChildren) {
  return (
    <Provider store={store}>
      <MaxUI>
        <AppTheme>{children}</AppTheme>
      </MaxUI>
    </Provider>
  );
}
