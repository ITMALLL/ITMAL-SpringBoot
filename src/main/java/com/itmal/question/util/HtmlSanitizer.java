package com.itmal.question.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

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

    // HTML 본문에서 태그를 제거한 평문 추출 (Papago 번역 입력용)
    public static String toPlainText(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }
        return Jsoup.parse(html).text();
    }
}
