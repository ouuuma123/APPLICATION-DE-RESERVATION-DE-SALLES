import { DatePipe, UpperCasePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { NgIcon } from '@ng-icons/core';
import { AuthService } from '../core/auth.service';
import { Reservation, Room, RoomlyNotification, RoomlyUser } from '../core/models';
import { WorkspaceService } from '../core/workspace.service';

type PageType =
  | 'dashboard' | 'rooms' | 'room-details' | 'new-reservation' | 'calendar'
  | 'reservations' | 'notifications' | 'admin' | 'admin-rooms' | 'admin-users'
  | 'admin-reservations' | 'settings';
type CalendarView = 'Week' | 'Day' | 'Month';
type ReservationFilter = 'All' | 'Confirmed' | 'Pending' | 'Cancelled';

const localDateString = (date: Date): string => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};

const defaultReservationTimes = (): { date: string; start: string; end: string } => {
  const start = new Date();
  start.setMinutes(0, 0, 0);
  start.setHours(start.getHours() + 1);
  if (start.getHours() >= 22) {
    start.setDate(start.getDate() + 1);
    start.setHours(9, 0, 0, 0);
  }
  const end = new Date(start);
  end.setHours(end.getHours() + 1);
  return {
    date: localDateString(start),
    start: `${String(start.getHours()).padStart(2, '0')}:00`,
    end: `${String(end.getHours()).padStart(2, '0')}:00`,
  };
};

@Component({
  selector: 'app-workspace-page',
  standalone: true,
  imports: [
    DatePipe, UpperCasePipe, ReactiveFormsModule, RouterLink, NgIcon,
    MatButtonModule, MatFormFieldModule, MatInputModule,
  ],
  templateUrl: './workspace-page.html',
  styleUrl: './workspace-page.scss',
})
export class WorkspacePage {
  private readonly route = inject(ActivatedRoute);
  readonly router = inject(Router);
  private readonly formBuilder = inject(FormBuilder);
  readonly auth = inject(AuthService);
  readonly workspace = inject(WorkspaceService);
  readonly page = this.route.snapshot.data['page'] as PageType;
  private readonly initialSkeleton = signal(true);
  readonly loading = computed(() => this.initialSkeleton() || this.workspace.isLoading());
  readonly roomSearch = signal('');
  readonly selectedRoomId = signal(Number(this.route.snapshot.paramMap.get('id')) || 1);
  readonly minCapacity = signal(0);
  readonly equipmentFilter = signal('All equipment');
  readonly statusFilter = signal<ReservationFilter>('All');
  readonly reservationSearch = signal('');
  readonly reservationPage = signal(1);
  readonly calendarView = signal<CalendarView>('Week');
  readonly calendarOffset = signal(0);
  readonly selectedRoomFilter = signal('All rooms');
  readonly selectedReservation = signal<Reservation | null>(null);
  readonly toastMessage = signal('');
  readonly formError = signal('');
  readonly formSuccess = signal('');
  readonly roomDetailDate = signal(localDateString(new Date()));
  readonly showRoomForm = signal(false);
  readonly showUserForm = signal(false);
  readonly activeTab = signal('General');
  readonly user = this.auth.currentUser;
  readonly today = new Date();
  readonly firstName = computed(() => this.user()?.name.split(' ')[0] ?? 'there');
  readonly departmentCount = computed(() => new Set(this.workspace.users().map((member) => member.department)).size);
  readonly isActiveReservation = (reservation: Reservation): boolean => reservation.status !== 'CANCELLED';
  readonly matchesUser = (member: RoomlyUser): boolean =>
    !this.reservationSearch().trim()
    || `${member.name} ${member.email} ${member.department}`.toLowerCase().includes(this.reservationSearch().trim().toLowerCase());

