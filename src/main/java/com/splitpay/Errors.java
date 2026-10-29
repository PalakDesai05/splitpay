package com.splitpay;

import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class Errors {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<?> api(ApiException e) { return res(e.status, e.getMessage()); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> invalid(MethodArgumentNotValidException e) {
        var f = e.getBindingResult().getFieldError();
        return res(HttpStatus.BAD_REQUEST, f == null ? "Invalid request" : f.getField() + " " + f.getDefaultMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<?> unreadable(HttpMessageNotReadableException e) { return res(HttpStatus.BAD_REQUEST, "Malformed JSON body"); }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> conflict(DataIntegrityViolationException e) { return res(HttpStatus.CONFLICT, "Conflicting or duplicate data"); }

    static ResponseEntity<Map<String, Object>> res(HttpStatus s, String m) {
        return ResponseEntity.status(s).body(Map.of("status", s.value(), "error", m));
    }
}
