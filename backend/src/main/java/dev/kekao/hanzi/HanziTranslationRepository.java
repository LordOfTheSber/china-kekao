package dev.kekao.hanzi;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface HanziTranslationRepository extends JpaRepository<HanziTranslationEntity, Long> {

    @Query("""
            SELECT ht FROM HanziTranslationEntity ht
            WHERE ht.hanzi.id = :hanziId
            """)
    List<HanziTranslationEntity> findByHanziId(@Param("hanziId") Long hanziId);

    /**
     * Projection returning (hanziId, meanings) rows directly so callers building a map by hanzi id
     * do not need to materialise translation entities (or their associated hanzi row).
     */
    @Query("""
            SELECT ht.hanzi.id, ht.meanings FROM HanziTranslationEntity ht
            WHERE ht.hanzi.id IN :hanziIds AND ht.language = :language
            """)
    List<Object[]> findMeaningsByHanziIdInAndLanguage(@Param("hanziIds") Collection<Long> hanziIds,
                                                     @Param("language") String language);

    @Query("""
            SELECT ht.meanings FROM HanziTranslationEntity ht
            WHERE ht.hanzi.id = :hanziId AND ht.language = :language
            """)
    List<List<String>> findMeaningsByHanziIdAndLanguage(@Param("hanziId") Long hanziId,
                                                        @Param("language") String language);
}
