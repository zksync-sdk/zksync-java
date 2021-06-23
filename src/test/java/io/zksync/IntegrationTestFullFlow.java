package io.zksync;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tuples.generated.Tuple2;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;
import org.web3j.utils.Convert.Unit;

import io.zksync.domain.ChainId;
import io.zksync.domain.TimeRange;
import io.zksync.domain.fee.TransactionFee;
import io.zksync.domain.fee.TransactionFeeBatchRequest;
import io.zksync.domain.fee.TransactionFeeDetails;
import io.zksync.domain.fee.TransactionFeeRequest;
import io.zksync.domain.fee.TransactionType;
import io.zksync.domain.state.AccountState;
import io.zksync.domain.swap.Order;
import io.zksync.domain.token.Token;
import io.zksync.ethereum.EthereumProvider;
import io.zksync.provider.Provider;
import io.zksync.signer.DefaultEthSigner;
import io.zksync.signer.ZkSigner;
import io.zksync.wallet.ZkSyncWallet;

// TODO: Comment @Ignore and fill placeholders to use this test
@Ignore
public class IntegrationTestFullFlow {

    private static final SecureRandom rand = new SecureRandom();

    private static final String PRIVATE_KEY = "{{ethereum_private_key}}";
    private static final Token ETHEREUM_COIN = Token.createETH();

    private ZkSyncWallet wallet;
    private EthereumProvider ethereum;

    private DefaultEthSigner ethSigner;
    private ZkSigner zkSigner;

    @Before
    public void setup() {
        Web3j web3j = Web3j.build(new HttpService("{{ethereum_web3_rpc_url}}"));
        ethSigner = DefaultEthSigner.fromRawPrivateKey(web3j, PRIVATE_KEY);
        zkSigner = ZkSigner.fromEthSigner(ethSigner, ChainId.Rinkeby);

        wallet = ZkSyncWallet.build(ethSigner, zkSigner, Provider.betaProvider(ChainId.Rinkeby));
        ethereum = wallet.createEthereumProvider(
                web3j,
                new DefaultGasProvider());
    }

    @Test
    public void createAccount() throws InterruptedException, ExecutionException {
        TransactionReceipt receipt = ethereum.deposit(
            ETHEREUM_COIN,
            Convert.toWei(BigDecimal.valueOf(1), Unit.ETHER).toBigInteger(),
            ethSigner.getAddress()
        ).get();

        assertTrue(receipt.isStatusOK());
    }

    @Test
    public void setupPublicKey() {
        AccountState state = wallet.getState();
        TransactionFeeDetails details = wallet.getProvider().getTransactionFee(
            TransactionFeeRequest.builder()
                .transactionType(TransactionType.CHANGE_PUB_KEY_ECDSA)
                .address(state.getAddress())
                .tokenIdentifier(ETHEREUM_COIN.getAddress())
                .build()
        );
        TransactionFee fee = new TransactionFee(ETHEREUM_COIN.getAddress(), details.getTotalFeeInteger());
        String hash =  wallet.setSigningKey(fee, state.getCommitted().getNonce(), false, new TimeRange(0, 4294967295L));

        System.out.println(hash);
    }

    @Test
    public void setupPublicKeyOnChain() throws InterruptedException, ExecutionException {
        AccountState state = wallet.getState();
        TransactionReceipt receipt = ethereum.setAuthPubkeyHash(
            zkSigner.getPublicKeyHash().substring(5),
            BigInteger.valueOf(state.getCommitted().getNonce())).get();

        assertTrue(receipt.isStatusOK());
    }

    @Test
    public void testIsPublicKeyIsSetOnChain() throws InterruptedException, ExecutionException {
        AccountState state = wallet.getState();
        boolean result = ethereum.isOnChainAuthPubkeyHashSet(BigInteger.valueOf(state.getCommitted().getNonce())).get();

        assertTrue(result);
    }

