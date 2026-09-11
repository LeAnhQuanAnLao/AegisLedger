package com.aegisledger.core;

import com.aegisledger.core.dto.ApiResponse;
import com.aegisledger.core.exception.AccountNotFoundException;
import com.aegisledger.core.exception.GlobalExceptionHandler;
import com.aegisledger.core.exception.InsufficientFundsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Unit Tests for GlobalExceptionHandler (Tier 0)")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Should translate InsufficientFundsException to 400 Bad Request")
    void testHandleInsufficientFunds() {
        InsufficientFundsException ex = new InsufficientFundsException("Balance 10 is less than 50");
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals("INSUFFICIENT_FUNDS", response.getBody().code());
    }

    @Test
    @DisplayName("Should translate AccountNotFoundException to 404 Not Found")
    void testHandleAccountNotFound() {
        UUID id = UUID.randomUUID();
        AccountNotFoundException ex = new AccountNotFoundException(id);
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ACCOUNT_NOT_FOUND", response.getBody().code());
    }
}
