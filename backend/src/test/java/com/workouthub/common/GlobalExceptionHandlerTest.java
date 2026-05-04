package com.workouthub.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.workouthub.common.web.ApiError;
import com.workouthub.common.web.ConflictException;
import com.workouthub.common.web.GlobalExceptionHandler;
import com.workouthub.common.web.NotFoundException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void notFoundMapsTo404WithPath() {
        var req = new MockHttpServletRequest("GET", "/api/users/missing");

        var response = handler.handleNotFound(new NotFoundException("user not found"), req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("user not found");
        assertThat(response.getBody().path()).isEqualTo("/api/users/missing");
    }

    @Test
    void conflictMapsTo409() {
        var req = new MockHttpServletRequest("POST", "/api/auth/register");

        var response = handler.handleConflict(new ConflictException("email taken"), req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).isEqualTo("email taken");
        assertThat(response.getBody().code()).isNull();
    }

    @Test
    void conflictWithCodePropagatesCodeToApiError() {
        var req = new MockHttpServletRequest("POST", "/api/auth/register");

        var response = handler.handleConflict(
                new ConflictException("EMAIL_TAKEN", "email taken"), req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).isEqualTo("email taken");
        assertThat(response.getBody().code()).isEqualTo("EMAIL_TAKEN");
    }

    @Test
    void badCredentialsMapsTo401GenericMessage() {
        var req = new MockHttpServletRequest("POST", "/api/auth/login");

        var response = handler.handleBadCredentials(new BadCredentialsException("bad"), req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().message()).isEqualTo("Invalid credentials");
    }

    @Test
    void accessDeniedMapsTo403() {
        var req = new MockHttpServletRequest("DELETE", "/api/admin/exercises/42");

        var response = handler.handleDenied(new AccessDeniedException("no"), req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void validationErrorsCollectedAsFieldErrors() {
        var req = new MockHttpServletRequest("POST", "/api/something");
        var binding = mock(BindingResult.class);
        when(binding.getFieldErrors()).thenReturn(List.of(
                new FieldError("obj", "email", null, false, null, null, "must not be blank"),
                new FieldError("obj", "password", null, false, null, null, "too short")));
        var ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(binding);

        var response = handler.handleValidation(ex, req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        List<ApiError.FieldError> errors = response.getBody().errors();
        assertThat(errors).hasSize(2);
        assertThat(errors).extracting(ApiError.FieldError::field)
                .containsExactlyInAnyOrder("email", "password");
    }

    @Test
    void unhandledExceptionReturns500GenericMessage() {
        var req = new MockHttpServletRequest("GET", "/api/x");

        var response = handler.handleUnhandled(new RuntimeException("boom"), req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).isEqualTo("Internal server error");
    }
}
