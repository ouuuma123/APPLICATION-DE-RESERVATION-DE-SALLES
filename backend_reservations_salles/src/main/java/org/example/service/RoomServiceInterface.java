package org.example.service;

import org.example.model.Room;

import java.util.List;
import java.util.Optional;

public interface RoomServiceInterface {
    List<Room> getAllRooms();
    Optional<Room> getRoomById(Long id);
    Room createRoom(Room room);
    Room updateRoom(Long id, Room room);
    Room setRoomActive(Long id, boolean active);
    void deleteRoom(Long id);
    boolean checkAvailability(Long roomId);
    void lockRoomForBooking(Long roomId);
}
