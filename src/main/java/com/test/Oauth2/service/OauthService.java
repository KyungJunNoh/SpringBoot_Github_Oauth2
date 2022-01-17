package com.test.Oauth2.service;

import com.test.Oauth2.attributes.OauthAttributes;
import com.test.Oauth2.dto.OauthTokenResponse;
import com.test.Oauth2.entity.Member;
import com.test.Oauth2.profile.UserProfile;
import com.test.Oauth2.provider.JwtTokenProvider;
import com.test.Oauth2.provider.OauthProvider;
import com.test.Oauth2.repository.InMemoryProviderRepository;
import com.test.Oauth2.repository.MemberRepository;
import com.test.Oauth2.response.LoginResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

@Service
public class OauthService {

    private final InMemoryProviderRepository inMemoryProviderRepository;
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    public OauthService(InMemoryProviderRepository inMemoryProviderRepository, MemberRepository memberRepository, JwtTokenProvider jwtTokenProvider) {
        this.inMemoryProviderRepository = inMemoryProviderRepository;
        this.memberRepository = memberRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public LoginResponse login(String providerName, String code) {
        // 프론트에서 넘어온 provider 이름을 통해 InMemoryProviderRepository에서 OauthProvider 가져오기
        OauthProvider provider = inMemoryProviderRepository.findByProviderName(providerName);

        // TODO access token 가져오기
        OauthTokenResponse tokenResponse = getToken(code, provider);

        // TODO 유저 정보 가져오기
        UserProfile userProfile = getUserProfile(providerName, tokenResponse, provider);

        // TODO 유저 DB에 저장
        Member member = saveOrUpdate(userProfile);

        // 우리 애플리케이션의 JWT 토큰 만들기
        String accessToken = jwtTokenProvider.createAccessToken(String.valueOf(member.getId()));
        String refreshToken = jwtTokenProvider.createRefreshToken();

        return LoginResponse.builder()
                .id(member.getId())
                .name(member.getName())
                .email(member.getEmail())
                .imageUrl(member.getImageUrl())
                .role(member.getRole())
                .tokenType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    private OauthTokenResponse getToken(String code, OauthProvider provider) {
        return WebClient.create() // WebClient 를 만든다.
                .post() // post 방식으로
                .uri(provider.getTokenUrl()) // application-oauth.yml 파일에 명시된 https://github.com/login/oauth/access_token 링크로
                .headers(header -> { // 헤더에 정보를 실는다.
                    header.setBasicAuth(provider.getClientId(), provider.getClientSecret());
                    header.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                    header.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                    header.setAcceptCharset(Collections.singletonList(StandardCharsets.UTF_8));
                })
                .bodyValue(tokenRequest(code, provider))
                .retrieve()
                .bodyToMono(OauthTokenResponse.class) // Mono 방식으로
                .block();
    }

    private MultiValueMap<String, String> tokenRequest(String code, OauthProvider provider) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>(); // TODO MultiValueMap 이란?
        formData.add("code", code);
        formData.add("grant_type", "authorization_code");
        formData.add("redirect_uri", provider.getRedirectUrl());
        return formData;
    }

    private UserProfile getUserProfile(String providerName, OauthTokenResponse tokenResponse, OauthProvider provider) {
        Map<String, Object> userAttributes = getUserAttributes(provider, tokenResponse);
        // TODO 유저 정보(map)를 통해 UserProfile 만들기
        return OauthAttributes.extract(providerName, userAttributes);
    }

    // OAuth 서버에서 유저 정보 map으로 가져오기
    private Map<String, Object> getUserAttributes(OauthProvider provider, OauthTokenResponse tokenResponse) {
        return WebClient.create()
                .get()
                .uri(provider.getUserInfoUrl())
                .headers(header -> header.setBearerAuth(tokenResponse.getAccessToken()))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
    }

    private Member saveOrUpdate(UserProfile userProfile) {
        Member member = memberRepository.findByOauthId(userProfile.getOauthId())
                .map(entity -> entity.update(
                        userProfile.getEmail(), userProfile.getName(), userProfile.getImageUrl())) // 깃허브에서 정보 변경이 있을 수도 있기 때문에 매 요청마다 업데이트
                .orElseGet(userProfile::toMember);
        return memberRepository.save(member);
    }

}
