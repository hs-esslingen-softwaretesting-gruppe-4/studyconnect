package de.softwaretesting.studyconnect.repositories;

import de.softwaretesting.studyconnect.models.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {}
