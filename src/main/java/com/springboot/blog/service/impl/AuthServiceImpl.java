package com.springboot.blog.service.impl;

import com.springboot.blog.exception.BlogAPIException;
import com.springboot.blog.model.entity.AccessToken;
import com.springboot.blog.model.entity.RefreshToken;
import com.springboot.blog.model.entity.Role;
import com.springboot.blog.model.entity.User;
import com.springboot.blog.model.payload.dto.LoginDto;
import com.springboot.blog.model.payload.dto.RegisterDto;
import com.springboot.blog.model.payload.responseModel.JWTAuthResponse;
import com.springboot.blog.repository.AccessTokenRepository;
import com.springboot.blog.repository.RefreshTokenRepository;
import com.springboot.blog.repository.RoleRepository;
import com.springboot.blog.repository.UserRepository;
import com.springboot.blog.security.JwtTokenProvider;
import com.springboot.blog.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class AuthServiceImpl implements AuthService {

    private AuthenticationManager authenticationManager;
    private UserRepository userRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;
    private AccessTokenRepository accessTokenRepository;
    private UserDetailsService userDetailsService;
    private ModelMapper modelMapper;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           UserRepository userRepository,
                           RefreshTokenRepository refreshTokenRepository, RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider,
                           AccessTokenRepository accessTokenRepository,
                           UserDetailsService userDetailsService, ModelMapper modelMapper) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.accessTokenRepository = accessTokenRepository;
        this.userDetailsService = userDetailsService;
        this.modelMapper = modelMapper;
    }


    @Override
    public JWTAuthResponse login(LoginDto loginDto) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                loginDto.getUsernameOrEmail(),
                loginDto.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        User user = userRepository.findByUsernameOrEmail(loginDto.getUsernameOrEmail(), loginDto.getUsernameOrEmail())
                .orElseThrow(() -> new BlogAPIException(HttpStatus.BAD_REQUEST, "User not found"));
        if (!user.isEnabled()) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST, "Account is not Enabled");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        revokeRefreshTokens(accessToken);
        RefreshToken savedRefreshToken = saveUserRefreshToken(refreshToken);

        revokeAllUserAccessTokens(user);
        saveUserAccessToken(user, accessToken, savedRefreshToken);

        return JWTAuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public String register(RegisterDto registerDto) {

        // add check for username exist in database
        if (userRepository.existsByUsername(registerDto.getUsername())) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST, "Username is already exists!.");
        }

        // add check for email exist in database
        if (userRepository.existsByEmail(registerDto.getEmail())) {
            throw new BlogAPIException(HttpStatus.BAD_REQUEST, "Email is already exists!.");
        }

        User user = modelMapper.map(registerDto, User.class);
        user.setEnabled(false);
        user.setPassword(passwordEncoder.encode(registerDto.getPassword()));

        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName("ROLE_USER").get();
        roles.add(userRole);
        user.setRoles(roles);

        userRepository.save(user);

        return "We sent you email to active your account. Please check ";
    }

    @Override
    public JWTAuthResponse refreshToken(HttpServletRequest request
            , HttpServletResponse response) {

        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        final String refreshToken;
        final String userEmail;
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        refreshToken = authHeader.substring(7);
        userEmail = jwtTokenProvider.getUsername(refreshToken);

        if (userEmail != null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
            RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                    .orElseThrow(() -> new BlogAPIException(HttpStatus.BAD_REQUEST, "Invalid refresh token"));
            if (!token.isRevoked() && !token.isExpired()) {

                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

                User user = userRepository.findByUsernameOrEmail(userEmail, userEmail)
                        .orElseThrow(() -> new BlogAPIException(HttpStatus.BAD_REQUEST, "invalid user"));

                String accessToken = jwtTokenProvider.generateAccessToken(authentication);
//                String newRefreshToken = jwtTokenProvider.generateRefreshToken(authentication);

//                revokeRefreshTokens(accessToken);
//                RefreshToken savedRefreshToken = saveUserRefreshToken(newRefreshToken);

                revokeAllUserAccessTokens(user);
                saveUserAccessToken(user, accessToken, token);

                return JWTAuthResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build();

            } else {
                throw new BlogAPIException(HttpStatus.BAD_REQUEST, "Refresh token isn't exist");
            }
        }

        return null;
    }

    private void revokeAllUserAccessTokens(User user) {
        var validUserTokens = accessTokenRepository.findALlValidTokensByUser(user.getId());
        if (validUserTokens.isEmpty()) {
            return;
        }

        validUserTokens.forEach(t -> {
            t.setExpired(true);
            t.setRevoked(true);
        });
        accessTokenRepository.saveAll(validUserTokens);
    }

    private void revokeRefreshTokens(String accessToken) {
        var token = accessTokenRepository.findByToken(accessToken);
        if (token != null) {
            RefreshToken refreshToken = token.getRefreshToken();
            refreshToken.setExpired(true);
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
        }
    }

    private void saveUserAccessToken(User user, String accessToken, RefreshToken refreshToken) {
        var token = AccessToken.builder()
                .user(user)
                .token(accessToken)
                .refreshToken(refreshToken)
                .revoked(false)
                .expired(false)
                .build();
        accessTokenRepository.save(token);
    }

    private RefreshToken saveUserRefreshToken(String refreshToken) {
        var token = RefreshToken.builder()
                .token(refreshToken)
                .revoked(false)
                .expired(false)
                .build();
        return refreshTokenRepository.save(token);
    }

}
