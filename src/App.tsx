import {
  Authenticated,
  ErrorComponent,
  Refine,
} from "@refinedev/core";
import { DevtoolsPanel, DevtoolsProvider } from "@refinedev/devtools";
import { RefineKbar, RefineKbarProvider } from "@refinedev/kbar";
import routerProvider, {
  DocumentTitleHandler,
  UnsavedChangesNotifier,
} from "@refinedev/react-router";
import { BrowserRouter, Outlet, Route, Routes } from "react-router";
import {
  BookOpen,
  FileText,
  Flag,
  LayoutDashboard,
  ScrollText,
  Shield,
  Tags,
  Users,
} from "lucide-react";
import "./App.css";
import { Layout } from "./components/layout";
import { Toaster } from "./components/ui/sonner";
import { accessProvider } from "./providers/access-provider";
import { authProvider } from "./providers/auth-provider";
import { dataProvider } from "./providers/data-provider";
import { AuditLogList } from "./pages/audit-logs/list";
import { BlogList } from "./pages/blog/list";
import { Dashboard } from "./pages/dashboard";
import { CategoryCreate, CategoryEdit } from "./pages/categories/form";
import { CategoryList } from "./pages/categories/list";
import { LessonCreate, LessonEdit } from "./pages/lessons/form";
import { LessonList } from "./pages/lessons/list";
import { Login } from "./pages/login";
import { ModerationList } from "./pages/moderation/list";
import { ReportList } from "./pages/reports/list";
import { UserList } from "./pages/users/list";

function App() {
  return (
    <BrowserRouter>
      <RefineKbarProvider>
        <DevtoolsProvider>
          <Refine
            dataProvider={dataProvider}
            authProvider={authProvider}
            accessControlProvider={accessProvider}
            routerProvider={routerProvider}
            resources={[
              {
                name: "dashboard",
                list: "/",
                meta: { label: "Dashboard", icon: <LayoutDashboard size={16} /> },
              },
              {
                name: "users",
                list: "/users",
                meta: { label: "Users", icon: <Users size={16} /> },
              },
              {
                name: "lessons",
                list: "/lessons",
                create: "/lessons/create",
                edit: "/lessons/edit/:id",
                meta: { label: "Lessons", icon: <BookOpen size={16} /> },
              },
              {
                name: "categories",
                list: "/categories",
                create: "/categories/create",
                edit: "/categories/edit/:id",
                meta: { label: "Categories", icon: <Tags size={16} /> },
              },
              {
                name: "reports",
                list: "/reports",
                meta: { label: "Reports", icon: <Flag size={16} /> },
              },
              {
                name: "blog",
                list: "/blog",
                meta: { label: "Blog queue", icon: <FileText size={16} /> },
              },
              {
                name: "moderation",
                list: "/moderation",
                meta: { label: "Hidden content", icon: <Shield size={16} /> },
              },
              {
                name: "audit-logs",
                list: "/audit-logs",
                meta: { label: "Audit logs", icon: <ScrollText size={16} /> },
              },
            ]}
            options={{
              syncWithLocation: true,
              warnWhenUnsavedChanges: true,
              disableTelemetry: true,
            }}
          >
            <Routes>
              <Route path="/login" element={<Login />} />
              <Route
                element={
                  <Authenticated
                    key="authenticated-routes"
                    fallback={<Login />}
                  >
                    <Layout>
                      <Outlet />
                    </Layout>
                  </Authenticated>
                }
              >
                <Route index element={<Dashboard />} />
                <Route path="/users" element={<UserList />} />
                <Route path="/lessons">
                  <Route index element={<LessonList />} />
                  <Route path="create" element={<LessonCreate />} />
                  <Route path="edit/:id" element={<LessonEdit />} />
                </Route>
                <Route path="/categories">
                  <Route index element={<CategoryList />} />
                  <Route path="create" element={<CategoryCreate />} />
                  <Route path="edit/:id" element={<CategoryEdit />} />
                </Route>
                <Route path="/reports" element={<ReportList />} />
                <Route path="/blog" element={<BlogList />} />
                <Route path="/moderation" element={<ModerationList />} />
                <Route path="/audit-logs" element={<AuditLogList />} />
                <Route path="*" element={<ErrorComponent />} />
              </Route>
            </Routes>

            <RefineKbar />
            <UnsavedChangesNotifier />
            <DocumentTitleHandler />
          </Refine>
          <DevtoolsPanel />
        </DevtoolsProvider>
      </RefineKbarProvider>
      <Toaster richColors position="top-right" />
    </BrowserRouter>
  );
}

export default App;
