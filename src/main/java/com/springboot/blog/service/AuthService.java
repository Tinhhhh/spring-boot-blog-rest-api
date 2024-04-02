package com.springboot.blog.service;

import com.springboot.blog.model.payload.dto.LoginDto;
import com.springboot.blog.model.payload.dto.RegisterDto;
import com.springboot.blog.model.payload.responseModel.JWTAuthResponse;

public interface AuthService {
    JWTAuthResponse login(LoginDto loginDto);

    String register(RegisterDto registerDto);
}
