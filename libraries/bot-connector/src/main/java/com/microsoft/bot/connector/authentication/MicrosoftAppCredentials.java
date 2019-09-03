// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.bot.connector.authentication;

import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;
import com.microsoft.rest.credentials.ServiceClientCredentials;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

/**
 * MicrosoftAppCredentials auth implementation
 */
public class MicrosoftAppCredentials implements ServiceClientCredentials {
    public static final String MICROSOFTAPPID = "MicrosoftAppId";
    public static final String MICROSOFTAPPPASSWORD = "MicrosoftAppPassword";


    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static ConcurrentMap<String, LocalDateTime> trustHostNames = new ConcurrentHashMap<>();

    static {
        trustHostNames.put("api.botframework.com", LocalDateTime.MAX);
        trustHostNames.put("token.botframework.com", LocalDateTime.MAX);
        trustHostNames.put("api.botframework.azure.us", LocalDateTime.MAX);
        trustHostNames.put("token.botframework.azure.us", LocalDateTime.MAX);
    }

    private String appId;
    private String appPassword;
    private String channelAuthTenant;
    private AdalAuthenticator authenticator;

    public MicrosoftAppCredentials(String appId, String appPassword) {
        this.appId = appId;
        this.appPassword = appPassword;
    }

    public MicrosoftAppCredentials(String appId, String appPassword, String channelAuthTenant) throws MalformedURLException {
        this.appId = appId;
        this.appPassword = appPassword;
        setChannelAuthTenant(channelAuthTenant);
    }

    public static MicrosoftAppCredentials empty() {
        return new MicrosoftAppCredentials(null, null);
    }

    public static void trustServiceUrl(URI serviceUrl) {
        trustServiceUrl(serviceUrl.toString(), LocalDateTime.now().plusDays(1));
    }

    public static void trustServiceUrl(String serviceUrl) {
        trustServiceUrl(serviceUrl, LocalDateTime.now().plusDays(1));
    }

    public static void trustServiceUrl(String serviceUrl, LocalDateTime expirationTime) {
        try {
            URL url = new URL(serviceUrl);
            trustServiceUrl(url, expirationTime);
        } catch (MalformedURLException e) {
            LoggerFactory.getLogger(MicrosoftAppCredentials.class).error("trustServiceUrl", e);
        }
    }

    public static void trustServiceUrl(URL serviceUrl, LocalDateTime expirationTime) {
        trustHostNames.put(serviceUrl.getHost(), expirationTime);
    }

    public static boolean isTrustedServiceUrl(String serviceUrl) {
        try {
            URL url = new URL(serviceUrl);
            return isTrustedServiceUrl(url);
        } catch (MalformedURLException e) {
            LoggerFactory.getLogger(MicrosoftAppCredentials.class).error("trustServiceUrl", e);
            return false;
        }
    }

    public static boolean isTrustedServiceUrl(URL url) {
        return !trustHostNames.getOrDefault(url.getHost(), LocalDateTime.MIN).isBefore(LocalDateTime.now().minusMinutes(5));
    }

    public static boolean isTrustedServiceUrl(HttpUrl url) {
        return !trustHostNames.getOrDefault(url.host(), LocalDateTime.MIN).isBefore(LocalDateTime.now().minusMinutes(5));
    }

    public String appId() {
        return this.appId;
    }

    public String appPassword() {
        return this.appPassword;
    }

    public MicrosoftAppCredentials withAppId(String appId) {
        this.appId = appId;
        return this;
    }

    public MicrosoftAppCredentials withAppPassword(String appPassword) {
        this.appPassword = appPassword;
        return this;
    }

    public String channelAuthTenant() {
        return channelAuthTenant == null ? AuthenticationConstants.DEFAULT_CHANNEL_AUTH_TENANT : channelAuthTenant;
    }

    public void setChannelAuthTenant(String authTenant) throws MalformedURLException {
        String originalAuthTenant = channelAuthTenant;
        try {
            channelAuthTenant = authTenant;
            new URL(oAuthEndpoint()).toString();
        } catch(MalformedURLException e) {
            channelAuthTenant = originalAuthTenant;
        }
    }

    public MicrosoftAppCredentials withChannelAuthTenant(String authTenant) throws MalformedURLException {
        setChannelAuthTenant(authTenant);
        return this;
    }

    public String oAuthEndpoint() {
        return String.format(AuthenticationConstants.TO_CHANNEL_FROM_BOT_LOGIN_URL_TEMPLATE, channelAuthTenant());
    }

    public String oAuthScope() {
        return AuthenticationConstants.TO_CHANNEL_FROM_BOT_OAUTH_SCOPE;
    }

    public Future<AuthenticationResult> getToken() {
        return getAuthenticator().acquireToken();
    }

    protected boolean ShouldSetToken(String url) {
        return isTrustedServiceUrl(url);
    }

    private AdalAuthenticator getAuthenticator() {
        try {
            if (this.authenticator == null) {
                this.authenticator = new AdalAuthenticator(
                    new ClientCredential(this.appId, this.appPassword),
                    new OAuthConfiguration(oAuthEndpoint(), oAuthScope()));
            }
        } catch(MalformedURLException e) {
            // intentional no-op.  This class validates the URL on construction or setChannelAuthTenant.
            // That is... this will never happen.
            LoggerFactory.getLogger(MicrosoftAppCredentials.class).error("getAuthenticator", e);
        }

        return this.authenticator;
    }

    @Override
    public void applyCredentialsFilter(OkHttpClient.Builder clientBuilder) {
        clientBuilder.interceptors().add(new MicrosoftAppCredentialsInterceptor(this));
    }
}
