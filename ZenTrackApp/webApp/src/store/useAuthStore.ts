import { create } from 'zustand';
import * as authService from '../services/authService';

const TOKEN_KEY = 'zentrack_token';

interface AuthState {
  token: string | null;
  isLoading: boolean;
  error: string | null;
  login: (email: string, password: string) => Promise<void>;
  register: (name: string, email: string, password: string) => Promise<void>;
  logout: () => void;
  clearError: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  token: localStorage.getItem(TOKEN_KEY),
  isLoading: false,
  error: null,

  login: async (email, password) => {
    set({ isLoading: true, error: null });
    const result = await authService.login({ email, password });
    if (result.success) {
      localStorage.setItem(TOKEN_KEY, result.data.token);
      set({ token: result.data.token, isLoading: false });
    } else {
      set({ error: result.error, isLoading: false });
    }
  },

  register: async (name, email, password) => {
    set({ isLoading: true, error: null });
    const result = await authService.register({ name, email, password });
    if (result.success) {
      localStorage.setItem(TOKEN_KEY, result.data.token);
      set({ token: result.data.token, isLoading: false });
    } else {
      set({ error: result.error, isLoading: false });
    }
  },

  logout: () => {
    localStorage.removeItem(TOKEN_KEY);
    set({ token: null, error: null });
  },

  clearError: () => set({ error: null }),
}));
