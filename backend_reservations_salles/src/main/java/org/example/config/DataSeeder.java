package org.example.config;

import org.example.model.Booking;
import org.example.model.BookingStatus;
import org.example.model.Role;
import org.example.model.Room;
import org.example.model.User;
import org.example.repository.BookingRepository;
import org.example.repository.RoleRepository;
import org.example.repository.RoomRepository;
import org.example.repository.UserRepository;
import org.example.service.AuthService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataSeeder implements CommandLineRunner {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(RoomRepository roomRepository, UserRepository userRepository, 
                      BookingRepository bookingRepository, RoleRepository roleRepository,
                      PasswordEncoder passwordEncoder) {
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Check if data already exists
        if (userRepository.count() > 0) {
            return; // Data already seeded
        }

        // Seed Roles
        Role clientRole = new Role("CLIENT");
        roleRepository.save(clientRole);

        Role employeeRole = new Role("EMPLOYEE");
        roleRepository.save(employeeRole);

        Role adminRole = new Role("ADMIN");
        roleRepository.save(adminRole);

        // Seed Rooms
        Room room1 = new Room();
        room1.setName("Conference Room A");
        room1.setCapacity(20);
        room1.setLocation("Building A, Floor 1");
        room1.setEquipment("Projector, Whiteboard, Video Conferencing");
        roomRepository.save(room1);

        Room room2 = new Room();
        room2.setName("Meeting Room B");
        room2.setCapacity(8);
        room2.setLocation("Building A, Floor 2");
        room2.setEquipment("TV Screen, Whiteboard");
        roomRepository.save(room2);

        Room room3 = new Room();
        room3.setName("Training Room");
        room3.setCapacity(30);
        room3.setLocation("Building B, Floor 1");
        room3.setEquipment("Projector, Sound System, 30 Laptops");
        roomRepository.save(room3);

        Room room4 = new Room();
        room4.setName("Executive Boardroom");
        room4.setCapacity(12);
        room4.setLocation("Building C, Floor 5");
        room4.setEquipment("Video Conferencing, Smart Board, Catering Service");
        roomRepository.save(room4);

        // Seed Users
        User user1 = new User();
        user1.setName("Jean Dupont");
        user1.setEmail("jean.dupont@company.com");
        user1.setPassword(passwordEncoder.encode("password123"));
        user1.setDepartment("Engineering");
        user1.addRole(clientRole);
        user1.addRole(employeeRole);
        userRepository.save(user1);

        User user2 = new User();
        user2.setName("Marie Martin");
        user2.setEmail("marie.martin@company.com");
        user2.setPassword(passwordEncoder.encode("password123"));
        user2.setDepartment("Marketing");
        user2.addRole(clientRole);
        user2.addRole(employeeRole);
        userRepository.save(user2);

        User user3 = new User();
        user3.setName("Pierre Bernard");
        user3.setEmail("pierre.bernard@company.com");
        user3.setPassword(passwordEncoder.encode("password123"));
        user3.setDepartment("Human Resources");
        user3.addRole(clientRole);
        user3.addRole(employeeRole);
        userRepository.save(user3);

        User user4 = new User();
        user4.setName("Sophie Petit");
        user4.setEmail("sophie.petit@company.com");
        user4.setPassword(passwordEncoder.encode("password123"));
        user4.setDepartment("Sales");
        user4.addRole(clientRole);
        user4.addRole(employeeRole);
        userRepository.save(user4);

        User user5 = new User();
        user5.setName("Luc Dubois");
        user5.setEmail("luc.dubois@company.com");
        user5.setPassword(passwordEncoder.encode("password123"));
        user5.setDepartment("Finance");
        user5.addRole(clientRole);
        user5.addRole(employeeRole);
        userRepository.save(user5);

        // Create an admin user
        User adminUser = new User();
        adminUser.setName("Admin User");
        adminUser.setEmail("admin@company.com");
        adminUser.setPassword(passwordEncoder.encode("admin123"));
        adminUser.setDepartment("IT");
        adminUser.addRole(adminRole);
        adminUser.addRole(employeeRole);
        adminUser.addRole(clientRole);
        userRepository.save(adminUser);

        // Seed Bookings
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.plusDays(1);
        LocalDateTime dayAfterTomorrow = now.plusDays(2);

        Booking booking1 = new Booking();
        booking1.setTitle("Weekly Team Standup");
        booking1.setRoom(room1);
        booking1.setUser(user1);
        booking1.setStartTime(tomorrow.withHour(9).withMinute(0).withSecond(0));
        booking1.setEndTime(tomorrow.withHour(9).withMinute(30).withSecond(0));
        booking1.setAttendeesCount(15);
        booking1.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking1);

        Booking booking2 = new Booking();
        booking2.setTitle("Client Presentation");
        booking2.setRoom(room4);
        booking2.setUser(user2);
        booking2.setStartTime(tomorrow.withHour(14).withMinute(0).withSecond(0));
        booking2.setEndTime(tomorrow.withHour(16).withMinute(0).withSecond(0));
        booking2.setAttendeesCount(8);
        booking2.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking2);

        Booking booking3 = new Booking();
        booking3.setTitle("Training Session");
        booking3.setRoom(room3);
        booking3.setUser(user3);
        booking3.setStartTime(dayAfterTomorrow.withHour(10).withMinute(0).withSecond(0));
        booking3.setEndTime(dayAfterTomorrow.withHour(12).withMinute(0).withSecond(0));
        booking3.setAttendeesCount(25);
        booking3.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking3);

        Booking booking4 = new Booking();
        booking4.setTitle("Budget Review Meeting");
        booking4.setRoom(room2);
        booking4.setUser(user5);
        booking4.setStartTime(tomorrow.withHour(11).withMinute(0).withSecond(0));
        booking4.setEndTime(tomorrow.withHour(12).withMinute(0).withSecond(0));
        booking4.setAttendeesCount(5);
        booking4.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking4);

        // Add a cancelled booking for testing
        Booking cancelledBooking = new Booking();
        cancelledBooking.setTitle("Old Planning Meeting");
        cancelledBooking.setRoom(room1);
        cancelledBooking.setUser(user4);
        cancelledBooking.setStartTime(now.minusDays(2).withHour(10).withMinute(0).withSecond(0));
        cancelledBooking.setEndTime(now.minusDays(2).withHour(11).withMinute(0).withSecond(0));
        cancelledBooking.setAttendeesCount(10);
        cancelledBooking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(cancelledBooking);

        System.out.println("===========================================");
        System.out.println("DATABASE SEEDED WITH TEST DATA");
        System.out.println("===========================================");
        System.out.println("Rooms: " + roomRepository.count());
        System.out.println("Users: " + userRepository.count());
        System.out.println("Bookings: " + bookingRepository.count());
        System.out.println("===========================================");
    }
}