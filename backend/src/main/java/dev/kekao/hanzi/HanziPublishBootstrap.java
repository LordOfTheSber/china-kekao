package dev.kekao.hanzi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Promotes DRAFT hanzi that already carry a non-blank pinyin and at least
 * one non-empty English meaning to {@link HanziStatus#PUBLISHED}.
 *
 * <p>The CC-CEDICT importer creates entries in DRAFT so an admin can
 * curate them before they go live. In practice the imported data is
 * usually good enough out of the box, so we promote the obvious wins on
 * every startup. Anything still in DRAFT after this pass needs manual
 * editing (missing pinyin or meanings).</p>
 *
 * <p>Runs before {@link dev.kekao.deck.DeckBootstrap} via {@link Order}
 * so that the system HSK decks are populated with the freshly published
 * characters on the same startup.</p>
 */
@Component
@Order(1)
public class HanziPublishBootstrap {

    private static final Logger log = LoggerFactory.getLogger(HanziPublishBootstrap.class);

    private final HanziRepository hanzi;
    private final HanziTranslationRepository translations;

    @Autowired
    public HanziPublishBootstrap(HanziRepository hanzi, HanziTranslationRepository translations) {
        this.hanzi = hanzi;
        this.translations = translations;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            int promoted = publishCompleteDrafts();
            if (promoted > 0) {
                log.info("Auto-published {} DRAFT hanzi with complete data", promoted);
            }
        } catch (RuntimeException ex) {
            log.warn("Hanzi auto-publish failed; will retry on next startup", ex);
        }
    }

    @Transactional
    public int publishCompleteDrafts() {
        List<HanziEntity> drafts = hanzi.findAllByStatus(HanziStatus.DRAFT);
        if (drafts.isEmpty()) return 0;
        int promoted = 0;
        for (HanziEntity entity : drafts) {
            if (!isComplete(entity)) continue;
            entity.setStatus(HanziStatus.PUBLISHED);
            hanzi.save(entity);
            promoted++;
        }
        return promoted;
    }

    private boolean isComplete(HanziEntity entity) {
        if (entity.getPinyin() == null || entity.getPinyin().isBlank()) return false;
        return translations.findByHanziId(entity.getId()).stream()
                .filter(t -> "en".equals(t.getLanguage()))
                .anyMatch(t -> t.getMeanings() != null
                        && t.getMeanings().stream().anyMatch(m -> m != null && !m.isBlank()));
    }
}
