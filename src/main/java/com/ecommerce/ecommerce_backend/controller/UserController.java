package com.ecommerce.ecommerce_backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.ecommerce_backend.dto.request.LoginRequestDTO;
import com.ecommerce.ecommerce_backend.dto.request.RegisterRequestDTO;
import com.ecommerce.ecommerce_backend.dto.request.UpdateUserRequestDTO;
import com.ecommerce.ecommerce_backend.dto.response.ApiResponse;
import com.ecommerce.ecommerce_backend.dto.response.AuthResponseDTO;
import com.ecommerce.ecommerce_backend.dto.response.UserResponseDTO;
import com.ecommerce.ecommerce_backend.entity.Role;
import com.ecommerce.ecommerce_backend.entity.User;
import com.ecommerce.ecommerce_backend.service.AuthService;
import com.ecommerce.ecommerce_backend.service.UserService;
import com.ecommerce.ecommerce_backend.utils.JwtUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "APIs for user registration, login, profile management and admin user control")
public class UserController {

    @Autowired
    private AuthService authService;
    
    @Autowired
    private UserService userService;

    @Operation(summary = "Register a new user",description = "Registers a new customer account. Role is automatically set to CUSTOMER.")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponseDTO>> register(@Valid @RequestBody RegisterRequestDTO registerRequest) {
        UserResponseDTO data = authService.registerUser(registerRequest);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED.value(), "User registered successfully", data));
    }

    @Operation(summary = "Login",description = "Authenticates user and returns a JWT token. Use this token in the Authorize button above.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> login(@Valid @RequestBody LoginRequestDTO authRequest) {
        AuthResponseDTO response = authService.login(authRequest);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(), "Login successful", response));
    }


    @Operation(summary = "Get user by ID",
               description = "Fetch a user's profile. Customers can only fetch their own profile. Admins can fetch any.",
               security = @SecurityRequirement(name = "BearerAuth"))
    // Get User Profile by ID
    @GetMapping("/{id}")
    @PreAuthorize("#id == authentication.principal.id or hasRole('ADMIN')") // Admin can access any profile, users can access their own profile
    public ResponseEntity<ApiResponse<UserResponseDTO>> getUser(@PathVariable Long id) {
        UserResponseDTO data = userService.getUserById(id);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(), "User fetched successfully", data));
    }

    // Update User Profile (Name/Password)
    @Operation(summary = "Update user profile",
               description = "Update name and/or password. Customers can only update their own profile.",
               security = @SecurityRequirement(name = "BearerAuth"))
    @PutMapping("/{id}")
    @PreAuthorize("#id == authentication.principal.id or hasRole('ADMIN')") // Admin can update any profile, users can update their own profile
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateUser(@PathVariable Long id,@Valid @RequestBody UpdateUserRequestDTO userDTO) {
        UserResponseDTO data = userService.updateUser(id, userDTO);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(), "User updated successfully", data));
    }

    // Update User Role (Admin only)
    @Operation(summary = "Update user role (Admin only)",
               description = "Change a user's role to ADMIN or CUSTOMER.",
               security = @SecurityRequirement(name = "BearerAuth"))
    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')") // Only Admin can update roles
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateRole(@PathVariable Long id, @RequestParam Role role) {
        UserResponseDTO data = userService.updateUserRole(id, role);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(), "User role updated successfully", data));
    }

    // Delete User
    @Operation(summary = "Delete user (Admin only)",
               description = "Delete a user account. Only admins can perform this action.",
               security = @SecurityRequirement(name = "BearerAuth"))
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(), "User deleted successfully", null));
    }

    //get all Users
    @Operation(summary = "Get all users (Admin only)",
                description = "Fetch a list of all users. Only admins can perform this action.",
               security = @SecurityRequirement(name = "BearerAuth"))
    @GetMapping()
    public ResponseEntity<ApiResponse<List<UserResponseDTO>>> getAllUsers() {
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(), "Users fetched successfully", userService.getAllUsers()));
    }
}
