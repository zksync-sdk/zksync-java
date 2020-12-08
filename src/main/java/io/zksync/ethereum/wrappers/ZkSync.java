package io.zksync.ethereum.wrappers;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 4.7.0.
 */
@SuppressWarnings("rawtypes")
public class ZkSync extends Contract {
    public static final String BINARY = "Bin file was not provided";

    public static final String FUNC_EMPTY_STRING_KECCAK = "EMPTY_STRING_KECCAK";

    public static final String FUNC_CANCELOUTSTANDINGDEPOSITSFOREXODUSMODE = "cancelOutstandingDepositsForExodusMode";

    public static final String FUNC_COMMITBLOCK = "commitBlock";

    public static final String FUNC_COMPLETEWITHDRAWALS = "completeWithdrawals";

    public static final String FUNC_DEPOSITERC20 = "depositERC20";

    public static final String FUNC_DEPOSITETH = "depositETH";

    public static final String FUNC_EXIT = "exit";

    public static final String FUNC_FULLEXIT = "fullExit";

    public static final String FUNC_GETNOTICEPERIOD = "getNoticePeriod";

    public static final String FUNC_INITIALIZE = "initialize";

    public static final String FUNC_ISREADYFORUPGRADE = "isReadyForUpgrade";

    public static final String FUNC_REVERTBLOCKS = "revertBlocks";

    public static final String FUNC_SETAUTHPUBKEYHASH = "setAuthPubkeyHash";

    public static final String FUNC_TRIGGEREXODUSIFNEEDED = "triggerExodusIfNeeded";

    public static final String FUNC_UPGRADE = "upgrade";

    public static final String FUNC_UPGRADECANCELED = "upgradeCanceled";

    public static final String FUNC_UPGRADEFINISHES = "upgradeFinishes";

    public static final String FUNC_UPGRADENOTICEPERIODSTARTED = "upgradeNoticePeriodStarted";

    public static final String FUNC_UPGRADEPREPARATIONSTARTED = "upgradePreparationStarted";

    public static final String FUNC_VERIFYBLOCK = "verifyBlock";

    public static final String FUNC_WITHDRAWERC20 = "withdrawERC20";

    public static final String FUNC_WITHDRAWERC20GUARDED = "withdrawERC20Guarded";

    public static final String FUNC_WITHDRAWETH = "withdrawETH";

