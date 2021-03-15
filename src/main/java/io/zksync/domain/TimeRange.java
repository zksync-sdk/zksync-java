package io.zksync.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TimeRange {
    private long validFrom;
    private long validUntil;

    public TimeRange() {
        this.validFrom = 0;
        this.validUntil = 4294967295L;
    }
}
