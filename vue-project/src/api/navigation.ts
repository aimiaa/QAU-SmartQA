import { API_ENDPOINTS } from './endpoints';
import { http } from './http';
import type { NavGroup } from '../types';

export const navigationApi = {
  /**
   * 获取系统主导航。
   * 作用：替换当前 data.ts 中写死的 navGroups，让侧边栏菜单可由后端配置。
   */
  getNavGroups() {
    return http.get<NavGroup[]>(API_ENDPOINTS.navGroups);
  },
};
