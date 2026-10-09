package com.centre.workshops.repo;

import com.centre.workshops.domain.Registration;
import com.centre.workshops.domain.RegistrationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    List<Registration> findByWorkshopIdOrderByRegisteredAtDescIdDesc(Long workshopId);

    List<Registration> findByWorkshopIdAndStatusOrderByRegisteredAtDescIdDesc(Long workshopId, RegistrationStatus status);

    Optional<Registration> findByIdAndWorkshopId(Long id, Long workshopId);
 
    Optional<Registration> findFirstByWorkshopIdAndStatusOrderByRegisteredAtAscIdAsc(Long workshopId, RegistrationStatus status);

    boolean existsByWorkshopIdAndAttendeeEmailIgnoreCaseAndStatus(Long workshopId, String email, RegistrationStatus status);

    long countByWorkshopIdAndStatus(Long workshopId, RegistrationStatus status);
}
