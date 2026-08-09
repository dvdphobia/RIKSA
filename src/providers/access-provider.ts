import type { AccessControlProvider } from "@refinedev/core";
import { tokenStore } from "../lib/api";

const ADMIN_ONLY = ["users", "audit-logs"];

export const accessProvider: AccessControlProvider = {
  can: async ({ resource }) => {
    if (resource && ADMIN_ONLY.includes(resource)) {
      const user = tokenStore.getUser();
      return { can: user?.role === "ADMIN" };
    }
    return { can: true };
  },
};
