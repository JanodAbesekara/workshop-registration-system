import { configureStore } from '@reduxjs/toolkit';
import auth, { logout } from './authSlice';
import workshops from './workshopsSlice';
import users from './usersSlice';
import audit from './auditSlice';
import toasts from './toastSlice';
import { configureApi } from '../api/client';

const store = configureStore({ reducer: { auth, workshops, users, audit, toasts } });

// Attach the JWT to every request and sign out automatically when the server says 401.
configureApi(
  () => store.getState().auth.token,
  () => store.dispatch(logout())
);

export default store;
