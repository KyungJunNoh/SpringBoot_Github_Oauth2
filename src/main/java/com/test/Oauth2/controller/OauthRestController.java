package com.test.Oauth2.controller;

import com.test.Oauth2.response.LoginResponse;
import com.test.Oauth2.service.OauthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/api")
@RestController
public class OauthRestController {

    private final OauthService oauthService;

    public OauthRestController(OauthService oauthService) {
        this.oauthService = oauthService;
    }

    @GetMapping("/login/oauth/{provider}")
    public ResponseEntity<LoginResponse> login(@PathVariable String provider, @RequestParam String code) {
        LoginResponse loginResponse = oauthService.login(provider, code);
        return ResponseEntity.ok().body(loginResponse);
    }
}
