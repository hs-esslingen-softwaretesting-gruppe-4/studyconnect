package de.softwaretesting.studyconnect.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import de.softwaretesting.studyconnect.models.Task;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

	@Query("select u.id from Task t join t.assignees u where t.id = :taskId")
	List<Long> findAssigneeIdsByTaskId(@Param("taskId") Long taskId);

}
