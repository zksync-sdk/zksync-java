package io.zksync.transport;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;

public interface ZkSyncTransport {

    <T> T send(String method, List<Object> params, Class<T> returntype);
    <T> T send(String method, List<Object> params, TypeReference<T> returntype);
}
