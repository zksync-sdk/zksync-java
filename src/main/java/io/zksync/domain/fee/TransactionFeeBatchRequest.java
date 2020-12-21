package io.zksync.domain.fee;

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.apache.commons.lang3.tuple.Pair;

import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionFeeBatchRequest {

    @JsonIgnore
    @Singular
    private List<Pair<TransactionType, String>> transactionTypes;

    @Getter
    private String tokenIdentifier;

    public List<TransactionType> getTransactionTypes() {
        return this.transactionTypes
            .stream()
            .map(Pair::getLeft)
            .collect(Collectors.toList());
    }

    public List<Object> getTransactionTypesRaw() {
        return this.transactionTypes
            .stream()
            .map(Pair::getLeft)
            .map(TransactionType::getRaw)
            .collect(Collectors.toList());
    }

    public List<String> getAddresses() {
        return this.transactionTypes
            .stream()
            .map(Pair::getRight)
            .collect(Collectors.toList());
    }
    
}
