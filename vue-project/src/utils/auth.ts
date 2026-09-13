import { ref } from 'vue';

const AUTH_TOKEN_KEY = 'access_token';

const readToken = () => {
  if (typeof window === 'undefined') {
    return '';
  }

  return localStorage.getItem(AUTH_TOKEN_KEY) ?? '';
};

export const authTokenState = ref(readToken());

export const getAuthToken = () => authTokenState.value || readToken();

export const setAuthToken = (token: string) => {
  authTokenState.value = token;

  if (typeof window !== 'undefined') {
    localStorage.setItem(AUTH_TOKEN_KEY, token);
  }
};

export const clearAuthToken = () => {
  authTokenState.value = '';

  if (typeof window !== 'undefined') {
    localStorage.removeItem(AUTH_TOKEN_KEY);
  }
};

