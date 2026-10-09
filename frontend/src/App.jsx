import { useEffect } from 'react';
import { Navigate, Outlet, Route, Routes } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { fetchMe } from './store/authSlice';
import Layout from './components/Layout.jsx';
import Toasts from './components/Toasts.jsx';
import LoginPage from './pages/LoginPage.jsx';
import WorkshopsPage from './pages/WorkshopsPage.jsx';
import WorkshopDetailPage from './pages/WorkshopDetailPage.jsx';
import UsersPage from './pages/UsersPage.jsx';
import AuditPage from './pages/AuditPage.jsx';

export const homeFor = (role) => (role === 'ADMIN' ? '/users' : '/workshops');

function RequireAuth() {
  const user = useSelector((s) => s.auth.user);
  return user ? <Outlet /> : <Navigate to="/login" replace />;
}

// UI guard only. The real enforcement is on the server; this just avoids showing pages that would 403.
function RequireRole({ roles }) {
  const user = useSelector((s) => s.auth.user);
  return roles.includes(user.role) ? <Outlet /> : <Navigate to="/" replace />;
}

export default function App() {
  const dispatch = useDispatch();
  const { token, user } = useSelector((s) => s.auth);

  useEffect(() => {
    if (token && !user) dispatch(fetchMe());
  }, [dispatch, token, user]);

  if (token && !user) return <div className="splash">Loading…</div>;

  return (
    <>
      <Routes>
        <Route path="/login" element={user ? <Navigate to={homeFor(user.role)} replace /> : <LoginPage />} />
        <Route element={<RequireAuth />}>
          <Route element={<Layout />}>
            <Route index element={<Navigate to={homeFor(user?.role)} replace />} />
            <Route element={<RequireRole roles={['MANAGER', 'STAFF']} />}>
              <Route path="workshops" element={<WorkshopsPage />} />
              <Route path="workshops/:id" element={<WorkshopDetailPage />} />
            </Route>
            <Route element={<RequireRole roles={['ADMIN']} />}>
              <Route path="users" element={<UsersPage />} />
              <Route path="audit" element={<AuditPage />} />
            </Route>
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
      <Toasts />
    </>
  );
}
