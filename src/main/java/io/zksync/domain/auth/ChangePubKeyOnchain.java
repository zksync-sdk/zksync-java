package io.zksync.domain.auth;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChangePubKeyOnchain implements ChangePubKeyVariant {

    private final ChangePubKeyAuthType type = ChangePubKeyAuthType.Onchain;

    @Override
    public byte[] getBytes() {
        return new byte[32];
    }

}
