package io.zksync.domain.state;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccountState {

    private String address;

    private Integer id;

    private DepositingState depositing;

    private State committed;

    private State verified;
}
