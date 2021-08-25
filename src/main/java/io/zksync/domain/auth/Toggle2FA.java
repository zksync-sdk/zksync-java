package io.zksync.domain.auth;

import io.zksync.signer.EthSignature;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Toggle2FA {

    private boolean enable;

    private Integer accountId;

    private Long timestamp;

    private EthSignature signature;
}
