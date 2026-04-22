package lipari.academy.com.laptopverse.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lipari.academy.com.laptopverse.modules.user.model.UserRole;

public record UserUpdateDTO(
        @NotBlank(message = "Il nome non può essere vuoto")
        String firstName,
        
        @NotBlank(message = "Il cognome non può essere vuoto")
        String lastName,
        
        @NotBlank(message = "L'email non può essere vuota")
        @Email(message = "Inserire un'email valida")
        String email,
        
        @NotNull(message = "Il ruolo è obbligatorio [ADMIN, CUSTOMER]")
        UserRole role,
        
        @NotNull(message = "Lo stato 'abilitato' è obbligatorio [true, false]")
        Boolean isEnabled
) {
}
