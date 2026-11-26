import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { WorkspaceService } from './workspace.service';

describe('WorkspaceService', () => {
  let workspace: WorkspaceService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [WorkspaceService, provideHttpClient(), provideRouter([])],
    });
    workspace = TestBed.inject(WorkspaceService);
  });

  it('creates a reservation with the selected room details', () => {
    const startTime = new Date(Date.now() + 60 * 60 * 1000).toISOString().slice(0, 16);
    const endTime = new Date(Date.now() + 2 * 60 * 60 * 1000).toISOString().slice(0, 16);

    workspace.createReservation({
      title: 'Planning session',
      roomId: 1,
      startTime,
      endTime,
      attendeesCount: 6,
    }, 'Sophie Martin');

    const reservation = workspace.reservations()[0];
    expect(reservation.title).toBe('Planning session');
    expect(reservation.roomName).toBe('The Atrium');
    expect(reservation.userName).toBe('Sophie Martin');
    expect(reservation.status).toBe('CONFIRMED');
  });

  it('marks reservations as cancelled and keeps them in the reservation history', () => {
    workspace.cancelReservation(1042);

    expect(workspace.reservations().find((reservation) => reservation.id === 1042)?.status).toBe('CANCELLED');
  });

  it('updates individual and all notification read states', () => {
    workspace.markAsRead(1);
    expect(workspace.unreadCount()).toBe(2);

    workspace.markAllAsRead();
    expect(workspace.unreadCount()).toBe(0);
  });
});
