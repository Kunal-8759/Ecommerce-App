package com.ecommerce.ecommerce_backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ecommerce.ecommerce_backend.dto.request.UpdateUserRequestDTO;
import com.ecommerce.ecommerce_backend.dto.response.UserResponseDTO;
import com.ecommerce.ecommerce_backend.entity.Role;
import com.ecommerce.ecommerce_backend.entity.User;
import com.ecommerce.ecommerce_backend.exception.ResourceNotFoundException;
import com.ecommerce.ecommerce_backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ModelMapper modelMapper;

    @InjectMocks
    private UserService userService;

    private User mockUser;
    private UserResponseDTO mockUserResponse;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setName("John Doe");
        mockUser.setEmail("john@example.com");
        mockUser.setPassword("encodedPassword");
        mockUser.setRole(Role.CUSTOMER);

        mockUserResponse = new UserResponseDTO();
        mockUserResponse.setId(1L);
        mockUserResponse.setName("John Doe");
        mockUserResponse.setEmail("john@example.com");
        mockUserResponse.setRole(Role.CUSTOMER);
    }

    // ─── Get User By ID ────────────────────────────────────────────────

    @Test
    void getUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(modelMapper.map(mockUser, UserResponseDTO.class)).thenReturn(mockUserResponse);

        UserResponseDTO result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals("John Doe", result.getName());
        assertEquals("john@example.com", result.getEmail());
    }

    @Test
    void getUserById_ThrowsNotFoundException_WhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getUserById(99L));

        assertTrue(exception.getMessage().contains("99"));
    }

    // ─── Update User ───────────────────────────────────────────────────

    @Test
    void updateUser_Success_WithPasswordChange() {
        UpdateUserRequestDTO request = new UpdateUserRequestDTO();
        request.setName("John Updated");
        request.setPassword("newPassword123");

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.encode("newPassword123")).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(modelMapper.map(mockUser, UserResponseDTO.class)).thenReturn(mockUserResponse);

        UserResponseDTO result = userService.updateUser(1L, request);

        assertNotNull(result);
        verify(passwordEncoder, times(1)).encode("newPassword123");
        verify(userRepository, times(1)).save(mockUser);
    }

    @Test
    void updateUser_Success_WithoutPasswordChange() {
        UpdateUserRequestDTO request = new UpdateUserRequestDTO();
        request.setName("John Updated");
        request.setPassword(null); // no password change

        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(modelMapper.map(mockUser, UserResponseDTO.class)).thenReturn(mockUserResponse);

        userService.updateUser(1L, request);

        // Password encoder must NOT be called when password is null
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void updateUser_ThrowsNotFoundException_WhenUserDoesNotExist() {
        UpdateUserRequestDTO request = new UpdateUserRequestDTO();
        request.setName("Ghost");

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.updateUser(99L, request));
    }

    // ─── Delete User ───────────────────────────────────────────────────

    @Test
    void deleteUser_Success() {
        when(userRepository.existsById(1L)).thenReturn(true);
        doNothing().when(userRepository).deleteById(1L);

        assertDoesNotThrow(() -> userService.deleteUser(1L));
        verify(userRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteUser_ThrowsNotFoundException_WhenUserDoesNotExist() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> userService.deleteUser(99L));

        verify(userRepository, never()).deleteById(any());
    }

    // ─── Get All Users ─────────────────────────────────────────────────

    @Test
    void getAllUsers_ReturnsListOfUsers() {
        when(userRepository.findAll()).thenReturn(List.of(mockUser, mockUser));
        when(modelMapper.map(any(User.class), eq(UserResponseDTO.class)))
                .thenReturn(mockUserResponse);

        List<UserResponseDTO> result = userService.getAllUsers();

        assertEquals(2, result.size());
        verify(userRepository, times(1)).findAll();
    }
}