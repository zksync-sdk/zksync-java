package io.zksync.wallet;

import org.junit.Before;
import org.junit.Test;
import org.web3j.abi.datatypes.Address;
import org.web3j.tuples.generated.Tuple2;

import io.zksync.domain.ChainId;
import io.zksync.domain.Signature;
import io.zksync.domain.TimeRange;
import io.zksync.domain.auth.ChangePubKeyOnchain;
import io.zksync.domain.fee.TransactionFee;
import io.zksync.domain.state.AccountState;
import io.zksync.domain.state.DepositingBalance;
import io.zksync.domain.state.DepositingState;
import io.zksync.domain.state.State;
import io.zksync.domain.swap.Order;
import io.zksync.domain.token.NFT;
import io.zksync.domain.token.Token;
import io.zksync.domain.token.Tokens;
import io.zksync.domain.transaction.ChangePubKey;
import io.zksync.domain.transaction.ForcedExit;
import io.zksync.domain.transaction.MintNFT;
import io.zksync.domain.transaction.Transfer;
import io.zksync.domain.transaction.Withdraw;
import io.zksync.domain.transaction.WithdrawNFT;
import io.zksync.domain.transaction.Swap;
import io.zksync.provider.Provider;
import io.zksync.signer.EthSignature;
import io.zksync.signer.EthSigner;
import io.zksync.signer.DefaultEthSigner;
import io.zksync.signer.ZkSigner;
import io.zksync.signer.EthSignature.SignatureType;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigInteger;
import java.util.Collections;

public class DefaultZkSyncWalletTest {

    private static final String ETH_PRIVATE_KEY = "0x000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f";

    private ZkSyncWallet wallet;
    private EthSigner ethSigner;
    private ZkSigner zkSigner;

    @Before
    public void setUp() {
        ethSigner = DefaultEthSigner.fromRawPrivateKey(ETH_PRIVATE_KEY);
        zkSigner = ZkSigner.fromEthSigner(ethSigner, ChainId.Mainnet);

        Provider provider = mock(Provider.class);
        when(provider.getState(anyString())).thenReturn(defaultAccountState(44));

        wallet = ZkSyncWallet.build(ethSigner, zkSigner, provider);
    }

    @Test
    public void testSetSigningKey() {
        Provider provider = mock(Provider.class);
        when(provider.getState(anyString())).thenReturn(defaultAccountState(55));
        ZkSyncWallet wallet = ZkSyncWallet.build(ethSigner, zkSigner, provider);
        Token token = defaultToken();

        when(provider.getTokens()).thenReturn(new Tokens(Collections.singletonMap(token.getAddress(), token)));
        when(provider.submitTx(defaultZkSyncTransaction_ChangePubKey(), null, false))
                .thenReturn("success:hash");

        String response = wallet.setSigningKey(defaultTransactionFee(1000000000), 13, true, new TimeRange(0, 4294967295L));

        assertNotNull(response);
        assertEquals(response, "success:hash");
    }

    @Test
    public void testSyncTransfer() {
        Provider provider = wallet.getProvider();
        Token token = defaultToken();
        EthSignature ethSignature = new EthSignature(SignatureType.EthereumSignature,
                "0x4684a8f03c5da84676ff4eae89984f20057ce288b3a072605cbf93ef4bcc8a021306b13a88c6d3adc68347f4b68b1cbdf967861005e934afa50ce2e0c5bced791b");
        when(provider.getTokens()).thenReturn(new Tokens(Collections.singletonMap(token.getAddress(), token)));
        when(provider.submitTx(defaultZkSyncTransaction_Transfer(), ethSignature, false)).thenReturn("success:hash");
        String response = wallet.syncTransfer("0x19aa2ed8712072e918632259780e587698ef58df",
                BigInteger.valueOf(1000000000000L), defaultTransactionFee(1000000), 12, new TimeRange(0, 4294967295L));
        assertNotNull(response);
        assertEquals(response, "success:hash");
    }

