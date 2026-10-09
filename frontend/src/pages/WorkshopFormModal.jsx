import { useState } from 'react';
import { useDispatch } from 'react-redux';
import Modal from '../components/Modal.jsx';
import { Field } from '../components/ui.jsx';
import { saveWorkshop } from '../store/workshopsSlice';
import { pushToast } from '../store/toastSlice';
import { isoToLocalInput, localInputToIso } from '../utils/dates';

export default function WorkshopFormModal({ workshop, onClose, onSaved }) {
  const dispatch = useDispatch();
  const editing = Boolean(workshop);
  const [f, setF] = useState({
    code: workshop?.code || '',
    title: workshop?.title || '',
    instructor: workshop?.instructor || '',
    location: workshop?.location || '',
    description: workshop?.description || '',
    startsAt: isoToLocalInput(workshop?.startsAt),
    endsAt: isoToLocalInput(workshop?.endsAt),
    capacity: workshop?.capacity ?? 12,
    status: workshop?.status || 'SCHEDULED',
  });
  const [errors, setErrors] = useState({});
  const [formError, setFormError] = useState('');
  const [busy, setBusy] = useState(false);
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setErrors({});
    setFormError('');
    const body = {
      ...f,
      capacity: Number(f.capacity),
      startsAt: localInputToIso(f.startsAt),
      endsAt: localInputToIso(f.endsAt),
    };
    try {
      const saved = await dispatch(saveWorkshop({ id: workshop?.id, body })).unwrap();
      dispatch(pushToast('success', editing ? 'Workshop updated.' : 'Workshop created.'));
      onSaved?.(saved);
      onClose();
    } catch (err) {
      setErrors(err.fieldErrors || {});
      setFormError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal title={editing ? `Edit ${workshop.code}` : 'New workshop'} onClose={onClose} wide>
      <form onSubmit={submit} className="form-grid">
        <Field label="Workshop code" error={errors.code}><input value={f.code} onChange={set('code')} placeholder="POT-101" required /></Field>
        <Field label="Status" error={errors.status}>
          <select value={f.status} onChange={set('status')}>
            <option value="SCHEDULED">Scheduled</option>
            <option value="COMPLETED">Completed</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
        </Field>
        <Field label="Title" error={errors.title}><input value={f.title} onChange={set('title')} required /></Field>
        <Field label="Instructor" error={errors.instructor}><input value={f.instructor} onChange={set('instructor')} required /></Field>
        <Field label="Starts" error={errors.startsAt}><input type="datetime-local" value={f.startsAt} onChange={set('startsAt')} required /></Field>
        <Field label="Ends" error={errors.endsAt}><input type="datetime-local" value={f.endsAt} onChange={set('endsAt')} required /></Field>
        <Field label="Seats (capacity)" error={errors.capacity}><input type="number" min="1" max="1000" value={f.capacity} onChange={set('capacity')} required /></Field>
        <Field label="Location" error={errors.location}><input value={f.location} onChange={set('location')} placeholder="Studio A" /></Field>
        <div className="span-2">
          <Field label="Notes for staff" error={errors.description}><textarea rows="3" value={f.description} onChange={set('description')} /></Field>
        </div>
        {formError && <p className="form-error span-2" role="alert">{formError}</p>}
        <div className="actions span-2">
          <button type="button" className="btn" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={busy}>{busy ? 'Saving…' : editing ? 'Save changes' : 'Create workshop'}</button>
        </div>
      </form>
    </Modal>
  );
}
