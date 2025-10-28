
package de.softwaretesting.studyconnect.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import de.softwaretesting.studyconnect.models.Group;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long>{
    
}
