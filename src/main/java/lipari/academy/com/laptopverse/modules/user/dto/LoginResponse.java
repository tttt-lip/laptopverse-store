package lipari.academy.com.laptopverse.modules.user.dto;

import lipari.academy.com.laptopverse.modules.user.model.UserRole;
import lombok.Builder;
@Builder
public record LoginResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        UserRole role,
        long accesTokenExpiresIn
) {
}