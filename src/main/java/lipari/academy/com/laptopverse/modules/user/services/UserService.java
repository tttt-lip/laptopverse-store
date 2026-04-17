package lipari.academy.com.laptopverse.modules.user.services;


import lipari.academy.com.laptopverse.modules.user.dto.LoginRequest;
import lipari.academy.com.laptopverse.modules.user.dto.LoginResponse;
import lipari.academy.com.laptopverse.modules.user.dto.UserRequestDTO;
import lipari.academy.com.laptopverse.modules.user.dto.UserResponseDTO;
import lipari.academy.com.laptopverse.modules.user.model.User;

public interface UserService {
    UserResponseDTO register(UserRequestDTO dto);

    LoginResponse login(LoginRequest loginRequest);

    User findUserByEmail(String email);
}
