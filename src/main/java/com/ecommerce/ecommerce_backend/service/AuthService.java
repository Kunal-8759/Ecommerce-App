package com.ecommerce.ecommerce_backend.service;

import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ecommerce.ecommerce_backend.dto.request.LoginRequestDTO;
import com.ecommerce.ecommerce_backend.dto.request.RegisterRequestDTO;
import com.ecommerce.ecommerce_backend.dto.response.AuthResponseDTO;
import com.ecommerce.ecommerce_backend.dto.response.UserResponseDTO;
import com.ecommerce.ecommerce_backend.entity.Role;
import com.ecommerce.ecommerce_backend.entity.User;
import com.ecommerce.ecommerce_backend.exception.BadCredentialsException;
import com.ecommerce.ecommerce_backend.exception.DuplicateResourceException;
import com.ecommerce.ecommerce_backend.exception.ResourceNotFoundException;
import com.ecommerce.ecommerce_backend.repository.UserRepository;
import com.ecommerce.ecommerce_backend.utils.JwtUtil;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private JwtUtil jwtUtil;

    public UserResponseDTO registerUser(RegisterRequestDTO request){
        log.info("Registration attempt for email: {}", request.getEmail());
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed — email already registered: {}", request.getEmail());
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.CUSTOMER); // default role : CUSTOMER

        User savedUser = userRepository.save(user);
        log.info("User registered successfully — id: {}, email: {}", savedUser.getId(), savedUser.getEmail());
        //convert entity to dto to hide in response
        return modelMapper.map(savedUser , UserResponseDTO.class);
    }

    public AuthResponseDTO login(LoginRequestDTO request) {
        log.info("Login attempt for email: {}", request.getEmail());

        // i want to check here if authenticated or not and if not then i want to throw my custom exception which will be handled by global exception handler and return 401 with message "Invalid email or password"
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (Exception ex) {
            log.warn("Login failed — invalid credentials for email: {}", request.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        }
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() ->{
                log.warn("Login failed — user not found with email: {}", request.getEmail());
                return new ResourceNotFoundException("User not found with email: " + request.getEmail());
            });

        String token = jwtUtil.generateToken(user);
        log.info("Login successful — email: {}, role: {}", user.getEmail(), user.getRole());

        return new AuthResponseDTO(token, user.getEmail(), user.getRole().name());
    }


}
