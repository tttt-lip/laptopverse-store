package lipari.academy.com.laptopverse.modules.user.services;

import lipari.academy.com.laptopverse.modules.auth.model.JwtTokenPair;
import lipari.academy.com.laptopverse.modules.user.model.User;

public interface AuthService {

    JwtTokenPair generateTokens(User user);

    JwtTokenPair refreshToken(String jwtToken);

    void logout(String refreshToken);
}
