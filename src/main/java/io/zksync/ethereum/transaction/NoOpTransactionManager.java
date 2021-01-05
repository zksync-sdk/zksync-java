package io.zksync.ethereum.transaction;

import java.io.IOException;
import java.math.BigInteger;

import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.EthGetCode;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.response.NoOpProcessor;

public class NoOpTransactionManager extends TransactionManager {

    public NoOpTransactionManager(Credentials credentials) {
        super(new NoOpProcessor(null), credentials.getAddress());
    }

    @Override
    public EthSendTransaction sendTransaction(BigInteger gasPrice, BigInteger gasLimit, String to, String data,
            BigInteger value, boolean constructor) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public EthSendTransaction sendTransactionEIP1559(BigInteger gasPremium, BigInteger feeCap, BigInteger gasLimit,
            String to, String data, BigInteger value, boolean constructor) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String sendCall(String to, String data, DefaultBlockParameter defaultBlockParameter) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public EthGetCode getCode(String contractAddress, DefaultBlockParameter defaultBlockParameter) throws IOException {
        throw new UnsupportedOperationException();
    }
    
}
