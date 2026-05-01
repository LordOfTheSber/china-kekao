package dev.kekao.admin.imports;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CedictParserTest {

    private final CedictParser parser = new CedictParser();

    @Test
    void parsesStandardEntry() {
        CedictEntry entry = parser.parseLine("愛 爱 [ai4] /to love/to be fond of/");

        assertThat(entry).isNotNull();
        assertThat(entry.simplified()).isEqualTo("爱");
        assertThat(entry.traditional()).isEqualTo("愛");
        assertThat(entry.pinyin()).isEqualTo("ài");
        assertThat(entry.meanings()).containsExactly("to love", "to be fond of");
    }

    @Test
    void convertsAllToneNumbersToMarks() {
        assertThat(parser.parseLine("妈 妈 [ma1] /mom/").pinyin()).isEqualTo("mā");
        assertThat(parser.parseLine("麻 麻 [ma2] /hemp/").pinyin()).isEqualTo("má");
        assertThat(parser.parseLine("马 马 [ma3] /horse/").pinyin()).isEqualTo("mǎ");
        assertThat(parser.parseLine("骂 骂 [ma4] /to scold/").pinyin()).isEqualTo("mà");
        assertThat(parser.parseLine("吗 吗 [ma5] /(particle)/").pinyin()).isEqualTo("ma");
    }

    @Test
    void handlesUColonForUmlautAndMultipleSyllables() {
        CedictEntry entry = parser.parseLine("女 女 [nu:3] /female/woman/");

        assertThat(entry.pinyin()).isEqualTo("nǚ");
    }

    @Test
    void placesToneOnCorrectVowel() {
        // Rule: a > e > ou; otherwise the last vowel.
        assertThat(parser.parseLine("好 好 [hao3] /good/").pinyin()).isEqualTo("hǎo");
        assertThat(parser.parseLine("贵 贵 [gui4] /expensive/").pinyin()).isEqualTo("guì");
        assertThat(parser.parseLine("学 学 [xue2] /study/").pinyin()).isEqualTo("xué");
    }

    @Test
    void joinsMultipleSyllables() {
        CedictEntry entry = parser.parseLine("你好 你好 [ni3 hao3] /hello/");

        assertThat(entry.pinyin()).isEqualTo("nǐ hǎo");
    }

    @Test
    void skipsCommentAndBlankLines() {
        assertThat(parser.parseLine("# comment line")).isNull();
        assertThat(parser.parseLine("")).isNull();
        assertThat(parser.parseLine("   ")).isNull();
    }

    @Test
    void skipsMalformedLines() {
        assertThat(parser.parseLine("just garbage no brackets")).isNull();
        assertThat(parser.parseLine("爱 [ai4] /missing simplified/")).isNull();
    }

    @Test
    void parsesMultipleEntriesFromReader() throws Exception {
        String source = """
                # heading comment
                八 八 [ba1] /eight/8/

                爸 爸 [ba4] /father/dad/
                """;

        List<CedictEntry> entries = parser.parse(new StringReader(source));

        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).simplified()).isEqualTo("八");
        assertThat(entries.get(1).meanings()).containsExactly("father", "dad");
    }
}
