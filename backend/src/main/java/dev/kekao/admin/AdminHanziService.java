package dev.kekao.admin;

import dev.kekao.admin.AdminDtos.HanziAdminView;
import dev.kekao.admin.AdminDtos.HanziPage;
import dev.kekao.admin.AdminDtos.HanziUpdateRequest;
import dev.kekao.hanzi.HanziEntity;
import dev.kekao.hanzi.HanziRepository;
import dev.kekao.hanzi.HanziStatus;
import dev.kekao.hanzi.HanziTranslationEntity;
import dev.kekao.hanzi.HanziTranslationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AdminHanziService {

    private static final String DEFAULT_LANGUAGE = "en";
    private static final int MAX_PAGE_SIZE = 200;

    private final HanziRepository hanzi;
    private final HanziTranslationRepository translations;

    public AdminHanziService(HanziRepository hanzi, HanziTranslationRepository translations) {
        this.hanzi = hanzi;
        this.translations = translations;
    }

    @Transactional(readOnly = true)
    public HanziPage list(HanziStatus status, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = clamp(size, 1, MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.ASC, "id"));
        Page<HanziEntity> result = (status == null)
                ? hanzi.findAll(pageable)
                : hanzi.findAllByStatus(status, pageable);
        List<HanziAdminView> items = result.getContent().stream()
                .map(this::toView)
                .toList();
        return new HanziPage(items, safePage, safeSize, result.getTotalElements());
    }

    @Transactional
    public HanziAdminView update(Long id, HanziUpdateRequest req) {
        HanziEntity entity = hanzi.findById(id)
                .orElseThrow(() -> new HanziNotFoundException(id));
        entity.setPinyin(req.pinyin().strip());
        entity.setStrokeCount(req.strokeCount());
        entity.setHskLevel(req.hskLevel());
        hanzi.save(entity);
        replaceEnglishMeanings(entity, req.meaningsEn());
        return toView(entity);
    }

    @Transactional
    public HanziAdminView publish(Long id) {
        HanziEntity entity = hanzi.findById(id)
                .orElseThrow(() -> new HanziNotFoundException(id));
        if (entity.getStatus() == HanziStatus.PUBLISHED) {
            return toView(entity);
        }
        if (!isPublishable(entity)) {
            throw new HanziNotPublishableException(id);
        }
        entity.setStatus(HanziStatus.PUBLISHED);
        hanzi.save(entity);
        return toView(entity);
    }

    private boolean isPublishable(HanziEntity entity) {
        if (entity.getPinyin() == null || entity.getPinyin().isBlank()) {
            return false;
        }
        return findEnglishTranslation(entity.getId())
                .map(t -> t.getMeanings() != null && !t.getMeanings().isEmpty())
                .orElse(false);
    }

    private void replaceEnglishMeanings(HanziEntity owner, List<String> meanings) {
        Optional<HanziTranslationEntity> existing = findEnglishTranslation(owner.getId());
        if (existing.isPresent()) {
            HanziTranslationEntity translation = existing.get();
            translation.setMeanings(new ArrayList<>(meanings));
            translation.setPrimary(true);
            translations.save(translation);
            return;
        }
        translations.save(HanziTranslationEntity.builder()
                .hanzi(owner)
                .language(DEFAULT_LANGUAGE)
                .meanings(new ArrayList<>(meanings))
                .primary(true)
                .build());
    }

    private Optional<HanziTranslationEntity> findEnglishTranslation(Long hanziId) {
        return translations.findByHanziId(hanziId).stream()
                .filter(t -> DEFAULT_LANGUAGE.equals(t.getLanguage()))
                .findFirst();
    }

    private HanziAdminView toView(HanziEntity entity) {
        List<String> meanings = findEnglishTranslation(entity.getId())
                .map(HanziTranslationEntity::getMeanings)
                .map(List::copyOf)
                .orElse(List.of());
        return new HanziAdminView(
                entity.getId(),
                entity.getCharacter(),
                entity.getPinyin(),
                entity.getStrokeCount(),
                entity.getHskLevel(),
                entity.getStatus(),
                meanings);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static class HanziNotFoundException extends RuntimeException {
        public HanziNotFoundException(Long id) {
            super("Hanzi not found: " + id);
        }
    }

    public static class HanziNotPublishableException extends RuntimeException {
        public HanziNotPublishableException(Long id) {
            super("Hanzi " + id + " cannot be published: pinyin or English meanings are missing");
        }
    }
}
