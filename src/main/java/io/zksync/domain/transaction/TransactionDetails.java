package io.zksync.domain.transaction;

import io.zksync.domain.block.BlockInfo;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDetails {

    Boolean executed;
    Boolean success;
    String failReason;
    BlockInfo block;
    
}