    @Test
    public void transferFunds() {
        AccountState state = wallet.getState();
        TransactionFeeDetails details = wallet.getProvider().getTransactionFee(
            TransactionFeeRequest.builder()
                .transactionType(TransactionType.TRANSFER)
                .address("0x4F6071Dbd5818473EEEF6CE563e66bf22618d8c0".toLowerCase())
                .tokenIdentifier(ETHEREUM_COIN.getAddress())
                .build()
        );
        TransactionFee fee = new TransactionFee(ETHEREUM_COIN.getAddress(), details.getTotalFeeInteger());
        String hash = wallet.syncTransfer(
            "0x4F6071Dbd5818473EEEF6CE563e66bf22618d8c0".toLowerCase(),
            Convert.toWei(BigDecimal.valueOf(1000000), Unit.GWEI).toBigInteger(),
            fee,
            state.getCommitted().getNonce(),
            new TimeRange(0, 4294967295L)
        );

        System.out.println(hash);
    }

    @Test
    public void withdraw() {
        AccountState state = wallet.getState();
        TransactionFeeDetails details = wallet.getProvider().getTransactionFee(
            TransactionFeeRequest.builder()
                .transactionType(TransactionType.WITHDRAW)
                .address(state.getAddress())
                .tokenIdentifier(ETHEREUM_COIN.getAddress())
                .build()
        );
        TransactionFee fee = new TransactionFee(ETHEREUM_COIN.getAddress(), details.getTotalFeeInteger());
        String hash = wallet.syncWithdraw(
            state.getAddress(),
            Convert.toWei(BigDecimal.valueOf(1000), Unit.GWEI).toBigInteger(),
            fee,
            state.getCommitted().getNonce(),
            false,
            new TimeRange(0, 4294967295L)
        );

        System.out.println(hash);
    }

    @Test 
    public void forcedExit() {
        AccountState state = wallet.getState();
        TransactionFeeDetails details = wallet.getProvider().getTransactionFee(
            TransactionFeeRequest.builder()
                .transactionType(TransactionType.FORCED_EXIT)
                .address(state.getAddress())
                .tokenIdentifier(ETHEREUM_COIN.getAddress())
                .build()
        );
        TransactionFee fee = new TransactionFee(ETHEREUM_COIN.getAddress(), details.getTotalFeeInteger());
        String hash = wallet.syncForcedExit(
            state.getAddress(),
            fee,
            state.getCommitted().getNonce(),
            new TimeRange(0, 4294967295L)
        );

        System.out.println(hash);
    }

    @Test
    public void mintNFT() {
        AccountState state = wallet.getState();
        TransactionFeeDetails details = wallet.getProvider().getTransactionFee(
            TransactionFeeRequest.builder()
                .transactionType(TransactionType.MINT_NFT)
                .address(state.getAddress())
                .tokenIdentifier(ETHEREUM_COIN.getAddress())
                .build()
        );
        TransactionFee fee = new TransactionFee(ETHEREUM_COIN.getAddress(), details.getTotalFeeInteger());
        byte[] seed = rand.generateSeed(32);
        String contentHash = Numeric.toHexString(seed);
        String hash = wallet.syncMintNFT(state.getAddress(), contentHash, fee, state.getCommitted().getNonce());

        System.out.println(hash);
    }

    @Test
    public void withdrawNFT() {
        AccountState state = wallet.getState();
        TransactionFeeDetails details = wallet.getProvider().getTransactionFee(
            TransactionFeeRequest.builder()
                .transactionType(TransactionType.WITHDRAW_NFT)
                .address(state.getAddress())
                .tokenIdentifier(ETHEREUM_COIN.getAddress())
                .build()
        );
        TransactionFee fee = new TransactionFee(ETHEREUM_COIN.getAddress(), details.getTotalFeeInteger());
        String hash = wallet.syncWithdrawNFT(
            state.getAddress(),
            state.getCommitted().getNfts().values().stream().findAny().get(),
            fee,
            null,
            new TimeRange(0, 4294967295L)
        );

        System.out.println(hash);
    }

