import {
  createContext,
  type PropsWithChildren,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import { Navigate, useLocation } from "react-router";
import { ApiError, apiFetch, AUTH_EXPIRED_EVENT, tokenStore } from "./api";
import type {
  AuthUser,
  Role,
  TelegramAuthResult,
  TelegramAuthStartResponse,
  TelegramBotInfo,
} from "./types";

interface AuthContextValue {
  user: AuthUser | null;
  getTelegramBotInfo: () => Promise<TelegramBotInfo>;
  startTelegramAuth: () => Promise<TelegramAuthStartResponse>;
  checkTelegramStatus: (
    requestToken: string,
  ) => Promise<{ completed: boolean; expiresAt?: string }>;
  loginWithTelegramCode: (code: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export const AuthProvider = ({ children }: PropsWithChildren) => {
  const [user, setUser] = useState<AuthUser | null>(() => {
    const stored = tokenStore.getAccess() ? tokenStore.getUser() : null;
    if (!stored || stored.role !== "ADMIN") {
      tokenStore.clear();
      return null;
    }
    return stored;
  });

  // apiFetch fires this when a 401 survives the refresh attempt.
  useEffect(() => {
    const onExpired = () => setUser(null);
    window.addEventListener(AUTH_EXPIRED_EVENT, onExpired);
    return () => window.removeEventListener(AUTH_EXPIRED_EVENT, onExpired);
  }, []);

  // Re-validate stored session against /auth/me on startup to ensure role is still ADMIN.
  useEffect(() => {
    if (!tokenStore.getAccess()) return;
    let cancelled = false;
    apiFetch<{ id: string; username: string; role: Role }>("/auth/me")
      .then((profile) => {
        if (cancelled) return;
        if (profile.role !== "ADMIN") {
          tokenStore.clear();
          setUser(null);
          return;
        }
        const access = tokenStore.getAccess();
        const refresh = tokenStore.getRefresh();
        if (access && refresh) {
          const next: AuthUser = {
            userId: profile.id,
            username: profile.username,
            role: profile.role,
          };
          tokenStore.save(access, refresh, next);
          setUser(next);
        }
      })
      .catch((err) => {
        if (cancelled) return;
        if (err instanceof ApiError && (err.status === 401 || err.status === 403)) {
          tokenStore.clear();
          setUser(null);
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const resolveTelegramResult = useCallback(
    async (
      result: TelegramAuthResult,
    ): Promise<{ completed: boolean; expiresAt?: string }> => {
      switch (result.status) {
        case "PENDING":
          return { completed: false, expiresAt: result.expiresAt };
        case "DENIED":
          throw new Error("Sign-in request was declined in Telegram. Please try again.");
        case "EXPIRED":
          throw new Error("Sign-in request expired. Please start again.");
        case "PROFILE_REQUIRED":
          throw new Error(
            "No administrator account is linked to this Telegram account.",
          );
        case "AUTHENTICATED": {
          const data = result.auth;
          if (!data) {
            throw new Error("Invalid authentication response from server.");
          }
          if (data.role !== "ADMIN") {
            if (data.refreshToken) {
              try {
                await apiFetch("/auth/logout", {
                  method: "POST",
                  body: { refreshToken: data.refreshToken },
                });
              } catch {
                // best effort revocation of non-admin session
              }
            }
            throw new Error("Access denied. Only administrator accounts can sign in.");
          }
          const next: AuthUser = {
            userId: data.userId,
            username: data.username,
            role: data.role,
          };
          tokenStore.save(data.accessToken, data.refreshToken, next);
          setUser(next);
          return { completed: true };
        }
      }
    },
    [],
  );

  const getTelegramBotInfo = useCallback(
    () => apiFetch<TelegramBotInfo>("/auth/telegram/bot"),
    [],
  );

  const startTelegramAuth = useCallback(
    () =>
      apiFetch<TelegramAuthStartResponse>("/auth/telegram/start", {
        method: "POST",
      }),
    [],
  );

  const checkTelegramStatus = useCallback(
    async (requestToken: string) => {
      const result = await apiFetch<TelegramAuthResult>("/auth/telegram/status", {
        method: "POST",
        body: { requestToken },
      });
      return resolveTelegramResult(result);
    },
    [resolveTelegramResult],
  );

  const loginWithTelegramCode = useCallback(
    async (code: string) => {
      const result = await apiFetch<TelegramAuthResult>("/auth/telegram/code", {
        method: "POST",
        body: { code },
      });
      const resolved = await resolveTelegramResult(result);
      if (!resolved.completed) {
        throw new Error("Telegram authentication did not complete.");
      }
    },
    [resolveTelegramResult],
  );

  const logout = useCallback(async () => {
    const refreshToken = tokenStore.getRefresh();
    if (refreshToken) {
      try {
        await apiFetch("/auth/logout", { method: "POST", body: { refreshToken } });
      } catch {
        // best effort
      }
    }
    tokenStore.clear();
    setUser(null);
  }, []);

  const value = useMemo(
    () => ({
      user,
      getTelegramBotInfo,
      startTelegramAuth,
      checkTelegramStatus,
      loginWithTelegramCode,
      logout,
    }),
    [
      user,
      getTelegramBotInfo,
      startTelegramAuth,
      checkTelegramStatus,
      loginWithTelegramCode,
      logout,
    ],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside <AuthProvider>");
  return ctx;
};

export const useIsAdmin = () => useAuth().user?.role === "ADMIN";

/** Redirects to /login when signed out or when the account does not hold ADMIN role. */
export const RequireAuth = ({
  children,
}: PropsWithChildren<{ adminOnly?: boolean }>) => {
  const { user } = useAuth();
  const location = useLocation();
  if (!user || user.role !== "ADMIN") {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  return <>{children}</>;
};
