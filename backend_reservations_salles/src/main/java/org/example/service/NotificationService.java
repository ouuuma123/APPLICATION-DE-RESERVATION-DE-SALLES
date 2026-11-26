package org.example.service;

import org.example.model.Booking;
import org.example.model.Notification;
import org.example.repository.BookingRepository;
import org.example.repository.NotificationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final BookingRepository bookingRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            BookingRepository bookingRepository) {
        this.notificationRepository = notificationRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public void notifyBookingCreated(Booking booking) {
        notify(booking, "BOOKING_CONFIRMED", "Votre réservation « " + booking.getTitle() + " » est confirmée.");
    }

    @Transactional
    public void notifyBookingUpdated(Booking booking) {
        notify(booking, "BOOKING_UPDATED", "Votre réservation « " + booking.getTitle() + " » a été modifiée.");
    }

    @Transactional
    public void notifyBookingCancelled(Booking booking) {
        notify(booking, "BOOKING_CANCELLED", "Votre réservation « " + booking.getTitle() + " » a été annulée.");
    }

    public List<Notification> getForUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public Notification markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .filter(item -> item.getUser().getId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    @Scheduled(fixedDelayString = "${app.notifications.reminder-check-ms:60000}")
    @Transactional
    public void createUpcomingReminders() {
        LocalDateTime now = LocalDateTime.now();
        for (Booking booking : bookingRepository.findUpcomingBookings(now, now.plusMinutes(15))) {
            Long userId = booking.getUser().getId();
            if (!notificationRepository.existsByBookingIdAndUserIdAndType(
                    booking.getId(), userId, "BOOKING_REMINDER")) {
                notify(booking, "BOOKING_REMINDER",
                        "Rappel : votre réservation « " + booking.getTitle() + " » commence à "
                                + booking.getStartTime() + ".");
            }
        }
    }

    private void notify(Booking booking, String type, String message) {
        Notification notification = new Notification();
        notification.setType(type);
        notification.setMessage(message);
        notification.setUser(booking.getUser());
        notification.setBooking(booking);
        notificationRepository.save(notification);
    }
}
