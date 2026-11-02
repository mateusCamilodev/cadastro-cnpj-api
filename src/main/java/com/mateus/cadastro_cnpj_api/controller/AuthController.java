package com.mateus.cadastro_cnpj_api.controller;

import com.mateus.cadastro_cnpj_api.dto.TokenRequest;
import com.mateus.cadastro_cnpj_api.dto.TokenResponse;
import com.mateus.cadastro_cnpj_api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // POST /auth/token  (único endpoint aberto: não exige token)
    @PostMapping("/token")
    public ResponseEntity<TokenResponse> gerarToken(@RequestBody @Valid TokenRequest request) {
        return ResponseEntity.ok(authService.autenticar(request));
    }
}
