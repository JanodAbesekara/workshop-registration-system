import { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { dismissToast } from '../store/toastSlice';

function Toast({ toast }) {
  const dispatch = useDispatch();
  useEffect(() => {
    const t = setTimeout(() => dispatch(dismissToast(toast.id)), 5000);
    return () => clearTimeout(t);
  }, [dispatch, toast.id]);
  return (
    <div className={`toast toast-${toast.type}`} role="status">
      <span>{toast.text}</span>
      <button aria-label="Dismiss" onClick={() => dispatch(dismissToast(toast.id))}>×</button>
    </div>
  );
}

export default function Toasts() {
  const toasts = useSelector((s) => s.toasts);
  return <div className="toasts" aria-live="polite">{toasts.map((t) => <Toast key={t.id} toast={t} />)}</div>;
}
