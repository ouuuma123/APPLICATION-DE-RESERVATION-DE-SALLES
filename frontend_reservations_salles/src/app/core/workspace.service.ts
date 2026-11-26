import { Injectable, computed, inject, signal } from '@angular/core';
import { forkJoin, map, Observable, of, tap } from 'rxjs';
import { DEMO_MODE } from './api.config';
import { ApiService } from './api.service';
import { BookingStatus, Reservation, Room, RoomlyNotification, RoomlyUser, UserRole } from './models';
import { AuthService } from './auth.service';

const image = (id: string, width = 900) =>
  `https://images.unsplash.com/${id}?auto=format&fit=crop&w=${width}&q=85`;

const rooms: Room[] = [
  {
    id: 1, name: 'The Atrium', capacity: 12, location: 'North Wing', floor: 'Floor 4',
    equipment: ['Video conferencing', '4K display', 'Whiteboard'], image: image('photo-1497366754035-f200968a6e72'),
    accent: 'teal', active: true,
  },
  {
    id: 2, name: 'Studio One', capacity: 8, location: 'East Wing', floor: 'Floor 3',
    equipment: ['Video conferencing', 'Wireless display', 'Whiteboard'], image: image('photo-1497366811353-6870744d04b2'),
    accent: 'blue', active: true,
  },
  {
    id: 3, name: 'The Boardroom', capacity: 16, location: 'Executive Suite', floor: 'Floor 5',
    equipment: ['4K display', 'Video conferencing', 'Speakerphone'], image: image('photo-1497366216548-37526070297c'),
    accent: 'amber', active: true,
  },
  {
    id: 4, name: 'Focus Room 02', capacity: 4, location: 'South Wing', floor: 'Floor 2',
    equipment: ['Monitor', 'Whiteboard'], image: image('photo-1497366811353-6870744d04b2'),
    accent: 'rose', active: true,
  },
  {
    id: 5, name: 'The Greenhouse', capacity: 10, location: 'Atrium', floor: 'Floor 1',
    equipment: ['Video conferencing', '4K display', 'Natural light'], image: image('photo-1497366754035-f200968a6e72'),
    accent: 'green', active: true,
  },
  {
    id: 6, name: 'Creative Hub', capacity: 6, location: 'West Wing', floor: 'Floor 3',
    equipment: ['Wireless display', 'Whiteboard', 'Video conferencing'], image: image('photo-1497366216548-37526070297c'),
    accent: 'violet', active: true,
  },
];

const weekStart = (): Date => {
  const date = new Date();
  date.setHours(0, 0, 0, 0);
  date.setDate(date.getDate() - ((date.getDay() + 6) % 7));
  return date;
};

