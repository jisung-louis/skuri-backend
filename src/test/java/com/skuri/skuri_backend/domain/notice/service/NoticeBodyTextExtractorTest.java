package com.skuri.skuri_backend.domain.notice.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NoticeBodyTextExtractorTest {

    @Test
    void extract_헤더개행테이블셀을_보존한텍스트로정규화한다() {
        String html = """
                <h2>수강신청 안내</h2>
                <p>첫 줄<br>둘째 줄</p>
                <table>
                  <tr><th>대상</th><td>전체 학년</td></tr>
                </table>
                """;

        String text = NoticeBodyTextExtractor.extract(html);

        assertTrue(text.contains("수강신청 안내"));
        assertTrue(text.contains("첫 줄"));
        assertTrue(text.contains("둘째 줄"));
        assertTrue(text.contains("대상"));
        assertTrue(text.contains("전체 학년"));
        assertTrue(text.contains("\n"));
    }

    @Test
    void extract_minifiedHtml에서도_구분자를유지한다() {
        String html = "<h3>학사공지</h3><p>첫줄<br>둘째줄</p><table><tr><th>대상</th><td>1학년</td></tr></table>";

        String text = NoticeBodyTextExtractor.extract(html);

        assertTrue(text.contains("학사공지\n첫줄"));
        assertTrue(text.contains("첫줄\n둘째줄"));
        assertTrue(text.contains("대상\t1학년"));
    }
}
