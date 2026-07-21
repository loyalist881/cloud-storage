package com.github.loyalist.registrationprocess.controller;

import com.github.loyalist.dto.metadata.UserDto;
import com.github.loyalist.registrationprocess.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/")
public class RegistrationController {
    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/login")
    @Operation(summary = "Авторизация",
            description = "Получает DTO из MetadataService и валидирует пользовательский ввод")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Метаданные и id успешно получены"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    public ResponseEntity<String> check(@RequestBody UserDto userDto) {
        UsernamePasswordAuthenticationToken authRequest =
                new UsernamePasswordAuthenticationToken(userDto.getEmail(), userDto.getPassword());
        Authentication authentication = registrationService.authenticate(authRequest);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assert authentication != null;
        Long userId = (Long) authentication.getPrincipal();

        return ResponseEntity.ok("Пользователь найден под ID: " + userId);
    }
}
