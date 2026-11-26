package org.example.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.example.model.BookingStatus;
import org.example.model.Room;
import org.example.repository.BookingRepository;
import org.example.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class RoomServiceImpl implements RoomServiceInterface {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private Validator validator;

    @Override
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    @Override
    public Optional<Room> getRoomById(Long id) {
        return roomRepository.findById(id);
    }

    @Override
    public Room createRoom(Room room) {
        if (room.getName() == null || room.getName().isBlank()) {
            throw new IllegalArgumentException("Room name is required");
        }

        Set<ConstraintViolation<Room>> violations = validator.validate(room);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(violations.iterator().next().getMessage());
        }

        if (roomRepository.existsByName(room.getName())) {
            throw new IllegalArgumentException("Room name must be unique");
        }

        return roomRepository.save(room);
    }

    @Override
    public Room updateRoom(Long id, Room room) {
        Room existing = roomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with id: " + id));
        if (room.getName() == null || room.getName().isBlank()) {
            throw new IllegalArgumentException("Room name is required");
        }
        Set<ConstraintViolation<Room>> violations = validator.validate(room);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(violations.iterator().next().getMessage());
        }
        if (roomRepository.existsByNameAndIdNot(room.getName(), id)) {
            throw new IllegalArgumentException("Room name must be unique");
        }
        existing.setName(room.getName());
        existing.setCapacity(room.getCapacity());
        existing.setLocation(room.getLocation());
        existing.setEquipment(room.getEquipment());
        existing.setAmenities(room.getAmenities() == null ? new java.util.HashSet<>() : room.getAmenities());
        existing.setActive(room.isActive());
        return roomRepository.save(existing);
    }

    public Room setRoomActive(Long id, boolean active) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with id: " + id));
        room.setActive(active);
        return roomRepository.save(room);
    }

    @Override
    public void deleteRoom(Long id) {
        if (bookingRepository.existsByRoomIdAndStatus(id, BookingStatus.CONFIRMED)) {
            throw new IllegalStateException("Cannot delete room with active bookings");
        }
        roomRepository.deleteById(id);
    }

    @Override
    public boolean checkAvailability(Long roomId) {
        return roomRepository.existsById(roomId);
    }

    @Override
    public void lockRoomForBooking(Long roomId) {
        roomRepository.findByIdForUpdate(roomId)
                .orElseThrow(() -> new org.example.exception.RoomNotFoundException(roomId));
    }
}
