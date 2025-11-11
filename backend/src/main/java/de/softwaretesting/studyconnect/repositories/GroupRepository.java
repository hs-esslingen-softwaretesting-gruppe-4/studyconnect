
package de.softwaretesting.studyconnect.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import de.softwaretesting.studyconnect.models.Group;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

	@Query("select u.id from Group g join g.members u where g.id = :groupId")
	Optional<List<Long>> findMemberIdsByGroupId(@Param("groupId") Long groupId);

	Optional<Group> findByName(String name);

}