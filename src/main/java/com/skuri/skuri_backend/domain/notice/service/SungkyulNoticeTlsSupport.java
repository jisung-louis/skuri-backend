package com.skuri.skuri_backend.domain.notice.service;

import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

@Slf4j
final class SungkyulNoticeTlsSupport {

    private static final SSLSocketFactory INSECURE_SOCKET_FACTORY = createInsecureSocketFactory();

    private SungkyulNoticeTlsSupport() {
    }

    static SSLSocketFactory insecureSocketFactory() {
        return INSECURE_SOCKET_FACTORY;
    }

    private static SSLSocketFactory createInsecureSocketFactory() {
        try {
            TrustManager[] trustManagers = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, new SecureRandom());
            log.warn("성결대학교 공지 수집에 한해 TLS 인증서 검증을 비활성화했습니다.");
            return sslContext.getSocketFactory();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("공지 수집용 insecure SSL socket factory 초기화에 실패했습니다.", e);
        }
    }
}
