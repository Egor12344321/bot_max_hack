import { exchangeAuth } from "@/api/authApi";
import { getSession } from "@/api/sessionApi";
import { clearAccessToken, setAccessToken } from "@/api/client";
import { maxBridge } from "@/lib/max/bridge";

async function loadSession() {
  clearAccessToken();
  try {
    const launchParams = await maxBridge.getLaunchParams();
    const auth = await exchangeAuth(launchParams);
    setAccessToken(auth.accessToken);
    const session = await getSession(auth.session.id);
    return { session, accessToken: auth.accessToken };
  } catch (error) {
    clearAccessToken();
    throw error;
  }
}

// Share the pending exchange when StrictMode mounts the effect twice.
let pending: ReturnType<typeof loadSession> | null = null;

export function initializeSession() {
  pending ??= loadSession().finally(() => { pending = null; });
  return pending;
}
