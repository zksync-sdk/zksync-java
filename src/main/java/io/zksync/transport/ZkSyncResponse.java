package io.zksync.transport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZkSyncResponse<T> {

    private int id;

    private String jsonrpc;

    private T result;

    private ZkSyncError error;
}
