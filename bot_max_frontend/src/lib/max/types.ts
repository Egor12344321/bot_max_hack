export interface MaxWebApp {
  initData?: string;
}

export interface MaxBridge {
  getLaunchParams(): Promise<string>;
}

declare global {
  interface Window {
    WebApp?: MaxWebApp;
  }
}
