package org.example.mapper;

import org.example.dto.BookingRequestDTO;
import org.example.dto.BookingResponseDTO;
import org.example.dto.RoomDTO;
import org.example.dto.UserDTO;
import org.example.model.Booking;
import org.example.model.Room;
import org.example.model.User;

public class BookingMapper {

    public static Booking toEntity(BookingRequestDTO dto, Room room, User user) {
        Booking booking = new Booking();
        booking.setTitle(dto.getTitle());
        booking.setRoom(room);
        booking.setUser(user);
        booking.setStartTime(dto.getStartTime());
        booking.setEndTime(dto.getEndTime());
        booking.setAttendeesCount(dto.getAttendeesCount());
        return booking;
    }

    public static BookingResponseDTO toDTO(Booking booking) {
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

    public static RoomDTO toDTO(Room room) {
        return new RoomDTO(
                room.getId(),
                room.getName(),
                room.getCapacity(),
                room.getLocation(),
                room.getEquipment(),
                room.getAmenities(),
                room.isActive()
        );
    }

    public static UserDTO toDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getDepartment(),
                user.getRoles().stream().map(org.example.model.Role::getName).toList()
        );
    }
}
