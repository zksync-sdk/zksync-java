package io.zksync.domain.transaction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import io.zksync.domain.Signature;
import io.zksync.domain.TimeRange;
import io.zksync.domain.auth.ChangePubKeyVariant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePubKey <T extends ChangePubKeyVariant> implements ZkSyncTransaction {

    private final String type = "ChangePubKey";

    private Integer accountId;

    private String account;

    private String newPkHash;

    private Integer feeToken;

    private String fee;

    private Integer nonce;

    private Signature signature;

    private T ethAuthData;

    @JsonUnwrapped
    private TimeRange timeRange;

    @JsonIgnore
    public BigInteger getFeeInteger() {
        return new BigInteger(fee);
    }
}
