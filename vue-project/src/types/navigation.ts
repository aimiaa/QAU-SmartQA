import type { Component } from 'vue';

export interface NavItem {
  id: string;
  label: string;
  description: string;
  icon: Component;
}

export interface NavGroup {
  id: string;
  title: string;
  items: NavItem[];
}
