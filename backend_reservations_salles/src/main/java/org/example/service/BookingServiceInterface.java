package org.example.service;

import org.example.dto.BookingRequestDTO;
import org.example.dto.BookingResponseDTO;
import org.example.dto.RoomAvailabilityDTO;
import org.example.model.Booking;
import org.example.model.Room;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingServiceInterface {
    List<Booking> getAllBookings();
    Optional<Booking> getBookingById(Long id);
    List<Booking> getBookingsByUserId(Long userId);
    BookingResponseDTO createBooking(BookingRequestDTO dto);
    BookingResponseDTO updateBooking(Long id, BookingRequestDTO dto);
    void cancelBooking(Long id);
    void deleteBooking(Long id);
    RoomAvailabilityDTO getAvailability(Long roomId, LocalDate date);
    List<Booking> getCalendarBookings(LocalDateTime from, LocalDateTime to, Long roomId);
    List<Room> getAvailableRooms(LocalDateTime startTime, LocalDateTime endTime, Integer attendeesCount, String equipment);
}
