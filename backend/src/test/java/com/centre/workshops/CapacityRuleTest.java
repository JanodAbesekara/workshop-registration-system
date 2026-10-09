package com.centre.workshops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.centre.workshops.domain.Registration;
import com.centre.workshops.domain.RegistrationStatus;
import com.centre.workshops.domain.Role;
import com.centre.workshops.domain.Workshop;
import com.centre.workshops.exception.ApiException;
import com.centre.workshops.repo.RegistrationRepository;
import com.centre.workshops.repo.WorkshopRepository;
import com.centre.workshops.security.AuthUser;
import com.centre.workshops.service.RegistrationService;
import com.centre.workshops.web.dto.Dtos.CancelRequest;
import com.centre.workshops.web.dto.Dtos.RegisterRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CapacityRuleTest {

    private static final AuthUser ACTOR = new AuthUser(1L, "staff@test.lk", "Test Staff", Role.STAFF);

    private final WorkshopRepository workshops;
    private final RegistrationRepository registrationRepo;
    private final RegistrationService registrations;

    @Autowired
    CapacityRuleTest(WorkshopRepository workshops,
                     RegistrationRepository registrationRepo,
                     RegistrationService registrations) {
        this.workshops = workshops;
        this.registrationRepo = registrationRepo;
        this.registrations = registrations;
    }

    private Workshop newWorkshop(String code, int capacity) {
        Workshop w = new Workshop();
        w.setCode(code);
        w.setTitle("Test " + code);
        w.setInstructor("Tester");
        w.setStartsAt(Instant.now().plus(Duration.ofDays(1)));
        w.setEndsAt(Instant.now().plus(Duration.ofDays(1)).plus(Duration.ofHours(2)));
        w.setCapacity(capacity);
        return workshops.save(w);
    }

    @Test
    void neverOverbooksWhenManyStaffRegisterAtTheSameMoment() throws Exception {
        int capacity = 15;
        int attempts = 40;
        Workshop w = newWorkshop("CONC-1", capacity);

        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            final int n = i;
            futures.add(pool.submit(() -> {
                start.await();
                try {
                    registrations.register(w.getId(), new RegisterRequest("Person " + n, "p" + n + "@example.com"), ACTOR);
                    accepted.incrementAndGet();
                } catch (ApiException full) {
                    rejected.incrementAndGet();
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> f : futures) {
            f.get(60, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertEquals(capacity, accepted.get());
        assertEquals(attempts - capacity, rejected.get());
        assertEquals(capacity, registrationRepo.countByWorkshopIdAndStatus(w.getId(), RegistrationStatus.ACTIVE));
        assertEquals(capacity, workshops.findById(w.getId()).orElseThrow().getActiveCount());
    }

    @Test
    void cancelFreesTheSeatButKeepsTheRecord() {
        Workshop w = newWorkshop("CANCEL-1", 1);
        Registration first = registrations.register(w.getId(), new RegisterRequest("A", "a@example.com"), ACTOR);
        assertThrows(ApiException.class,
                () -> registrations.register(w.getId(), new RegisterRequest("B", "b@example.com"), ACTOR));

        registrations.cancel(w.getId(), first.getId(), new CancelRequest("changed mind"), ACTOR);
        registrations.register(w.getId(), new RegisterRequest("B", "b@example.com"), ACTOR);

        assertEquals(2, registrations.history(w.getId(), null).size());
        assertEquals(1, registrations.history(w.getId(), RegistrationStatus.ACTIVE).size());
        assertEquals(1, workshops.findById(w.getId()).orElseThrow().getActiveCount());
    }

    @Test
    void sameEmailCannotHoldTwoActiveSeats() {
        Workshop w = newWorkshop("DUP-1", 5);
        registrations.register(w.getId(), new RegisterRequest("A", "dup@example.com"), ACTOR);
        assertThrows(ApiException.class,
                () -> registrations.register(w.getId(), new RegisterRequest("A again", "DUP@example.com"), ACTOR));
    }

    @Test
    void waitlistQueuesAttendeesAndPromotesNextInLineOnCancel() {
        Workshop w = newWorkshop("WAIT-1", 1);
        Registration seatHolder = registrations.register(w.getId(), new RegisterRequest("First", "first@example.com"), ACTOR);
        assertEquals(RegistrationStatus.ACTIVE, seatHolder.getStatus());

        // Full workshop rejects standard registration
        assertThrows(ApiException.class,
                () -> registrations.register(w.getId(), new RegisterRequest("Second", "second@example.com", false), ACTOR));

        // Joining waitlist succeeds
        Registration waitlisted = registrations.register(w.getId(), new RegisterRequest("Second", "second@example.com", true), ACTOR);
        assertEquals(RegistrationStatus.WAITLISTED, waitlisted.getStatus());
        assertEquals(1, workshops.findById(w.getId()).orElseThrow().getActiveCount());

        // Cancel first registration frees the seat and auto-promotes waitlisted attendee
        registrations.cancel(w.getId(), seatHolder.getId(), new CancelRequest("Cannot attend"), ACTOR);

        assertEquals(1, workshops.findById(w.getId()).orElseThrow().getActiveCount());
        Registration promoted = registrationRepo.findById(waitlisted.getId()).orElseThrow();
        assertEquals(RegistrationStatus.ACTIVE, promoted.getStatus());
    }
}
