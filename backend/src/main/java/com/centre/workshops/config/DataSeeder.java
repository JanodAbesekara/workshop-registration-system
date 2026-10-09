package com.centre.workshops.config;

import com.centre.workshops.domain.Role;
import com.centre.workshops.domain.User;
import com.centre.workshops.domain.Workshop;
import com.centre.workshops.domain.WorkshopStatus;
import com.centre.workshops.repo.UserRepository;
import com.centre.workshops.repo.WorkshopRepository;
import com.centre.workshops.security.AuthUser;
import com.centre.workshops.service.RegistrationService;
import com.centre.workshops.web.dto.Dtos.CancelRequest;
import com.centre.workshops.web.dto.Dtos.RegisterRequest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String[] NAMES = {
            "Amali Jayasuriya", "Nuwan Perera", "Dilini Fernando", "Kasun Wijesinghe", "Tharushi Silva",
            "Ruwan Gunasekara", "Sanduni Rathnayake", "Chamod Bandara", "Ishara De Silva", "Lakmal Herath",
            "Hasini Karunaratne", "Pasindu Dissanayake", "Nimali Senanayake", "Yasiru Abeysekara", "Oshadi Weerasinghe",
            "Dinuka Amarasinghe", "Malsha Ekanayake", "Sahan Kumara", "Rashmi Liyanage", "Buddhika Jayawardena"};

    private final UserRepository users;
    private final WorkshopRepository workshops;
    private final RegistrationService registrations;
    private final PasswordEncoder encoder;

    @Value("${app.seed.admin-name}") private String adminName;
    @Value("${app.seed.admin-email}") private String adminEmail;
    @Value("${app.seed.admin-password}") private String adminPassword;
    @Value("${app.seed.demo-data}") private boolean demoData;

    public DataSeeder(UserRepository users, WorkshopRepository workshops,
                      RegistrationService registrations, PasswordEncoder encoder) {
        this.users = users;
        this.workshops = workshops;
        this.registrations = registrations;
        this.encoder = encoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!users.existsByEmailIgnoreCase(adminEmail)) {
            createUser(adminName, adminEmail, adminPassword, Role.ADMIN);
            log.info("Seeded admin account {}", adminEmail);
        }
        if (!demoData) {
            return;
        }
        User manager = ensureUser("Mahesh Manager", "manager@trainingcentre.lk", "Manager@12345", Role.MANAGER);
        User staff = ensureUser("Sithara Frontdesk", "staff@trainingcentre.lk", "Staff@12345", Role.STAFF);
        if (workshops.count() == 0) {
            seedWorkshops(new AuthUser(staff.getId(), staff.getEmail(), staff.getName(), staff.getRole()));
            log.info("Seeded demo workshops (manager={}, staff={})", manager.getEmail(), staff.getEmail());
        }
    }

    private User ensureUser(String name, String email, String password, Role role) {
        return users.findByEmailIgnoreCase(email).orElseGet(() -> createUser(name, email, password, role));
    }

    private User createUser(String name, String email, String password, Role role) {
        User u = new User();
        u.setName(name);
        u.setEmail(email.toLowerCase());
        u.setPasswordHash(encoder.encode(password));
        u.setRole(role);
        u.setActive(true);
        return users.save(u);
    }

    private void seedWorkshops(AuthUser staff) {
        Instant base = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC).plus(Duration.ofHours(4).plusMinutes(30));
        Workshop pottery = workshop("POT-101", "Beginner Pottery Wheel", "Nadeesha Perera", "Studio A", base.plus(Duration.ofDays(1)), 3, 12, WorkshopStatus.SCHEDULED);
        Workshop coding = workshop("COD-201", "Intro to Web Coding", "Kasun Fernando", "Lab 2", base.plus(Duration.ofDays(2)), 3, 20, WorkshopStatus.SCHEDULED);
        Workshop circuit = workshop("FIT-110", "Saturday Morning Circuit", "Ishara Silva", "Main Hall", base.plus(Duration.ofDays(3)), 1, 15, WorkshopStatus.SCHEDULED);
        workshop("POT-205", "Glaze Chemistry", "Nadeesha Perera", "Studio A", base.plus(Duration.ofDays(5)), 2, 10, WorkshopStatus.SCHEDULED);
        workshop("ART-120", "Watercolour Basics", "Piumi Rajapaksa", "Studio B", base.plus(Duration.ofDays(4)), 2, 14, WorkshopStatus.CANCELLED);
        Workshop data = workshop("COD-301", "Python for Data Work", "Kasun Fernando", "Lab 2", base.plus(Duration.ofDays(9)), 3, 18, WorkshopStatus.SCHEDULED);
        workshop("FIT-050", "Gentle Yoga", "Ishara Silva", "Main Hall", base.minus(Duration.ofDays(4)), 1, 20, WorkshopStatus.COMPLETED);

        fill(pottery, 11, staff);
        fill(coding, 6, staff);
        fill(circuit, 15, staff);
        fill(data, 4, staff);

        var first = registrations.register(coding.getId(), new RegisterRequest("Visitor Cancelled", "cancelled.visitor@example.com"), staff);
        registrations.cancel(coding.getId(), first.getId(), new CancelRequest("Schedule clash"), staff);
    }

    private Workshop workshop(String code, String title, String instructor, String location,
                              Instant start, int hours, int capacity, WorkshopStatus status) {
        Workshop w = new Workshop();
        w.setCode(code);
        w.setTitle(title);
        w.setInstructor(instructor);
        w.setLocation(location);
        w.setStartsAt(start);
        w.setEndsAt(start.plus(Duration.ofHours(hours)));
        w.setCapacity(capacity);
        w.setStatus(status);
        return workshops.save(w);
    }

    private void fill(Workshop w, int count, AuthUser staff) {
        for (int i = 0; i < count; i++) {
            String name = NAMES[i % NAMES.length];
            String email = name.toLowerCase().replace(' ', '.') + "." + i + "@example.com";
            registrations.register(w.getId(), new RegisterRequest(name, email), staff);
        }
    }
}
