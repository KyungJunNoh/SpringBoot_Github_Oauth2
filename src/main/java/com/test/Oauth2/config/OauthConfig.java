package com.test.Oauth2.config;

import com.test.Oauth2.adaptor.OauthAdapter;
import com.test.Oauth2.properties.OauthProperties;
import com.test.Oauth2.provider.OauthProvider;
import com.test.Oauth2.repository.InMemoryProviderRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@EnableConfigurationProperties(OauthProperties.class)
public class OauthConfig {

    private final OauthProperties properties;

    public OauthConfig(OauthProperties properties) {
        this.properties = properties;
    }

    // 추가된 부분
    @Bean
    public InMemoryProviderRepository inMemoryProviderRepository() {
        Map<String, OauthProvider> providers = OauthAdapter.getOauthProviders(properties);
        return new InMemoryProviderRepository(providers);
    }
}
