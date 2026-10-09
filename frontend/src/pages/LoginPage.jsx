import { useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { login } from '../store/authSlice';
import { Field } from '../components/ui.jsx';

export default function LoginPage() {
  const dispatch = useDispatch();
  const { loginStatus, loginError } = useSelector((s) => s.auth);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  const submit = (e) => {
    e.preventDefault();
    dispatch(login({ email, password }));
  };

  return (
    <div className="login">
      <div className="login-art">
        <h1>Every seat counted.</h1>
        <p>Register people for workshops without ever promising the same last seat twice.</p>
      </div>
      <form className="login-card" onSubmit={submit}>
        <h2>Sign in</h2>
        <Field label="Email">
          <input type="email" autoComplete="username" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus />
        </Field>
        <Field label="Password">
          <input type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </Field>
        {loginError && <p className="form-error" role="alert">{loginError}</p>}
        <button className="btn btn-primary" disabled={loginStatus === 'loading'}>
          {loginStatus === 'loading' ? 'Signing in…' : 'Sign in'}
        </button>
        <p className="hint">Accounts are created by an administrator.</p>
      </form>
    </div>
  );
}
