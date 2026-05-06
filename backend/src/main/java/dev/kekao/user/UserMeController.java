package dev.kekao.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class UserMeController {

    private final UserSettingsService service;

    @Autowired
    public UserMeController(UserSettingsService service) {
        this.service = service;
    }

    @GetMapping("/settings")
    public UserSettings getSettings(@AuthenticationPrincipal Jwt jwt) {
        return service.get(Long.parseLong(jwt.getSubject()));
    }

    @PatchMapping("/settings")
    public ResponseEntity<UserSettings> patchSettings(@AuthenticationPrincipal Jwt jwt,
                                                      @RequestBody UserSettings patch) {
        try {
            return ResponseEntity.ok(service.update(Long.parseLong(jwt.getSubject()), patch));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/settings")
    public ResponseEntity<UserSettings> putSettings(@AuthenticationPrincipal Jwt jwt,
                                                    @RequestBody UserSettings patch) {
        return patchSettings(jwt, patch);
    }
}
