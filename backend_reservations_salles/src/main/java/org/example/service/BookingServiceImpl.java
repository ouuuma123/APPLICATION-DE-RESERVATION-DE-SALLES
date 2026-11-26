package org.example.service;

import org.example.dto.BookingRequestDTO;
import org.example.dto.BookingResponseDTO;
import org.example.dto.RoomAvailabilityDTO;
import org.example.dto.TimeSlotDTO;
import org.example.exception.BookingConflictException;
import org.example.exception.InvalidBookingException;
import org.example.exception.RoomNotFoundException;
import org.example.exception.UserNotFoundException;
import org.example.model.Booking;
import org.example.model.BookingStatus;
import org.example.model.Room;
import org.example.model.User;
import org.example.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookingServiceImpl implements BookingServiceInterface {

    private static final Duration MIN_DURATION = Duration.ofMinutes(15);
    private static final Duration MAX_DURATION = Duration.ofHours(4);

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private org.example.service.NotificationService notificationService;

    @Autowired
    private RoomServiceInterface roomService;

    @Autowired
    private UserServiceInterface userService;

    @Override
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    @Override
    public Optional<Booking> getBookingById(Long id) {
        return bookingRepository.findById(id);
    }

    @Override
    public List<Booking> getBookingsByUserId(Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public BookingResponseDTO createBooking(BookingRequestDTO dto) {
        return createOrUpdateBooking(dto, -1L);
    }

    @Override
    @Transactional
    public BookingResponseDTO updateBooking(Long id, BookingRequestDTO dto) {
        return createOrUpdateBooking(dto, id);
    }

    private BookingResponseDTO createOrUpdateBooking(BookingRequestDTO dto, Long excludeId) {
        validateBookingRequest(dto);

        roomService.lockRoomForBooking(dto.getRoomId());
        Room room = roomService.getRoomById(dto.getRoomId())
                .orElseThrow(() -> new RoomNotFoundException(dto.getRoomId()));
        if (!room.isActive()) {
            throw new InvalidBookingException("Room is inactive");
        }

        User user = userService.getUserById(dto.getUserId())
                .orElseThrow(() -> new UserNotFoundException(dto.getUserId()));

        validateBookingRules(dto, room);

        List<Booking> conflictingBookings = bookingRepository.findConflictingBookings(
                dto.getRoomId(),
                dto.getStartTime(),
                dto.getEndTime(),
                excludeId
        );

        if (!conflictingBookings.isEmpty()) {
            throw new BookingConflictException("Room is already booked for this time slot");
        }

        Booking booking = new Booking();
        booking.setTitle(dto.getTitle());
        booking.setRoom(room);
        booking.setUser(user);
        booking.setStartTime(dto.getStartTime());
        booking.setEndTime(dto.getEndTime());
        booking.setAttendeesCount(dto.getAttendeesCount());
        booking.setStatus(BookingStatus.CONFIRMED);

        if (excludeId != -1L) {
            booking.setId(excludeId);
        }

        Booking savedBooking = bookingRepository.save(booking);
        if (excludeId == -1L) {
            notificationService.notifyBookingCreated(savedBooking);
        } else {
            notificationService.notifyBookingUpdated(savedBooking);
        }
        return mapToResponseDTO(savedBooking);
    }

    private void validateBookingRequest(BookingRequestDTO dto) {
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            throw new InvalidBookingException("Title is required");
        }
        if (dto.getRoomId() == null) {
            throw new InvalidBookingException("Room is required");
        }
        if (dto.getUserId() == null) {
            throw new InvalidBookingException("User is required");
        }
        if (dto.getStartTime() == null || dto.getEndTime() == null) {
            throw new InvalidBookingException("Start time and end time are required");
        }
    }

    private void validateBookingRules(BookingRequestDTO dto, Room room) {
        if (dto.getStartTime().isAfter(dto.getEndTime())) {
            throw new InvalidBookingException("Start time must be before end time");
        }

        if (dto.getStartTime().isBefore(LocalDateTime.now())) {
            throw new InvalidBookingException("Start time must be in the future");
        }

        Duration duration = Duration.between(dto.getStartTime(), dto.getEndTime());
        if (duration.compareTo(MIN_DURATION) < 0) {
            throw new InvalidBookingException("Booking duration must be at least 15 minutes");
        }
        if (duration.compareTo(MAX_DURATION) > 0) {
            throw new InvalidBookingException("Booking duration must not exceed 4 hours");
        }

        if (dto.getAttendeesCount() != null && dto.getAttendeesCount() > room.getCapacity()) {
            throw new InvalidBookingException(
                    "Attendees count (" + dto.getAttendeesCount() + ") exceeds room capacity (" + room.getCapacity() + ")"
            );
        }
    }

    @Override
    public void cancelBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new InvalidBookingException("Booking not found with id: " + id));
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        notificationService.notifyBookingCancelled(booking);
    }

    @Override
    public void deleteBooking(Long id) {
        bookingRepository.deleteById(id);
    }

    @Override
    public RoomAvailabilityDTO getAvailability(Long roomId, LocalDate date) {
        Room room = roomService.getRoomById(roomId)
                .orElseThrow(() -> new RoomNotFoundException(roomId));

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<Booking> bookings = bookingRepository
                .findByRoomIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                        roomId, BookingStatus.CONFIRMED, endOfDay, startOfDay);

        List<TimeSlotDTO> occupiedSlots = bookings.stream()
                .map(b -> new TimeSlotDTO(
                        b.getStartTime().isBefore(startOfDay) ? startOfDay : b.getStartTime(),
                        b.getEndTime().isAfter(endOfDay) ? endOfDay : b.getEndTime()))
                .collect(Collectors.toList());

        List<TimeSlotDTO> availableSlots = calculateAvailableSlots(startOfDay, endOfDay, occupiedSlots);

        return new RoomAvailabilityDTO(
                room.getId(),
                room.getName(),
                room.getCapacity(),
                occupiedSlots,
                availableSlots
        );
    }

    @Override
    public List<Booking> getCalendarBookings(LocalDateTime from, LocalDateTime to, Long roomId) {
        if (from == null || to == null || !from.isBefore(to)) {
            throw new InvalidBookingException("Calendar range must have a start before its end");
        }
        return bookingRepository.findCalendarBookings(from, to, roomId);
    }

    @Override
    public List<Room> getAvailableRooms(
            LocalDateTime startTime, LocalDateTime endTime, Integer attendeesCount, String equipment) {
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new InvalidBookingException("Start time must be before end time");
        }
        return roomService.getAllRooms().stream()
                .filter(Room::isActive)
                .filter(room -> attendeesCount == null || room.getCapacity() >= attendeesCount)
                .filter(room -> equipment == null || equipment.isBlank()
                        || (room.getEquipment() != null
                        && room.getEquipment().toLowerCase().contains(equipment.toLowerCase()))
                        || (room.getAmenities() != null && room.getAmenities().stream()
                        .anyMatch(amenity -> amenity.equalsIgnoreCase(equipment))))
                .filter(room -> bookingRepository.findConflictingBookings(
                        room.getId(), startTime, endTime, -1L).isEmpty())
                .toList();
    }

    private List<TimeSlotDTO> calculateAvailableSlots(LocalDateTime dayStart, LocalDateTime dayEnd, List<TimeSlotDTO> occupiedSlots) {
        List<TimeSlotDTO> availableSlots = new ArrayList<>();

        if (occupiedSlots.isEmpty()) {
            availableSlots.add(new TimeSlotDTO(dayStart, dayEnd));
            return availableSlots;
        }

        occupiedSlots.sort((a, b) -> a.getStartTime().compareTo(b.getStartTime()));

        LocalDateTime currentStart = dayStart;

        for (TimeSlotDTO slot : occupiedSlots) {
            if (currentStart.isBefore(slot.getStartTime())) {
                availableSlots.add(new TimeSlotDTO(currentStart, slot.getStartTime()));
            }
            currentStart = slot.getEndTime();
        }

        if (currentStart.isBefore(dayEnd)) {
            availableSlots.add(new TimeSlotDTO(currentStart, dayEnd));
        }

        return availableSlots;
    }

    private BookingResponseDTO mapToResponseDTO(Booking booking) {
        return new BookingResponseDTO(
                booking.getId(),
                booking.getTitle(),
                booking.getRoom().getId(),
                booking.getRoom().getName(),
                booking.getUser().getId(),
                booking.getUser().getName(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getAttendeesCount(),
                booking.getStatus().name(),
                booking.getCreatedAt()
        );
    }
}
