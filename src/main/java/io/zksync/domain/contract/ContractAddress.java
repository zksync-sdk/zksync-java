package io.zksync.domain.contract;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractAddress {
    
    String mainContract;
    String govContract;

}
