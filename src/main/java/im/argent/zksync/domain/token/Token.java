package im.argent.zksync.domain.token;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Token {

    private Integer id;

    private String address;

    private String symbol;

    private Integer decimals;

    public String formatToken(BigInteger amount) {
        return new BigDecimal(amount).divide(BigDecimal.TEN.pow(decimals)).toString();
    }
}
