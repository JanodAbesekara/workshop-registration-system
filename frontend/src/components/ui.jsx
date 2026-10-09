const STATUS_LABEL = { SCHEDULED: 'Scheduled', COMPLETED: 'Completed', CANCELLED: 'Cancelled', ACTIVE: 'Active', WAITLISTED: 'Waitlisted' };

export function StatusBadge({ status }) {
  return <span className={`badge badge-${status.toLowerCase()}`}>{STATUS_LABEL[status] || status}</span>;
}

export function Field({ label, error, children }) {
  return (
    <label className="field">
      <span>{label}</span>
      {children}
      {error && <em className="field-error">{error}</em>}
    </label>
  );
}

export function seatTone(w) {
  const left = w.capacity - w.activeCount;
  if (left <= 0) return 'full';
  if (left / w.capacity <= 0.25) return 'low';
  return 'open';
}

export function SeatMeter({ workshop }) {
  const left = Math.max(0, workshop.capacity - workshop.activeCount);
  const pct = Math.min(100, Math.round((workshop.activeCount / workshop.capacity) * 100));
  const tone = seatTone(workshop);
  return (
    <div className="meter" title={`${workshop.activeCount} of ${workshop.capacity} seats taken`}>
      <div className="meter-bar"><div className={`meter-fill tone-${tone}`} style={{ width: `${pct}%` }} /></div>
      <span className={`meter-text tone-text-${tone}`}>{left === 0 ? 'Full' : `${left} left`}<small> of {workshop.capacity}</small></span>
    </div>
  );
}

export function Pager({ page, totalPages, onChange }) {
  if (totalPages <= 1) return null;
  return (
    <div className="pager">
      <button className="btn" disabled={page <= 0} onClick={() => onChange(page - 1)}>Previous</button>
      <span>Page {page + 1} of {totalPages}</span>
      <button className="btn" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>Next</button>
    </div>
  );
}
