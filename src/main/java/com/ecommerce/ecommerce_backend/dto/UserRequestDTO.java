package com.ecommerce.ecommerce_backend.dto;

import lombok.Data;

@Data
public class UserRequestDTO {
    private String name;
    private String email;
    private String password;
    private String role;
}
