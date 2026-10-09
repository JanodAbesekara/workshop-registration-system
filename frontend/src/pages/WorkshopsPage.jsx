import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { fetchWorkshops, resetFilters, setFilters } from '../store/workshopsSlice';
import { Pager, SeatMeter, StatusBadge } from '../components/ui.jsx';
import { formatDateTime } from '../utils/dates';
import { presetRange } from '../utils/dates';
import WorkshopFormModal from './WorkshopFormModal.jsx';

export default function WorkshopsPage() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const user = useSelector((s) => s.auth.user);
  const { list, filters, listStatus, listError } = useSelector((s) => s.workshops);
  const [showForm, setShowForm] = useState(false);
  const [q, setQ] = useState(filters.q);

  useEffect(() => { dispatch(fetchWorkshops()); }, [dispatch, filters]);

  useEffect(() => {
    const t = setTimeout(() => dispatch(setFilters({ q })), 300);
    return () => clearTimeout(t);
  }, [q, dispatch]);

  const activePreset = ['today', 'week', 'next7'].find((p) => {
    const r = presetRange(p);
    return r.from === filters.from && r.to === filters.to;
  }) || (!filters.from && !filters.to ? 'any' : '');

  const preset = (name) => dispatch(setFilters(presetRange(name)));
  const hasAnyFilter = filters.from || filters.to || filters.status || filters.hasSeats || filters.q;

  return (
    <>
      <div className="page-head">
        <div>
          <h1>Workshops</h1>
          <p className="sub">{list.totalElements} {list.totalElements === 1 ? 'workshop' : 'workshops'} match</p>
        </div>
        {user.role === 'MANAGER' && <button className="btn btn-primary" onClick={() => setShowForm(true)}>New workshop</button>}
      </div>

      <section className="filters" aria-label="Find workshops">
        <div className="chips">
          {[['today', 'Today'], ['week', 'This week'], ['next7', 'Next 7 days'], ['any', 'Any date']].map(([k, label]) => (
            <button key={k} className={`chip ${activePreset === k ? 'chip-on' : ''}`} onClick={() => preset(k)}>{label}</button>
          ))}
        </div>
        <div className="filter-row">
          <label className="field"><span>From</span><input type="date" value={filters.from} onChange={(e) => dispatch(setFilters({ from: e.target.value }))} /></label>
          <label className="field"><span>To</span><input type="date" value={filters.to} min={filters.from || undefined} onChange={(e) => dispatch(setFilters({ to: e.target.value }))} /></label>
          <label className="field"><span>Status</span>
            <select value={filters.status} onChange={(e) => dispatch(setFilters({ status: e.target.value }))}>
              <option value="">Any status</option>
              <option value="SCHEDULED">Scheduled</option>
              <option value="COMPLETED">Completed</option>
              <option value="CANCELLED">Cancelled</option>
            </select>
          </label>
          <label className="field grow"><span>Search</span><input placeholder="Title, code or instructor" value={q} onChange={(e) => setQ(e.target.value)} /></label>
          <label className="check">
            <input type="checkbox" checked={filters.hasSeats} onChange={(e) => dispatch(setFilters({ hasSeats: e.target.checked }))} />
            Only with free seats
          </label>
          {hasAnyFilter && <button className="btn" onClick={() => { setQ(''); dispatch(resetFilters()); }}>Clear</button>}
        </div>
      </section>

      {listError && <p className="form-error" role="alert">{listError}</p>}

      <div className={`table-wrap ${listStatus === 'loading' ? 'is-loading' : ''}`}>
        <table>
          <thead><tr><th>Workshop</th><th>When</th><th>Seats</th><th>Status</th></tr></thead>
          <tbody>
            {list.content.map((w) => (
              <tr key={w.id} className="row-link" tabIndex={0}
                  onClick={() => navigate(`/workshops/${w.id}`)}
                  onKeyDown={(e) => e.key === 'Enter' && navigate(`/workshops/${w.id}`)}>
                <td><strong>{w.title}</strong><div className="muted">{w.code} · {w.instructor}</div></td>
                <td>{formatDateTime(w.startsAt)}{w.location && <div className="muted">{w.location}</div>}</td>
                <td><SeatMeter workshop={w} /></td>
                <td><StatusBadge status={w.status} /></td>
              </tr>
            ))}
          </tbody>
        </table>
        {listStatus === 'ready' && list.content.length === 0 && (
          <div className="empty">
            <strong>No workshops match these filters.</strong>
            <span>Try “Any date” or switch off “Only with free seats”.</span>
          </div>
        )}
      </div>

      <Pager page={list.page} totalPages={list.totalPages} onChange={(p) => dispatch(setFilters({ page: p }))} />
      {showForm && <WorkshopFormModal onClose={() => setShowForm(false)} onSaved={(w) => navigate(`/workshops/${w.id}`)} />}
    </>
  );
}
