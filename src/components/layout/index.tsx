import { useEffect } from "react";
import { NavLink, useLocation } from "react-router";
import { LogOut } from "lucide-react";
import { useAuth } from "../../lib/auth";
import { NAV_ITEMS } from "../../lib/nav";
import { cn } from "../../lib/utils";
import { Button } from "../ui/button";
import { Separator } from "../ui/separator";
import type { PropsWithChildren } from "react";

export const Layout: React.FC<PropsWithChildren> = ({ children }) => {
  const { user, logout } = useAuth();
  const { pathname } = useLocation();
  const menuItems = NAV_ITEMS.filter(
    (item) => !item.adminOnly || user?.role === "ADMIN",
  );

  useEffect(() => {
    const current = NAV_ITEMS.find((item) =>
      item.path === "/" ? pathname === "/" : pathname.startsWith(item.path),
    );
    document.title = current ? `${current.label} · Riksa Admin` : "Riksa Admin";
  }, [pathname]);

  return (
    <div className="flex min-h-screen bg-muted/30">
      <aside className="fixed inset-y-0 left-0 z-20 flex w-60 flex-col border-r bg-card">
        <div className="flex h-14 items-center gap-2 border-b px-4">
          <div className="flex h-7 w-7 items-center justify-center rounded-md bg-primary text-sm font-bold text-primary-foreground">
            R
          </div>
          <span className="font-semibold">Riksa Admin</span>
        </div>
        <nav className="flex-1 space-y-1 overflow-y-auto p-3">
          {menuItems.map((item, i) => (
            <div key={item.path}>
              {item.section && item.section !== menuItems[i - 1]?.section && (
                <p className="px-3 pb-1 pt-4 text-xs font-medium uppercase tracking-wide text-muted-foreground">
                  {item.section}
                </p>
              )}
              <NavLink
                to={item.path}
                end={item.path === "/"}
                className={({ isActive }) =>
                  cn(
                    "flex items-center gap-2 rounded-md px-3 py-2 text-sm font-medium transition-colors",
                    isActive
                      ? "bg-primary text-primary-foreground"
                      : "text-muted-foreground hover:bg-accent hover:text-accent-foreground",
                  )
                }
              >
                {item.icon}
                {item.label}
              </NavLink>
            </div>
          ))}
        </nav>
        <Separator />
        <div className="flex items-center justify-between p-3">
          <div className="min-w-0">
            <p className="truncate text-sm font-medium">
              {user?.username ?? "..."}
            </p>
            <p className="text-xs text-muted-foreground">{user?.role}</p>
          </div>
          <Button
            variant="ghost"
            size="icon"
            onClick={() => logout()}
            title="Sign out"
          >
            <LogOut className="h-4 w-4" />
          </Button>
        </div>
      </aside>
      <main className="ml-60 flex-1 p-6">
        <div className="mx-auto max-w-6xl">{children}</div>
      </main>
    </div>
  );
};
