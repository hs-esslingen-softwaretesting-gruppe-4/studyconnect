package de.softwaretesting.studyconnect.services;

import de.softwaretesting.studyconnect.dtos.request.UserCreateRequestDTO;
import de.softwaretesting.studyconnect.dtos.request.UserUpdateRequestDTO;
import de.softwaretesting.studyconnect.dtos.response.KeycloakUserResponseDTO;
import de.softwaretesting.studyconnect.dtos.response.UserResponseDTO;
import de.softwaretesting.studyconnect.exceptions.BadRequestException;
import de.softwaretesting.studyconnect.exceptions.ConflictException;
import de.softwaretesting.studyconnect.exceptions.InternalServerErrorException;
import de.softwaretesting.studyconnect.exceptions.NotFoundException;
import de.softwaretesting.studyconnect.mappers.request.UserRequestMapper;
import de.softwaretesting.studyconnect.mappers.response.UserResponseMapper;
import de.softwaretesting.studyconnect.models.User;
import de.softwaretesting.studyconnect.repositories.UserRepository;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final UserResponseMapper userResponseMapper;
  private final UserRequestMapper userRequestMapper;
  private final KeycloakService keycloakService;
  private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

  @Value("${keycloak.default-client-role}")
  private String defaultClientRole;

  @Value("${keycloak.default-admin-role}")
  private String defaultAdminRole;

  /**
   * Initializes the service by ensuring the Keycloak realm exists and syncing users. This method is
   * called after the service is constructed.
   */
  @PostConstruct
  public void init() {

    // Create the configured realm
    boolean created = keycloakService.createRealm();
    if (!created) {
      LOGGER.error("Failed to create Keycloak realm");
      return;
    }

    boolean rolesCreated =
        keycloakService.addRolesToRealm(List.of(defaultClientRole, defaultAdminRole));
    if (!rolesCreated) {
      LOGGER.error("Failed to create Keycloak roles");
      return;
    }

    // Create the configured clients
    boolean clientsCreated = keycloakService.createClients();
    if (!clientsCreated) {
      LOGGER.error("Failed to create Keycloak clients");
      return;
    }

    // Retrieve all existing users in the realm and create their local
    // representations if they don't
    // exist
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
        LOGGER.info(
            "Created local user for Keycloak user: {} - {}",
            user.getKeycloakUUID(),
            user.getUsername());
      }
    }
  }

  /**
   * Retrieves a user by ID and maps it to a UserResponseDTO.
   *
   * @param userId the ID of the user to retrieve
   * @return a ResponseEntity containing the user's response DTO
   * @throws NotFoundException if the user is not found
   */
  public ResponseEntity<UserResponseDTO> getUserById(Long userId) {
    // Map DTO to entity
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new NotFoundException("User with id " + userId + " not found"));

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
  public ResponseEntity<UserResponseDTO> updateUserWithId(
      Long userId, UserUpdateRequestDTO userUpdateRequestDTO) {
    User existingUser =
        userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));

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
    LOGGER.info("Starting user creation for email: {}", userCreateRequestDTO.getEmail());

    // Check if user already exists locally
    Optional<User> existingUser = userRepository.findByEmail(userCreateRequestDTO.getEmail());
    if (existingUser.isPresent()) {
      LOGGER.info("User with email {} already exists locally.", userCreateRequestDTO.getEmail());
      throw new ConflictException(
          "User with email " + userCreateRequestDTO.getEmail() + " already exists");
    }

    try {
      keycloakService.createUserInRealm(
          userCreateRequestDTO.getPassword(),
          userCreateRequestDTO.getEmail(),
          userCreateRequestDTO.getSurname(),
          userCreateRequestDTO.getLastname());

      // On successful creation in Keycloak, proceed with local user creation
      LOGGER.info(
          "User creation in Keycloak successful for email: {}", userCreateRequestDTO.getEmail());

      KeycloakUserResponseDTO createdKeycloakUser =
          keycloakService.retrieveUserByEmail(userCreateRequestDTO.getEmail());

      newUser.setKeycloakUUID(createdKeycloakUser.getKeycloakUUID());
      newUser.setEmail(createdKeycloakUser.getEmail());
      newUser.setSurname(createdKeycloakUser.getFirstName());
      newUser.setLastname(createdKeycloakUser.getLastName());
      userRepository.save(newUser);
      UserResponseDTO userResponseDTO = userResponseMapper.toDto(newUser);
      return ResponseEntity.status(HttpStatus.CREATED).body(userResponseDTO);

    } catch (BadRequestException e) {
      LOGGER.error("Exception during user creation in Keycloak: {}", e.getMessage());
      throw e;
    } catch (Exception e) {
      LOGGER.error("Unexpected exception during user creation in Keycloak: {}", e.getMessage());
      throw new InternalServerErrorException(
          "Internal error during user creation in Keycloak: " + e.getMessage());
    }
  }

  /**
   * Creates a new admin user for the application with the provided UserCreateRequestDTO.
   *
   * @param userCreateRequestDTO the user request DTO containing admin user details
   * @return a ResponseEntity containing the created admin user's response DTO
   */
  public ResponseEntity<UserResponseDTO> createAdmin(UserCreateRequestDTO userCreateRequestDTO) {

    User newUser = new User();

    try {
      this.keycloakService.createAdminUserInRealm(
          userCreateRequestDTO.getPassword(),
          userCreateRequestDTO.getEmail(),
          userCreateRequestDTO.getSurname(),
          userCreateRequestDTO.getLastname());

      // On successful creation in Keycloak, proceed with local user creation
      KeycloakUserResponseDTO createdKeycloakUser =
          keycloakService.retrieveUserByEmail(userCreateRequestDTO.getEmail());

      newUser.setKeycloakUUID(createdKeycloakUser.getKeycloakUUID());
      newUser.setEmail(createdKeycloakUser.getEmail());
      newUser.setSurname(createdKeycloakUser.getFirstName());
      newUser.setLastname(createdKeycloakUser.getLastName());
      userRepository.save(newUser);

      UserResponseDTO userResponseDTO = userResponseMapper.toDto(newUser);
      return ResponseEntity.status(HttpStatus.CREATED).body(userResponseDTO);

    } catch (BadRequestException e) {
      throw e;
    } catch (Exception e) {
      // Log unexpected exceptions
      LOGGER.error(
          "Unexpected exception during admin user creation in Keycloak: {}", e.getMessage());
      throw new InternalServerErrorException(
          "Internal error during admin user creation in Keycloak: " + e.getMessage());
    }
  }

  /**
   * Retrieves a user entity by its ID.
   *
   * @param userId the ID of the user
   * @return an Optional containing the user entity if found, or empty if not found
   */
  public Optional<User> getUserByIdEntity(Long userId) {
    return userRepository.findById(userId);
  }

  /**
   * Retrieves the user associated with the current access token.
   *
   * @return a ResponseEntity containing the user's response DTO
   * @throws BadRequestException if the access token is invalid or missing required claims
   * @throws NotFoundException if the user is not found
   */
  public ResponseEntity<UserResponseDTO> getUserByAccessToken() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
      throw new BadRequestException("No JWT access token found");
    }

    Jwt jwt = jwtAuth.getToken();
    String keycloakUuid = jwt.getSubject();
    if (keycloakUuid == null || keycloakUuid.isBlank()) {
      throw new BadRequestException("Access token missing subject (sub)");
    }

    Optional<User> byUuid = userRepository.findByKeycloakUUID(keycloakUuid);
    if (byUuid.isPresent()) {
      return ResponseEntity.ok(userResponseMapper.toDto(byUuid.get()));
    } else {
      LOGGER.warn("User with Keycloak UUID {} not found locally.", keycloakUuid);
      throw new NotFoundException("User not found");
    }
  }

  /**
   * Retrieves multiple user entities by their IDs in a single query.
   *
   * @param userIds the user IDs to fetch
   * @return a map of userId -> User for all users that exist
   */
  public Map<Long, User> getUsersByIdMap(Set<Long> userIds) {
    if (userIds == null || userIds.isEmpty()) {
      return Map.of();
    }

    List<User> users = userRepository.findAllById(userIds);
    Map<Long, User> result = new HashMap<>();
    for (User user : users) {
      if (user != null && user.getId() != null) {
        result.put(user.getId(), user);
      }
    }
    return result;
  }

  /**
   * Retrieves a set of users by their IDs.
   *
   * @param userIds the IDs of the users to retrieve
   * @return a set of User entities
   */
  public Set<User> getUsersByIds(Set<Long> userIds) {
    if (userIds == null || userIds.isEmpty()) {
      return Set.of();
    }
    Set<User> users = new HashSet<>(userRepository.findAllById(userIds));
    if (users.isEmpty()) {
      throw new NotFoundException("No users found for the provided IDs");
    } else if (users.size() < userIds.size()) {
      throw new NotFoundException("Some users not found for the provided IDs");
    }
    return users;
  }

  /**
   * Retrieves a set of UserResponseDTOs by their IDs.
   *
   * @param userIds the IDs of the users to retrieve
   * @return a set of UserResponseDTOs
   */
  public Set<UserResponseDTO> getUsersByIdsDTOs(Set<Long> userIds) {
    try {
      Set<User> users = getUsersByIds(userIds);
      Set<UserResponseDTO> userDTOs = new HashSet<>();
      for (User user : users) {
        userDTOs.add(userResponseMapper.toDto(user));
      }
      return userDTOs;
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      LOGGER.error("Error retrieving users by IDs: {}", e.getMessage());
      throw new InternalServerErrorException(
          "Internal error retrieving users by IDs: " + e.getMessage());
    }
  }

  /**
   * Retrieves a user entity by its ID, throwing NotFoundException if not found.
   *
   * @param userId the ID of the user
   * @return the user entity
   * @throws NotFoundException if the user is not found
   */
  public User retrieveUserById(Long userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));
  }

  public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
    List<User> users = userRepository.findAll();
    List<UserResponseDTO> userDTOs = userResponseMapper.toDtoList(users);
    return ResponseEntity.ok(userDTOs);
  }
}
