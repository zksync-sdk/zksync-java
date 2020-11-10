package im.argent.zksync.signer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class EthSignature {

    private String type;

    private String signature;
}
