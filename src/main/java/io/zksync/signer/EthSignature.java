package io.zksync.signer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class EthSignature {

    private SignatureType type;

    private String signature;

    public enum SignatureType {
        EthereumSignature,
        EIP1271Signature
    }
}
