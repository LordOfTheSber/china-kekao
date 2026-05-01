package dev.kekao.admin.imports;

import java.util.List;

/**
 * Single dictionary entry parsed from a CC-CEDICT line.
 *
 * @param simplified  simplified Chinese form
 * @param traditional traditional form
 * @param pinyin      space-separated pinyin with tone marks
 *                    (already converted from CC-CEDICT's numeric form)
 * @param meanings    English glosses, in source order
 */
public record CedictEntry(
        String simplified,
        String traditional,
        String pinyin,
        List<String> meanings
) {
}
