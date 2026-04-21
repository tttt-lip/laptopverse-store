package lipari.academy.com.laptopverse.modules.user.dto;

import lipari.academy.com.laptopverse.modules.user.model.UserRole;
import lombok.Builder;

import java.util.UUID;

@Builder
public record LoginResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        UserRole role,
        long accesTokenExpiresIn
) {
}