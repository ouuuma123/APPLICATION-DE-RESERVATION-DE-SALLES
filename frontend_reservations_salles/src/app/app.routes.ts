import { Routes } from '@angular/router';
import { AppShell } from './core/app-shell';
import { authGuard, roleGuard } from './core/route.guards';

export const routes: Routes = [
  {
    path: '',
    component: AppShell,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () => import('./features/workspace-page').then((module) => module.WorkspacePage),
        data: { page: 'dashboard' },
      },
      {
        path: 'rooms',
        loadComponent: () => import('./features/workspace-page').then((module) => module.WorkspacePage),
        data: { page: 'rooms' },
      },
      {
        path: 'rooms/:id',
        loadComponent: () => import('./features/workspace-page').then((module) => module.WorkspacePage),
        data: { page: 'room-details' },
      },
      {
        path: 'calendar',
        loadComponent: () => import('./features/workspace-page').then((module) => module.WorkspacePage),
        data: { page: 'calendar' },
      },
      {
        path: 'reservations/new',
        loadComponent: () => import('./features/workspace-page').then((module) => module.WorkspacePage),
        data: { page: 'new-reservation' },
      },
      {
        path: 'reservations',
        loadComponent: () => import('./features/workspace-page').then((module) => module.WorkspacePage),
        data: { page: 'reservations' },
      },
      {
        path: 'notifications',
        loadComponent: () => import('./features/workspace-page').then((module) => module.WorkspacePage),
        data: { page: 'notifications' },
      },
      {
        path: 'admin',
        canActivate: [roleGuard],
        loadComponent: () => import('./features/workspace-page').then((module) => module.WorkspacePage),
        data: { page: 'admin', role: 'ADMIN' },
      },
      {
        path: 'admin/rooms',
        canActivate: [roleGuard],
        loadComponent: () => import('./features/workspace-page').then((module) => module.WorkspacePage),
        data: { page: 'admin-rooms', role: 'ADMIN' },
      },
      {
        path: 'admin/users',
        canActivate: [roleGuard],
        loadComponent: () => import('./features/workspace-page').then((module) => module.WorkspacePage),
        data: { page: 'admin-users', role: 'ADMIN' },
      },
      {
        path: 'admin/reservations',
        canActivate: [roleGuard],
        loadComponent: () => import('./features/workspace-page').then((module) => module.WorkspacePage),
        data: { page: 'admin-reservations', role: 'ADMIN' },
      },
      {
        path: 'settings',
        loadComponent: () => import('./features/workspace-page').then((module) => module.WorkspacePage),
        data: { page: 'settings' },
      },
    ],
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth-page').then((module) => module.AuthPage),
    data: { mode: 'login' },
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth-page').then((module) => module.AuthPage),
    data: { mode: 'register' },
  },
  { path: '**', redirectTo: 'dashboard' },
];
