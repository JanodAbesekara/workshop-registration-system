import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import api, { toRejection } from '../api/client';

const PAGE_SIZE = 12;

const startOfDayIso = (d) => new Date(`${d}T00:00:00`).toISOString();
const endOfDayIso = (d) => new Date(`${d}T23:59:59.999`).toISOString();

export const fetchWorkshops = createAsyncThunk('workshops/list', async (_, { getState, rejectWithValue }) => {
  const f = getState().workshops.filters;
  const params = { page: f.page, size: PAGE_SIZE, sort: f.sort };
  if (f.from) params.from = startOfDayIso(f.from);
  if (f.to) params.to = endOfDayIso(f.to);
  if (f.status) params.status = f.status;
  if (f.hasSeats) params.hasSeats = true;
  if (f.q) params.q = f.q;
  try {
    const { data } = await api.get('/api/workshops', { params });
    return data;
  } catch (e) {
    return rejectWithValue(toRejection(e));
  }
});

export const fetchWorkshop = createAsyncThunk('workshops/one', async (id, { rejectWithValue }) => {
  try {
    const { data } = await api.get(`/api/workshops/${id}`);
    return data;
  } catch (e) {
    return rejectWithValue(toRejection(e));
  }
});

export const saveWorkshop = createAsyncThunk('workshops/save', async ({ id, body }, { rejectWithValue }) => {
  try {
    const { data } = id ? await api.put(`/api/workshops/${id}`, body) : await api.post('/api/workshops', body);
    return data;
  } catch (e) {
    return rejectWithValue(toRejection(e));
  }
});

export const fetchRegistrations = createAsyncThunk('workshops/regs', async ({ id, status }, { rejectWithValue }) => {
  try {
    const { data } = await api.get(`/api/workshops/${id}/registrations`, { params: status ? { status } : {} });
    return data;
  } catch (e) {
    return rejectWithValue(toRejection(e));
  }
});

export const registerAttendee = createAsyncThunk('workshops/register', async ({ id, body }, { rejectWithValue }) => {
  try {
    const { data } = await api.post(`/api/workshops/${id}/registrations`, body);
    return data;
  } catch (e) {
    return rejectWithValue(toRejection(e));
  }
});

export const cancelRegistration = createAsyncThunk('workshops/cancel', async ({ id, regId, reason }, { rejectWithValue }) => {
  try {
    const { data } = await api.post(`/api/workshops/${id}/registrations/${regId}/cancel`, { reason: reason || null });
    return data;
  } catch (e) {
    return rejectWithValue(toRejection(e));
  }
});

export const defaultFilters = { from: '', to: '', status: '', hasSeats: false, q: '', page: 0, sort: 'asc' };

const slice = createSlice({
  name: 'workshops',
  initialState: {
    filters: defaultFilters,
    list: { content: [], page: 0, totalElements: 0, totalPages: 0 },
    listStatus: 'idle',
    listError: null,
    current: null,
    currentStatus: 'idle',
    currentError: null,
    registrations: [],
    regStatus: 'idle',
  },
  reducers: {
    setFilters(state, action) {
      const next = { ...state.filters, ...('page' in action.payload ? {} : { page: 0 }), ...action.payload };
      if (JSON.stringify(next) !== JSON.stringify(state.filters)) state.filters = next;
    },
    resetFilters(state) { state.filters = defaultFilters; },
    clearCurrent(state) { state.current = null; state.registrations = []; state.currentStatus = 'idle'; },
  },
  extraReducers: (b) => {
    b.addCase(fetchWorkshops.pending, (s) => { s.listStatus = 'loading'; s.listError = null; })
      .addCase(fetchWorkshops.fulfilled, (s, a) => { s.listStatus = 'ready'; s.list = a.payload; })
      .addCase(fetchWorkshops.rejected, (s, a) => { s.listStatus = 'error'; s.listError = a.payload?.message || 'Could not load workshops.'; })
      .addCase(fetchWorkshop.pending, (s) => { s.currentStatus = 'loading'; s.currentError = null; })
      .addCase(fetchWorkshop.fulfilled, (s, a) => { s.currentStatus = 'ready'; s.current = a.payload; })
      .addCase(fetchWorkshop.rejected, (s, a) => { s.currentStatus = 'error'; s.currentError = a.payload?.message || 'Could not load workshop.'; })
      .addCase(fetchRegistrations.pending, (s) => { s.regStatus = 'loading'; })
      .addCase(fetchRegistrations.fulfilled, (s, a) => { s.regStatus = 'ready'; s.registrations = a.payload; })
      .addCase(fetchRegistrations.rejected, (s) => { s.regStatus = 'error'; });
  },
});

export const { setFilters, resetFilters, clearCurrent } = slice.actions;
export default slice.reducer;
