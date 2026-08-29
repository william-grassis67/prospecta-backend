package com.leadly.demo.controller;

import com.leadly.demo.entity.User;
import com.leadly.demo.service.AuthService;
import com.leadly.demo.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@CrossOrigin(origins = "*")
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService){
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<User> me(Authentication authentication){
        User user = userService.getMe(authentication);

        return ResponseEntity.ok(user);
    }
}
