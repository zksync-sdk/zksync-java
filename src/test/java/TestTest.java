import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.zksync.domain.contract.ContractAddress;
import io.zksync.domain.fee.TransactionFee;
import io.zksync.domain.fee.TransactionFeeDetails;
import io.zksync.domain.fee.TransactionType;
import io.zksync.domain.token.Tokens;
import io.zksync.provider.DefaultProvider;
import io.zksync.provider.Provider;
import io.zksync.signer.EthSigner;
import io.zksync.signer.ZkSigner;
import io.zksync.transport.HttpTransport;
import io.zksync.wallet.ZkSyncWallet;
import org.junit.Ignore;
import org.junit.Test;
import org.web3j.crypto.Hash;
import org.web3j.utils.Convert;

@Ignore
public class TestTest {

    private static final String MNEMONIC = "unlock arrange six taste scorpion label work ghost dismiss soul adult chalk";

    private static final byte[] ZK_KEY_SEED = Hash.sha3(new byte[] {Integer.valueOf(1).byteValue()});

    @Test
    public void testSetSigningKey() throws JsonProcessingException {
        final EthSigner ethSigner = EthSigner.fromMnemonic(MNEMONIC);
        final ZkSigner zkSigner = ZkSigner.fromSeed(ZK_KEY_SEED);

        final ZkSyncWallet wallet = ZkSyncWallet
                .build(ethSigner, zkSigner, new HttpTransport("https://rinkeby-api.zksync.io/jsrpc"));

        final TransactionFeeDetails feeDetails = wallet.getTransactionFee(
                TransactionType.CHANGE_PUB_KEY, "0x0000000000000000000000000000000000000000");

        TransactionFee fee = TransactionFee
                .builder()
                .fee(feeDetails.getTotalFeeInteger())
                .feeToken("0x0000000000000000000000000000000000000000")
                .build();

        wallet.setSigningKey(fee, 0, false);

        System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(wallet.getState()));
    }

    @Test
    public void testTransfer() throws JsonProcessingException {
        final EthSigner ethSigner = EthSigner.fromMnemonic(MNEMONIC);
        final ZkSigner zkSigner = ZkSigner.fromSeed(ZK_KEY_SEED);

        final String destination = EthSigner.fromMnemonic(MNEMONIC, 1).getAddress();

        final ZkSyncWallet wallet = ZkSyncWallet
                .build(ethSigner, zkSigner, new HttpTransport("https://rinkeby-api.zksync.io/jsrpc"));

        final String token = "0x0000000000000000000000000000000000000000";
        final TransactionFeeDetails feeDetails = wallet.getTransactionFee(
                TransactionType.TRANSFER, destination, token);

        System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(wallet.getState()));

        wallet.syncTransfer(destination, token,
                Convert.toWei("0.001", Convert.Unit.ETHER).toBigInteger(), feeDetails.getTotalFeeInteger(), null);

        System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(wallet.getState()));
    }

    @Test
    public void testWithdraw() throws JsonProcessingException {
        final EthSigner ethSigner = EthSigner.fromMnemonic(MNEMONIC);
        final ZkSigner zkSigner = ZkSigner.fromSeed(ZK_KEY_SEED);

        final ZkSyncWallet wallet = ZkSyncWallet
                .build(ethSigner, zkSigner, new HttpTransport("https://rinkeby-api.zksync.io/jsrpc"));

        final String token = "0x000000000000000000000000000000000000000000";
        final TransactionFeeDetails feeDetails = wallet.getTransactionFee(
                TransactionType.WITHDRAW, ethSigner.getAddress(), token);

        System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(wallet.getState()));

        wallet.syncWithdraw(ethSigner.getAddress(), token, Convert.toWei("0.001", Convert.Unit.ETHER).toBigInteger(),
                feeDetails.getTotalFeeInteger(), null, false);

        System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(wallet.getState()));
    }

    @Test
    public void testForcedExit() throws JsonProcessingException {
        final EthSigner ethSigner = EthSigner.fromMnemonic(MNEMONIC);
        final ZkSigner zkSigner = ZkSigner.fromSeed(ZK_KEY_SEED);

        final String target = EthSigner.fromMnemonic(MNEMONIC, 1).getAddress();

        final ZkSyncWallet wallet = ZkSyncWallet
                .build(ethSigner, zkSigner, new HttpTransport("https://rinkeby-api.zksync.io/jsrpc"));

        final String token = "0x0000000000000000000000000000000000000000";
        final TransactionFeeDetails feeDetails = wallet.getTransactionFee(
                TransactionType.FORCED_EXIT, target, token);

        wallet.syncForcedExit(target, token, feeDetails.getTotalFeeInteger(), null);

        System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(wallet.getProvider().getState(target)));
    }

    @Test
    public void getState() throws JsonProcessingException {
        final EthSigner ethSigner = EthSigner.fromMnemonic(MNEMONIC);
        final ZkSigner zkSigner = ZkSigner.fromSeed(ZK_KEY_SEED);

        final ZkSyncWallet wallet = ZkSyncWallet
                .build(ethSigner, zkSigner, new HttpTransport("https://rinkeby-api.zksync.io/jsrpc"));

        System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(wallet.getState()));
    }

    @Test
    public void getContractAddress() throws JsonProcessingException {
        final Provider provider = new DefaultProvider(new HttpTransport("https://rinkeby-api.zksync.io/jsrpc"));

        final ContractAddress contractAddress = provider.contractAddress();

        System.out.println(contractAddress);
        
    }

    @Test
    public void getTokens() throws JsonProcessingException {
        final Provider provider = new DefaultProvider(new HttpTransport("https://rinkeby-api.zksync.io/jsrpc"));

        final Tokens tokens = provider.getTokens();

        System.out.println(tokens);
    }
}
