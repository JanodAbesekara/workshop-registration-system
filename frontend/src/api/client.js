import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '',
  headers: { 'Content-Type': 'application/json' },
});

let getToken = () => null;
let onUnauthorized = () => {};

/** Called once from the store so this module never imports the store (no circular import). */
export function configureApi(tokenGetter, unauthorizedHandler) {
  getToken = tokenGetter;
  onUnauthorized = unauthorizedHandler;
}

api.interceptors.request.use((config) => {
  const token = getToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    const isLogin = err.config?.url?.includes('/api/auth/login');
    if (err.response?.status === 401 && !isLogin) onUnauthorized();
    return Promise.reject(err);
  }
);

/** Shape used by every thunk's rejectWithValue. */
export function toRejection(err) {
  return {
    status: err.response?.status ?? 0,
    message: err.response ? err.response.data?.message || 'Request failed.' : 'Cannot reach the server.',
    fieldErrors: err.response?.data?.fieldErrors || {},
  };
}

export default api;
