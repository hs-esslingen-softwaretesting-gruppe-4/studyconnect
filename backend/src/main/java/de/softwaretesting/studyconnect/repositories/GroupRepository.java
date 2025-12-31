package de.softwaretesting.studyconnect.repositories;

import de.softwaretesting.studyconnect.models.Group;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

  @Query("select u.id from Group g join g.members u where g.id = :groupId")
  Optional<Set<Long>> findMemberIdsByGroupId(@Param("groupId") Long groupId);

  @Query("select u.id from Group g join g.admins u where g.id = :groupId")
  Optional<Set<Long>> findAdminIdsByGroupId(@Param("groupId") Long groupId);

  @Query(
      "select (count(a) > 0) from Group g join g.admins a where g.id = :groupId and a.id = :userId")
  boolean existsAdminByGroupIdAndUserId(
      @Param("groupId") Long groupId, @Param("userId") Long userId);

  Optional<List<Group>> findByIsPublicTrue();

  Optional<Group> findByName(String name);

  Optional<List<Group>> findByMembersId(Long userId);

  Optional<Group> findByInviteCode(String inviteCode);

  @Query(
      "select g from Group g where g.isPublic = true and lower(g.name) like lower(concat('%', :query, '%'))")
  Optional<List<Group>> searchPublicGroupsByName(@Param("query") String query);
}
