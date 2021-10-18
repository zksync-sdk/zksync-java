package io.zksync;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
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
import io.zksync.domain.TransactionBuildHelper;
import io.zksync.domain.auth.ChangePubKeyECDSA;
import io.zksync.domain.fee.TransactionFeeBatchRequest;
import io.zksync.domain.fee.TransactionFeeDetails;
import io.zksync.domain.fee.TransactionType;
import io.zksync.domain.swap.Order;
import io.zksync.domain.token.NFT;
import io.zksync.domain.token.Token;
import io.zksync.domain.transaction.ChangePubKey;
import io.zksync.domain.transaction.ForcedExit;
import io.zksync.domain.transaction.MintNFT;
import io.zksync.domain.transaction.Swap;
import io.zksync.domain.transaction.TransactionDetails;
import io.zksync.domain.transaction.Transfer;
import io.zksync.domain.transaction.Withdraw;
import io.zksync.domain.transaction.WithdrawNFT;
import io.zksync.ethereum.EthereumProvider;
import io.zksync.provider.AsyncProvider;
import io.zksync.signer.DefaultEthSigner;
import io.zksync.signer.EthSignature;
import io.zksync.signer.ZkSigner;
import io.zksync.transport.ZkTransactionStatus;
import io.zksync.transport.receipt.ZkSyncPollingTransactionReceiptProcessor;
import io.zksync.transport.receipt.ZkSyncTransactionReceiptProcessor;
import io.zksync.wallet.SignedTransaction;
import io.zksync.wallet.ZkASyncWallet;

@Ignore
public class IntegrationTestTransactionReceiptProcessor {
    private static final SecureRandom rand = new SecureRandom();

    private static final String PRIVATE_KEY = "{{ethereum_private_key}}";
    private static final Token ETHEREUM_COIN = Token.createETH();

    private ZkASyncWallet wallet;
    private EthereumProvider ethereum;

    private DefaultEthSigner ethSigner;
    private ZkSigner zkSigner;

    private ZkSyncTransactionReceiptProcessor receiptProcessor;

    @Before
    public void setup() {
        Web3j web3j = Web3j.build(new HttpService("{{ethereum_web3_rpc_url}}"));
        ethSigner = DefaultEthSigner.fromRawPrivateKey(web3j, PRIVATE_KEY);
        zkSigner = ZkSigner.fromEthSigner(ethSigner, ChainId.Ropsten);

        wallet = ZkASyncWallet.build(ethSigner, zkSigner, AsyncProvider.defaultProvider(ChainId.Ropsten));
        ethereum = wallet.createEthereumProvider(
                web3j,
                new DefaultGasProvider());
        receiptProcessor = new ZkSyncPollingTransactionReceiptProcessor(wallet);
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
        TransactionBuildHelper helper = new TransactionBuildHelper(this.wallet, this.wallet.getTokens().join());
        ChangePubKey<ChangePubKeyECDSA> changePubKey = helper.changePubKey(zkSigner.getPublicKeyHash(), ETHEREUM_COIN, new ChangePubKeyECDSA(null, null)).join();
        changePubKey = ethSigner.signAuth(changePubKey).join();
        EthSignature ethSignature = ethSigner.signTransaction(changePubKey, changePubKey.getNonce(), ETHEREUM_COIN, changePubKey.getFeeInteger()).join();
        SignedTransaction<ChangePubKey<ChangePubKeyECDSA>> transaction = new SignedTransaction<>(zkSigner.signChangePubKey(changePubKey), ethSignature);
        String hash = wallet.submitTransaction(transaction).join();

        System.out.println(hash);

        TransactionDetails receipt = receiptProcessor.waitForTransaction(hash, ZkTransactionStatus.COMMITED).get(30, TimeUnit.SECONDS);
        assertNotNull(receipt);
        assertTrue(receipt.getExecuted());
        assertTrue(receipt.getSuccess());
    }

    @Test
    public void transferFunds() throws InterruptedException, ExecutionException, TimeoutException {
        TransactionBuildHelper helper = new TransactionBuildHelper(this.wallet, this.wallet.getTokens().join());
        Transfer transfer = helper.transfer(ethSigner.getAddress(), Convert.toWei(BigDecimal.valueOf(1000000), Unit.GWEI).toBigInteger(), ETHEREUM_COIN).join();
        EthSignature ethSignature = ethSigner.signTransaction(transfer, transfer.getNonce(), ETHEREUM_COIN, transfer.getFeeInteger()).join();
        SignedTransaction<Transfer> transaction = new SignedTransaction<>(zkSigner.signTransfer(transfer), ethSignature);
        String hash = wallet.submitTransaction(transaction).join();

        System.out.println(hash);

        TransactionDetails receipt = receiptProcessor.waitForTransaction(hash, ZkTransactionStatus.COMMITED).get(30, TimeUnit.SECONDS);
        assertNotNull(receipt);
        assertTrue(receipt.getExecuted());
        assertTrue(receipt.getSuccess());
    }

