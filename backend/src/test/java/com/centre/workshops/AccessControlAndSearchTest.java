package com.centre.workshops;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.centre.workshops.domain.Role;
import com.centre.workshops.domain.User;
import com.centre.workshops.domain.Workshop;
import com.centre.workshops.domain.WorkshopStatus;
import com.centre.workshops.repo.UserRepository;
import com.centre.workshops.repo.WorkshopRepository;
import com.centre.workshops.security.TokenService;
import com.centre.workshops.web.dto.Dtos.RegisterRequest;
import com.centre.workshops.web.dto.Dtos.RegistrationResponse;
import com.centre.workshops.web.dto.Dtos.WorkshopRequest;
import com.centre.workshops.web.dto.Dtos.WorkshopResponse;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AccessControlAndSearchTest {

    private final RestClient restClient;
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final WorkshopRepository workshopRepository;
    private final PasswordEncoder passwordEncoder;

    private String adminToken;
    private String managerToken;
    private String staffToken;

    @Autowired
    AccessControlAndSearchTest(@Value("${local.server.port}") int port,
                              TokenService tokenService,
                              UserRepository userRepository,
                              WorkshopRepository workshopRepository,
                              PasswordEncoder passwordEncoder) {
        this.restClient = RestClient.builder().baseUrl("http://localhost:" + port).build();
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.workshopRepository = workshopRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @BeforeEach
    void setup() {
        User admin = getOrCreateUser("test.admin@trainingcentre.lk", "Test Admin", Role.ADMIN);
        User manager = getOrCreateUser("test.manager@trainingcentre.lk", "Test Manager", Role.MANAGER);
        User staff = getOrCreateUser("test.staff@trainingcentre.lk", "Test Staff", Role.STAFF);

        adminToken = "Bearer " + tokenService.issue(admin).value();
        managerToken = "Bearer " + tokenService.issue(manager).value();
        staffToken = "Bearer " + tokenService.issue(staff).value();
    }

    private User getOrCreateUser(String email, String name, Role role) {
        return userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            User u = new User();
            u.setName(name);
            u.setEmail(email.toLowerCase());
            u.setPasswordHash(passwordEncoder.encode("Password@123"));
            u.setRole(role);
            u.setActive(true);
            return userRepository.save(u);
        });
    }

    @Test
    void unauthenticatedRequestsAreRejectedWith401() {
        var workshopsStatus = restClient.get()
                .uri("/api/workshops")
                .exchange((req, res) -> res.getStatusCode());
        assertEquals(HttpStatusCode.valueOf(401), workshopsStatus);

        var usersStatus = restClient.get()
                .uri("/api/users")
                .exchange((req, res) -> res.getStatusCode());
        assertEquals(HttpStatusCode.valueOf(401), usersStatus);
    }

    @Test
    void adminCanAccessUsersAndAuditButIsForbiddenFromWorkshops() {
        var usersStatus = restClient.get()
                .uri("/api/users")
                .header(HttpHeaders.AUTHORIZATION, adminToken)
                .exchange((req, res) -> res.getStatusCode());
        assertEquals(HttpStatusCode.valueOf(200), usersStatus);

        var auditStatus = restClient.get()
                .uri("/api/audit")
                .header(HttpHeaders.AUTHORIZATION, adminToken)
                .exchange((req, res) -> res.getStatusCode());
        assertEquals(HttpStatusCode.valueOf(200), auditStatus);

        var workshopsStatus = restClient.get()
                .uri("/api/workshops")
                .header(HttpHeaders.AUTHORIZATION, adminToken)
                .exchange((req, res) -> res.getStatusCode());
        assertEquals(HttpStatusCode.valueOf(403), workshopsStatus);

        WorkshopRequest newWorkshop = new WorkshopRequest(
                "ADM-1", "Test", "Tester", "Studio", "Desc",
                Instant.now().plus(Duration.ofDays(1)),
                Instant.now().plus(Duration.ofDays(1)).plus(Duration.ofHours(2)),
                10, WorkshopStatus.SCHEDULED);

        var createStatus = restClient.post()
                .uri("/api/workshops")
                .header(HttpHeaders.AUTHORIZATION, adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(newWorkshop)
                .exchange((req, res) -> res.getStatusCode());
        assertEquals(HttpStatusCode.valueOf(403), createStatus);
    }

    @Test
    void managerCanCreateAndSearchWorkshopsButCannotAccessUsersOrAudit() {
        var usersStatus = restClient.get()
                .uri("/api/users")
                .header(HttpHeaders.AUTHORIZATION, managerToken)
                .exchange((req, res) -> res.getStatusCode());
        assertEquals(HttpStatusCode.valueOf(403), usersStatus);

        var auditStatus = restClient.get()
                .uri("/api/audit")
                .header(HttpHeaders.AUTHORIZATION, managerToken)
                .exchange((req, res) -> res.getStatusCode());
        assertEquals(HttpStatusCode.valueOf(403), auditStatus);

        WorkshopRequest newWorkshop = new WorkshopRequest(
                "MGR-101", "Manager Workshop", "John Doe", "Studio 1", "Notes",
                Instant.now().plus(Duration.ofDays(2)),
                Instant.now().plus(Duration.ofDays(2)).plus(Duration.ofHours(2)),
                15, WorkshopStatus.SCHEDULED);

        WorkshopResponse created = restClient.post()
                .uri("/api/workshops")
                .header(HttpHeaders.AUTHORIZATION, managerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(newWorkshop)
                .retrieve()
                .body(WorkshopResponse.class);

        assertNotNull(created);
        assertEquals("MGR-101", created.code());

        var searchStatus = restClient.get()
                .uri("/api/workshops")
                .header(HttpHeaders.AUTHORIZATION, managerToken)
                .exchange((req, res) -> res.getStatusCode());
        assertEquals(HttpStatusCode.valueOf(200), searchStatus);
    }

    @Test
    void staffCanViewWorkshopsAndRegisterAttendeesButCannotCreateWorkshopsOrAccessUsers() {
        var usersStatus = restClient.get()
                .uri("/api/users")
                .header(HttpHeaders.AUTHORIZATION, staffToken)
                .exchange((req, res) -> res.getStatusCode());
        assertEquals(HttpStatusCode.valueOf(403), usersStatus);

        var auditStatus = restClient.get()
                .uri("/api/audit")
                .header(HttpHeaders.AUTHORIZATION, staffToken)
                .exchange((req, res) -> res.getStatusCode());
        assertEquals(HttpStatusCode.valueOf(403), auditStatus);

        WorkshopRequest newWorkshop = new WorkshopRequest(
                "STF-101", "Staff Workshop", "Jane Doe", "Studio 2", "Notes",
                Instant.now().plus(Duration.ofDays(1)),
                Instant.now().plus(Duration.ofDays(1)).plus(Duration.ofHours(2)),
                10, WorkshopStatus.SCHEDULED);

        var createStatus = restClient.post()
                .uri("/api/workshops")
                .header(HttpHeaders.AUTHORIZATION, staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(newWorkshop)
                .exchange((req, res) -> res.getStatusCode());
        assertEquals(HttpStatusCode.valueOf(403), createStatus);

        Workshop w = new Workshop();
        w.setCode("STF-REG-1");
        w.setTitle("Registration Target");
        w.setInstructor("Instructor");
        w.setStartsAt(Instant.now().plus(Duration.ofDays(1)));
        w.setEndsAt(Instant.now().plus(Duration.ofDays(1)).plus(Duration.ofHours(2)));
        w.setCapacity(5);
        w.setStatus(WorkshopStatus.SCHEDULED);
        w = workshopRepository.save(w);

        RegisterRequest regReq = new RegisterRequest("Attendee One", "attendee1@example.com");
        RegistrationResponse regRes = restClient.post()
                .uri("/api/workshops/" + w.getId() + "/registrations")
                .header(HttpHeaders.AUTHORIZATION, staffToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(regReq)
                .retrieve()
                .body(RegistrationResponse.class);

        assertNotNull(regRes);
        assertEquals("attendee1@example.com", regRes.attendeeEmail());
    }

    @Test
    void workshopSearchByStatusAndAvailableSeats() {
        Workshop fullWorkshop = new Workshop();
        fullWorkshop.setCode("FULL-99");
        fullWorkshop.setTitle("Full Workshop");
        fullWorkshop.setInstructor("Instructor Full");
        fullWorkshop.setStartsAt(Instant.now().plus(Duration.ofDays(3)));
        fullWorkshop.setEndsAt(Instant.now().plus(Duration.ofDays(3)).plus(Duration.ofHours(2)));
        fullWorkshop.setCapacity(2);
        fullWorkshop.setActiveCount(2);
        fullWorkshop.setStatus(WorkshopStatus.SCHEDULED);
        workshopRepository.save(fullWorkshop);

        Workshop openWorkshop = new Workshop();
        openWorkshop.setCode("OPEN-99");
        openWorkshop.setTitle("Open Workshop");
        openWorkshop.setInstructor("Instructor Open");
        openWorkshop.setStartsAt(Instant.now().plus(Duration.ofDays(4)));
        openWorkshop.setEndsAt(Instant.now().plus(Duration.ofDays(4)).plus(Duration.ofHours(2)));
        openWorkshop.setCapacity(10);
        openWorkshop.setActiveCount(2);
        openWorkshop.setStatus(WorkshopStatus.SCHEDULED);
        workshopRepository.save(openWorkshop);

        String searchOpen = restClient.get()
                .uri("/api/workshops?q=Open Workshop&hasSeats=true")
                .header(HttpHeaders.AUTHORIZATION, staffToken)
                .retrieve()
                .body(String.class);

        assertNotNull(searchOpen);
        assertTrue(searchOpen.contains("OPEN-99"));

        String searchFull = restClient.get()
                .uri("/api/workshops?q=Full Workshop&hasSeats=true")
                .header(HttpHeaders.AUTHORIZATION, staffToken)
                .retrieve()
                .body(String.class);

        assertNotNull(searchFull);
        assertFalse(searchFull.contains("FULL-99"));
    }
}
