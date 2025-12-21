package de.softwaretesting.studyconnect.repositories;

import de.softwaretesting.studyconnect.models.Group;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

  @Query("select u.id from Group g join g.members u where g.id = :groupId")
  Optional<List<Long>> findMemberIdsByGroupId(@Param("groupId") Long groupId);

  Optional<List<Group>> findByIsPublicTrue();

  Optional<Group> findByName(String name);

  Optional<List<Group>> findByMembersId(Long userId);
}
