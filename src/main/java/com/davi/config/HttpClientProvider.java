package com.davi.config;

import java.net.http.HttpClient;

public class HttpClientProvider {
    public static final HttpClient CLIENT = HttpClient.newHttpClient();
}