    @Deprecated
    protected ZkSync(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected ZkSync(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected ZkSync(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected ZkSync(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteFunctionCall<byte[]> EMPTY_STRING_KECCAK() {
        final Function function = new Function(FUNC_EMPTY_STRING_KECCAK, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeRemoteCallSingleValueReturn(function, byte[].class);
    }

    public RemoteFunctionCall<TransactionReceipt> cancelOutstandingDepositsForExodusMode(BigInteger _n) {
        final Function function = new Function(
                FUNC_CANCELOUTSTANDINGDEPOSITSFOREXODUSMODE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint64(_n)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> commitBlock(BigInteger _blockNumber, BigInteger _feeAccount, List<byte[]> _newBlockInfo, byte[] _publicData, byte[] _ethWitness, List<BigInteger> _ethWitnessSizes) {
        final Function function = new Function(
                FUNC_COMMITBLOCK, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint32(_blockNumber), 
                new org.web3j.abi.datatypes.generated.Uint32(_feeAccount), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Bytes32>(
                        org.web3j.abi.datatypes.generated.Bytes32.class,
                        org.web3j.abi.Utils.typeMap(_newBlockInfo, org.web3j.abi.datatypes.generated.Bytes32.class)), 
                new org.web3j.abi.datatypes.DynamicBytes(_publicData), 
                new org.web3j.abi.datatypes.DynamicBytes(_ethWitness), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Uint32>(
                        org.web3j.abi.datatypes.generated.Uint32.class,
                        org.web3j.abi.Utils.typeMap(_ethWitnessSizes, org.web3j.abi.datatypes.generated.Uint32.class))), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> completeWithdrawals(BigInteger _n) {
        final Function function = new Function(
                FUNC_COMPLETEWITHDRAWALS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint32(_n)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> depositERC20(String _token, BigInteger _amount, String _franklinAddr) {
        final Function function = new Function(
                FUNC_DEPOSITERC20, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _token), 
                new org.web3j.abi.datatypes.generated.Uint104(_amount), 
                new org.web3j.abi.datatypes.Address(160, _franklinAddr)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> depositETH(String _franklinAddr, BigInteger weiValue) {
        final Function function = new Function(
                FUNC_DEPOSITETH, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _franklinAddr)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function, weiValue);
    }

    public RemoteFunctionCall<TransactionReceipt> exit(BigInteger _accountId, BigInteger _tokenId, BigInteger _amount, List<BigInteger> _proof) {
        final Function function = new Function(
                FUNC_EXIT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint32(_accountId), 
                new org.web3j.abi.datatypes.generated.Uint16(_tokenId), 
                new org.web3j.abi.datatypes.generated.Uint128(_amount), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Uint256>(
                        org.web3j.abi.datatypes.generated.Uint256.class,
                        org.web3j.abi.Utils.typeMap(_proof, org.web3j.abi.datatypes.generated.Uint256.class))), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> fullExit(BigInteger _accountId, String _token) {
        final Function function = new Function(
                FUNC_FULLEXIT, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint32(_accountId), 
                new org.web3j.abi.datatypes.Address(160, _token)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> getNoticePeriod() {
        final Function function = new Function(
                FUNC_GETNOTICEPERIOD, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> initialize(byte[] initializationParameters) {
        final Function function = new Function(
                FUNC_INITIALIZE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicBytes(initializationParameters)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> isReadyForUpgrade() {
        final Function function = new Function(
                FUNC_ISREADYFORUPGRADE, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> revertBlocks(BigInteger _maxBlocksToRevert) {
        final Function function = new Function(
                FUNC_REVERTBLOCKS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint32(_maxBlocksToRevert)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> setAuthPubkeyHash(byte[] _pubkey_hash, BigInteger _nonce) {
        final Function function = new Function(
                FUNC_SETAUTHPUBKEYHASH, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicBytes(_pubkey_hash), 
                new org.web3j.abi.datatypes.generated.Uint32(_nonce)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> triggerExodusIfNeeded() {
        final Function function = new Function(
                FUNC_TRIGGEREXODUSIFNEEDED, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> upgrade(byte[] upgradeParameters) {
        final Function function = new Function(
                FUNC_UPGRADE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.DynamicBytes(upgradeParameters)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> upgradeCanceled() {
        final Function function = new Function(
                FUNC_UPGRADECANCELED, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> upgradeFinishes() {
        final Function function = new Function(
                FUNC_UPGRADEFINISHES, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> upgradeNoticePeriodStarted() {
        final Function function = new Function(
                FUNC_UPGRADENOTICEPERIODSTARTED, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> upgradePreparationStarted() {
        final Function function = new Function(
                FUNC_UPGRADEPREPARATIONSTARTED, 
                Arrays.<Type>asList(), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> verifyBlock(BigInteger _blockNumber, List<BigInteger> _proof, byte[] _withdrawalsData) {
        final Function function = new Function(
                FUNC_VERIFYBLOCK, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint32(_blockNumber), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Uint256>(
                        org.web3j.abi.datatypes.generated.Uint256.class,
                        org.web3j.abi.Utils.typeMap(_proof, org.web3j.abi.datatypes.generated.Uint256.class)), 
                new org.web3j.abi.datatypes.DynamicBytes(_withdrawalsData)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> withdrawERC20(String _token, BigInteger _amount) {
        final Function function = new Function(
                FUNC_WITHDRAWERC20, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _token), 
                new org.web3j.abi.datatypes.generated.Uint128(_amount)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> withdrawERC20Guarded(String _token, String _to, BigInteger _amount, BigInteger _maxAmount) {
        final Function function = new Function(
                FUNC_WITHDRAWERC20GUARDED, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(160, _token), 
                new org.web3j.abi.datatypes.Address(160, _to), 
                new org.web3j.abi.datatypes.generated.Uint128(_amount), 
                new org.web3j.abi.datatypes.generated.Uint128(_maxAmount)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteFunctionCall<TransactionReceipt> withdrawETH(BigInteger _amount) {
        final Function function = new Function(
                FUNC_WITHDRAWETH, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint128(_amount)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public TransactionManager getTransactionManager() {
        return this.transactionManager;
    }

    public ContractGasProvider getGasProvider() {
        return this.gasProvider;
    }

    @Deprecated
    public static ZkSync load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new ZkSync(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static ZkSync load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new ZkSync(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static ZkSync load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new ZkSync(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static ZkSync load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new ZkSync(contractAddress, web3j, transactionManager, contractGasProvider);
    }
}
