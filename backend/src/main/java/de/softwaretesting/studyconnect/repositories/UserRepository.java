package de.softwaretesting.studyconnect.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import de.softwaretesting.studyconnect.models.User;

public interface UserRepository extends JpaRepository<User, Long> {
    
}
