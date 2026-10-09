import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import api, { toRejection } from '../api/client';

export const fetchAudit = createAsyncThunk('audit/list', async ({ entityType, page }, { rejectWithValue }) => {
  try {
    const { data } = await api.get('/api/audit', { params: { entityType: entityType || undefined, page, size: 20 } });
    return data;
  } catch (e) {
    return rejectWithValue(toRejection(e));
  }
});

const slice = createSlice({
  name: 'audit',
  initialState: { data: { content: [], page: 0, totalPages: 0, totalElements: 0 }, status: 'idle', error: null },
  reducers: {},
  extraReducers: (b) => {
    b.addCase(fetchAudit.pending, (s) => { s.status = 'loading'; s.error = null; })
      .addCase(fetchAudit.fulfilled, (s, a) => { s.status = 'ready'; s.data = a.payload; })
      .addCase(fetchAudit.rejected, (s, a) => { s.status = 'error'; s.error = a.payload?.message || 'Could not load audit trail.'; });
  },
});

export default slice.reducer;
