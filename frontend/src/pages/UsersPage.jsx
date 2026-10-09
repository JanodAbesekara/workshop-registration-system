import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { createUser, fetchUsers, resetPassword, updateUser } from '../store/usersSlice';
import { pushToast } from '../store/toastSlice';
import Modal from '../components/Modal.jsx';
import { Field } from '../components/ui.jsx';
import { formatDate } from '../utils/dates';

const ROLES = [['ADMIN', 'Administrator'], ['MANAGER', 'Programme manager'], ['STAFF', 'Front desk']];
const roleLabel = (r) => ROLES.find(([k]) => k === r)?.[1] || r;

function UserModal({ user, selfId, onClose }) {
  const dispatch = useDispatch();
  const editing = Boolean(user);
  const isSelf = editing && user.id === selfId;
  const [f, setF] = useState({ name: user?.name || '', email: user?.email || '', password: '', role: user?.role || 'STAFF', active: user?.active ?? true });
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setErrors({});
    setFormError('');
    try {
      if (editing) await dispatch(updateUser({ id: user.id, body: { name: f.name, role: f.role, active: f.active } })).unwrap();
      else await dispatch(createUser({ name: f.name, email: f.email, password: f.password, role: f.role })).unwrap();
      dispatch(pushToast('success', editing ? 'Account updated.' : 'Account created.'));
      onClose();
    } catch (err) {
      setErrors(err.fieldErrors || {});
      setFormError(err.message);
    }
  };

  return (
    <Modal title={editing ? 'Edit account' : 'New account'} onClose={onClose}>
      <form onSubmit={submit}>
        <Field label="Full name" error={errors.name}><input value={f.name} onChange={set('name')} required /></Field>
        {!editing && <Field label="Email" error={errors.email}><input type="email" value={f.email} onChange={set('email')} required /></Field>}
        {!editing && <Field label="Temporary password (8+ characters)" error={errors.password}><input type="text" autoComplete="off" value={f.password} onChange={set('password')} minLength={8} required /></Field>}
        <Field label="Role" error={errors.role}>
          <select value={f.role} onChange={set('role')} disabled={isSelf}>
            {ROLES.map(([k, label]) => <option key={k} value={k}>{label}</option>)}
          </select>
        </Field>
        {editing && (
          <label className="check">
            <input type="checkbox" checked={f.active} disabled={isSelf} onChange={(e) => setF({ ...f, active: e.target.checked })} />
            Account can sign in
          </label>
        )}
        {isSelf && <p className="hint">You cannot change your own role or deactivate yourself.</p>}
        {formError && <p className="form-error" role="alert">{formError}</p>}
        <div className="actions">
          <button type="button" className="btn" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary">{editing ? 'Save changes' : 'Create account'}</button>
        </div>
      </form>
    </Modal>
  );
}

function PasswordModal({ user, onClose }) {
  const dispatch = useDispatch();
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const submit = async (e) => {
    e.preventDefault();
    try {
      await dispatch(resetPassword({ id: user.id, password })).unwrap();
      dispatch(pushToast('success', `Password reset for ${user.name}.`));
      onClose();
    } catch (err) {
      setError(err.fieldErrors?.password || err.message);
    }
  };
  return (
    <Modal title={`Reset password for ${user.name}`} onClose={onClose}>
      <form onSubmit={submit}>
        <Field label="New password (8+ characters)" error={error}><input type="text" autoComplete="off" value={password} onChange={(e) => setPassword(e.target.value)} minLength={8} required /></Field>
        <div className="actions">
          <button type="button" className="btn" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary">Reset password</button>
        </div>
      </form>
    </Modal>
  );
}

export default function UsersPage() {
  const dispatch = useDispatch();
  const me = useSelector((s) => s.auth.user);
  const { items, status, error } = useSelector((s) => s.users);
  const [modal, setModal] = useState(null); // {type:'create'|'edit'|'password', user}

  useEffect(() => { dispatch(fetchUsers()); }, [dispatch]);

  return (
    <>
      <div className="page-head">
        <div><h1>Accounts</h1><p className="sub">There is no public signup. Every account is created here.</p></div>
        <button className="btn btn-primary" onClick={() => setModal({ type: 'create' })}>New account</button>
      </div>
      {error && <p className="form-error" role="alert">{error}</p>}
      <div className={`table-wrap ${status === 'loading' ? 'is-loading' : ''}`}>
        <table>
          <thead><tr><th>Name</th><th>Role</th><th>Status</th><th>Created</th><th /></tr></thead>
          <tbody>
            {items.map((u) => (
              <tr key={u.id}>
                <td><strong>{u.name}{u.id === me.id && ' (you)'}</strong><div className="muted">{u.email}</div></td>
                <td>{roleLabel(u.role)}</td>
                <td><span className={`badge ${u.active ? 'badge-active' : 'badge-cancelled'}`}>{u.active ? 'Active' : 'Disabled'}</span></td>
                <td>{formatDate(u.createdAt)}</td>
                <td className="right">
                  <button className="btn" onClick={() => setModal({ type: 'edit', user: u })}>Edit</button>{' '}
                  <button className="btn" onClick={() => setModal({ type: 'password', user: u })}>Reset password</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {modal?.type === 'create' && <UserModal selfId={me.id} onClose={() => setModal(null)} />}
      {modal?.type === 'edit' && <UserModal user={modal.user} selfId={me.id} onClose={() => setModal(null)} />}
      {modal?.type === 'password' && <PasswordModal user={modal.user} onClose={() => setModal(null)} />}
    </>
  );
}
