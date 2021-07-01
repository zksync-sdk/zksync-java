package io.zksync.transport;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ZkSyncTransport {

    <R, T extends ZkSyncResponse<R>> R send(String method, List<Object> params, Class<T> returntype);
    <R, T extends ZkSyncResponse<R>> CompletableFuture<R> sendAsync(String method, List<Object> params, Class<T> returntype);
}
