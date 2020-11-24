package io.zksync.domain.block;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockInfo {
    
    Integer blockNumber;
    Boolean committed;
    Boolean verified;

}
