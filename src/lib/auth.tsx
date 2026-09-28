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
import { apiFetch, AUTH_EXPIRED_EVENT, tokenStore } from "./api";
import type { AuthUser, Role } from "./types";

interface LoginResponse {
  userId: string;
  accessToken: string;
  refreshToken: string;
  username: string;
  role: Role;
}

interface AuthContextValue {
  user: AuthUser | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export const AuthProvider = ({ children }: PropsWithChildren) => {
  const [user, setUser] = useState<AuthUser | null>(() =>
    tokenStore.getAccess() ? tokenStore.getUser() : null,
  );

  // apiFetch fires this when a 401 survives the refresh attempt.
  useEffect(() => {
    const onExpired = () => setUser(null);
    window.addEventListener(AUTH_EXPIRED_EVENT, onExpired);
    return () => window.removeEventListener(AUTH_EXPIRED_EVENT, onExpired);
  }, []);

  const login = useCallback(async (username: string, password: string) => {
    const data = await apiFetch<LoginResponse>("/auth/login", {
      method: "POST",
      body: { username, password },
    });
    if (data.role === "USER") {
      throw new Error("This account does not have admin access.");
    }
    const next = { userId: data.userId, username: data.username, role: data.role };
    tokenStore.save(data.accessToken, data.refreshToken, next);
    setUser(next);
  }, []);

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

  const value = useMemo(() => ({ user, login, logout }), [user, login, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside <AuthProvider>");
  return ctx;
};

export const useIsAdmin = () => useAuth().user?.role === "ADMIN";

/** Redirects to /login when signed out, and to / when the role is too low. */
export const RequireAuth = ({
  adminOnly = false,
  children,
}: PropsWithChildren<{ adminOnly?: boolean }>) => {
  const { user } = useAuth();
  const location = useLocation();
  if (!user) return <Navigate to="/login" replace state={{ from: location }} />;
  if (adminOnly && user.role !== "ADMIN") return <Navigate to="/" replace />;
  return <>{children}</>;
};