    @Test
    public void testSyncWithdraw() {
        Provider provider = wallet.getProvider();
        Token token = defaultToken();
        EthSignature ethSignature = new EthSignature(SignatureType.EthereumSignature, "0xa87d458c96f2b78c8b615c7703540d5af0c0b5266b12dbd648d8f6824958ed907f40cae683fa77e7a8a5780381cae30a94acf67f880ed30483c5a8480816fc9d1c");
        when(provider.getTokens()).thenReturn(new Tokens(Collections.singletonMap(token.getAddress(), token)));
        when(provider.submitTx(defaultZkSyncTransaction_Withdraw(), ethSignature, false)).thenReturn("success:hash");
        String response = wallet.syncWithdraw(
            "0x19aa2ed8712072e918632259780e587698ef58df",
            BigInteger.valueOf(1000000000000L),
            defaultTransactionFee(1000000),
            12,
            false,
            new TimeRange(0, 4294967295L)
        );
        assertNotNull(response);
        assertEquals(response, "success:hash");
    }

    @Test
    public void testSyncMintNFT() {
        Provider provider = wallet.getProvider();
        Token token = defaultToken();
        EthSignature ethSignature = new EthSignature(SignatureType.EthereumSignature, "0xac4f8b1ad65ea143dd2a940c72dd778ba3e07ee766355ed237a89a0b7e925fe76ead0a04e23db1cc1593399ee69faeb31b2e7e0c6fbec70d5061d6fbc431d64a1b");
        when(provider.getTokens()).thenReturn(new Tokens(Collections.singletonMap(token.getAddress(), token)));
        when(provider.submitTx(defaultZkSyncTransaction_MintNFT(), ethSignature, false)).thenReturn("success:hash");
        String response = wallet.syncMintNFT(
            "0x19aa2ed8712072e918632259780e587698ef58df",
            "0x0000000000000000000000000000000000000000000000000000000000000123",
            defaultTransactionFee(1000000),
            12
        );
        assertNotNull(response);
        assertEquals(response, "success:hash");
    }

    @Test
    public void testSyncWithdrawNFT() {
        Provider provider = wallet.getProvider();
        Token token = defaultToken();
        NFT nft = new NFT(100000, "NFT-100000", 3, "0x0000000000000000000000000000000000000000000000000000000000000123", "0x19aa2ed8712072e918632259780e587698ef58df", 1, "0x7059cafb9878ac3c95daa5bc33a5728c678d28b3");
        EthSignature ethSignature = new EthSignature(SignatureType.EthereumSignature, "0x4a50341da6d2b1f0b64a4e37f753c02c43623e89cb0a291026c37fdcc723da9665453ce622f4dd6237bd98430ef0d75755694b1968f3b2d0ea8598f8bc43accf1b");
        when(provider.getTokens()).thenReturn(new Tokens(Collections.singletonMap(token.getAddress(), token)));
        when(provider.submitTx(defaultZkSyncTransaction_WithdrawNFT(), ethSignature, false)).thenReturn("success:hash");
        String response = wallet.syncWithdrawNFT(
            "0x19aa2ed8712072e918632259780e587698ef58df",
            nft,
            defaultTransactionFee(1000000),
            12,
            new TimeRange(0, 4294967295L)
        );
        assertNotNull(response);
        assertEquals(response, "success:hash");
    }

    @Test
    public void testSyncForceExit() {
        Provider provider = wallet.getProvider();
        Token token = defaultToken();
        EthSignature ethSignature = new EthSignature(SignatureType.EthereumSignature, "0x4db4eaa3ca3c1b750bc95361847c7dcda5bcc08644f5a80590c604d728f5a01f52bc767a15e8d6fc8293c3ac46f8fbb3ae4aa4bd3db7db1b0ec8959e63b1861e1c");
        when(provider.getTokens()).thenReturn(new Tokens(Collections.singletonMap(token.getAddress(), token)));
        when(provider.submitTx(defaultZkSyncTransaction_ForceExit(), ethSignature, false)).thenReturn("success:hash");
        String response = wallet.syncForcedExit(
            "0x19aa2ed8712072e918632259780e587698ef58df",
            defaultTransactionFee(1000000),
            12,
            new TimeRange(0, 4294967295L)
        );
        assertNotNull(response);
        assertEquals(response, "success:hash");
    }

