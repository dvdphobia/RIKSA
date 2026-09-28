import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Navigate, Outlet, Route, Routes } from "react-router";
import "./App.css";
import { Layout } from "./components/layout";
import { Toaster } from "./components/ui/sonner";
import { AuthProvider, RequireAuth } from "./lib/auth";
import { AuditLogList } from "./pages/audit-logs/list";
import { BlogList } from "./pages/blog/list";
import { Dashboard } from "./pages/dashboard";
import { Gamification } from "./pages/gamification";
import { Leaderboard } from "./pages/leaderboard";
import { Platform } from "./pages/platform";
import { Roadmap } from "./pages/roadmap";
import { CategoryCreate, CategoryEdit } from "./pages/categories/form";
import { CategoryList } from "./pages/categories/list";
import { LessonCreate, LessonEdit } from "./pages/lessons/form";
import { LessonList } from "./pages/lessons/list";
import { Login } from "./pages/login";
import { ModerationList } from "./pages/moderation/list";
import { ReportList } from "./pages/reports/list";
import { UserDetail } from "./pages/users/detail";
import { UserList } from "./pages/users/list";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { refetchOnWindowFocus: false, retry: 1 },
  },
});

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route
              element={
                <RequireAuth>
                  <Layout>
                    <Outlet />
                  </Layout>
                </RequireAuth>
              }
            >
              <Route index element={<Dashboard />} />
              <Route
                path="/users"
                element={
                  <RequireAuth adminOnly>
                    <Outlet />
                  </RequireAuth>
                }
              >
                <Route index element={<UserList />} />
                <Route path=":id" element={<UserDetail />} />
              </Route>
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
              <Route path="/roadmap" element={<Roadmap />} />
              <Route
                path="/gamification"
                element={
                  <RequireAuth adminOnly>
                    <Gamification />
                  </RequireAuth>
                }
              />
              <Route
                path="/leaderboard"
                element={
                  <RequireAuth adminOnly>
                    <Leaderboard />
                  </RequireAuth>
                }
              />
              <Route
                path="/platform"
                element={
                  <RequireAuth adminOnly>
                    <Platform />
                  </RequireAuth>
                }
              />
              <Route path="/reports" element={<ReportList />} />
              <Route path="/blog" element={<BlogList />} />
              <Route path="/moderation" element={<ModerationList />} />
              <Route
                path="/audit-logs"
                element={
                  <RequireAuth adminOnly>
                    <AuditLogList />
                  </RequireAuth>
                }
              />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Route>
          </Routes>
        </AuthProvider>
      </BrowserRouter>
      <Toaster richColors position="top-right" />
    </QueryClientProvider>
  );
}

export default App;
