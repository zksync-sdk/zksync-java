package im.argent.zksync.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import im.argent.zksync.domain.fee.TransactionFeeDetails;
import im.argent.zksync.domain.fee.TransactionFeeRequest;
import im.argent.zksync.domain.fee.TransactionType;
import im.argent.zksync.domain.state.AccountState;
import im.argent.zksync.domain.token.Token;
import im.argent.zksync.domain.token.Tokens;
import im.argent.zksync.domain.transaction.ZkSyncTransaction;
import im.argent.zksync.exception.ZkSyncException;
import im.argent.zksync.signer.EthSignature;
import im.argent.zksync.transport.ZkSyncTransport;
import lombok.AllArgsConstructor;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

@AllArgsConstructor
public class DefaultProvider implements Provider {

    private ZkSyncTransport transport;

    @Override
    public AccountState getState(String accountAddress) {

        final AccountState response = transport.send("account_info",
                Collections.singletonList(accountAddress), AccountState.class);

        return response;
    }

    @Override
    public TransactionFeeDetails getTransactionFee(TransactionFeeRequest feeRequest) {
        try {

            Object transactionType = feeRequest.getTransactionType().getFeeIdentifier();

            if (feeRequest.getTransactionType() == TransactionType.CHANGE_PUB_KEY) {
                transactionType = new ObjectMapper().readValue("{ \"ChangePubKey\": { \"onchainPubkeyAuth\": false }}", JsonNode.class);
            }

            return transport.send("get_tx_fee", Arrays.asList(
                    transactionType,
                    feeRequest.getAddress(),
                    feeRequest.getTokenIdentifier()), TransactionFeeDetails.class);

        } catch (IOException e) {
            throw new ZkSyncException(e);
        }
    }

    @Override
    public Tokens getTokens() {
        try {
            //TODO fix this parsing (this ends up being triple parsed!)
            final JsonNode responseNode = transport.send("tokens", Collections.emptyList(), JsonNode.class);

            final Map<String, Token> response = new ObjectMapper().readValue(responseNode.toString(),
                    new TypeReference<Map<String, Token>>() {});

            return Tokens
                    .builder()
                    .tokens(response)
                    .build();
        } catch (IOException e) {
            throw new ZkSyncException(e);
        }
    }

    @Override
    public String submitTx(ZkSyncTransaction tx, EthSignature ethereumSignature, boolean fastProcessing) {
        final String responseBody = transport.send("tx_submit",
                Arrays.asList(tx, ethereumSignature, fastProcessing), String.class);

        System.out.println(responseBody);

        return responseBody;
    }
}
