import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import api, { toRejection } from '../api/client';

const call = (fn) => async (arg, { rejectWithValue }) => {
  try { return (await fn(arg)).data; } catch (e) { return rejectWithValue(toRejection(e)); }
};

export const fetchUsers = createAsyncThunk('users/list', call(() => api.get('/api/users')));
export const createUser = createAsyncThunk('users/create', call((body) => api.post('/api/users', body)));
export const updateUser = createAsyncThunk('users/update', call(({ id, body }) => api.put(`/api/users/${id}`, body)));
export const resetPassword = createAsyncThunk('users/reset', call(({ id, password }) => api.post(`/api/users/${id}/reset-password`, { password })));

const slice = createSlice({
  name: 'users',
  initialState: { items: [], status: 'idle', error: null },
  reducers: {},
  extraReducers: (b) => {
    b.addCase(fetchUsers.pending, (s) => { s.status = 'loading'; s.error = null; })
      .addCase(fetchUsers.fulfilled, (s, a) => { s.status = 'ready'; s.items = a.payload; })
      .addCase(fetchUsers.rejected, (s, a) => { s.status = 'error'; s.error = a.payload?.message || 'Could not load users.'; })
      .addCase(createUser.fulfilled, (s, a) => { s.items.push(a.payload); })
      .addCase(updateUser.fulfilled, (s, a) => {
        const i = s.items.findIndex((u) => u.id === a.payload.id);
        if (i >= 0) s.items[i] = a.payload;
      });
  },
});

export default slice.reducer;
