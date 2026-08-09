import type { AuthProvider } from "@refinedev/core";
import { apiFetch, tokenStore } from "../lib/api";
import type { Role } from "../lib/types";

interface LoginResponse {
  userId: string;
  accessToken: string;
  refreshToken: string;
  username: string;
  role: Role;
}

export const authProvider: AuthProvider = {
  login: async ({ username, password }) => {
    try {
      const data = await apiFetch<LoginResponse>("/auth/login", {
        method: "POST",
        body: { username, password },
      });
      if (data.role === "USER") {
        return {
          success: false,
          error: new Error("This account does not have admin access."),
        };
      }
      tokenStore.save(data.accessToken, data.refreshToken, {
        userId: data.userId,
        username: data.username,
        role: data.role,
      });
      return { success: true, redirectTo: "/" };
    } catch (e) {
      return {
        success: false,
        error: e instanceof Error ? e : new Error("Login failed"),
      };
    }
  },

  logout: async () => {
    const refreshToken = tokenStore.getRefresh();
    if (refreshToken) {
      try {
        await apiFetch("/auth/logout", {
          method: "POST",
          body: { refreshToken },
        });
      } catch {
        // best effort
      }
    }
    tokenStore.clear();
    return { success: true, redirectTo: "/login" };
  },

  check: async () => {
    if (tokenStore.getAccess() && tokenStore.getUser()) {
      return { authenticated: true };
    }
    return { authenticated: false, logout: true, redirectTo: "/login" };
  },

  onError: async (error) => {
    if (error?.statusCode === 401) {
      tokenStore.clear();
      return { logout: true, redirectTo: "/login" };
    }
    return {};
  },

  getIdentity: async () => {
    const user = tokenStore.getUser();
    if (!user) return null;
    return { id: user.userId, name: user.username, role: user.role };
  },
};
