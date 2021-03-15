package io.zksync.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zksync.exception.ZkSyncException;
import okhttp3.*;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

public class HttpTransport implements ZkSyncTransport {

    public static final MediaType APPLICATION_JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;

    private final String url;

    private final ObjectMapper objectMapper;

    public HttpTransport(String url) {
        this.url = url;

        httpClient = new OkHttpClient
                .Builder()
                .callTimeout(Duration.ofSeconds(5))
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        objectMapper = new ObjectMapper();
    }

    @Override
    public <R, T extends ZkSyncResponse<R>> R send(String method, List<Object> params, Class<T> returntype) {
        try {
            final ZkSyncRequest zkRequest = ZkSyncRequest
                    .builder()
                    .method(method)
                    .params(params)
                    .build();
            final String bodyJson = objectMapper.writeValueAsString(zkRequest);
            final RequestBody body = RequestBody.create(bodyJson, APPLICATION_JSON);

            final Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            final Response response = httpClient.newCall(request).execute();

            final String responseString = response.body().string();

            final ZkSyncResponse<R> resultJson = objectMapper.readValue(responseString, returntype);

            if (resultJson.getError() != null) {
                throw new ZkSyncException(resultJson.getError());
            }

            return resultJson.getResult();

        } catch (IOException e) {
            throw new ZkSyncException("There was an error when sending the request", e);
        }
    }
}
