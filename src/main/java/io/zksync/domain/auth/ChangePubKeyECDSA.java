package io.zksync.domain.auth;

import org.web3j.utils.Numeric;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChangePubKeyECDSA implements ChangePubKeyVariant {

    private final ChangePubKeyAuthType type = ChangePubKeyAuthType.ECDSA;

    private String ethSignature;
    private String batchHash;

    @Override
    public byte[] getBytes() {
        return Numeric.hexStringToByteArray(batchHash);
    }

}
