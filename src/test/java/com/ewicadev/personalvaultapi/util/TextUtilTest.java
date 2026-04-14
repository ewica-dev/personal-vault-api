package com.ewicadev.personalvaultapi.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TextUtilTest {

  @Nested
  class NormalizeTitle {

    @Test
    void nullInputReturnsNull() {
      assertThat(TextUtil.normalizeTitle(null)).isNull();
    }

    @Test
    void trimWhitespace() {
      assertThat(TextUtil.normalizeTitle("  Hello  ")).isEqualTo("Hello");
    }

    @Test
    void collapseMultipleSpacesToSingleSpace() {
      assertThat(TextUtil.normalizeTitle("Hello    World")).isEqualTo("Hello World");
    }

    @Test
    void collapseMultipleWhitespaceToSingleSpace() {
      assertThat(TextUtil.normalizeTitle("Hello\t\nWorld")).isEqualTo("Hello World");
    }

    @Test
    void trimAndCollapseMultipleSpaces() {
      assertThat(TextUtil.normalizeTitle("  Hello   World  ")).isEqualTo("Hello World");
    }

    @Test
    void preserveInternalSingleSpace() {
      assertThat(TextUtil.normalizeTitle("Hello World")).isEqualTo("Hello World");
    }

    @Test
    void emptyStringBecomesEmpty() {
      assertThat(TextUtil.normalizeTitle("   ")).isEmpty();
    }
  }

  @Nested
  class NormalizeContent {

    @Test
    void nullInputReturnsNull() {
      assertThat(TextUtil.normalizeContent(null)).isNull();
    }

    @Test
    void crlfNormalizesToLf() {
      assertThat(TextUtil.normalizeContent("line1\r\nline2")).isEqualTo("line1\nline2");
    }

    @Test
    void carriageReturnNormalizesToLf() {
      assertThat(TextUtil.normalizeContent("line1\rline2")).isEqualTo("line1\nline2");
    }

    @Test
    void preservesLeadingWhitespace() {
      assertThat(TextUtil.normalizeContent("  leading spaces")).isEqualTo("  leading spaces");
    }

    @Test
    void preservesTrailingWhitespace() {
      assertThat(TextUtil.normalizeContent("trailing spaces  ")).isEqualTo("trailing spaces  ");
    }

    @Test
    void preservesLeadingNewlines() {
      assertThat(TextUtil.normalizeContent("\n\nparagraph")).isEqualTo("\n\nparagraph");
    }

    @Test
    void preservesTrailingNewlines() {
      assertThat(TextUtil.normalizeContent("paragraph\n\n")).isEqualTo("paragraph\n\n");
    }

    @Test
    void preservesBlankLinesBetweenParagraphs() {
      String input = "paragraph1\n\nparagraph2";
      assertThat(TextUtil.normalizeContent(input)).isEqualTo(input);
    }

    @Test
    void preservesInternalSpaces() {
      assertThat(TextUtil.normalizeContent("Hello   World")).isEqualTo("Hello   World");
    }

    @Test
    void preservesHtmlTags() {
      assertThat(TextUtil.normalizeContent("<script>alert(1)</script>")).isEqualTo("<script>alert(1)</script>");
    }

    @Test
    void preservesInequalitySigns() {
      assertThat(TextUtil.normalizeContent("2 < 3")).isEqualTo("2 < 3");
    }

    @Test
    void preservesCodeSnippet() {
      assertThat(TextUtil.normalizeContent("if (x < y) { return true; }")).isEqualTo("if (x < y) { return true; }");
    }

    @Test
    void normalizesMixedLineEndings() {
      assertThat(TextUtil.normalizeContent("line1\r\nline2\rline3\nline4"))
          .isEqualTo("line1\nline2\nline3\nline4");
    }

    @Test
    void preservesTabs() {
      assertThat(TextUtil.normalizeContent("Hello\tWorld")).isEqualTo("Hello\tWorld");
    }

    @Test
    void emptyStringReturnsEmpty() {
      assertThat(TextUtil.normalizeContent("")).isEmpty();
    }
  }
}
