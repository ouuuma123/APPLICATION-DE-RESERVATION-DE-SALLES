export type UserRole = 'EMPLOYEE' | 'ADMIN';
export type BookingStatus = 'CONFIRMED' | 'PENDING' | 'CANCELLED';

export interface Room {
  id: number;
  name: string;
  capacity: number;
  location: string;
  floor: string;
  equipment: string[];
  image: string;
  accent: string;
  active: boolean;
}

export interface Reservation {
  id: number;
  title: string;
  roomId: number;
  roomName: string;
  userName: string;
  startTime: string;
  endTime: string;
  attendeesCount: number;
  status: BookingStatus;
}

export interface RoomlyUser {
  id: number;
  name: string;
  email: string;
  department: string;
  role: UserRole;
  initials: string;
}

export interface RoomlyNotification {
  id: number;
  title: string;
  message: string;
  type: 'booking' | 'reminder' | 'update';
  time: string;
  read: boolean;
}

export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: RoomlyUser;
  roles: string[];
}
