package com.ecommerce.ecommerce_backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ecommerce.ecommerce_backend.dto.request.LoginRequestDTO;
import com.ecommerce.ecommerce_backend.dto.request.RegisterRequestDTO;
import com.ecommerce.ecommerce_backend.dto.response.AuthResponseDTO;
import com.ecommerce.ecommerce_backend.dto.response.UserResponseDTO;
import com.ecommerce.ecommerce_backend.entity.Role;
import com.ecommerce.ecommerce_backend.entity.User;
import com.ecommerce.ecommerce_backend.exception.DuplicateResourceException;
import com.ecommerce.ecommerce_backend.repository.UserRepository;
import com.ecommerce.ecommerce_backend.utils.JwtUtil;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ModelMapper modelMapper;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User mockUser;
    private RegisterRequestDTO registerRequest;
    private LoginRequestDTO loginRequest;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setName("John Doe");
        mockUser.setEmail("john@example.com");
        mockUser.setPassword("encodedPassword");
        mockUser.setRole(Role.CUSTOMER);

        registerRequest = new RegisterRequestDTO();
        registerRequest.setName("John Doe");
        registerRequest.setEmail("john@example.com");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequestDTO();
        loginRequest.setEmail("john@example.com");
        loginRequest.setPassword("password123");
    }

    //  Register Tests 

    @Test
    void register_Success() {
        // Arrange
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(modelMapper.map(mockUser, UserResponseDTO.class)).thenReturn(new UserResponseDTO());

        // Act
        UserResponseDTO result = authService.registerUser(registerRequest);

        // Assert
        assertNotNull(result);
        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordEncoder, times(1)).encode("password123");
    }

    @Test
    void register_ThrowsDuplicateException_WhenEmailAlreadyExists() {
        // Arrange
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        // Act & Assert
        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> authService.registerUser(registerRequest));

        assertTrue(exception.getMessage().contains("john@example.com"));
        verify(userRepository, never()).save(any()); // save must never be called
    }

    //  Login Tests 

    @Test
    void login_Success() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null); // authenticate passes silently
        when(userRepository.findByEmail(loginRequest.getEmail()))
                .thenReturn(Optional.of(mockUser));
        when(jwtUtil.generateToken(mockUser)).thenReturn("mock.jwt.token");

        // Act
        AuthResponseDTO result = authService.login(loginRequest);

        // Assert
        assertNotNull(result);
        assertEquals("mock.jwt.token", result.getToken());
        assertEquals("CUSTOMER", result.getRole());
        assertEquals("john@example.com", result.getEmail());
    }

    @Test
    void login_ThrowsBadCredentials_WhenAuthenticationFails() {
        // Arrange
        when(authenticationManager.authenticate(any()))
                .thenThrow(new org.springframework.security.authentication
                        .BadCredentialsException("Bad credentials"));

        // Act & Assert
        assertThrows(Exception.class, () -> authService.login(loginRequest));
    }
}