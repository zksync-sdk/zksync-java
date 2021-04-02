package io.zksync.wallet;

import org.junit.Before;
import org.junit.Test;

import io.zksync.domain.ChainId;
import io.zksync.domain.Signature;
import io.zksync.domain.TimeRange;
import io.zksync.domain.auth.ChangePubKeyOnchain;
import io.zksync.domain.fee.TransactionFee;
import io.zksync.domain.state.AccountState;
import io.zksync.domain.state.DepositingBalance;
import io.zksync.domain.state.DepositingState;
import io.zksync.domain.state.State;
import io.zksync.domain.token.Token;
import io.zksync.domain.token.Tokens;
import io.zksync.domain.transaction.ChangePubKey;
import io.zksync.domain.transaction.ForcedExit;
import io.zksync.domain.transaction.Transfer;
import io.zksync.domain.transaction.Withdraw;
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
    public void testGetState() {
        Provider provider = spy(wallet.getProvider());
        when(provider.getState(anyString())).thenReturn(defaultAccountState(44));
        assertEquals(wallet.getState(), defaultAccountState(44));
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
            Collections.singletonMap("ETH", BigInteger.TEN.toString())
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
                .signature("3c206b2d9b6dc055aba53ccbeca6c1620a42fc45bdd66282618fd1f055fdf90c00101973507694fb66edaa5d4591a2b4f56bbab876dc7579a17c7fe309c80301")
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
                .signature("5e5089771f94222d64ad7d4a8853bf83d53bf3c063b91250ece46ccefd45d19a1313aee79f19e73dcf11f12ae0fb8c3fdb83bf4fa704384c5c82b4de0831ea03")
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
                .signature("849281ea1b3a97b3fe30fbd25184db3e7860db96e3be9d53cf643bd5cf7805a30dbf685c1e63fd75968a61bd83d3a1fb3a0b1c68c71fe87d96f1c1cb7de45b05")
                .build(),
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
                .signature("ee8b58e252ecdf76fc4275e87c88072d0c4d50b53c40ac3fd83a396f0989d108d92983a943f08c7ca5a63d9be891185867b89c2450f4d9b73526e1c35c4bf600")
                .build(),
            new TimeRange(0, 4294967295L)
        );
        return tx;
    }
    
}
