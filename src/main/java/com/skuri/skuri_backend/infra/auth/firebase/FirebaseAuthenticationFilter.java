package com.skuri.skuri_backend.infra.auth.firebase;

import com.skuri.skuri_backend.common.exception.BusinessException;
import com.skuri.skuri_backend.domain.member.entity.Member;
import com.skuri.skuri_backend.domain.member.repository.MemberRepository;
import com.skuri.skuri_backend.infra.auth.config.ApiAccessDeniedHandler;
import com.skuri.skuri_backend.infra.auth.config.ApiAuthenticationEntryPoint;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final FirebaseTokenVerifier firebaseTokenVerifier;
    private final ObjectProvider<MemberRepository> memberRepositoryProvider;
    private final ApiAuthenticationEntryPoint authenticationEntryPoint;
    private final ApiAccessDeniedHandler accessDeniedHandler;

    @Value("${security.allowed-email-domain:sungkyul.ac.kr}")
    private String allowedEmailDomain;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!HttpMethod.GET.matches(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        return uri.startsWith("/v1/app-notices/") || "/v1/app-notices".equals(uri) || uri.startsWith("/v1/app-versions/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String idToken = resolveIdToken(request);
        if (!StringUtils.hasText(idToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            FirebaseTokenClaims claims = firebaseTokenVerifier.verify(idToken);
            validateEmailDomain(claims.email());

            AuthenticatedMember principal = AuthenticatedMember.from(claims);
            ensureMemberAccessAllowed(request, principal.uid());
            Collection<? extends GrantedAuthority> authorities = resolveAuthorities(principal.uid());
            UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                    principal,
                    null,
                    authorities
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);

            filterChain.doFilter(request, response);
        } catch (EmailDomainRestrictedException e) {
            SecurityContextHolder.clearContext();
            accessDeniedHandler.handle(request, response, e);
        } catch (WithdrawnMemberAccessDeniedException e) {
            SecurityContextHolder.clearContext();
            accessDeniedHandler.handle(request, response, e);
        } catch (BusinessException e) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response, new BadCredentialsException(e.getMessage(), e));
        }
    }

    private String resolveIdToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(BEARER_PREFIX.length()).trim();
    }

    private void validateEmailDomain(String email) {
        if (!StringUtils.hasText(email)) {
            throw new EmailDomainRestrictedException();
        }
        String normalizedAllowedDomain = "@" + allowedEmailDomain.toLowerCase(Locale.ROOT);
        if (!email.toLowerCase(Locale.ROOT).endsWith(normalizedAllowedDomain)) {
            throw new EmailDomainRestrictedException();
        }
    }

    private void ensureMemberAccessAllowed(HttpServletRequest request, String uid) {
        if (isMemberBootstrapRequest(request)) {
            return;
        }

        MemberRepository memberRepository = memberRepositoryProvider.getIfAvailable();
        if (memberRepository == null) {
            return;
        }

        memberRepository.findById(uid)
                .filter(Member::isWithdrawn)
                .ifPresent(member -> {
                    throw new WithdrawnMemberAccessDeniedException();
                });
    }

    private boolean isMemberBootstrapRequest(HttpServletRequest request) {
        return HttpMethod.POST.matches(request.getMethod()) && "/v1/members".equals(request.getRequestURI());
    }

    private Collection<? extends GrantedAuthority> resolveAuthorities(String uid) {
        MemberRepository memberRepository = memberRepositoryProvider.getIfAvailable();
        if (memberRepository == null) {
            return Collections.emptyList();
        }
        return memberRepository.findById(uid)
                .filter(Member::isAdmin)
                .map(value -> List.of(new SimpleGrantedAuthority(ROLE_ADMIN)))
                .orElse(Collections.emptyList());
    }
}
