package io.zksync.domain.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChangePubKeyCREATE2 implements ChangePubKeyVariant {

    private final ChangePubKeyAuthType type = ChangePubKeyAuthType.CREATE2;

    private String creatorAddress;
    private String saltArg;
    private String codeHash;

    @Override
    public byte[] getBytes() {
        return new byte[32];
    }

}
