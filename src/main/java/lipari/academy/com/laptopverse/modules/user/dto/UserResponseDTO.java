package lipari.academy.com.laptopverse.modules.user.dto;


import lipari.academy.com.laptopverse.modules.user.model.UserRole;

public record UserResponseDTO(
        Long id,
        String firstName,
        String lastName,
        String email,
        UserRole role
) {
}