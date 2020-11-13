package io.zksync.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Signature {

    String pubKey;

    String signature;
}
