package dev.kekao.hanzi;

import dev.kekao.hanzi.HanziDtos.HanziDetailView;
import dev.kekao.hanzi.HanziDtos.HanziSearchPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/hanzi")
public class HanziController {

    private final HanziService service;

    @Autowired
    public HanziController(HanziService service) {
        this.service = service;
    }

    @GetMapping("/search")
    public HanziSearchPage search(@RequestParam(value = "q", required = false) String q,
                                  @RequestParam(value = "hsk", required = false) Short hsk,
                                  @RequestParam(value = "page", defaultValue = "0") int page,
                                  @RequestParam(value = "size", defaultValue = "20") int size) {
        return service.search(q, hsk, page, size);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HanziDetailView> detail(@PathVariable Long id,
                                                  @AuthenticationPrincipal Jwt jwt) {
        Long userId = jwt == null ? null : Long.parseLong(jwt.getSubject());
        try {
            return ResponseEntity.ok(service.detail(id, userId));
        } catch (NoSuchElementException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
