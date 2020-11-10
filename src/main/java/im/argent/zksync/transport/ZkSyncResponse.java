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
public class ZkSyncResponse<T> {

    private int id;

    private String jsonrpc;

    private T result;
}