    @Test
    public void testSyncSwap() {
        Provider provider = mock(Provider.class);
        when(provider.getState(anyString())).thenReturn(defaultAccountState(5));
        ZkSyncWallet wallet = ZkSyncWallet.build(ethSigner, zkSigner, provider);

        Token token = new Token(3, Address.DEFAULT.getValue(), "USDT", 1);
        EthSignature ethSignature = new EthSignature(SignatureType.EthereumSignature, "0x3a459b40838e9445adc59e0cba4bf769b68deda8dadfedfe415f9e8be1c55443090f66cfbd13d96019b9faafb996a5a69d1bc0d1061f08ebf7cb8a1687e09a0f1c");
        when(provider.getTokens()).thenReturn(new Tokens(Collections.singletonMap(token.getAddress(), token)));
        when(provider.submitTx(defaultZkSyncTransaction_Swap(), new EthSignature[]{ethSignature, null, null})).thenReturn("success:hash");
        String response = wallet.syncSwap(
            defaultOrderA(),
            defaultOrderB(),
            BigInteger.valueOf(1000000),
            BigInteger.valueOf(2500000),
            defaultTransactionFee(123),
            1
        );
        assertNotNull(response);
        assertEquals(response, "success:hash");
    }

    @Test
    public void testGetState() {
        Provider provider = spy(wallet.getProvider());
        when(provider.getState(anyString())).thenReturn(defaultAccountState(44));
        assertEquals(wallet.getState(), defaultAccountState(44));
    }

    @Test
    public void testCreateOrder() {
        Provider provider = mock(Provider.class);
        when(provider.getState(anyString())).thenReturn(defaultAccountState(6));
        ZkSyncWallet wallet = ZkSyncWallet.build(ethSigner, zkSigner, provider);

        Token tokenSell = new Token(0, Address.DEFAULT.getValue(), "ETH", 3);
        Token tokenBuy = new Token(2, Address.DEFAULT.getValue(), "DAI", 3);

        Order order = defaultOrder();
        Order result = wallet.buildSignedOrder("0x823b6a996cea19e0c41e250b20e2e804ea72ccdf", tokenSell, tokenBuy, new Tuple2<>(BigInteger.valueOf(1), BigInteger.valueOf(2)), BigInteger.valueOf(1000000), 18, new TimeRange(0, 4294967295L));

        assertEquals(result, order);
    }

    private AccountState defaultAccountState(Integer accountId) {
        AccountState account = new AccountState(
            "0xede35562d3555e61120a151b3c8e8e91d83a378a",
            accountId,
            defaultDepositingState(),
            defaultState(),
            defaultState()
        );
        return account;
    }

    private DepositingState defaultDepositingState() {
        DepositingState depositing = new DepositingState(
            Collections.singletonMap("ETH", new DepositingBalance(BigInteger.TEN.toString(), BigInteger.valueOf(12345)))
        );
        return depositing;
    }

    private State defaultState() {
        State state = new State(
            Integer.MAX_VALUE,
            "17f3708f5e2b2c39c640def0cf0010fd9dd9219650e389114ea9da47f5874184",
            Collections.singletonMap("ETH", BigInteger.TEN.toString()),
            Collections.emptyMap(),
            Collections.emptyMap()
        );
        return state;
    }

    private Token defaultToken() {
        Token token = new Token(
            0,
            "0x0000000000000000000000000000000000000000",
            "ETH",
            0 // INFO: only for testing purpose. 1:1
        );
        return token;
    }

    private TransactionFee defaultTransactionFee(long amount) {
        TransactionFee fee = new TransactionFee(
            "0x0000000000000000000000000000000000000000",
            BigInteger.valueOf(amount)
        );
        return fee;
    }

