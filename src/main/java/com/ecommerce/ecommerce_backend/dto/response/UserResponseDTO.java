package com.ecommerce.ecommerce_backend.dto.response;


import com.ecommerce.ecommerce_backend.entity.Role;

import lombok.Data;

@Data
public class UserResponseDTO {
    private Long id;
    private String name;
    private String email;
    private Role role;

}