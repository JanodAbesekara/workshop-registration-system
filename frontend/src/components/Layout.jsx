import { NavLink, Outlet } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { logout } from '../store/authSlice';

const ROLE_LABEL = { ADMIN: 'Administrator', MANAGER: 'Programme manager', STAFF: 'Front desk' };

export default function Layout() {
  const dispatch = useDispatch();
  const user = useSelector((s) => s.auth.user);

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">Workshop Desk</div>
        <nav>
          {user.role !== 'ADMIN' && <NavLink to="/workshops">Workshops</NavLink>}
          {user.role === 'ADMIN' && <NavLink to="/users">Accounts</NavLink>}
          {user.role === 'ADMIN' && <NavLink to="/audit">Audit trail</NavLink>}
        </nav>
        <div className="who">
          <strong>{user.name}</strong>
          <span>{ROLE_LABEL[user.role]}</span>
          <button className="btn btn-ghost-light" onClick={() => dispatch(logout())}>Sign out</button>
        </div>
      </aside>
      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