    private ChangePubKey<ChangePubKeyOnchain> defaultZkSyncTransaction_ChangePubKey() {
        ChangePubKey<ChangePubKeyOnchain> tx = new ChangePubKey<>(
            55,
            "0xede35562d3555e61120a151b3c8e8e91d83a378a",
            "sync:18e8446d7748f2de52b28345bdbc76160e6b35eb",
            0,
            "1000000000",
            13,
            Signature.builder()
                .pubKey("40771354dc314593e071eaf4d0f42ccb1fad6c7006c57464feeb7ab5872b7490")
                .signature("85782959384c1728192b0fe9466a4273b6d0e78e913eea894b780e0236fc4c9d673d3833e895bce992fc113a4d16bba47ef73fed9c4fca2af09ed06cd6885802")
                .build(),
            new ChangePubKeyOnchain(),
            new TimeRange(0, 4294967295L)
        );
        return tx;
    }

    private ForcedExit defaultZkSyncTransaction_ForceExit() {
        ForcedExit tx = new ForcedExit(
            44,
            "0x19aa2ed8712072e918632259780e587698ef58df",
            0,
            "1000000",
            12,
            Signature.builder()
                .pubKey("40771354dc314593e071eaf4d0f42ccb1fad6c7006c57464feeb7ab5872b7490")
                .signature("b1b82f7ac37e2d4bd675e4a5cd5e48d9fad1739282db8a979c3e4d9e39d794915667ee2c125ba24f4fe81ad6d19491eef0be849a823ea6567517b7e207214705")
                .build(),
            new TimeRange(0, 4294967295L)
        );
        return tx;
    }

    private Transfer defaultZkSyncTransaction_Transfer() {
        Transfer tx = new Transfer(
            44,
            "0xede35562d3555e61120a151b3c8e8e91d83a378a",
            "0x19aa2ed8712072e918632259780e587698ef58df",
            0,
            BigInteger.valueOf(1000000000000L),
            "1000000",
            12,
            Signature.builder()
                .pubKey("40771354dc314593e071eaf4d0f42ccb1fad6c7006c57464feeb7ab5872b7490")
                .signature("b3211c7e15d31d64619e0c7f65fce8c6e45637b5cfc8711478c5a151e6568d875ec7f48e040225fe3cc7f1e7294625cad6d98b4595d007d36ef62122de16ae01")
                .build(),
            null,
            new TimeRange(0, 4294967295L)
        );
        return tx;
    }

    private Withdraw defaultZkSyncTransaction_Withdraw() {
        Withdraw tx = new Withdraw(
            44,
            "0xede35562d3555e61120a151b3c8e8e91d83a378a",
            "0x19aa2ed8712072e918632259780e587698ef58df",
            0,
            BigInteger.valueOf(1000000000000L),
            "1000000",
            12,
            Signature.builder()
                .pubKey("40771354dc314593e071eaf4d0f42ccb1fad6c7006c57464feeb7ab5872b7490")
                .signature("11dc47fced9e6ffabe33112a4280c02d0c1ffa649ba3843eec256d427b90ed82e495c0cee2138d5a9e20328d31cb97b70d7e2ede0d8d967678803f4b5896f701")
                .build(),
            new TimeRange(0, 4294967295L)
        );
        return tx;
    }

    private MintNFT defaultZkSyncTransaction_MintNFT() {
        MintNFT tx = new MintNFT(
            44,
            "0xede35562d3555e61120a151b3c8e8e91d83a378a",
            "0x0000000000000000000000000000000000000000000000000000000000000123",
            "0x19aa2ed8712072e918632259780e587698ef58df",
            "1000000",
            0,
            12,
            Signature.builder()
                .pubKey("40771354dc314593e071eaf4d0f42ccb1fad6c7006c57464feeb7ab5872b7490")
                .signature("5cf4ef4680d58e23ede08cc2f8dd33123c339788721e307a813cdf82bc0bac1c10bc861c68d0b5328e4cb87b610e4dfdc13ddf8a444a4a2ac374ac3c73dbec05")
                .build()
        );
        return tx;
    }

