package com.test.Oauth2.adaptor;

import com.test.Oauth2.properties.OauthProperties;
import com.test.Oauth2.provider.OauthProvider;

import java.util.HashMap;
import java.util.Map;

public class OauthAdapter {

    private OauthAdapter() {}

    public static Map<String, OauthProvider> getOauthProviders(OauthProperties properties) {
        Map<String, OauthProvider> oauthProvider = new HashMap<>();

        properties.getUser().forEach((key, value) -> oauthProvider.put(key,
                new OauthProvider(value, properties.getProvider().get(key))));
        return oauthProvider;
    }
}
