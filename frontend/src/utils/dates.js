const pad = (n) => String(n).padStart(2, '0');

export const toDateInput = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;

export function presetRange(name) {
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  if (name === 'today') return { from: toDateInput(today), to: toDateInput(today) };
  if (name === 'week') {
    const monday = new Date(today);
    monday.setDate(today.getDate() - ((today.getDay() + 6) % 7));
    const sunday = new Date(monday);
    sunday.setDate(monday.getDate() + 6);
    return { from: toDateInput(monday), to: toDateInput(sunday) };
  }
  if (name === 'next7') {
    const end = new Date(today);
    end.setDate(today.getDate() + 7);
    return { from: toDateInput(today), to: toDateInput(end) };
  }
  return { from: '', to: '' };
}

const dateTimeFmt = new Intl.DateTimeFormat(undefined, { weekday: 'short', day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit' });
const dateFmt = new Intl.DateTimeFormat(undefined, { day: 'numeric', month: 'short', year: 'numeric' });
const timeFmt = new Intl.DateTimeFormat(undefined, { hour: 'numeric', minute: '2-digit' });

export const formatDateTime = (iso) => (iso ? dateTimeFmt.format(new Date(iso)) : '');
export const formatDate = (iso) => (iso ? dateFmt.format(new Date(iso)) : '');
export const formatTime = (iso) => (iso ? timeFmt.format(new Date(iso)) : '');
export const formatStamp = (iso) => (iso ? `${dateFmt.format(new Date(iso))}, ${timeFmt.format(new Date(iso))}` : '');

export function isoToLocalInput(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  return `${toDateInput(d)}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}
export const localInputToIso = (v) => (v ? new Date(v).toISOString() : null);
