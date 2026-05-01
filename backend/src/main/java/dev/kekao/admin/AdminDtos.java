package dev.kekao.admin;

import dev.kekao.hanzi.HanziStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class AdminDtos {

    private AdminDtos() {}

    public record HanziAdminView(
            Long id,
            String character,
            String pinyin,
            Short strokeCount,
            Short hskLevel,
            HanziStatus status,
            List<String> meaningsEn
    ) {}

    public record HanziPage(
            List<HanziAdminView> items,
            int page,
            int size,
            long total
    ) {}

    public record HanziUpdateRequest(
            @NotBlank @Size(max = 64) String pinyin,
            @Min(0) @Max(40) Short strokeCount,
            @Min(1) @Max(9) Short hskLevel,
            @Size(min = 1, max = 20) List<@NotBlank @Size(max = 200) String> meaningsEn
    ) {}
}
