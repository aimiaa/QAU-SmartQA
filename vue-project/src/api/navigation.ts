import { API_ENDPOINTS } from './endpoints';
import { http } from './request';
import {
  BookOpen,
  CalendarDays,
  Database,
  GraduationCap,
  LayoutDashboard,
  MessageSquare,
  Settings,
  ShieldCheck,
  UsersRound,
} from 'lucide-vue-next';
import type { Component } from 'vue';
import type { NavGroup } from '../types';

export interface NavItemResponse {
  id: string;
  label: string;
  description: string;
  icon: string;
}

export interface NavGroupResponse {
  id: string;
  title: string;
  items: NavItemResponse[];
}

const iconMap: Record<string, Component> = {
  BookOpen,
  CalendarDays,
  Database,
  GraduationCap,
  LayoutDashboard,
  MessageSquare,
  Settings,
  ShieldCheck,
  UsersRound,
};

const mapNavGroups = (groups: NavGroupResponse[]): NavGroup[] =>
  groups.map((group) => ({
    ...group,
    items: group.items.map((item) => ({
      ...item,
      icon: iconMap[item.icon] ?? MessageSquare,
    })),
  }));

export const navigationApi = {
  /**
   * 获取系统主导航。
   * 作用：替换当前 data.ts 中写死的 navGroups，让侧边栏菜单可由后端配置。
   */
  async getNavGroups(): Promise<NavGroup[]> {
    const groups = await http.get<NavGroupResponse[]>(API_ENDPOINTS.navGroups);

    return mapNavGroups(groups);
  },
};
