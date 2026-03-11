package com.ecommerce.ecommerce_backend.dto;

import javax.management.relation.Role;

import lombok.Data;

@Data
public class UserResponseDTO {
    private Long id;
    private String name;
    private String email;
    private Role role;

}