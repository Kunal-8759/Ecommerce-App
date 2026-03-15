package com.ecommerce.ecommerce_backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.sql.Update;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ecommerce.ecommerce_backend.dto.request.UpdateUserRequestDTO;
import com.ecommerce.ecommerce_backend.dto.response.UserResponseDTO;
import com.ecommerce.ecommerce_backend.entity.Role;
import com.ecommerce.ecommerce_backend.entity.User;
import com.ecommerce.ecommerce_backend.exception.ResourceNotFoundException;
import com.ecommerce.ecommerce_backend.repository.UserRepository;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ModelMapper modelMapper;


    public UserResponseDTO getUserById(Long id) {

        log.debug("Fetching user by id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found with id: {}", id);
                    return new ResourceNotFoundException("User not found with id: " + id);
                });
        return modelMapper.map(user, UserResponseDTO.class);
    }

    // Update User Profile (Name/Password)-> once logged in user can update his profile but not role
    public UserResponseDTO updateUser(Long id, UpdateUserRequestDTO request) {
        log.info("Updating user with id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Update failed — user not found with id: {}", id);
                    return new ResourceNotFoundException("User not found with id: " + id);
                });

        user.setName(request.getName());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            log.debug("Password updated for user id: {}", id);
        }

        userRepository.save(user);
        log.info("User updated successfully with id: {}", id);
        return modelMapper.map(user, UserResponseDTO.class);
    }

    // Update User Role (ADMIN ONLY logic)
    public UserResponseDTO updateUserRole(Long id, Role newRole) {
        log.info("Updating role for user id: {} to {}", id, newRole);

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Role update failed — user not found with id: {}", id);
                    return new ResourceNotFoundException("User not found with id: " + id);
                });
        
        user.setRole(newRole);
        User updatedUser = userRepository.save(user);
        log.info("Role updated successfully — user id: {}, new role: {}", id, newRole);
        return modelMapper.map(updatedUser, UserResponseDTO.class);
    }

    // Delete User
    public void deleteUser(Long id) {
        log.info("Deleting user id: {}", id);
        if (!userRepository.existsById(id)) {
            log.warn("Delete failed — user not found with id: {}", id);
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
        log.info("User deleted successfully with id: {}", id);
    }

    //get all users (admin only)
    public List<UserResponseDTO> getAllUsers() {
        log.info("Fetching all users");
        List<User> users = userRepository.findAll();
        log.debug("Total users fetched: {}", users.size());
        return users.stream()
                .map(user -> modelMapper.map(user, UserResponseDTO.class))
                .collect(Collectors.toList());
    }


}
