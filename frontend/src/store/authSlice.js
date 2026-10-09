import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import api, { toRejection } from '../api/client';

const TOKEN_KEY = 'wd_token';
const readToken = () => {
  try { return localStorage.getItem(TOKEN_KEY); } catch { return null; }
};
const writeToken = (t) => {
  try { t ? localStorage.setItem(TOKEN_KEY, t) : localStorage.removeItem(TOKEN_KEY); } catch { /* ignore */ }
};

export const login = createAsyncThunk('auth/login', async (credentials, { rejectWithValue }) => {
  try {
    const { data } = await api.post('/api/auth/login', credentials);
    return data;
  } catch (e) {
    return rejectWithValue(toRejection(e));
  }
});

export const fetchMe = createAsyncThunk('auth/me', async (_, { rejectWithValue }) => {
  try {
    const { data } = await api.get('/api/auth/me');
    return data;
  } catch (e) {
    return rejectWithValue(toRejection(e));
  }
});

const authSlice = createSlice({
  name: 'auth',
  initialState: { token: readToken(), user: null, loginStatus: 'idle', loginError: null },
  reducers: {
    logout(state) {
      state.token = null;
      state.user = null;
      writeToken(null);
    },
  },
  extraReducers: (b) => {
    b.addCase(login.pending, (s) => { s.loginStatus = 'loading'; s.loginError = null; })
      .addCase(login.fulfilled, (s, a) => {
        s.loginStatus = 'idle';
        s.token = a.payload.token;
        s.user = a.payload.user;
        writeToken(a.payload.token);
      })
      .addCase(login.rejected, (s, a) => { s.loginStatus = 'idle'; s.loginError = a.payload?.message || 'Sign-in failed.'; })
      .addCase(fetchMe.fulfilled, (s, a) => { s.user = a.payload; })
      .addCase(fetchMe.rejected, (s) => { s.token = null; s.user = null; writeToken(null); });
  },
});

export const { logout } = authSlice.actions;
export default authSlice.reducer;
