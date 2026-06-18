package com.flourish.intelliride.services;

import com.flourish.intelliride.dtos.DriverDto;
import com.flourish.intelliride.dtos.SignupDto;
import com.flourish.intelliride.dtos.UserDto;

public interface AuthService {

    String[] login(String email, String password);// will return a access and refresh token

    UserDto signup(SignupDto signupDto);

    DriverDto onboardNewDriver(Long userId,String vehicleId);

    String refreshToken(String refreshToken);
}
