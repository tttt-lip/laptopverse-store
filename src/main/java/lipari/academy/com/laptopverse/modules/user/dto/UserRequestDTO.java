package lipari.academy.com.laptopverse.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lipari.academy.com.laptopverse.modules.user.model.UserRole;

public record UserRequestDTO(
        @NotBlank(message = "Il display name non può essere vuoto.")
        @Size(min = 3, max = 50, message = "Il display name deve essere tra 3 e 50 caratteri.")
        String displayName,

        @NotBlank(message = "La password non può essere vuota.")
        @Size(min = 8, message = "La password deve essere lunga almeno 8 caratteri.")
        String password,

        @NotBlank(message = "L'email non può essere vuota.")
        @Email(message = "Per favore, inserisci un indirizzo email valido.")
        String email,

        @NotNull(message = "Il ruolo non può essere vuoto.")
        UserRole role
) {
}
