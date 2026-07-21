package com.github.loyalist.registrationprocess.service;

import com.github.loyalist.dto.metadata.UserDto;
import com.github.loyalist.registrationprocess.exception.RegistrationException;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class RegistrationService implements AuthenticationProvider {
    private final WebClient metadataWebClient;

    @Autowired
    public RegistrationService(WebClient metadataWebClient) {
        this.metadataWebClient = metadataWebClient;
    }

    public UserDto check(String email, String password) {
        UserDto userDto = UserDto.builder()
                .email(email)
                .password(password)
                .build();
        try {
            return metadataWebClient.post()
                    .uri("/files/check")
                    .bodyValue(userDto)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.createException()
                                    .flatMap(error -> Mono.error(new RegistrationException(
                                            "Ошибка сервиса метаданных (" + error.getStatusCode() + "): " + error.getResponseBodyAsString()
                                    )))
                    )
                    .bodyToMono(UserDto.class)
                    .block();
        } catch (RegistrationException e) {
            throw e;
        } catch (Exception e) {
            throw new RegistrationException("MetadataService недоступен или произошла системная ошибка", e);
        }
    }


    @Override
    public @Nullable Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String emailA = authentication.getName();
        String passwordA = authentication.getCredentials().toString();

        UserDto userDto = check(emailA, passwordA);

        return new UsernamePasswordAuthenticationToken(
                userDto.getId(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