    @Test
    public void withdraw() throws InterruptedException, ExecutionException, TimeoutException {
        TransactionBuildHelper helper = new TransactionBuildHelper(this.wallet, this.wallet.getTokens().join());
        Withdraw withdraw = helper.withdraw(ethSigner.getAddress(), Convert.toWei(BigDecimal.valueOf(1000000), Unit.GWEI).toBigInteger(), ETHEREUM_COIN, false).join();
        EthSignature ethSignature = ethSigner.signTransaction(withdraw, withdraw.getNonce(), ETHEREUM_COIN, withdraw.getFeeInteger()).join();
        SignedTransaction<Withdraw> transaction = new SignedTransaction<>(zkSigner.signWithdraw(withdraw), ethSignature);
        String hash = wallet.submitTransaction(transaction).join();

        System.out.println(hash);

        TransactionDetails receipt = receiptProcessor.waitForTransaction(hash, ZkTransactionStatus.COMMITED).get(30, TimeUnit.SECONDS);
        assertNotNull(receipt);
        assertTrue(receipt.getExecuted());
        assertTrue(receipt.getSuccess());
    }

    @Test 
    public void forcedExit() throws InterruptedException, ExecutionException, TimeoutException {
        TransactionBuildHelper helper = new TransactionBuildHelper(this.wallet, this.wallet.getTokens().join());
        ForcedExit forcedExit = helper.forcedExit(ETHEREUM_COIN.getAddress(), ETHEREUM_COIN).join();
        EthSignature ethSignature = ethSigner.signTransaction(forcedExit, forcedExit.getNonce(), ETHEREUM_COIN, forcedExit.getFeeInteger()).join();
        SignedTransaction<ForcedExit> transaction = new SignedTransaction<>(zkSigner.signForcedExit(forcedExit), ethSignature);
        String hash = wallet.submitTransaction(transaction).join();

        System.out.println(hash);

        TransactionDetails receipt = receiptProcessor.waitForTransaction(hash, ZkTransactionStatus.COMMITED).get(30, TimeUnit.SECONDS);
        assertNotNull(receipt);
        assertTrue(receipt.getExecuted());
        assertTrue(receipt.getSuccess());
    }

    @Test
    public void mintNFT() throws InterruptedException, ExecutionException, TimeoutException {
        byte[] seed = rand.generateSeed(32);
        String contentHash = Numeric.toHexString(seed);

        TransactionBuildHelper helper = new TransactionBuildHelper(this.wallet, this.wallet.getTokens().join());
        MintNFT mintNft = helper.mintNFT(ethSigner.getAddress(), contentHash, ETHEREUM_COIN).join();
        EthSignature ethSignature = ethSigner.signTransaction(mintNft, mintNft.getNonce(), ETHEREUM_COIN, mintNft.getFeeInteger()).join();
        SignedTransaction<MintNFT> transaction = new SignedTransaction<>(zkSigner.signMintNFT(mintNft), ethSignature);
        String hash = wallet.submitTransaction(transaction).join();

        System.out.println(hash);

        TransactionDetails receipt = receiptProcessor.waitForTransaction(hash, ZkTransactionStatus.COMMITED).get(30, TimeUnit.SECONDS);
        assertNotNull(receipt);
        assertTrue(receipt.getExecuted());
        assertTrue(receipt.getSuccess());
    }

    @Test
    public void withdrawNFT() throws InterruptedException, ExecutionException, TimeoutException {
        NFT nft = wallet.getState().thenApply(state -> state.getCommitted().getNfts().values().stream().findAny()).join().get();
        TransactionBuildHelper helper = new TransactionBuildHelper(this.wallet, this.wallet.getTokens().join());
        WithdrawNFT withdrawNft = helper.withdrawNFT(ethSigner.getAddress(), nft, ETHEREUM_COIN, false).join();
        EthSignature ethSignature = ethSigner.signTransaction(withdrawNft, withdrawNft.getNonce(), ETHEREUM_COIN, withdrawNft.getFeeInteger()).join();
        SignedTransaction<WithdrawNFT> transaction = new SignedTransaction<>(zkSigner.signWithdrawNFT(withdrawNft), ethSignature);
        String hash = wallet.submitTransaction(transaction).join();

        System.out.println(hash);

        TransactionDetails receipt = receiptProcessor.waitForTransaction(hash, ZkTransactionStatus.COMMITED).get(30, TimeUnit.SECONDS);
        assertNotNull(receipt);
        assertTrue(receipt.getExecuted());
        assertTrue(receipt.getSuccess());
    }

