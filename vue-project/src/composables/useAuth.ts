import { computed } from 'vue';
import { authApi, type LoginPayload, type LoginResponse } from '../api/auth';
import { authTokenState, clearAuthToken, getAuthToken, setAuthToken } from '../utils/auth';

const extractToken = (response: LoginResponse | string): string => {
  if (typeof response === 'string') {
    return response.trim();
  }

  const token = response.token;
  return typeof token === 'string' ? token.trim() : '';
};

export const useAuth = () => {
  const isAuthenticated = computed(() => Boolean(authTokenState.value || getAuthToken()));

  const login = async (payload: LoginPayload) => {
    const response = await authApi.login(payload);
    const token = extractToken(response);

    if (!token) {
      throw new Error('登录成功，但后端没有返回可用的 token');
    }

    setAuthToken(token);
    return response;
  };

  const logout = async () => {
    try {
      await authApi.logout();
    } finally {
      clearAuthToken();
    }
  };

  return {
    authToken: authTokenState,
    isAuthenticated,
    login,
    logout,
  };
};
