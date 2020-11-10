package im.argent.zksync.transport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZkSyncRequest {

    private final int id = 1;

    private final String jsonrpc = "2.0";

    private String method;

    private List<Object> params;
}