    @Test
    public void transferNFT() {
        AccountState state = wallet.getState();
        TransactionFeeDetails details = wallet.getProvider().getTransactionFee(
            TransactionFeeBatchRequest.builder()
                .transactionType(Pair.of(TransactionType.TRANSFER, state.getAddress()))
                .transactionType(Pair.of(TransactionType.TRANSFER, state.getAddress()))
                .tokenIdentifier(ETHEREUM_COIN.getAddress())
                .build()
        );
        TransactionFee fee = new TransactionFee(ETHEREUM_COIN.getAddress(), details.getTotalFeeInteger());
        List<String> hashes = wallet.syncTransferNFT(
            state.getAddress(),
            state.getCommitted().getNfts().values().stream().findAny().get(),
            fee,
            null,
            new TimeRange(0, 4294967295L)
        );

        System.out.println(hashes);
    }

    @Test
    public void swapTokens() {
        AccountState state = wallet.getState();
        TransactionFeeDetails details = wallet.getProvider().getTransactionFee(
            TransactionFeeBatchRequest.builder()
                .transactionType(Pair.of(TransactionType.SWAP, state.getAddress()))
                .tokenIdentifier(ETHEREUM_COIN.getAddress())
                .build()
        );
        TransactionFee fee = new TransactionFee(ETHEREUM_COIN.getAddress(), details.getTotalFeeInteger());
        BigInteger amount1 = Convert.toWei(BigDecimal.valueOf(1000), Unit.GWEI).toBigInteger();
        BigInteger amount2 = Convert.toWei(BigDecimal.valueOf(1000), Unit.GWEI).toBigInteger();
        Order order1 = wallet.buildSignedOrder(state.getAddress(), ETHEREUM_COIN, ETHEREUM_COIN, new Tuple2<>(BigInteger.ONE, BigInteger.ONE), amount1, state.getCommitted().getNonce(), new TimeRange(0, 4294967295L));
        Order order2 = wallet.buildSignedOrder(state.getAddress(), ETHEREUM_COIN, ETHEREUM_COIN, new Tuple2<>(BigInteger.ONE, BigInteger.ONE), amount2, state.getCommitted().getNonce(), new TimeRange(0, 4294967295L));
        String hash = wallet.syncSwap(order1, order2, amount1, amount2, fee, state.getCommitted().getNonce());

        System.out.println(hash);
    }

    @Test
    public void fullExit() throws InterruptedException, ExecutionException {
        AccountState state = wallet.getState();
        TransactionReceipt receipt = ethereum.fullExit(
            ETHEREUM_COIN,
            state.getId()
        ).get();

        assertTrue(receipt.isStatusOK());
    }

    @Test
    public void getTransactionFeeBatch() {
        TransactionFeeDetails details = wallet.getProvider().getTransactionFee(
            TransactionFeeBatchRequest.builder()
                .tokenIdentifier(ETHEREUM_COIN.getAddress())
                .transactionType(Pair.of(TransactionType.FORCED_EXIT, "0x98122427eE193fAcbb9Fbdbf6BDE7d9042A95a0f"))
                .transactionType(Pair.of(TransactionType.TRANSFER, "0xC8568F373484Cd51FDc1FE3675E46D8C0dc7D246"))
                .transactionType(Pair.of(TransactionType.TRANSFER, "0x98122427eE193fAcbb9Fbdbf6BDE7d9042A95a0f"))
                .transactionType(Pair.of(TransactionType.CHANGE_PUB_KEY_ECDSA, "0x98122427eE193fAcbb9Fbdbf6BDE7d9042A95a0f"))
                .build()
        );

        System.out.println(details.getTotalFee());
    }

    @Test
    public void getTokenPrice() {
        BigDecimal price = wallet.getProvider().getTokenPrice(ETHEREUM_COIN);

        System.out.println(price.toString());
    }

    @Test
    public void getConfirmationsForEthOpAmount() {
        BigInteger amount = wallet.getProvider().getConfirmationsForEthOpAmount();

        System.out.println(amount.toString());
    }
    
}
