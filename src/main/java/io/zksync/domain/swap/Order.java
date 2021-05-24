package io.zksync.domain.swap;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import org.web3j.tuples.generated.Tuple2;

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
public class Order {
    
    private Integer accountId;

    private String recipientAddress;

    private Integer nonce;

    private Integer tokenBuy;

    private Integer tokenSell;

    private Tuple2<BigInteger, BigInteger> ratio;

    private BigInteger amount;

    private Signature signature;

    @JsonUnwrapped
    private TimeRange timeRange;
}
