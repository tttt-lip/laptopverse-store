package lipari.academy.com.laptopverse.modules.user.dto;


import lipari.academy.com.laptopverse.modules.user.model.UserRole;

import java.util.UUID;

public record UserResponseDTO(
        UUID id,
        String firstName,
        String lastName,
        String email,
        UserRole role
) {
}