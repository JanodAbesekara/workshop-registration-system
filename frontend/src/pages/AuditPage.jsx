import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { fetchAudit } from '../store/auditSlice';
import { Pager } from '../components/ui.jsx';
import { formatStamp } from '../utils/dates';

const LABELS = {
  USER_CREATED: 'Account created', USER_UPDATED: 'Account changed', USER_PASSWORD_RESET: 'Password reset',
  WORKSHOP_CREATED: 'Workshop created', WORKSHOP_UPDATED: 'Workshop changed',
};

export default function AuditPage() {
  const dispatch = useDispatch();
  const { data, status, error } = useSelector((s) => s.audit);
  const [entityType, setEntityType] = useState('');
  const [page, setPage] = useState(0);

  useEffect(() => { dispatch(fetchAudit({ entityType, page })); }, [dispatch, entityType, page]);

  return (
    <>
      <div className="page-head">
        <div><h1>Audit trail</h1><p className="sub">Who changed what, and when. Newest first.</p></div>
        <label className="field"><span>Show</span>
          <select value={entityType} onChange={(e) => { setPage(0); setEntityType(e.target.value); }}>
            <option value="">Everything</option>
            <option value="USER">Accounts and roles</option>
            <option value="WORKSHOP">Workshops</option>
          </select>
        </label>
      </div>
      {error && <p className="form-error" role="alert">{error}</p>}
      <div className={`table-wrap ${status === 'loading' ? 'is-loading' : ''}`}>
        <table>
          <thead><tr><th>When</th><th>Who</th><th>What</th><th>Details</th></tr></thead>
          <tbody>
            {data.content.map((a) => (
              <tr key={a.id}>
                <td>{formatStamp(a.createdAt)}</td>
                <td>{a.actorName}</td>
                <td>{LABELS[a.action] || a.action}</td>
                <td className="muted wrap">{a.details}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {status === 'ready' && data.content.length === 0 && <div className="empty"><strong>Nothing recorded yet.</strong><span>Changes to accounts and workshops will appear here.</span></div>}
      </div>
      <Pager page={data.page} totalPages={data.totalPages} onChange={setPage} />
    </>
  );
}