  readonly roomQuery = this.route.snapshot.queryParamMap.get('q') ?? '';
  readonly roomId = Number(this.route.snapshot.paramMap.get('id'));
  readonly selectedRoom = computed(() =>
    this.workspace.rooms().find((room) => room.id === this.selectedRoomId()) ?? this.workspace.rooms()[0],
  );
  readonly visibleRooms = computed(() => {
    const query = this.roomSearch().trim().toLowerCase();
    return this.workspace.rooms().filter((room) => {
      const matchesQuery = !query || [room.name, room.location, room.floor, ...room.equipment].join(' ').toLowerCase().includes(query);
      const matchesCapacity = room.capacity >= this.minCapacity();
      const matchesEquipment = this.equipmentFilter() === 'All equipment'
        || room.equipment.some((item) => item.toLowerCase().includes(this.equipmentFilter().toLowerCase()));
      return room.active && matchesQuery && matchesCapacity && matchesEquipment;
    });
  });
  readonly reservationsForUser = computed(() =>
    this.workspace.reservations().filter((item) => item.userName === this.user()?.name),
  );
  readonly visibleReservations = computed(() => {
    const query = this.reservationSearch().trim().toLowerCase();
    return this.reservationsForUser().filter((item) => {
      const status = this.statusFilter().toUpperCase();
      const matchesStatus = status === 'ALL' || item.status === status;
      const matchesSearch = !query || `${item.title} ${item.roomName} ${item.id}`.toLowerCase().includes(query);
      return matchesStatus && matchesSearch;
    });
  });
  readonly pagedReservations = computed(() => {
    const start = (this.reservationPage() - 1) * 8;
    return this.visibleReservations().slice(start, start + 8);
  });
  readonly reservationPageCount = computed(() => Math.max(1, Math.ceil(this.visibleReservations().length / 8)));
  readonly reservationPageStart = computed(() =>
    this.visibleReservations().length ? (this.reservationPage() - 1) * 8 + 1 : 0,
  );
  readonly reservationPageEnd = computed(() =>
    Math.min(this.reservationPage() * 8, this.visibleReservations().length),
  );
  readonly allVisibleReservations = computed(() => {
    const query = this.reservationSearch().trim().toLowerCase();
    const status = this.statusFilter().toUpperCase();
    return this.workspace.reservations().filter((item) =>
      (status === 'ALL' || item.status === status)
      && (!query || `${item.title} ${item.roomName} ${item.userName} ${item.id}`.toLowerCase().includes(query)),
    );
  });
  readonly upcomingReservations = computed(() =>
    this.reservationsForUser()
      .filter((item) => item.status !== 'CANCELLED' && new Date(item.endTime).getTime() >= Date.now())
      .sort((a, b) => a.startTime.localeCompare(b.startTime)),
  );
  readonly upcomingThisWeek = computed(() => {
    const end = new Date();
    end.setDate(end.getDate() + 7);
    return this.upcomingReservations().filter((item) => new Date(item.startTime) <= end).length;
  });
  readonly thisWeekDates = computed(() => {
    const monday = new Date();
    monday.setHours(0, 0, 0, 0);
    monday.setDate(monday.getDate() - ((monday.getDay() + 6) % 7));
    return Array.from({ length: 7 }, (_, index) => {
      const date = new Date(monday);
      date.setDate(monday.getDate() + index);
      return date;
    });
  });
  readonly calendarDates = computed(() => {
    const today = new Date();
    if (this.calendarView() === 'Day') {
      today.setDate(today.getDate() + this.calendarOffset());
      return [today];
    }

    if (this.calendarView() === 'Month') {
      const month = new Date(today.getFullYear(), today.getMonth() + this.calendarOffset(), 1);
      const mondayOffset = (month.getDay() + 6) % 7;
      const start = new Date(month);
      start.setDate(1 - mondayOffset);
      return Array.from({ length: 42 }, (_, index) => {
        const date = new Date(start);
        date.setDate(start.getDate() + index);
        return date;
      });
    }
    const monday = new Date(today);
    monday.setHours(0, 0, 0, 0);
    monday.setDate(monday.getDate() - ((monday.getDay() + 6) % 7) + this.calendarOffset() * 7);
    return Array.from({ length: 7 }, (_, index) => {
      const date = new Date(monday);
      date.setDate(monday.getDate() + index);
      return date;
    });
  });
  readonly monthLabel = computed(() => this.calendarDates()[0]?.toLocaleDateString('en-US', { month: 'long', year: 'numeric' }) ?? '');
  readonly availableRooms = computed(() => this.workspace.rooms().filter((room) => room.active).length);
  readonly unreadNotifications = computed(() => this.workspace.unreadCount());
  readonly occupancy = computed(() => Math.round((this.workspace.reservations().filter((item) => item.status !== 'CANCELLED').length / Math.max(this.availableRooms() * 5, 1)) * 100));
  readonly roomDateChoices = Array.from({ length: 7 }, (_, index) => {
    const date = new Date();
    date.setDate(date.getDate() + index);
    return date;
  });
  private readonly defaultTimes = defaultReservationTimes();
  readonly reservationForm = this.formBuilder.nonNullable.group({
    roomId: [String(this.roomId || 1), Validators.required],
    title: ['', [Validators.required, Validators.maxLength(100)]],
    date: [this.defaultTimes.date, Validators.required],
    start: [this.defaultTimes.start, Validators.required],
    end: [this.defaultTimes.end, Validators.required],
    attendeesCount: [4, [Validators.required, Validators.min(1)]],
    description: [''],
  });
  readonly roomForm = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    capacity: [8, [Validators.required, Validators.min(1)]],
    location: ['', Validators.required],
    equipment: ['Video conferencing, Whiteboard'],
  });
  readonly userForm = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    department: ['', Validators.required],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  constructor() {
    this.roomSearch.set(this.roomQuery);
    this.reservationForm.controls.roomId.valueChanges.subscribe((roomId) => {
      this.selectedRoomId.set(Number(roomId) || 1);
    });
    window.setTimeout(() => this.initialSkeleton.set(false), 350);
    this.route.queryParamMap.subscribe((params) => {
      const query = params.get('q') ?? '';
      if (query !== this.roomSearch()) this.roomSearch.set(query);
      if (this.page === 'new-reservation') {
        const roomId = params.get('roomId');
        const date = params.get('date');
        const start = params.get('start');
        this.reservationForm.patchValue({
          ...(roomId ? { roomId } : {}),
          ...(date ? { date } : {}),
          ...(start ? { start: this.parseTime(start) } : {}),
        });
        if (roomId) this.selectedRoomId.set(Number(roomId));
      }
    });
  }

  submitReservation(): void {
    this.formError.set('');
    this.formSuccess.set('');
    if (this.reservationForm.invalid) {
      this.reservationForm.markAllAsTouched();
      this.formError.set('Please complete each required field before continuing.');
      return;
    }
    const form = this.reservationForm.getRawValue();
    const room = this.workspace.rooms().find((item) => item.id === Number(form.roomId));
    if (!room?.active) {
      this.formError.set('Please choose an active meeting room.');
      return;
    }
    const startTime = `${form.date}T${form.start}`;
    const endTime = `${form.date}T${form.end}`;
    if (new Date(startTime).getTime() <= Date.now()) {
      this.formError.set('Choose a start time in the future.');
      return;
    }
    if (new Date(endTime).getTime() <= new Date(startTime).getTime()) {
      this.formError.set('The end time must be after the start time.');
      return;
    }
    if (form.attendeesCount > room.capacity) {
      this.formError.set(`${room.name} accommodates up to ${room.capacity} people.`);
      return;
    }
    const conflict = this.workspace.reservations().some((item) =>
      item.roomId === room.id && item.status !== 'CANCELLED'
      && new Date(startTime) < new Date(item.endTime)
      && new Date(endTime) > new Date(item.startTime),
    );
    if (conflict) {
      this.formError.set(`${room.name} is already booked during that time. Try another room or time.`);
      return;
    }

    this.workspace.createReservation({
      title: form.title.trim(),
      roomId: room.id,
      startTime,
      endTime,
      attendeesCount: form.attendeesCount,
    }, this.user()?.name ?? 'Roomly member').subscribe({
      next: () => {
        this.formSuccess.set('Your reservation is confirmed. We’ve added it to your calendar.');
        this.reservationForm.controls.title.reset('');
        this.reservationForm.controls.description.reset('');
      },
      error: (error: unknown) => this.formError.set(
        error instanceof Error ? error.message : 'Your reservation could not be saved. Please try again.',
      ),
    });
  }

  cancelReservation(reservation: Reservation): void {
    if (reservation.status === 'CANCELLED') return;
    const confirmed = window.confirm(`Cancel “${reservation.title}” in ${reservation.roomName}?`);
    if (!confirmed) return;
    this.workspace.cancelReservation(reservation.id).subscribe({
      next: () => {
        this.toastMessage.set('Reservation cancelled.');
        this.clearToast();
      },
      error: (error: unknown) => {
        this.toastMessage.set(error instanceof Error ? error.message : 'The reservation could not be cancelled.');
        this.clearToast();
      },
    });
  }

  markNotificationAsRead(notification: RoomlyNotification): void {
    this.workspace.markAsRead(notification.id).subscribe({
      error: (error: unknown) => {
        this.toastMessage.set(error instanceof Error ? error.message : 'The notification could not be updated.');
        this.clearToast();
      },
    });
  }

  notificationIcon(notification: RoomlyNotification): string {
    if (notification.type === 'reminder') return 'lucideClock3';
    if (notification.type === 'update') return 'lucideBuilding2';
    return 'lucideCalendarCheck';
  }

  reservationsForDate(date: Date): Reservation[] {
    const day = localDateString(date);
    return this.workspace.reservations()
      .filter((item) => item.startTime.slice(0, 10) === day && item.status !== 'CANCELLED')
      .filter((item) => this.selectedRoomFilter() === 'All rooms' || item.roomName === this.selectedRoomFilter())
      .sort((a, b) => a.startTime.localeCompare(b.startTime));
  }

  bookingsThisDay(date: Date): number {
    return this.workspace.reservations().filter((item) =>
      item.startTime.slice(0, 10) === localDateString(date) && item.status !== 'CANCELLED',
    ).length;
  }

  chartHeight(date: Date): number {
    const count = this.bookingsThisDay(date);
    return Math.max(20, Math.min(93, count * 24 + 14));
  }

  timeLabel(value: string): string {
    return new Date(value).toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' });
  }

  dateKey(date: Date): string {
    return localDateString(date);
  }

  monthMatches(date: Date): boolean {
    const dates = this.calendarDates();
    return date.getMonth() === dates[15]?.getMonth();
  }

  selectMonthDay(date: Date): void {
    const today = new Date();
    const target = new Date(date.getFullYear(), date.getMonth(), date.getDate());
    const current = new Date(today.getFullYear(), today.getMonth(), today.getDate());
    this.calendarView.set('Day');
    this.calendarOffset.set(Math.round((target.getTime() - current.getTime()) / 86400000));
  }

  bookSelectedSlot(room: Room, date: string, slot: string): void {
    void this.router.navigate(['/reservations/new'], {
      queryParams: { roomId: room.id, date, start: slot },
    });
  }

  isToday(date: Date): boolean {
    return localDateString(date) === localDateString(new Date());
  }

  availabilityFor(date: Date): 'Available' | 'Filling up' | 'Busy' {
    const booked = this.bookingsThisDay(date);
    return booked >= 3 ? 'Busy' : booked > 0 ? 'Filling up' : 'Available';
  }

  dateAvailability(): 'Available' | 'Filling up' | 'Busy' {
    return this.availabilityFor(new Date(`${this.roomDetailDate()}T00:00:00`));
  }

  isSlotAvailable(room: Room, date: string, slot: string): boolean {
    const startTime = `${date}T${this.parseTime(slot)}`;
    const end = new Date(startTime);
    end.setHours(end.getHours() + 1);
    return !this.workspace.reservations().some((reservation) =>
      reservation.roomId === room.id && reservation.status !== 'CANCELLED'
      && new Date(startTime) < new Date(reservation.endTime)
      && end > new Date(reservation.startTime),
    );
  }

  roomLocation(roomId: number): string {
    return this.workspace.rooms().find((room) => room.id === roomId)?.location ?? '';
  }

  setReservationFilter(filter: ReservationFilter): void {
    this.statusFilter.set(filter);
    this.reservationPage.set(1);
  }

  setReservationSearch(value: string): void {
    this.reservationSearch.set(value);
    this.reservationPage.set(1);
  }

  moveReservationPage(direction: number): void {
    this.reservationPage.update((page) => Math.min(
      this.reservationPageCount(),
      Math.max(1, page + direction),
    ));
  }

  setCalendarView(view: CalendarView): void {
    this.calendarView.set(view);
  }

  moveCalendar(direction: number): void {
    this.calendarOffset.update((offset) => offset + direction);
  }

  changeRoomStatus(room: Room): void {
    this.workspace.toggleRoom(room.id).subscribe({
      next: (updated) => {
        this.toastMessage.set(`${room.name} is now ${updated.active ? 'available' : 'inactive'}.`);
        this.clearToast();
      },
      error: (error: unknown) => {
        this.toastMessage.set(error instanceof Error ? error.message : 'The room status could not be changed.');
        this.clearToast();
      },
    });
  }

  createRoom(): void {
    if (this.roomForm.invalid) {
      this.roomForm.markAllAsTouched();
      return;
    }
    const details = this.roomForm.getRawValue();
    const room: Room = {
      id: Date.now(),
      name: details.name.trim(),
      capacity: details.capacity,
      location: details.location.trim(),
      floor: 'New space',
      equipment: details.equipment.split(',').map((item) => item.trim()).filter(Boolean),
      image: 'https://images.unsplash.com/photo-1497366754035-f200968a6e72?auto=format&fit=crop&w=900&q=85',
      accent: 'teal',
      active: true,
    };
    this.workspace.createRoom(room).subscribe({
      next: (created) => {
        this.roomForm.reset({ name: '', capacity: 8, location: '', equipment: 'Video conferencing, Whiteboard' });
        this.showRoomForm.set(false);
        this.toastMessage.set(`${created.name} has been added.`);
        this.clearToast();
      },
      error: (error: unknown) => {
        this.toastMessage.set(error instanceof Error ? error.message : 'The room could not be added.');
        this.clearToast();
      },
    });
  }

  createUser(): void {
    if (this.userForm.invalid) {
      this.userForm.markAllAsTouched();
      return;
    }
    const { password, ...details } = this.userForm.getRawValue();
    const initials = details.name.split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase();
    const user: RoomlyUser = { id: Date.now(), ...details, role: 'EMPLOYEE', initials };
    this.workspace.createUser(user, password).subscribe({
      next: (created) => {
        this.userForm.reset({ name: '', email: '', department: '', password: '' });
        this.showUserForm.set(false);
        this.toastMessage.set(`${created.name} has been added to the workspace.`);
        this.clearToast();
      },
      error: (error: unknown) => {
        this.toastMessage.set(error instanceof Error ? error.message : 'The team member could not be added.');
        this.clearToast();
      },
    });
  }

  toggleUserRole(user: RoomlyUser): void {
    this.workspace.setUserRole(user).subscribe({
      error: (error: unknown) => {
        this.toastMessage.set(error instanceof Error ? error.message : 'The team member role could not be changed.');
        this.clearToast();
      },
    });
  }

  markAllNotificationsRead(): void {
    this.workspace.markAllAsRead().subscribe({
      next: () => {
        this.toastMessage.set('You’re all caught up.');
        this.clearToast();
      },
      error: (error: unknown) => {
        this.toastMessage.set(error instanceof Error ? error.message : 'Notifications could not be updated.');
        this.clearToast();
      },
    });
  }

  changeRole(role: 'EMPLOYEE' | 'ADMIN'): void {
    if (!this.auth.demoMode) {
      this.toastMessage.set('Your access level is managed by your workspace administrator.');
      this.clearToast();
      return;
    }
    this.auth.setRole(role);
    this.toastMessage.set(`You’re now viewing Roomly as an ${role.toLowerCase()}.`);
    this.clearToast();
  }

  exportReservations(): void {
    const columns = ['Meeting', 'Room', 'Host', 'Start', 'End', 'Attendees', 'Status'];
    const rows = this.allVisibleReservations().map((reservation) => [
      reservation.title,
      reservation.roomName,
      reservation.userName,
      reservation.startTime,
      reservation.endTime,
      String(reservation.attendeesCount),
      reservation.status,
    ]);
    const csv = [columns, ...rows]
      .map((row) => row.map((value) => `"${value.replaceAll('"', '""')}"`).join(','))
      .join('\r\n');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
    const link = document.createElement('a');
    link.href = url;
    link.download = 'roomly-reservations.csv';
    link.click();
    URL.revokeObjectURL(url);
    this.toastMessage.set('Your reservations report has been downloaded.');
    this.clearToast();
  }

  private clearToast(): void {
    window.setTimeout(() => this.toastMessage.set(''), 3200);
  }

  private parseTime(value: string): string {
    const match = /^(\d{1,2}):(\d{2})\s*(AM|PM)$/i.exec(value.trim());
    if (!match) return value;
    let hour = Number(match[1]) % 12;
    if (match[3].toUpperCase() === 'PM') hour += 12;
    return `${String(hour).padStart(2, '0')}:${match[2]}`;
  }
}
