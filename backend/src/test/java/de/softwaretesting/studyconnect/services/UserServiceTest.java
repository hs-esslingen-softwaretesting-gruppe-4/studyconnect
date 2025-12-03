package de.softwaretesting.studyconnect.services;

import de.softwaretesting.studyconnect.models.User;
import de.softwaretesting.studyconnect.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldRegisterUserSuccessfully() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("Password123!");
        user.setSurname("Max");
        user.setLastname("Mustermann");
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User registered = userService.register(user);

        assertEquals("test@example.com", registered.getEmail());
        assertEquals("hashed", registered.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldLoginWithCorrectCredentials() {
        User user = new User();
        user.setEmail("login@example.com");
        user.setPassword("hashed");
        when(userRepository.findByEmail("login@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "hashed")).thenReturn(true);

        User loggedIn = userService.login("login@example.com", "Password123!");
        assertNotNull(loggedIn);
        assertEquals("login@example.com", loggedIn.getEmail());
    }

    @Test
    void shouldRejectLoginWithIncorrectCredentials() {
        User user = new User();
        user.setEmail("login@example.com");
        user.setPassword("hashed");
        when(userRepository.findByEmail("login@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "hashed")).thenReturn(false);

        User loggedIn = userService.login("login@example.com", "wrongpass");
        assertNull(loggedIn);
    }

    @Test
    void shouldValidatePasswordRules() {
        assertTrue(userService.isPasswordValid("Password123!"));
        assertFalse(userService.isPasswordValid("short"));
        assertFalse(userService.isPasswordValid("nouppercase123!"));
        assertFalse(userService.isPasswordValid("NOLOWERCASE123!"));
        assertFalse(userService.isPasswordValid("NoSpecialChar123"));
    }

    @Test
    void shouldAssignUserRole() {
        User user = new User();
        user.setEmail("role@example.com");
        user.setPassword("Password123!");
        user.setSurname("Role");
        user.setLastname("Test");
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User registered = userService.registerWithRole(user, "ADMIN");

        assertEquals("ADMIN", registered.getRole());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldVerifyRepositoryInteractions() {
        User user = new User();
        user.setEmail("verify@example.com");
        user.setPassword("Password123!");
        user.setSurname("Verify");
        user.setLastname("Test");
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.register(user);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("verify@example.com", captor.getValue().getEmail());
    }
}
