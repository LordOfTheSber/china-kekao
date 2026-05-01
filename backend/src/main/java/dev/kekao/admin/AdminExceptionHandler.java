package dev.kekao.admin;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice(assignableTypes = AdminHanziController.class)
public class AdminExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        f -> f.getField(),
                        f -> f.getDefaultMessage() == null ? "invalid" : f.getDefaultMessage(),
                        (a, b) -> a));
        return ResponseEntity.badRequest().body(Map.of(
                "error", "validation_failed",
                "fields", fields));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> body(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", "malformed_request"));
    }

    @ExceptionHandler(AdminHanziService.HanziNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(AdminHanziService.HanziNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "hanzi_not_found"));
    }

    @ExceptionHandler(AdminHanziService.HanziNotPublishableException.class)
    public ResponseEntity<Map<String, Object>> notPublishable(AdminHanziService.HanziNotPublishableException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", "hanzi_not_publishable"));
    }
}
