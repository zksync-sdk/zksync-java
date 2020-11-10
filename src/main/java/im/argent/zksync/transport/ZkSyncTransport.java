package im.argent.zksync.transport;

import java.util.List;

public interface ZkSyncTransport {

    <T> T send(String method, List<Object> params, Class<T> returntype);
}
