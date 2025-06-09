package com.davi.models;
import com.davi.config.AppConfig;
import com.davi.config.HttpClientProvider;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


public class OpenAI implements ChatModel{

    private AppConfig config = new AppConfig();
    private final HttpClient client = HttpClientProvider.CLIENT;
    private final String api_key = config.get("openai.api.key");
    private final String OPEN_AI_URI = "https://api.openai.com/v1/responses";
    private final String model = config.get("openai.model");

    @Override
    public String chat(String input) {
        if (api_key == null){
            throw new IllegalStateException("Must provide api key");
        }
        try{

            JSONObject body = new JSONObject()
                    .put("model", model)
                    .put("input" ,input);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(OPEN_AI_URI))
                    .header("Authorization", "Bearer " + api_key)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            JSONObject json = new JSONObject(res.body());
            return json.getJSONArray("output")
                    .getJSONObject(0).getJSONArray("content")
                    .getJSONObject(0).getString("text");

        }
        catch (Exception e){
            e.printStackTrace();
            return "An error has occurred" + e.getMessage();

        }


    }
}
