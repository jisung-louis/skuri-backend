package com.skuri.skuri_backend.infra.auth.firebase;

import com.skuri.skuri_backend.domain.member.entity.Member;
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

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
        MemberRepository memberRepository = mock(MemberRepository.class);
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
        when(memberRepositoryProvider.getIfAvailable()).thenReturn(memberRepository);
        when(memberRepository.findById("firebase-uid")).thenReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/sse/notifications");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AtomicReference<Authentication> initialAuthenticationRef = new AtomicReference<>();
        FilterChain initialFilterChain = (servletRequest, servletResponse) ->
                initialAuthenticationRef.set(SecurityContextHolder.getContext().getAuthentication());

        filter.doFilter(request, response, initialFilterChain);

        request.setDispatcherType(DispatcherType.ASYNC);
        SecurityContextHolder.clearContext();

        AtomicReference<Authentication> asyncAuthenticationRef = new AtomicReference<>();
        FilterChain asyncFilterChain = (servletRequest, servletResponse) ->
                asyncAuthenticationRef.set(SecurityContextHolder.getContext().getAuthentication());

        filter.doFilter(request, response, asyncFilterChain);

        assertThat(initialAuthenticationRef.get()).isNotNull();
        assertThat(asyncAuthenticationRef.get()).isNotNull();
        assertThat(asyncAuthenticationRef.get()).isEqualTo(initialAuthenticationRef.get());
        assertThat(asyncAuthenticationRef.get().getPrincipal()).isEqualTo(new AuthenticatedMember(
                "firebase-uid",
                "user@sungkyul.ac.kr",
                "google.com",
                "google-provider-id",
                "홍길동",
                "https://example.com/profile.jpg"
        ));
        verify(tokenVerifier, times(1)).verify("valid-token");
        verify(memberRepository, times(1)).findById("firebase-uid");
    }

    @Test
    void async재디스패치에서는_탈퇴상태를_재검사하지않는다() throws Exception {
        FirebaseTokenVerifier tokenVerifier = mock(FirebaseTokenVerifier.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<MemberRepository> memberRepositoryProvider = mock(ObjectProvider.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
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
        when(memberRepositoryProvider.getIfAvailable()).thenReturn(memberRepository);
        Member withdrawnMember = mock(Member.class);
        when(withdrawnMember.isWithdrawn()).thenReturn(true);
        when(memberRepository.findById("firebase-uid"))
                .thenReturn(Optional.empty(), Optional.of(withdrawnMember));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/sse/notifications");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            // no-op
        });

        request.setDispatcherType(DispatcherType.ASYNC);
        SecurityContextHolder.clearContext();

        AtomicReference<Authentication> asyncAuthenticationRef = new AtomicReference<>();
        FilterChain asyncFilterChain = (servletRequest, servletResponse) ->
                asyncAuthenticationRef.set(SecurityContextHolder.getContext().getAuthentication());

        filter.doFilter(request, response, asyncFilterChain);

        assertThat(asyncAuthenticationRef.get()).isNotNull();
        verify(tokenVerifier, times(1)).verify("valid-token");
        verify(memberRepository, times(1)).findById("firebase-uid");
        verify(accessDeniedHandler, never()).handle(any(), any(), any());
    }
}
