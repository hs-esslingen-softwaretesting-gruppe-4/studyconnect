package de.softwaretesting.studyconnect.repositories;

import de.softwaretesting.studyconnect.models.Task;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

  @Query("select u.id from Task t join t.assignees u where t.id = :taskId")
  List<Long> findAssigneeIdsByTaskId(@Param("taskId") Long taskId);

  @Query(
      """
      SELECT DISTINCT t
      FROM Task t
      LEFT JOIN FETCH t.assignees
      LEFT JOIN FETCH t.createdBy
      LEFT JOIN FETCH t.group
      WHERE t.group.id = :groupId
      """)
  List<Task> findByGroupId(@Param("groupId") Long groupId);

  @Query(
      """
      SELECT DISTINCT t
      FROM Task t
      JOIN t.assignees aFilter
      LEFT JOIN FETCH t.assignees
      LEFT JOIN FETCH t.createdBy
      LEFT JOIN FETCH t.group
      WHERE aFilter.id = :userId
      """)
  List<Task> findByAssigneesId(@Param("userId") Long userId);
}
