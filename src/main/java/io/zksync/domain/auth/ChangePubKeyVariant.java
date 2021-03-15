package io.zksync.domain.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface ChangePubKeyVariant {

    ChangePubKeyAuthType getType();

    @JsonIgnore
    byte[] getBytes();
}
