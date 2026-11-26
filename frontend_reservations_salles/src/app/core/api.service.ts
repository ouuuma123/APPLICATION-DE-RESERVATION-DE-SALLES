import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';
import { BookingStatus, Reservation, Room, RoomlyNotification, RoomlyUser, TokenResponse, UserRole } from './models';

interface ApiRoomDTO {
  id: number;
  name: string;
  capacity: number;
  location: string;
  equipment: string;
  amenities: string[];
  active: boolean;
}

interface ApiUserDTO {
  id: number;
  name: string;
  email: string;
  department: string;
  roles: string[];
}

interface ApiTokenResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: ApiUserDTO;
  roles: string[];
}

interface ApiReservationDTO {
  id: number;
  title: string;
  roomId: number;
  roomName: string;
  userId: number;
  userName: string;
  startTime: string;
  endTime: string;
  attendeesCount: number;
  status: string;
  createdAt: string;
}

interface ApiNotificationDTO {
  id: number;
  type: string;
  message: string;
  read: boolean;
  createdAt: string;
  bookingId: number | null;
}

const toUser = (user: ApiUserDTO): RoomlyUser => {
  const role: UserRole = user.roles.includes('ADMIN') ? 'ADMIN' : 'EMPLOYEE';
  return {
    id: user.id,
    name: user.name,
    email: user.email,
    department: user.department,
    role,
    initials: user.name.split(/\s+/).slice(0, 2).map((part) => part[0] ?? '').join('').toUpperCase(),
  };
};

const roomImages = [
  'https://images.unsplash.com/photo-1497366754035-f200968a6e72?auto=format&fit=crop&w=900&q=85',
  'https://images.unsplash.com/photo-1497366811353-6870744d04b2?auto=format&fit=crop&w=900&q=85',
  'https://images.unsplash.com/photo-1497366216548-37526070297c?auto=format&fit=crop&w=900&q=85',
];

const toRoom = (room: ApiRoomDTO): Room => {
  const equipment = room.equipment.split(',').map((item) => item.trim()).filter(Boolean);
  return {
    ...room,
    floor: room.location.match(/\bfloor\s+\w+/i)?.[0] ?? 'Workspace',
    equipment: [...new Set([...equipment, ...(room.amenities ?? [])])],
    image: roomImages[(room.id - 1) % roomImages.length],
    accent: 'teal',
  };
};

const toReservation = (booking: ApiReservationDTO): Reservation => {
  const allowedStatuses: BookingStatus[] = ['CONFIRMED', 'PENDING', 'CANCELLED'];
  const status = allowedStatuses.find((value) => value === booking.status) ?? 'PENDING';
  return { ...booking, status };
};

const toNotification = (notification: ApiNotificationDTO): RoomlyNotification => {
  const type: RoomlyNotification['type'] =
    notification.type.toLowerCase().includes('remind') ? 'reminder'
      : notification.type.toLowerCase().includes('room') || notification.type.toLowerCase().includes('update') ? 'update'
        : 'booking';
  return {
    id: notification.id,
    title: type === 'reminder' ? 'A friendly reminder'
      : type === 'update' ? 'A workspace update'
        : 'Reservation update',
    message: notification.message,
    type,
    time: new Date(notification.createdAt).toLocaleString(),
    read: notification.read,
  };
};

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest extends LoginRequest {
  name: string;
  department: string;
}

export interface CreateRoomRequest {
  name: string;
  capacity: number;
  location: string;
  equipment: string;
  amenities: string[];
}

export interface BookingRequest {
  title: string;
  roomId: number;
  startTime: string;
  endTime: string;
  attendeesCount: number;
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = API_BASE_URL;

  login(credentials: LoginRequest): Observable<TokenResponse> {
    return this.http.post<ApiTokenResponse>(`${this.base}/auth/login`, credentials).pipe(
      map((response) => ({ ...response, user: toUser(response.user) })),
    );
  }

  register(details: RegisterRequest): Observable<TokenResponse> {
    return this.http.post<ApiTokenResponse>(`${this.base}/auth/register`, details).pipe(
      map((response) => ({ ...response, user: toUser(response.user) })),
    );
  }

  getRooms(): Observable<Room[]> {
    return this.http.get<ApiRoomDTO[]>(`${this.base}/rooms`).pipe(map((rooms) => rooms.map(toRoom)));
  }

  getRoom(id: number): Observable<Room> {
    return this.http.get<ApiRoomDTO>(`${this.base}/rooms/${id}`).pipe(map(toRoom));
  }

  createRoom(room: CreateRoomRequest): Observable<Room> {
    return this.http.post<ApiRoomDTO>(`${this.base}/rooms`, room).pipe(map(toRoom));
  }

  setRoomActive(id: number, active: boolean): Observable<Room> {
    return this.http.patch<ApiRoomDTO>(`${this.base}/rooms/${id}/active`, { active }).pipe(map(toRoom));
  }

  getReservations(): Observable<Reservation[]> {
    return this.http.get<ApiReservationDTO[]>(`${this.base}/bookings`).pipe(
      map((bookings) => bookings.map(toReservation)),
    );
  }

  createReservation(request: BookingRequest): Observable<Reservation> {
    return this.http.post<ApiReservationDTO>(`${this.base}/bookings`, request).pipe(map(toReservation));
  }

  cancelReservation(id: number): Observable<void> {
    return this.http.patch<void>(`${this.base}/bookings/${id}/cancel`, {});
  }

  getNotifications(): Observable<RoomlyNotification[]> {
    return this.http.get<ApiNotificationDTO[]>(`${this.base}/notifications`).pipe(
      map((notifications) => notifications.map(toNotification)),
    );
  }

  markNotificationAsRead(id: number): Observable<RoomlyNotification> {
    return this.http.patch<ApiNotificationDTO>(`${this.base}/notifications/${id}/read`, {}).pipe(
      map(toNotification),
    );
  }

  getUsers(): Observable<RoomlyUser[]> {
    return this.http.get<ApiUserDTO[]>(`${this.base}/users`).pipe(map((users) => users.map(toUser)));
  }

  createUser(details: RegisterRequest): Observable<RoomlyUser> {
    return this.http.post<ApiUserDTO>(`${this.base}/users`, details).pipe(map(toUser));
  }

  setUserRole(id: number, role: UserRole): Observable<RoomlyUser> {
    return this.http.patch<ApiUserDTO>(`${this.base}/users/${id}/role`, { role }).pipe(map(toUser));
  }

  getAvailableRooms(startTime: string, endTime: string, attendeesCount: number): Observable<Room[]> {
    const params = new HttpParams()
      .set('startTime', startTime)
      .set('endTime', endTime)
      .set('attendeesCount', attendeesCount);
    return this.http.get<ApiRoomDTO[]>(`${this.base}/rooms/available`, { params }).pipe(
      map((rooms) => rooms.map(toRoom)),
    );
  }
}
