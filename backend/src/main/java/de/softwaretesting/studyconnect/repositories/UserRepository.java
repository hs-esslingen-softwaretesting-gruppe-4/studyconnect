package de.softwaretesting.studyconnect.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.softwaretesting.studyconnect.models.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
}
