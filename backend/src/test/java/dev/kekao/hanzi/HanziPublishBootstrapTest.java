package dev.kekao.hanzi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HanziPublishBootstrapTest {

    @Mock private HanziRepository hanzi;
    @Mock private HanziTranslationRepository translations;
    @InjectMocks private HanziPublishBootstrap bootstrap;

    @Test
    void promotesDraftsThatHavePinyinAndAtLeastOneEnglishMeaning() {
        HanziEntity complete = HanziEntity.builder()
                .id(1L).character("你").pinyin("nǐ").status(HanziStatus.DRAFT).build();
        HanziEntity missingPinyin = HanziEntity.builder()
                .id(2L).character("吗").pinyin("").status(HanziStatus.DRAFT).build();
        HanziEntity missingMeanings = HanziEntity.builder()
                .id(3L).character("呢").pinyin("ne").status(HanziStatus.DRAFT).build();

        when(hanzi.findAllByStatus(HanziStatus.DRAFT))
                .thenReturn(List.of(complete, missingPinyin, missingMeanings));
        when(translations.findByHanziId(1L)).thenReturn(List.of(translation("you")));
        when(translations.findByHanziId(3L)).thenReturn(List.of()); // no meanings

        int promoted = bootstrap.publishCompleteDrafts();

        assertThat(promoted).isEqualTo(1);
        ArgumentCaptor<HanziEntity> saved = ArgumentCaptor.forClass(HanziEntity.class);
        verify(hanzi, times(1)).save(saved.capture());
        assertThat(saved.getValue().getId()).isEqualTo(1L);
        assertThat(saved.getValue().getStatus()).isEqualTo(HanziStatus.PUBLISHED);
    }

    @Test
    void doesNothingWhenNoDrafts() {
        when(hanzi.findAllByStatus(HanziStatus.DRAFT)).thenReturn(List.of());
        assertThat(bootstrap.publishCompleteDrafts()).isZero();
        verify(hanzi, never()).save(any());
    }

    @Test
    void ignoresTranslationsInOtherLanguages() {
        HanziEntity entity = HanziEntity.builder()
                .id(7L).character("好").pinyin("hǎo").status(HanziStatus.DRAFT).build();
        when(hanzi.findAllByStatus(HanziStatus.DRAFT)).thenReturn(List.of(entity));
        HanziTranslationEntity ru = HanziTranslationEntity.builder()
                .hanzi(entity).language("ru").meanings(List.of("ты")).primary(false).build();
        when(translations.findByHanziId(7L)).thenReturn(List.of(ru));

        assertThat(bootstrap.publishCompleteDrafts()).isZero();
        verify(hanzi, never()).save(any());
    }

    @Test
    void ignoresEnglishTranslationsWithBlankMeanings() {
        HanziEntity entity = HanziEntity.builder()
                .id(8L).character("们").pinyin("men").status(HanziStatus.DRAFT).build();
        when(hanzi.findAllByStatus(HanziStatus.DRAFT)).thenReturn(List.of(entity));
        when(translations.findByHanziId(8L)).thenReturn(List.of(
                HanziTranslationEntity.builder()
                        .hanzi(entity).language("en").meanings(List.of("  "))
                        .primary(true).build()));

        assertThat(bootstrap.publishCompleteDrafts()).isZero();
        verify(hanzi, never()).save(any());
    }

    private HanziTranslationEntity translation(String... meanings) {
        return HanziTranslationEntity.builder()
                .language("en")
                .meanings(List.of(meanings))
                .primary(true)
                .build();
    }
}
