package im.argent.zksync.domain.fee;

import lombok.*;

import java.math.BigInteger;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionFee {

    String feeToken;

    BigInteger fee;
}
