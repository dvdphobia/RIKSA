import type { AuthUser } from "./types";

export const API_URL =
  (import.meta.env.VITE_API_URL as string | undefined) ??
  "http://localhost:8080/api/v1";

/** Dispatched on window when the session can't be refreshed. */
export const AUTH_EXPIRED_EVENT = "riksa:auth-expired";

const ACCESS_KEY = "riksa_admin_access_token";
const REFRESH_KEY = "riksa_admin_refresh_token";
const USER_KEY = "riksa_admin_user";

export const tokenStore = {
  getAccess: () => localStorage.getItem(ACCESS_KEY),
  getRefresh: () => localStorage.getItem(REFRESH_KEY),
  getUser: (): AuthUser | null => {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? (JSON.parse(raw) as AuthUser) : null;
  },
  save: (accessToken: string, refreshToken: string, user: AuthUser) => {
    localStorage.setItem(ACCESS_KEY, accessToken);
    localStorage.setItem(REFRESH_KEY, refreshToken);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  },
  clear: () => {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    localStorage.removeItem(USER_KEY);
  },
};

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
  ) {
    super(message);
  }
}

async function parseError(res: Response): Promise<ApiError> {
  let message = `${res.status} ${res.statusText}`;
  try {
    const body = await res.json();
    if (body?.message) message = body.message;
  } catch {
    // keep default message
  }
  return new ApiError(res.status, message);
}

let refreshPromise: Promise<boolean> | null = null;

async function refreshTokens(): Promise<boolean> {
  const refreshToken = tokenStore.getRefresh();
  if (!refreshToken) return false;
  try {
    const res = await fetch(`${API_URL}/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });
    if (!res.ok) return false;
    const data = await res.json();
    const user = tokenStore.getUser();
    if (!data.accessToken || !user) return false;
    tokenStore.save(data.accessToken, data.refreshToken ?? refreshToken, user);
    return true;
  } catch {
    return false;
  }
}

export interface ApiOptions {
  method?: string;
  body?: unknown;
  params?: Record<string, string | number | boolean | undefined | null>;
  formData?: FormData;
}

export async function apiFetch<T>(path: string, options: ApiOptions = {}): Promise<T> {
  const doFetch = async () => {
    const url = new URL(`${API_URL}${path}`);
    if (options.params) {
      for (const [k, v] of Object.entries(options.params)) {
        if (v !== undefined && v !== null && v !== "") {
          url.searchParams.set(k, String(v));
        }
      }
    }
    const headers: Record<string, string> = {};
    const token = tokenStore.getAccess();
    if (token) headers.Authorization = `Bearer ${token}`;
    let body: BodyInit | undefined;
    if (options.formData) {
      body = options.formData;
    } else if (options.body !== undefined) {
      headers["Content-Type"] = "application/json";
      body = JSON.stringify(options.body);
    }
    return fetch(url, { method: options.method ?? "GET", headers, body });
  };

  let res = await doFetch();
  if (res.status === 401) {
    refreshPromise ??= refreshTokens().finally(() => {
      refreshPromise = null;
    });
    const refreshed = await refreshPromise;
    if (refreshed) res = await doFetch();
    if (res.status === 401 && tokenStore.getAccess()) {
      tokenStore.clear();
      window.dispatchEvent(new Event(AUTH_EXPIRED_EVENT));
    }
  }
  if (res.status === 204) return undefined as T;
  if (!res.ok) throw await parseError(res);
  return (await res.json()) as T;
}
