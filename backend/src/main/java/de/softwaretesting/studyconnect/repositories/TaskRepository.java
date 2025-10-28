package de.softwaretesting.studyconnect.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.softwaretesting.studyconnect.models.Task;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

}
