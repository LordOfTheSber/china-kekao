package dev.kekao.study;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice(assignableTypes = StudyController.class)
public class StudyExceptionHandler {

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

    @ExceptionHandler(UserCardNotFoundException.class)
    public ResponseEntity<Map<String, Object>> notFound(UserCardNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "user_card_not_found"));
    }

    @ExceptionHandler(StudyController.ReviewForbiddenException.class)
    public ResponseEntity<Map<String, Object>> forbidden(StudyController.ReviewForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "user_card_forbidden"));
    }

    @ExceptionHandler(StudyController.ReviewModeMismatchException.class)
    public ResponseEntity<Map<String, Object>> modeMismatch(StudyController.ReviewModeMismatchException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", "mode_mismatch"));
    }
}
