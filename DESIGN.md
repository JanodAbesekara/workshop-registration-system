# System Design & Architecture Document

## 1. Business Context & Problem Solved
This system is designed for a community training centre with 3 locations and ~15 staff handling registrations by phone and in person. 

**The Core Problem Solved:**
Previously, shared spreadsheets resulted in overbooked workshops when multiple staff promised the last seat simultaneously ("Saturday morning chaos"), and cancellations failed to free seats or record accountability. This application completely eliminates overbooking via database row-level locking, enforces non-destructive cancellation histories, and automates seat reassignment through a waitlist queue.

---

## 2. Technology Stack Justification
- **Backend:** **Spring Boot 4 / Java 21 / Spring Data JPA / MySQL (InnoDB)**
  - *Why:* This is a transactional, data-integrity-critical application. MySQL InnoDB provides ACID compliance and true row-level locks (`SELECT ... FOR UPDATE`), which are required to prevent race conditions during concurrent phone registrations.
  - *Clean Architecture:* Standard Constructor Dependency Injection, pure POJO entities, and Java 17+ `record` DTOs (eliminating third-party bytecode manipulation like Lombok).
- **Frontend:** **React 19 / Vite / Redux Toolkit / Vanilla CSS**
  - *Why:* Redux Toolkit provides predictable client-side state for user sessions, workshop filtering, real-time seat tracking, and registration history. Pure Vanilla CSS avoids bulky build-time utility frameworks and provides a clean, responsive layout suited for non-technical front-desk staff.

---

## 3. Access Control & Permission Matrix
Enforced strictly on the backend using Spring Security method security (`@PreAuthorize`), denying any unlisted access by default:

| Action | Admin | Manager | Staff | Backend Enforcement |
|---|:---:|:---:|:---:|---|
| **Create accounts & set roles** | **Yes** | No | No | `@PreAuthorize("hasRole('ADMIN')")` |
| **Add & edit workshops** | No | **Yes** | No | `@PreAuthorize("hasRole('MANAGER')")` |
| **Register & cancel attendees** | No | **Yes** | **Yes** | `@PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")` |
| **View workshops & registration history** | No | **Yes** | **Yes** | `@PreAuthorize("hasAnyRole('MANAGER', 'STAFF')")` |

- **No Public Signup:** The initial Admin is seeded (`admin@trainingcentre.lk`). Admins create all other staff/manager accounts.
- **Immediate Role Revocation:** The user's status and role are evaluated directly against the database on authenticated requests, ensuring disabled accounts or role changes take effect immediately without waiting for token expiration.
- **OWASP Alignment:** Passwords hashed with BCrypt; all queries parameterized against SQL Injection (OWASP A03); stateless JWT tokens.

---

## 4. Concurrency & The Capacity Rule
> *“Two of us promising the last seat at the same time is exactly what we are trying to stop.”*

- **Pessimistic Locking:** Both `register()` and `cancel()` acquire a database row lock via `workshops.findByIdForUpdate(workshopId)` (`SELECT ... FOR UPDATE`).
- **Atomic Evaluation:** Under `READ_COMMITTED` transaction isolation, concurrent registration requests queue sequentially on the row lock. The second request immediately detects `activeCount >= capacity` and is rejected with `409 Conflict ("This workshop is full")`.
- **Editing Workshops:** Changing workshop capacity also acquires the row lock, ensuring capacity can never be lowered below the currently active attendee count.
- **Automated Concurrency Test:** `CapacityRuleTest` runs 40 concurrent registration attempts against a workshop with 15 seats. Exactly 15 succeed, 25 are rejected, and zero overbooking occurs.

---

## 5. Registration History & Non-Destructive Cancellations
- **Immutable Audit History:** Registration records are **never deleted**.
- **Cancellations:** Cancelling updates the registration status to `CANCELLED`, records the cancelling staff member's ID/name (`cancelledByName`), records the exact timestamp, and saves the optional reason.
- **Seat Reclamation:** Cancelling immediately decrements `activeCount` to free the seat for another attendee (or auto-promotes from the waitlist).

---

## 6. Workshop Catalogue & Search
- Tracks all required fields: Workshop code, Title, Instructor, Date & time, Capacity, and Status (`SCHEDULED`, `CANCELLED`, `COMPLETED`).
- **Additional useful fields tracked:** Location (covering the 3 centres), Description, End time (ensures registration closes automatically once a workshop ends), and live available seats calculation.
- **Search & Filters:** Real-time search by keyword (code, title, instructor), status, date range (from/to), and a dedicated *"Only with available seats"* filter.

---

## 7. Bonus Features Implemented
1. **Audit Trail:**
   - Full history of administrative and catalogue modifications (workshop creations/edits, user account creation, role changes, deactivations, and password resets).
   - Dedicated Admin-accessible Audit Log API (`/api/audit`) and Frontend Audit Trail screen.
2. **Waitlist with Auto-Promotion:**
   - When a workshop reaches full capacity, front desk staff can place attendees onto a FIFO Waitlist queue (`WAITLISTED` status).
   - When an active registration is cancelled, the system automatically offers and promotes the first waitlisted attendee to `ACTIVE` status without manual staff intervention.

---

## 8. Trade-Offs & Skipped Items
- **Stored Counter vs Dynamic Count:** `activeCount` is stored as a synchronized column on the workshop table updated strictly under row lock, providing $O(1)$ fast lookups during heavy catalogue browsing.
- **Skipped (as per 3-hour constraint):** Email dispatching service (attendee emails are collected but not emailed), self-service password reset (handled by Admin as per staff model), refresh token rotation, and public cloud deployment.
