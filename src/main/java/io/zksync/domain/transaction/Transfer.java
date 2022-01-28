package io.zksync.domain.transaction;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import io.zksync.domain.Signature;
import io.zksync.domain.TimeRange;
import io.zksync.domain.token.TokenId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transfer implements ZkSyncTransaction {

    private final String type = "Transfer";

    private Integer accountId;

    private String from;

    private String to;

    private Integer token;

    @JsonIgnore
    private BigInteger amount;

    private String fee;

    private Integer nonce;

    private Signature signature;

    @JsonIgnore
    private TokenId tokenId;

    @JsonUnwrapped
    private TimeRange timeRange;

    @JsonGetter("amount")
    public String getAmountString() {
        return amount.toString();
    }

    @JsonSetter("amount")
    public void setAmountString(String amount) {
        this.amount = new BigInteger(amount);
    }

    @JsonIgnore
    public BigInteger getFeeInteger() {
        return new BigInteger(fee);
    }
}
