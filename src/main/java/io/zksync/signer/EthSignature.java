package io.zksync.signer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class EthSignature {

    private SignatureType type;

    private String signature;

    public enum SignatureType {
        EthereumSignature,
        EIP1271Signature
    }
}