    @Test
    public void transferNFT() throws InterruptedException, ExecutionException, TimeoutException {
        NFT nft = wallet.getState().thenApply(state -> state.getCommitted().getNfts().values().stream().findAny()).join().get();
        TransactionBuildHelper helper = new TransactionBuildHelper(this.wallet, this.wallet.getTokens().join());
        Tuple2<Transfer, Transfer> transferNft = helper.transferNFT(ethSigner.getAddress(), nft, ETHEREUM_COIN).join();
        EthSignature ethSignature = ethSigner.signBatch(Arrays.asList(transferNft.component1(), transferNft.component2()), transferNft.component1().getNonce(), ETHEREUM_COIN, transferNft.component2().getFeeInteger()).join();
        zkSigner.signTransfer(transferNft.component1());
        zkSigner.signTransfer(transferNft.component2());
        List<String> transactions = wallet.getProvider().submitTxBatch(Arrays.asList(Pair.of(transferNft.component1(), null), Pair.of(transferNft.component2(), null)), ethSignature).join();

        transactions.forEach(System.out::println);

        for (String hash : transactions) {
            TransactionDetails receipt = receiptProcessor.waitForTransaction(hash, ZkTransactionStatus.COMMITED).get(30, TimeUnit.SECONDS);
            assertNotNull(receipt);
            assertTrue(receipt.getExecuted());
            assertTrue(receipt.getSuccess());
        }
    }

    @Test
    public void swapTokens() throws InterruptedException, ExecutionException, TimeoutException {
        TransactionBuildHelper helper = new TransactionBuildHelper(this.wallet, this.wallet.getTokens().join());
        BigInteger amount1 = Convert.toWei(BigDecimal.valueOf(1000), Unit.GWEI).toBigInteger();
        BigInteger amount2 = Convert.toWei(BigDecimal.valueOf(1000), Unit.GWEI).toBigInteger();
        Order order1 = wallet.buildSignedOrder(ethSigner.getAddress(), ETHEREUM_COIN, ETHEREUM_COIN, new Tuple2<>(BigInteger.ONE, BigInteger.ONE), amount1, null, null).join();
        Order order2 = wallet.buildSignedOrder(ethSigner.getAddress(), ETHEREUM_COIN, ETHEREUM_COIN, new Tuple2<>(BigInteger.ONE, BigInteger.ONE), amount2, null, null).join();
        Swap swap = helper.swap(order1, order2, amount1, amount2, ETHEREUM_COIN).join();
        EthSignature ethSignature = ethSigner.signTransaction(swap, swap.getNonce(), ETHEREUM_COIN, swap.getFeeInteger()).join();
        SignedTransaction<Swap> transaction = new SignedTransaction<>(zkSigner.signSwap(swap), ethSignature, order1.getEthereumSignature(), order2.getEthereumSignature());
        String hash = wallet.submitTransaction(transaction).join();

        System.out.println(hash);

        TransactionDetails receipt = receiptProcessor.waitForTransaction(hash, ZkTransactionStatus.COMMITED).get(30, TimeUnit.SECONDS);
        assertNotNull(receipt);
        assertTrue(receipt.getExecuted());

        // Here should be failed transaction cause ZkSync restricts swaps the same tokens.
        // This just testing purpose
        assertFalse(receipt.getSuccess());
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
        ).join();

        System.out.println(details.getTotalFee());
    }

    @Test
    public void getTokenPrice() {
        BigDecimal price = wallet.getProvider().getTokenPrice(ETHEREUM_COIN).join();

        System.out.println(price.toString());
    }

    @Test
    public void getConfirmationsForEthOpAmount() {
        BigInteger amount = wallet.getProvider().getConfirmationsForEthOpAmount().join();

        System.out.println(amount.toString());
    }

    @Test
    public void testEnable2FA() {
        boolean success = wallet.enable2FA().join();

        assertTrue(success);
    }

    @Test
    public void testDisable2FA() {
        boolean success = wallet.disable2FA(null).join();

        assertTrue(success);
    }

    @Test
    public void testDisable2FAToPubKey() {
        boolean success = wallet.disable2FA(zkSigner.getPublicKeyHash()).join();

        assertTrue(success);
    }

}
