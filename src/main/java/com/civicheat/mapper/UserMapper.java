package com.civicheat.mapper;

import com.civicheat.dto.response.UserResponse;
import com.civicheat.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mapper for transforming User entity into UserResponse DTO.
 */
@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole())
                .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null)
                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
