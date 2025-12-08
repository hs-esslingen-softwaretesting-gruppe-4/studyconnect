package de.softwaretesting.studyconnect.repositories;

import de.softwaretesting.studyconnect.models.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByKeycloakUUID(String keycloakUUID);

  Optional<User> findByEmail(String email);
}
