package io.zksync.domain.swap;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import org.web3j.tuples.generated.Tuple2;

import io.zksync.domain.Signature;
import io.zksync.domain.TimeRange;
import io.zksync.signer.EthSignature;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    
    private Integer accountId;

    @JsonIgnore
    private String recipientAddress;

    private Integer nonce;

    private Integer tokenBuy;

    private Integer tokenSell;

    @JsonIgnore
    private Tuple2<BigInteger, BigInteger> ratio;

    private BigInteger amount;

    private Signature signature;

    @JsonGetter
    public String getRecipient() {
        return this.recipientAddress;
    }

    @JsonSetter
    public void setRecipient(String recipientAddress) {
        this.recipientAddress = recipientAddress;
    }

    @JsonGetter("ratio")
    public List<BigInteger> getRatioJson() {
        return Arrays.asList(ratio.component1(), ratio.component2());
    }

    @JsonSetter("ratio")
    public void setRatio(List<BigInteger> ratio) {
        if (ratio == null || ratio.size() != 2) {
            throw new IllegalArgumentException("Incorrect amount of ratio");
        }
        this.ratio = new Tuple2<>(ratio.get(0), ratio.get(1));
    }

    @JsonIgnore
    private EthSignature ethereumSignature;

    @JsonUnwrapped
    private TimeRange timeRange;
}
