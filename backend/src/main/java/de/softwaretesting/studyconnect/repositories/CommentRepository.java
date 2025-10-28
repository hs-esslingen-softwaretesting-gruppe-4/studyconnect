package de.softwaretesting.studyconnect.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import de.softwaretesting.studyconnect.models.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    
}
