package com.abrahamjaimes.billing.dto.response;

import com.abrahamjaimes.billing.entity.Role;
import com.abrahamjaimes.billing.entity.User;

public record UserResponse(Long id, String email, String firstName, String lastName, Role role) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getRole());
    }
}
