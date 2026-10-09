import { createSlice, nanoid } from '@reduxjs/toolkit';

const toastSlice = createSlice({
  name: 'toasts',
  initialState: [],
  reducers: {
    pushToast: {
      reducer: (state, a) => { state.push(a.payload); },
      prepare: (type, text) => ({ payload: { id: nanoid(), type, text } }),
    },
    dismissToast: (state, a) => state.filter((t) => t.id !== a.payload),
  },
});

export const { pushToast, dismissToast } = toastSlice.actions;
export default toastSlice.reducer;
