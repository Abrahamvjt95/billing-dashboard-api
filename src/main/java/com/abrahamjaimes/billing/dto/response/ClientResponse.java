package com.abrahamjaimes.billing.dto.response;

import com.abrahamjaimes.billing.entity.Client;

import java.time.LocalDateTime;

public record ClientResponse(
        Long id,
        String name,
        String email,
        String phone,
        String address,
        String taxId,
        String notes,
        LocalDateTime createdAt
) {
    public static ClientResponse from(Client c) {
        return new ClientResponse(c.getId(), c.getName(), c.getEmail(), c.getPhone(),
                c.getAddress(), c.getTaxId(), c.getNotes(), c.getCreatedAt());
    }
}
