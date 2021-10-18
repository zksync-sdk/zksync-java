package io.zksync;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
import io.zksync.domain.transaction.TransactionDetails;
import io.zksync.ethereum.EthereumProvider;
import io.zksync.provider.AsyncProvider;
import io.zksync.provider.Provider;
import io.zksync.signer.DefaultEthSigner;
import io.zksync.signer.ZkSigner;
import io.zksync.transport.ZkTransactionStatus;
import io.zksync.transport.receipt.ZkSyncPollingTransactionReceiptProcessor;
import io.zksync.transport.receipt.ZkSyncTransactionReceiptProcessor;
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

    private ZkSyncTransactionReceiptProcessor receiptProcessor;

    @Before
    public void setup() {
        Web3j web3j = Web3j.build(new HttpService("{{ethereum_web3_rpc_url}}"));
        ethSigner = DefaultEthSigner.fromRawPrivateKey(web3j, PRIVATE_KEY);
        zkSigner = ZkSigner.fromEthSigner(ethSigner, ChainId.Ropsten);

        wallet = ZkSyncWallet.build(ethSigner, zkSigner, Provider.defaultProvider(ChainId.Ropsten));
        ethereum = wallet.createEthereumProvider(
                web3j,
                new DefaultGasProvider());
        receiptProcessor = new ZkSyncPollingTransactionReceiptProcessor(AsyncProvider.defaultProvider(ChainId.Ropsten));
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
    public void setupPublicKey() throws InterruptedException, ExecutionException, TimeoutException {
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

        TransactionDetails receipt = receiptProcessor.waitForTransaction(hash, ZkTransactionStatus.COMMITED).get(30, TimeUnit.SECONDS);
        assertNotNull(receipt);
        assertTrue(receipt.getExecuted());
        assertTrue(receipt.getSuccess());
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
    public void transferFunds() throws InterruptedException, ExecutionException, TimeoutException {
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

        TransactionDetails receipt = receiptProcessor.waitForTransaction(hash, ZkTransactionStatus.COMMITED).get(30, TimeUnit.SECONDS);
        assertNotNull(receipt);
        assertTrue(receipt.getExecuted());
        assertTrue(receipt.getSuccess());
    }

    @Test
    public void withdraw() throws InterruptedException, ExecutionException, TimeoutException {
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

        TransactionDetails receipt = receiptProcessor.waitForTransaction(hash, ZkTransactionStatus.COMMITED).get(30, TimeUnit.SECONDS);
        assertNotNull(receipt);
        assertTrue(receipt.getExecuted());
        assertTrue(receipt.getSuccess());
    }

    @Test 
    public void forcedExit() throws InterruptedException, ExecutionException, TimeoutException {
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

        TransactionDetails receipt = receiptProcessor.waitForTransaction(hash, ZkTransactionStatus.COMMITED).get(30, TimeUnit.SECONDS);
        assertNotNull(receipt);
        assertTrue(receipt.getExecuted());
        assertTrue(receipt.getSuccess());
    }

    @Test
    public void mintNFT() throws InterruptedException, ExecutionException, TimeoutException {
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

        TransactionDetails receipt = receiptProcessor.waitForTransaction(hash, ZkTransactionStatus.COMMITED).get(30, TimeUnit.SECONDS);
        assertNotNull(receipt);
        assertTrue(receipt.getExecuted());
        assertTrue(receipt.getSuccess());
    }

    @Test
    public void withdrawNFT() throws InterruptedException, ExecutionException, TimeoutException {
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

        TransactionDetails receipt = receiptProcessor.waitForTransaction(hash, ZkTransactionStatus.COMMITED).get(30, TimeUnit.SECONDS);
        assertNotNull(receipt);
        assertTrue(receipt.getExecuted());
        assertTrue(receipt.getSuccess());
    }

    @Test
    public void transferNFT() throws InterruptedException, ExecutionException, TimeoutException {
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

        hashes.forEach(System.out::println);

        for (String hash : hashes) {
            TransactionDetails receipt = receiptProcessor.waitForTransaction(hash, ZkTransactionStatus.COMMITED).get(30, TimeUnit.SECONDS);
            assertNotNull(receipt);
            assertTrue(receipt.getExecuted());
            assertTrue(receipt.getSuccess());
        }
    }

    @Test
    public void swapTokens() throws InterruptedException, ExecutionException, TimeoutException {
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

        TransactionDetails receipt = receiptProcessor.waitForTransaction(hash, ZkTransactionStatus.COMMITED).get(30, TimeUnit.SECONDS);
        assertNotNull(receipt);
        assertTrue(receipt.getExecuted());
        
        // Here should be failed transaction cause ZkSync restricts swaps the same tokens.
        // This just testing purpose
        assertFalse(receipt.getSuccess());
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

    @Test
    public void testEnable2FA() {
        boolean success = wallet.enable2FA();

        assertTrue(success);
    }

    @Test
    public void testDisable2FA() {
        boolean success = wallet.disable2FA(null);

        assertTrue(success);
    }

    @Test
    public void testDisable2FAToPubKey() {
        boolean success = wallet.disable2FA(zkSigner.getPublicKeyHash());

        assertTrue(success);
    }
    
}