    private WithdrawNFT defaultZkSyncTransaction_WithdrawNFT() {
        WithdrawNFT tx = new WithdrawNFT(
            44,
            "0xede35562d3555e61120a151b3c8e8e91d83a378a",
            "0x19aa2ed8712072e918632259780e587698ef58df",
            100000,
            0,
            "1000000",
            12,
            Signature.builder()
                .pubKey("40771354dc314593e071eaf4d0f42ccb1fad6c7006c57464feeb7ab5872b7490")
                .signature("1236180fe01b42c0c3c084d152b0582e714fa19da85900777e811f484a5b3ea434af320f66c7c657a33024d7be22cea44b7406d0af88c097a9d7d6b5d7154d02")
                .build(),
            new TimeRange(0, 4294967295L)
        );
        return tx;
    }

    private Swap defaultZkSyncTransaction_Swap() {
        Swap tx = new Swap(
            5,
            "0xede35562d3555e61120a151b3c8e8e91d83a378a",
            1,
            new Tuple2<>(defaultOrderA(), defaultOrderB()),
            new Tuple2<>(BigInteger.valueOf(1000000), BigInteger.valueOf(2500000)),
            "123",
            3,
            Signature.builder()
                .pubKey("40771354dc314593e071eaf4d0f42ccb1fad6c7006c57464feeb7ab5872b7490")
                .signature("c13aabacf96448efb47763554753bfe2acc303a8297c8af59e718d685d422a901a43c42448f95cca632821df1ccb754950196e8444c0acef253c42c1578b5401")
                .build()
        );
        return tx;
    }

    private Order defaultOrder() {
        Order order = Order.builder()
            .accountId(6)
            .amount(BigInteger.valueOf(1000000))
            .recipientAddress("0x823b6a996cea19e0c41e250b20e2e804ea72ccdf")
            .tokenSell(0)
            .tokenBuy(2)
            .ratio(new Tuple2<>(BigInteger.valueOf(1), BigInteger.valueOf(2)))
            .nonce(18)
            .timeRange(new TimeRange(0, 4294967295L))
            .ethereumSignature(
                EthSignature.builder()
                    .signature("0x841a4ed62572883b2272a56164eb33f7b0649029ba588a7230928cff698b49383045b47d35dcdee1beb33dd4ca6b944b945314a206f3f2838ddbe389a34fc8cb1c")
                    .type(SignatureType.EthereumSignature)
                    .build()
            )
            .signature(
                Signature.builder()
                    .pubKey("40771354dc314593e071eaf4d0f42ccb1fad6c7006c57464feeb7ab5872b7490")
                    .signature("b76c83011ea9e14cf679d35b9a7084832a78bf3f975c5b5c3315f80993c227afb7a1cd7e7b8fc225a48d8c9be78335736115890df5bbacfc52ecf47b4e089500")
                    .build()
            )
            .build();

        return order;
    }

    private Order defaultOrderA() {
        Order order = Order.builder()
            .accountId(6)
            .amount(BigInteger.valueOf(1000000))
            .recipientAddress("0x823b6a996cea19e0c41e250b20e2e804ea72ccdf")
            .tokenSell(1)
            .tokenBuy(2)
            .ratio(new Tuple2<>(BigInteger.valueOf(1), BigInteger.valueOf(2)))
            .nonce(18)
            .timeRange(new TimeRange(0, 4294967295L))
            .ethereumSignature(null)
            .signature(null)
            .build();

        return order;
    }

    private Order defaultOrderB() {
        Order order = Order.builder()
            .accountId(44)
            .amount(BigInteger.valueOf(2500000))
            .recipientAddress("0x63adbb48d1bc2cf54562910ce54b7ca06b87f319")
            .tokenSell(2)
            .tokenBuy(1)
            .ratio(new Tuple2<>(BigInteger.valueOf(3), BigInteger.valueOf(1)))
            .nonce(101)
            .timeRange(new TimeRange(0, 4294967295L))
            .ethereumSignature(null)
            .signature(null)
            .build();

        return order;
    }
    
}
