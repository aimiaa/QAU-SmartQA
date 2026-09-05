import axios, { AxiosError, AxiosHeaders, type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios';

export interface ApiEnvelope<T> {
  code?: number | string;
  message?: string;
  data?: T;
  success?: boolean;
}

export class ApiError extends Error {
  status?: number;
  code?: number | string;
  details?: unknown;

  constructor(message: string, options: { status?: number; code?: number | string; details?: unknown } = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = options.status;
    this.code = options.code;
    this.details = options.details;
  }
}

const AUTH_TOKEN_KEY = 'access_token';

const service = axios.create({
  // 后端基础地址在 .env 中配置：VITE_API_BASE_URL=实际服务地址
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  timeout: 15_000,
  headers: {
    'Content-Type': 'application/json',
  },
});

service.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  // 如后端需要登录鉴权，可把 token 存到 localStorage，所有请求会自动携带。
  const token = localStorage.getItem(AUTH_TOKEN_KEY);

  if (token) {
    const headers = AxiosHeaders.from(config.headers);
    headers.set('Authorization', `Bearer ${token}`);
    config.headers = headers;
  }

  return config;
});

service.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiEnvelope<unknown>>) => {
    const status = error.response?.status;
    const payload = error.response?.data;
    const message = payload?.message || error.message || '请求失败，请稍后重试';

    return Promise.reject(
      new ApiError(message, {
        status,
        code: payload?.code,
        details: payload,
      }),
    );
  },
);

const isRecord = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null;

const isApiEnvelope = <T>(value: unknown): value is ApiEnvelope<T> =>
  isRecord(value) && ('data' in value || 'success' in value || 'code' in value);

const unwrapResponse = <T>(payload: ApiEnvelope<T> | T): T => {
  if (!isApiEnvelope<T>(payload)) return payload;

  if (payload.success === false || (typeof payload.code === 'number' && payload.code >= 400)) {
    throw new ApiError(payload.message || '接口返回失败', {
      code: payload.code,
      details: payload,
    });
  }

  return payload.data as T;
};

export const http = {
  async get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    const response = await service.get<ApiEnvelope<T> | T>(url, config);
    return unwrapResponse<T>(response.data);
  },

  async post<T, D = unknown>(url: string, data?: D, config?: AxiosRequestConfig): Promise<T> {
    const response = await service.post<ApiEnvelope<T> | T>(url, data, config);
    return unwrapResponse<T>(response.data);
  },

  async put<T, D = unknown>(url: string, data?: D, config?: AxiosRequestConfig): Promise<T> {
    const response = await service.put<ApiEnvelope<T> | T>(url, data, config);
    return unwrapResponse<T>(response.data);
  },

  async delete<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    const response = await service.delete<ApiEnvelope<T> | T>(url, config);
    return unwrapResponse<T>(response.data);
  },
};

export default service;
