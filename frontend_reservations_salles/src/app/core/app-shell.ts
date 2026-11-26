import { Component, HostListener, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { NgIcon } from '@ng-icons/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from './auth.service';
import { WorkspaceService } from './workspace.service';

interface NavigationItem {
  label: string;
  path: string;
  icon: string;
  exact?: boolean;
}

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, NgIcon, MatButtonModule],
  templateUrl: './app-shell.html',
  styleUrl: './app-shell.scss',
})
export class AppShell {
  readonly auth = inject(AuthService);
  readonly workspace = inject(WorkspaceService);
  private readonly router = inject(Router);
  readonly mobileNavOpen = signal(false);
  readonly profileOpen = signal(false);
  readonly notificationOpen = signal(false);
  readonly notificationError = signal('');
  readonly searchText = signal('');
  readonly user = this.auth.currentUser;
  readonly isAdmin = computed(() => this.user()?.role === 'ADMIN');
  readonly navItems = computed<NavigationItem[]>(() => [
    { label: 'Dashboard', path: '/dashboard', icon: 'lucideLayoutDashboard' },
    { label: 'Rooms', path: '/rooms', icon: 'lucideDoorOpen' },
    { label: 'Calendar', path: '/calendar', icon: 'lucideCalendarDays' },
    { label: 'My reservations', path: '/reservations', icon: 'lucideCalendarCheck' },
    { label: 'Notifications', path: '/notifications', icon: 'lucideBell' },
    ...(this.isAdmin()
      ? [
          { label: 'Administration', path: '/admin', icon: 'lucideChartNoAxesCombined', exact: true },
          { label: 'Manage rooms', path: '/admin/rooms', icon: 'lucideBuilding2' },
          { label: 'Manage users', path: '/admin/users', icon: 'lucideUsers' },
          { label: 'All reservations', path: '/admin/reservations', icon: 'lucideClipboardList' },
        ]
      : []),
  ]);
  readonly pageTitle = signal('Overview');

  constructor() {
    this.updatePageTitle(this.router.url);
    this.router.events.pipe(filter((event) => event instanceof NavigationEnd)).subscribe((event) => {
      this.updatePageTitle(event.urlAfterRedirects);
      this.mobileNavOpen.set(false);
      this.profileOpen.set(false);
      this.notificationOpen.set(false);
    });
  }

  @HostListener('document:keydown.escape')
  closeOverlays(): void {
    this.mobileNavOpen.set(false);
    this.profileOpen.set(false);
    this.notificationOpen.set(false);
  }

  search(): void {
    const query = this.searchText().trim();
    if (!query) return;
    void this.router.navigate(['/rooms'], { queryParams: { q: query } });
  }

  useRole(role: 'EMPLOYEE' | 'ADMIN'): void {
    this.auth.setRole(role);
    this.profileOpen.set(false);
    void this.router.navigate(['/dashboard']);
  }

  markNotificationAsRead(id: number): void {
    this.workspace.markAsRead(id).subscribe({
      next: () => this.notificationError.set(''),
      error: (error: unknown) => this.notificationError.set(
        error instanceof Error ? error.message : 'The notification could not be updated.',
      ),
    });
  }

  private updatePageTitle(url: string): void {
    const path = url.split('?')[0];
    const titles: Record<string, string> = {
      '/dashboard': 'Overview',
      '/rooms': 'Explore rooms',
      '/calendar': 'Room calendar',
      '/reservations': 'My reservations',
      '/reservations/new': 'New reservation',
      '/notifications': 'Notifications',
      '/admin': 'Workspace overview',
      '/admin/rooms': 'Room management',
      '/admin/users': 'Team directory',
      '/admin/reservations': 'All reservations',
      '/settings': 'Preferences',
    };
    this.pageTitle.set(path.startsWith('/rooms/') ? 'Room details' : titles[path] ?? 'Overview');
  }
}
