package io.zksync.domain.transaction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import io.zksync.domain.Signature;
import io.zksync.domain.TimeRange;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForcedExit implements ZkSyncTransaction {

    private final String type = "ForcedExit";

    private Integer initiatorAccountId;

    private String target;

    private Integer token;

    private String fee;

    private Integer nonce;

    private Signature signature;

    @JsonUnwrapped
    private TimeRange timeRange;

    @JsonIgnore
    public BigInteger getFeeInteger() {
        return new BigInteger(fee);
    }
}
