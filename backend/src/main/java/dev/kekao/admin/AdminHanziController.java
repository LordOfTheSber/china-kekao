package dev.kekao.admin;

import dev.kekao.admin.AdminDtos.HanziAdminView;
import dev.kekao.admin.AdminDtos.HanziPage;
import dev.kekao.admin.AdminDtos.HanziUpdateRequest;
import dev.kekao.hanzi.HanziStatus;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/hanzi")
public class AdminHanziController {

    private final AdminHanziService service;

    public AdminHanziController(AdminHanziService service) {
        this.service = service;
    }

    @GetMapping
    public HanziPage list(@RequestParam(required = false) HanziStatus status,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "50") int size) {
        return service.list(status, page, size);
    }

    @PutMapping("/{id}")
    public HanziAdminView update(@PathVariable Long id,
                                 @Valid @RequestBody HanziUpdateRequest req) {
        return service.update(id, req);
    }

    @PostMapping("/{id}/publish")
    public HanziAdminView publish(@PathVariable Long id) {
        return service.publish(id);
    }
}
