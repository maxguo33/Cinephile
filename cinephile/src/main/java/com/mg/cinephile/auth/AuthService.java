package com.mg.cinephile.auth;

import com.mg.cinephile.auth.AuthDtos.LoginRequest;
import com.mg.cinephile.auth.AuthDtos.RegisterRequest;
import com.mg.cinephile.auth.AuthDtos.TokenResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(AppUserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateUsernameException(request.username());
        }

        String hash = passwordEncoder.encode(request.password());
        userRepository.save(new AppUser(request.username(), hash, "ROLE_USER"));

        String token = jwtService.generateToken(request.username());
        return new TokenResponse(token);
    }

    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()));

        String token = jwtService.generateToken(request.username());
        return new TokenResponse(token);
    }
}
