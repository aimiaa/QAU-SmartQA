import { API_ENDPOINTS } from './endpoints';
import { http } from './request';

export interface LoginPayload {
  username: string;
  password: string;
}

export interface LoginUser {
  id?: number;
  username?: string;
  realName?: string;
  roleCode?: string;
  department?: string;
}

export interface LoginResponse {
  token?: string;
  user?: LoginUser;
  [key: string]: unknown;
}

export const authApi = {
  async login(payload: LoginPayload): Promise<LoginResponse> {
    return http.post<LoginResponse, LoginPayload>(API_ENDPOINTS.adminUserLogin, payload);
  },

  async logout(): Promise<void> {
    await http.post<void>(API_ENDPOINTS.adminUserLogout);
  },
};
