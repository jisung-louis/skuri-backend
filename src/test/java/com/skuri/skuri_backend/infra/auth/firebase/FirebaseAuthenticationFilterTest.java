package com.skuri.skuri_backend.infra.auth.firebase;

import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.infra.auth.config.ApiAccessDeniedHandler;
import com.skuri.skuri_backend.infra.auth.config.ApiAuthenticationEntryPoint;
import com.skuri.skuri_backend.infra.storage.MediaStorageProperties;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FirebaseAuthenticationFilterTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void async디스패치에서도_인증을_복원한다() throws Exception {
        FirebaseTokenVerifier tokenVerifier = mock(FirebaseTokenVerifier.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<MemberRepository> memberRepositoryProvider = mock(ObjectProvider.class);
        ApiAuthenticationEntryPoint authenticationEntryPoint = mock(ApiAuthenticationEntryPoint.class);
        ApiAccessDeniedHandler accessDeniedHandler = mock(ApiAccessDeniedHandler.class);
        MediaStorageProperties mediaStorageProperties = new MediaStorageProperties();
        FirebaseAuthenticationFilter filter = new FirebaseAuthenticationFilter(
                tokenVerifier,
                memberRepositoryProvider,
                authenticationEntryPoint,
                accessDeniedHandler,
                mediaStorageProperties
        );
        ReflectionTestUtils.setField(filter, "allowedEmailDomain", "sungkyul.ac.kr");

        when(tokenVerifier.verify("valid-token"))
                .thenReturn(new FirebaseTokenClaims(
                        "firebase-uid",
                        "user@sungkyul.ac.kr",
                        "google.com",
                        "google-provider-id",
                        "홍길동",
                        "https://example.com/profile.jpg"
                ));
        when(memberRepositoryProvider.getIfAvailable()).thenReturn(null);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/sse/notifications");
        request.setDispatcherType(DispatcherType.ASYNC);
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<Authentication> authenticationRef = new AtomicReference<>();
        FilterChain filterChain = (servletRequest, servletResponse) ->
                authenticationRef.set(SecurityContextHolder.getContext().getAuthentication());

        filter.doFilter(request, response, filterChain);

        assertThat(authenticationRef.get()).isNotNull();
        assertThat(authenticationRef.get().getPrincipal()).isEqualTo(new AuthenticatedMember(
                "firebase-uid",
                "user@sungkyul.ac.kr",
                "google.com",
                "google-provider-id",
                "홍길동",
                "https://example.com/profile.jpg"
        ));
    }
}
