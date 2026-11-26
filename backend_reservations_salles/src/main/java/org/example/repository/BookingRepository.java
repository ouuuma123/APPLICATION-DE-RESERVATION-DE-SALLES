package org.example.repository;

import org.example.model.Booking;
import org.example.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);
    List<Booking> findByRoomIdAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
            Long roomId, BookingStatus status, LocalDateTime endTime, LocalDateTime startTime);

    @Query("SELECT b FROM Booking b WHERE b.status = 'CONFIRMED' " +
           "AND b.startTime < :to AND b.endTime > :from " +
           "AND (:roomId IS NULL OR b.room.id = :roomId) ORDER BY b.startTime")
    List<Booking> findCalendarBookings(@Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to,
                                       @Param("roomId") Long roomId);

    @Query("SELECT b FROM Booking b WHERE b.status = 'CONFIRMED' " +
           "AND b.startTime > :now AND b.startTime <= :until")
    List<Booking> findUpcomingBookings(@Param("now") LocalDateTime now,
                                       @Param("until") LocalDateTime until);
    boolean existsByRoomIdAndStatus(Long roomId, BookingStatus status);
    boolean existsByUserIdAndStatus(Long userId, BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.room.id = :roomId " +
           "AND b.status = 'CONFIRMED' " +
           "AND b.id != :excludeId " +
           "AND b.startTime < :endTime AND b.endTime > :startTime")
    List<Booking> findConflictingBookings(@Param("roomId") Long roomId,
                                           @Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime,
                                           @Param("excludeId") Long excludeId);
}
