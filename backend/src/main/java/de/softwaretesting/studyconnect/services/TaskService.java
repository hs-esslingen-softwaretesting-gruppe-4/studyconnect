package de.softwaretesting.studyconnect.services;

import org.springframework.stereotype.Service;

import de.softwaretesting.studyconnect.mappers.TaskRequestMapper;
import de.softwaretesting.studyconnect.repositories.GroupRepository;
import de.softwaretesting.studyconnect.repositories.TaskRepository;
import de.softwaretesting.studyconnect.repositories.UserRepository;
import lombok.AllArgsConstructor;

/**
 * Service class for managing tasks within the StudyConnect application.
 */
@Service
@AllArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final TaskRequestMapper taskRequestMapper;
    
}
