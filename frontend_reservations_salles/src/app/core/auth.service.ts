import { isPlatformBrowser } from '@angular/common';
import { Injectable, PLATFORM_ID, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, map, of } from 'rxjs';
import { DEMO_MODE } from './api.config';
import { ApiService, LoginRequest, RegisterRequest } from './api.service';
import { RoomlyUser, TokenResponse, UserRole } from './models';

const demoUser: RoomlyUser = {
  id: 1,
  name: 'Sophie Martin',
  email: 'sophie.martin@roomly.app',
  department: 'Product Design',
  role: 'EMPLOYEE',
  initials: 'SM',
};

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));
  private readonly userKey = 'roomly.user';
  private readonly tokenKey = 'roomly.access-token';
  private readonly signedOutKey = 'roomly.signed-out';

  readonly demoMode = DEMO_MODE;
  readonly currentUser = signal<RoomlyUser | null>(this.restoreUser());
  readonly token = signal<string | null>(this.readStorage(this.tokenKey));

  login(credentials: LoginRequest): Observable<RoomlyUser> {
    if (this.isBrowser) localStorage.removeItem(this.signedOutKey);
    if (DEMO_MODE) {
      const email = credentials.email.trim().toLowerCase();
      const role: UserRole = email.startsWith('admin') ? 'ADMIN' : 'EMPLOYEE';
      const name = role === 'ADMIN' ? 'Alex Morgan' : 'Sophie Martin';
      const user: RoomlyUser = {
        ...demoUser,
        id: role === 'ADMIN' ? 2 : 1,
        name,
        email,
        role,
        initials: role === 'ADMIN' ? 'AM' : 'SM',
        department: role === 'ADMIN' ? 'Workplace Operations' : 'Product Design',
      };
      this.saveUser(user);
      return of(user);
    }

    return this.api.login(credentials).pipe(map((response) => this.saveResponse(response)));
  }

  register(details: RegisterRequest): Observable<RoomlyUser> {
    if (this.isBrowser) localStorage.removeItem(this.signedOutKey);
    if (DEMO_MODE) {
      const initials = details.name.split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase();
      const user: RoomlyUser = {
        id: Date.now(),
        name: details.name,
        email: details.email,
        department: details.department,
        role: 'EMPLOYEE',
        initials,
      };
      this.saveUser(user);
      return of(user);
    }

    return this.api.register(details).pipe(map((response) => this.saveResponse(response)));
  }

  setRole(role: UserRole): void {
    if (!DEMO_MODE) return;
    const user = this.currentUser();
    if (!user) return;
    this.saveUser({
      ...user,
      role,
      name: role === 'ADMIN' ? 'Alex Morgan' : 'Sophie Martin',
      initials: role === 'ADMIN' ? 'AM' : 'SM',
      department: role === 'ADMIN' ? 'Workplace Operations' : 'Product Design',
    });
  }

  logout(): void {
    this.currentUser.set(null);
    this.token.set(null);
    if (this.isBrowser) {
      localStorage.removeItem(this.userKey);
      localStorage.removeItem(this.tokenKey);
      localStorage.setItem(this.signedOutKey, 'true');
    }
    void this.router.navigate(['/login']);
  }

  private saveResponse(response: TokenResponse): RoomlyUser {
    this.token.set(response.accessToken);
    this.saveUser(response.user);
    return response.user;
  }

  private saveUser(user: RoomlyUser): void {
    this.currentUser.set(user);
    if (this.isBrowser) {
      localStorage.setItem(this.userKey, JSON.stringify(user));
      localStorage.removeItem(this.signedOutKey);
    }
  }

  private restoreUser(): RoomlyUser | null {
    if (!this.isBrowser) return demoUser;
    if (localStorage.getItem(this.signedOutKey) === 'true') return null;
    const stored = this.readStorage(this.userKey);
    if (!stored) return DEMO_MODE ? demoUser : null;
    try {
      return JSON.parse(stored) as RoomlyUser;
    } catch {
      localStorage.removeItem(this.userKey);
      return DEMO_MODE ? demoUser : null;
    }
  }

  private readStorage(key: string): string | null {
    return this.isBrowser ? localStorage.getItem(key) : null;
  }
}
