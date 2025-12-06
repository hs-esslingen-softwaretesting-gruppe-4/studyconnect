package de.softwaretesting.studyconnect.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import de.softwaretesting.studyconnect.dtos.request.UserCreateRequestDTO;
import de.softwaretesting.studyconnect.dtos.request.UserUpdateRequestDTO;
import de.softwaretesting.studyconnect.dtos.response.UserResponseDTO;
import de.softwaretesting.studyconnect.mappers.response.UserResponseMapper;
import de.softwaretesting.studyconnect.mappers.request.UserRequestMapper;
import de.softwaretesting.studyconnect.exceptions.InternalServerErrorException;
import de.softwaretesting.studyconnect.exceptions.NotFoundException;
import de.softwaretesting.studyconnect.models.User;
import de.softwaretesting.studyconnect.repositories.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import de.softwaretesting.studyconnect.dtos.response.KeycloakUserResponseDTO;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserResponseMapper userResponseMapper;
    private final UserRequestMapper userRequestMapper;
    private final KeycloakService keycloakService;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    /**
     * Initializes the service by ensuring the Keycloak realm exists and syncing users.
     * This method is called after the service is constructed.
     */
    @PostConstruct
    public void init() {
        
        // Create the configured realm
        boolean created = keycloakService.createRealm();
        if (!created) {
            return;
        }

        // Retrieve all existing users in the realm and create their local representations if they don't exist
        List<KeycloakUserResponseDTO> users = keycloakService.retrieveAllUsersInRealm();
        for (KeycloakUserResponseDTO user : users) {
            Optional<User> existingUser = userRepository.findByKeycloakUUID(user.getKeycloakUUID());
            if (existingUser.isEmpty()) {
                // User doesn't exist locally, create it
                User newUser = new User();
                newUser.setKeycloakUUID(user.getKeycloakUUID());
                newUser.setEmail(user.getEmail());
                newUser.setSurname(user.getFirstName());
                newUser.setLastname(user.getLastName());
                userRepository.save(newUser);
                logger.info("Created local user for Keycloak user: {} - {}", user.getKeycloakUUID(), user.getUsername());
            }
        }
    }


    /**
     * Retrieves a user by ID and maps it to a UserResponseDTO.
     * 
     * @param userId the ID of the user to retrieve
     * @param taskRequestDTO the user request DTO containing user details
     * @return a ResponseEntity containing the user's response DTO
     * @throws NotFoundException if the user is not found
     */
    public ResponseEntity<UserResponseDTO> getUserById(Long userId) {
        // Map DTO to entity
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User with id " + userId + " not found"));

        // Return user
        UserResponseDTO userResponseDTO = userResponseMapper.toDto(user);
        return new ResponseEntity<>(userResponseDTO, HttpStatus.OK);
    }

    /**
     * Updates a user by ID with the provided UserRequestDTO.
     * 
     * @param userId the ID of the user to update
     * @param userUpdateRequestDTO the user request DTO containing updated user details
     * @return a ResponseEntity containing the updated user's response DTO
     * @throws NotFoundException if the user is not found
     */
    public ResponseEntity<UserResponseDTO> updateUserWithId(Long userId, UserUpdateRequestDTO userUpdateRequestDTO) {
        User existingUser = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        User updatedUser = userRequestMapper.toEntity(userUpdateRequestDTO);
        updatedUser.setId(existingUser.getId());
        updatedUser.setEmail(existingUser.getEmail());
        updatedUser.setCreatedAt(existingUser.getCreatedAt());

        User savedUser = userRepository.save(updatedUser);
        UserResponseDTO userResponseDTO = userResponseMapper.toDto(savedUser);
        return ResponseEntity.ok(userResponseDTO);
    }

    /**
     * Creates a new user with the provided UserCreateRequestDTO.
     * 
     * @param userCreateRequestDTO the user request DTO containing user details
     * @return a ResponseEntity containing the created user's response DTO
     */    
    public ResponseEntity<UserResponseDTO> createUser(UserCreateRequestDTO userCreateRequestDTO) {

        User newUser = new User();
        boolean successfulUserCreation = keycloakService.createUserInRealm(
            userCreateRequestDTO.getPassword(),
            userCreateRequestDTO.getEmail(),
            userCreateRequestDTO.getSurname(),
            userCreateRequestDTO.getLastname()
        );

        if (!successfulUserCreation) {
            throw new InternalServerErrorException("Internal error during user creation in Keycloak");
        }
        
        // On successful creation in Keycloak, create local user
        KeycloakUserResponseDTO createdKeycloakUser = keycloakService.retrieveUserByEmail(userCreateRequestDTO.getEmail());
        if (createdKeycloakUser == null) {
            throw new InternalServerErrorException("Internal error: Error retrieving created user from Keycloak");
        }

        newUser.setKeycloakUUID(createdKeycloakUser.getKeycloakUUID());
        newUser.setEmail(createdKeycloakUser.getEmail());
        newUser.setSurname(createdKeycloakUser.getFirstName());
        newUser.setLastname(createdKeycloakUser.getLastName());
        userRepository.save(newUser);

        UserResponseDTO userResponseDTO = userResponseMapper.toDto(newUser);
        return new ResponseEntity<>(userResponseDTO, HttpStatus.CREATED);
    }


    /**
     * Creates a new admin user for the application with the provided UserCreateRequestDTO.
     * 
     * @param userCreateRequestDTO the user request DTO containing admin user details
     * @return a ResponseEntity containing the created admin user's response DTO
     */
    public ResponseEntity<UserResponseDTO> createAdmin(UserCreateRequestDTO userCreateRequestDTO) {

        User newUser = new User();
        boolean successfulUserCreation = keycloakService.createAdminUserInRealm(
            userCreateRequestDTO.getPassword(),
            userCreateRequestDTO.getEmail(),
            userCreateRequestDTO.getSurname(),
            userCreateRequestDTO.getLastname()
        );

        if (!successfulUserCreation) {
            throw new InternalServerErrorException("Internal error during admin creation in Keycloak");
        }
        
        // On successful creation in Keycloak, create local user
        KeycloakUserResponseDTO createdKeycloakUser = keycloakService.retrieveUserByEmail(userCreateRequestDTO.getEmail());
        if (createdKeycloakUser == null) {
            throw new InternalServerErrorException("Internal error: Error retrieving created admin from Keycloak");
        }

        newUser.setKeycloakUUID(createdKeycloakUser.getKeycloakUUID());
        newUser.setEmail(createdKeycloakUser.getEmail());
        newUser.setSurname(createdKeycloakUser.getFirstName());
        newUser.setLastname(createdKeycloakUser.getLastName());
        userRepository.save(newUser);

        UserResponseDTO userResponseDTO = userResponseMapper.toDto(newUser);
        return new ResponseEntity<>(userResponseDTO, HttpStatus.CREATED);
    }
        
}
