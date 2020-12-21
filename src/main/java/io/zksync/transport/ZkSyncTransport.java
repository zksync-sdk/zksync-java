package io.zksync.transport;

import java.util.List;

public interface ZkSyncTransport {

    <R, T extends ZkSyncResponse<R>> R send(String method, List<Object> params, Class<T> returntype);
}
