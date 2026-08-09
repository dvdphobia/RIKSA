import { useGetIdentity, useLogout, useMenu } from "@refinedev/core";
import { NavLink } from "react-router";
import { LogOut } from "lucide-react";
import { cn } from "../../lib/utils";
import { Button } from "../ui/button";
import { Separator } from "../ui/separator";
import type { PropsWithChildren } from "react";

interface Identity {
  id: string;
  name: string;
  role: string;
}

export const Layout: React.FC<PropsWithChildren> = ({ children }) => {
  const { menuItems } = useMenu();
  const { mutate: logout } = useLogout();
  const { data: identity } = useGetIdentity<Identity>();

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
          {menuItems.map((item) => (
            <NavLink
              key={item.key}
              to={item.route ?? "/"}
              end={item.route === "/"}
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
          ))}
        </nav>
        <Separator />
        <div className="flex items-center justify-between p-3">
          <div className="min-w-0">
            <p className="truncate text-sm font-medium">
              {identity?.name ?? "..."}
            </p>
            <p className="text-xs text-muted-foreground">{identity?.role}</p>
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
