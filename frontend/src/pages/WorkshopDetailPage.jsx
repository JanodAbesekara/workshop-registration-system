import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import {
  cancelRegistration, clearCurrent, fetchRegistrations, fetchWorkshop, registerAttendee,
} from '../store/workshopsSlice';
import { pushToast } from '../store/toastSlice';
import Modal from '../components/Modal.jsx';
import { Field, SeatMeter, StatusBadge, seatTone } from '../components/ui.jsx';
import { formatDateTime, formatStamp, formatTime } from '../utils/dates';
import WorkshopFormModal from './WorkshopFormModal.jsx';

function SeatBoard({ workshop }) {
  if (workshop.capacity > 80) return <SeatMeter workshop={workshop} />;
  const tone = seatTone(workshop);
  return (
    <div className="seat-board" role="img" aria-label={`${workshop.activeCount} of ${workshop.capacity} seats taken`}>
      {Array.from({ length: workshop.capacity }, (_, i) => (
        <span key={i} className={`seat ${i < workshop.activeCount ? `seat-taken tone-${tone}` : ''}`} />
      ))}
    </div>
  );
}

export default function WorkshopDetailPage() {
  const { id } = useParams();
  const dispatch = useDispatch();
  const user = useSelector((s) => s.auth.user);
  const { current: w, currentStatus, currentError, registrations, regStatus } = useSelector((s) => s.workshops);
  const [regFilter, setRegFilter] = useState('');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [busy, setBusy] = useState(false);
  const [editing, setEditing] = useState(false);
  const [cancelTarget, setCancelTarget] = useState(null);
  const [reason, setReason] = useState('');

  const reload = useCallback(() => {
    dispatch(fetchWorkshop(id));
    dispatch(fetchRegistrations({ id, status: regFilter }));
  }, [dispatch, id, regFilter]);

  useEffect(() => { reload(); }, [reload]);
  useEffect(() => () => { dispatch(clearCurrent()); }, [dispatch]);

  if (currentStatus === 'error') {
    return <div className="empty"><strong>{currentError}</strong><Link to="/workshops">Back to workshops</Link></div>;
  }
  if (!w || String(w.id) !== String(id)) return <p className="muted">Loading workshop…</p>;

  const ended = new Date(w.endsAt) <= new Date();
  const isFull = w.seatsAvailable <= 0;
  const waitlistCount = registrations.filter((r) => r.status === 'WAITLISTED').length;

  let blocked = '';
  if (w.status !== 'SCHEDULED') blocked = `Registration is closed: this workshop is ${w.status.toLowerCase()}.`;
  else if (ended) blocked = 'Registration is closed: this workshop has already ended.';

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setErrors({});
    setFormError('');
    try {
      const r = await dispatch(registerAttendee({ id, body: { attendeeName: name, attendeeEmail: email, waitlist: isFull } })).unwrap();
      dispatch(pushToast('success', r.status === 'WAITLISTED' ? `${r.attendeeName} added to waitlist.` : `${r.attendeeName} is registered.`));
      setName('');
      setEmail('');
    } catch (err) {
      setErrors(err.fieldErrors || {});
      setFormError(err.message);
    } finally {
      setBusy(false);
      reload();
    }
  };

  const confirmCancel = async () => {
    try {
      await dispatch(cancelRegistration({ id, regId: cancelTarget.id, reason })).unwrap();
      dispatch(pushToast('success', cancelTarget.status === 'WAITLISTED' ? 'Waitlist entry cancelled.' : 'Registration cancelled and seat freed (next waitlisted attendee auto-promoted if available).'));
    } catch (err) {
      dispatch(pushToast('error', err.message));
    } finally {
      setCancelTarget(null);
      setReason('');
      reload();
    }
  };

  return (
    <>
      <Link to="/workshops" className="back">← All workshops</Link>
      <div className="page-head">
        <div>
          <h1>{w.title}</h1>
          <p className="sub">{w.code} · {w.instructor}{w.location ? ` · ${w.location}` : ''}</p>
          <p className="sub">{formatDateTime(w.startsAt)} – {formatTime(w.endsAt)}</p>
        </div>
        <div className="head-actions">
          <StatusBadge status={w.status} />
          {user.role === 'MANAGER' && <button className="btn" onClick={() => setEditing(true)}>Edit workshop</button>}
        </div>
      </div>
      {w.description && <p className="notes">{w.description}</p>}

      <section className="panel board-panel">
        <div>
          <div className={`big-count tone-text-${seatTone(w)}`}>{w.seatsAvailable}<small> {w.seatsAvailable === 1 ? 'seat' : 'seats'} left</small></div>
          <p className="muted">
            {w.activeCount} of {w.capacity} taken
            {waitlistCount > 0 && <span style={{ marginLeft: '.5rem', color: '#b45309', fontWeight: 600 }}>· {waitlistCount} on waitlist</span>}
          </p>
        </div>
        <SeatBoard workshop={w} />
      </section>

      <section className="panel">
        <h2>{isFull ? 'Join the waitlist' : 'Register an attendee'}</h2>
        {isFull && !blocked && (
          <p className="muted" style={{ marginBottom: '.8rem' }}>
            All {w.capacity} seats are booked. You can queue this attendee on the waitlist. If a seat is cancelled, the next attendee in line is automatically promoted to the freed seat.
          </p>
        )}
        {blocked ? <p className="blocked">{blocked}</p> : (
          <form className="inline-form" onSubmit={submit}>
            <Field label="Attendee name" error={errors.attendeeName}><input value={name} onChange={(e) => setName(e.target.value)} required /></Field>
            <Field label="Attendee email" error={errors.attendeeEmail}><input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required /></Field>
            <button className={`btn ${isFull ? '' : 'btn-primary'}`} style={isFull ? { background: '#f59e0b', color: '#fff', borderColor: '#d97706' } : {}} disabled={busy}>
              {busy ? (isFull ? 'Queueing…' : 'Registering…') : (isFull ? 'Join Waitlist' : 'Register')}
            </button>
          </form>
        )}
        {formError && <p className="form-error" role="alert">{formError}</p>}
      </section>

      <section className="panel">
        <div className="panel-head">
          <h2>Registration history</h2>
          <div className="chips">
            {[['', 'All'], ['ACTIVE', 'Active'], ['WAITLISTED', 'Waitlisted'], ['CANCELLED', 'Cancelled']].map(([k, label]) => (
              <button key={k} className={`chip ${regFilter === k ? 'chip-on' : ''}`} onClick={() => setRegFilter(k)}>{label}</button>
            ))}
          </div>
        </div>
        <div className={`table-wrap flat ${regStatus === 'loading' ? 'is-loading' : ''}`}>
          <table>
            <thead><tr><th>Attendee</th><th>Status</th><th>Registered</th><th>Cancelled</th><th /></tr></thead>
            <tbody>
              {registrations.map((r) => (
                <tr key={r.id} className={r.status === 'CANCELLED' ? 'struck' : ''}>
                  <td><strong>{r.attendeeName}</strong><div className="muted">{r.attendeeEmail}</div></td>
                  <td><StatusBadge status={r.status} /></td>
                  <td>{formatStamp(r.registeredAt)}<div className="muted">by {r.registeredByName}</div></td>
                  <td>{r.cancelledAt ? <>{formatStamp(r.cancelledAt)}<div className="muted">by {r.cancelledByName}{r.cancelReason ? ` · ${r.cancelReason}` : ''}</div></> : <span className="muted">—</span>}</td>
                  <td className="right">{(r.status === 'ACTIVE' || r.status === 'WAITLISTED') && <button className="btn btn-danger-ghost" onClick={() => setCancelTarget(r)}>Cancel</button>}</td>
                </tr>
              ))}
            </tbody>
          </table>
          {regStatus === 'ready' && registrations.length === 0 && (
            <div className="empty"><strong>No registrations yet.</strong><span>Use the form above to register the first attendee.</span></div>
          )}
        </div>
      </section>

      {editing && <WorkshopFormModal workshop={w} onClose={() => setEditing(false)} onSaved={reload} />}
      {cancelTarget && (
        <Modal title={cancelTarget.status === 'WAITLISTED' ? 'Remove from waitlist?' : 'Cancel this registration?'} onClose={() => setCancelTarget(null)}>
          <p>
            {cancelTarget.status === 'WAITLISTED'
              ? `${cancelTarget.attendeeName} will be removed from the waitlist queue.`
              : `${cancelTarget.attendeeName} will lose their seat. If an attendee is on the waitlist, they will automatically be offered this seat.`}
          </p>
          <Field label="Reason (optional)"><input value={reason} onChange={(e) => setReason(e.target.value)} maxLength={300} /></Field>
          <div className="actions">
            <button className="btn" onClick={() => setCancelTarget(null)}>Keep</button>
            <button className="btn btn-danger" onClick={confirmCancel}>Confirm Cancel</button>
          </div>
        </Modal>
      )}
    </>
  );
}
