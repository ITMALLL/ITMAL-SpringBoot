package com.itmal.question.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/**
 * CKEditor로 작성된 question 본문 HTML을 저장 전에 정제(sanitize)한다.
 * 허용 태그/속성만 남기고 script 등 위험 요소를 제거하여 저장형 XSS를 방지한다.
 */
public final class HtmlSanitizer {

    private HtmlSanitizer() {
    }

    // CKEditor 기본 출력 태그 기준 화이트리스트
    private static final Safelist SAFELIST = Safelist.relaxed()
            .addTags("hr", "s", "figure", "figcaption", "span")
            // CKEditor 정렬/스타일용 class, style 허용
            .addAttributes(":all", "class", "style")
            // 링크 보안 속성
            .addAttributes("a", "target", "rel")
            .addProtocols("a", "href", "http", "https", "mailto")
            .addProtocols("img", "src", "http", "https", "data");

    private static final Document.OutputSettings OUTPUT_SETTINGS =
            new Document.OutputSettings().prettyPrint(false);

    public static String clean(String unsafeHtml) {
        if (unsafeHtml == null || unsafeHtml.isBlank()) {
            return unsafeHtml;
        }
        return Jsoup.clean(unsafeHtml, "", SAFELIST, OUTPUT_SETTINGS);
    }
}
