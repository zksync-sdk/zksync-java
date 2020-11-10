package im.argent.zksync.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Signature {

    String pubKey;

    String signature;
}
