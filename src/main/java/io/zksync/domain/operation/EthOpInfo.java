package io.zksync.domain.operation;

import io.zksync.domain.block.BlockInfo;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EthOpInfo {
    
    Boolean executed;
    BlockInfo block;

}