const atTime = (dayOffset: number, hour: number, minute = 0): string => {
  const date = weekStart();
  date.setDate(date.getDate() + dayOffset);
  date.setHours(hour, minute, 0, 0);
  return `${localDateString(date)}T${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
};

const localDateString = (date: Date): string => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

@Injectable({ providedIn: 'root' })
export class WorkspaceService {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  readonly rooms = signal<Room[]>(rooms);
  readonly reservations = signal<Reservation[]>([
    { id: 1042, title: 'Product roadmap review', roomId: 1, roomName: 'The Atrium', userName: 'Sophie Martin', startTime: atTime(0, 10), endTime: atTime(0, 11), attendeesCount: 8, status: 'CONFIRMED' },
    { id: 1043, title: 'Design sync', roomId: 2, roomName: 'Studio One', userName: 'Sophie Martin', startTime: atTime(0, 13), endTime: atTime(0, 14), attendeesCount: 5, status: 'CONFIRMED' },
    { id: 1044, title: 'Q4 planning session', roomId: 3, roomName: 'The Boardroom', userName: 'Sophie Martin', startTime: atTime(1, 9, 30), endTime: atTime(1, 11), attendeesCount: 12, status: 'PENDING' },
    { id: 1045, title: 'Creative brainstorm', roomId: 5, roomName: 'The Greenhouse', userName: 'Jordan Lee', startTime: atTime(1, 14), endTime: atTime(1, 15), attendeesCount: 7, status: 'CONFIRMED' },
    { id: 1046, title: 'All-hands preparation', roomId: 1, roomName: 'The Atrium', userName: 'Sophie Martin', startTime: atTime(2, 11), endTime: atTime(2, 12), attendeesCount: 10, status: 'CONFIRMED' },
    { id: 1047, title: 'Weekly team check-in', roomId: 4, roomName: 'Focus Room 02', userName: 'Sophie Martin', startTime: atTime(3, 10), endTime: atTime(3, 11), attendeesCount: 4, status: 'CONFIRMED' },
    { id: 1048, title: 'Client discovery call', roomId: 6, roomName: 'Creative Hub', userName: 'Sophie Martin', startTime: atTime(4, 15), endTime: atTime(4, 16), attendeesCount: 5, status: 'PENDING' },
  ]);
  readonly notifications = signal<RoomlyNotification[]>([
    { id: 1, title: 'Reservation confirmed', message: 'Your booking for The Atrium is confirmed for today at 10:00 AM.', type: 'booking', time: '12 min ago', read: false },
    { id: 2, title: 'Upcoming meeting', message: 'Product roadmap review starts in 30 minutes.', type: 'reminder', time: '28 min ago', read: false },
    { id: 3, title: 'Room update', message: 'Studio One now has a new wireless display.', type: 'update', time: '2 hours ago', read: false },
    { id: 4, title: 'Reservation updated', message: 'Your Q4 planning session request is awaiting approval.', type: 'booking', time: 'Yesterday', read: true },
  ]);
  readonly users = signal<RoomlyUser[]>([
    { id: 1, name: 'Sophie Martin', email: 'sophie.martin@roomly.app', department: 'Product Design', role: 'EMPLOYEE', initials: 'SM' },
    { id: 2, name: 'Alex Morgan', email: 'alex.morgan@roomly.app', department: 'Workplace Operations', role: 'ADMIN', initials: 'AM' },
    { id: 3, name: 'Jordan Lee', email: 'jordan.lee@roomly.app', department: 'Engineering', role: 'EMPLOYEE', initials: 'JL' },
    { id: 4, name: 'Taylor Chen', email: 'taylor.chen@roomly.app', department: 'Marketing', role: 'EMPLOYEE', initials: 'TC' },
  ]);
  readonly isLoading = signal(false);
  readonly loadError = signal('');
  readonly unreadCount = computed(() => this.notifications().filter((notification) => !notification.read).length);

  constructor() {
    if (!DEMO_MODE) this.reload();
  }

  reload(): void {
    this.isLoading.set(true);
    this.loadError.set('');
    this.rooms.set([]);
    this.reservations.set([]);
    this.notifications.set([]);
    this.users.set([]);
    const user = this.auth.currentUser();
    forkJoin({
      rooms: this.api.getRooms(),
      reservations: this.api.getReservations(),
      notifications: this.api.getNotifications(),
      users: user?.role === 'ADMIN' ? this.api.getUsers() : of([] as RoomlyUser[]),
    }).subscribe({
      next: (data) => {
        this.rooms.set(data.rooms);
        this.reservations.set(data.reservations);
        this.notifications.set(data.notifications);
        this.users.set(data.users);
        this.isLoading.set(false);
      },
      error: (error: unknown) => {
        this.isLoading.set(false);
        this.loadError.set(error instanceof Error ? error.message : 'Workspace data could not be loaded.');
      },
    });
  }

  createReservation(
    details: Pick<Reservation, 'title' | 'roomId' | 'startTime' | 'endTime' | 'attendeesCount'>,
    userName: string,
  ): Observable<void> {
    const room = this.rooms().find((item) => item.id === details.roomId);
    if (!room) throw new Error('The selected meeting room could not be found.');
    if (!DEMO_MODE) {
      return this.api.createReservation(details).pipe(
        tap((reservation) => this.reservations.update((items) => [reservation, ...items])),
        map(() => undefined),
      );
    }

    this.reservations.update((items) => [{
      ...details,
      id: Date.now(),
      roomName: room.name,
      userName,
      status: 'CONFIRMED',
    }, ...items]);
    return of(undefined);
  }

  cancelReservation(id: number): Observable<void> {
    if (!DEMO_MODE) {
      return this.api.cancelReservation(id).pipe(
        tap(() => this.updateReservationStatus(id, 'CANCELLED')),
      );
    }
    this.updateReservationStatus(id, 'CANCELLED');
    return of(undefined);
  }

  markAsRead(id: number): Observable<void> {
    if (!DEMO_MODE) {
      return this.api.markNotificationAsRead(id).pipe(
        tap(() => this.updateNotification(id)),
        map(() => undefined),
      );
    }
    this.updateNotification(id);
    return of(undefined);
  }

  markAllAsRead(): Observable<void> {
    if (!DEMO_MODE) {
      const updates = this.notifications()
        .filter((notification) => !notification.read)
        .map((notification) => this.api.markNotificationAsRead(notification.id));
      if (!updates.length) return of(undefined);
      return forkJoin(updates).pipe(
        tap(() => this.notifications.update((items) => items.map((item) => ({ ...item, read: true })))),
        map(() => undefined),
      );
    }
    this.notifications.update((items) => items.map((item) => ({ ...item, read: true })));
    return of(undefined);
  }

  toggleRoom(id: number): Observable<Room> {
    const room = this.rooms().find((item) => item.id === id);
    if (!room) throw new Error('The selected meeting room could not be found.');
    const active = !room.active;
    if (!DEMO_MODE) {
      return this.api.setRoomActive(id, active).pipe(
        tap((updated) => this.rooms.update((items) => items.map((item) => item.id === id ? updated : item))),
      );
    }
    const updated = { ...room, active };
    this.rooms.update((items) => items.map((item) => item.id === id ? updated : item));
    return of(updated);
  }

  createRoom(room: Room): Observable<Room> {
    if (!DEMO_MODE) {
      return this.api.createRoom({
        name: room.name,
        capacity: room.capacity,
        location: room.location,
        equipment: room.equipment.join(', '),
        amenities: [],
      }).pipe(tap((created) => this.rooms.update((items) => [...items, created])));
    }
    this.rooms.update((items) => [...items, room]);
    return of(room);
  }

  createUser(user: RoomlyUser, password: string): Observable<RoomlyUser> {
    if (!DEMO_MODE) {
      return this.api.createUser({
        name: user.name,
        email: user.email,
        department: user.department,
        password,
      }).pipe(tap((created) => this.users.update((items) => [...items, created])));
    }
    this.users.update((items) => [...items, user]);
    return of(user);
  }

  setUserRole(user: RoomlyUser): Observable<RoomlyUser> {
    const role: UserRole = user.role === 'ADMIN' ? 'EMPLOYEE' : 'ADMIN';
    if (!DEMO_MODE) {
      return this.api.setUserRole(user.id, role).pipe(
        tap((updated) => this.users.update((items) => items.map((member) => member.id === user.id ? updated : member))),
      );
    }
    const updated = { ...user, role };
    this.users.update((items) => items.map((member) => member.id === user.id ? updated : member));
    return of(updated);
  }

  private updateReservationStatus(id: number, status: BookingStatus): void {
    this.reservations.update((items) =>
      items.map((item) => item.id === id ? { ...item, status } : item),
    );
  }

  private updateNotification(id: number): void {
    this.notifications.update((items) =>
      items.map((item) => item.id === id ? { ...item, read: true } : item),
    );
  }
}
