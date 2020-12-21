package io.zksync.transport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZkSyncRequest {
    private static AtomicLong nextId = new AtomicLong(0);

    private final long id = nextId.getAndIncrement();

    private final String jsonrpc = "2.0";

    private String method;

    private List<Object> params;

}
