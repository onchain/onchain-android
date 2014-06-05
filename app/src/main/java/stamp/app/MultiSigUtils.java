package stamp.app;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.crypto.DeterministicHierarchy;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.google.bitcoin.crypto.MnemonicCode;
import com.google.bitcoin.crypto.TransactionSignature;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.script.ScriptBuilder;
import com.google.bitcoin.script.ScriptChunk;
import com.google.bitcoin.script.ScriptOpCodes;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.primitives.UnsignedBytes;

import org.spongycastle.util.encoders.Hex;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

class MultiSigUtils {

    public static Transaction signMultiSig(Transaction tx, ECKey key) throws AddressFormatException {

        for(int index = 0; index < tx.getInputs().size(); index++) {

            TransactionInput txin = tx.getInput(index);

            List<ScriptChunk> chunks = txin.getScriptSig().getChunks();

            Script redeemScript = (chunks.get(chunks.size() - 1).equalsOpCode(174)) ?
                txin.getScriptSig() : new Script(chunks.get(chunks.size() - 1).data);

            Sha256Hash hash = tx.hashForSignature(index, redeemScript.getProgram(), Transaction.SigHash.ALL, false);
            TransactionSignature signature = new TransactionSignature(key.sign(hash), Transaction.SigHash.ALL, false);

            if (chunks.size() < 2)
                throw new AddressFormatException("incorrect script size");

            ScriptBuilder builder = new ScriptBuilder();
            // OP_0 required for redeeming multisig due to bug in CHECKMULTISIG in bitcoind
            builder.op(ScriptOpCodes.OP_0);

            // Collect the signatures
            List<byte[]> sigs = Lists.newArrayList();

            // collect existing signatures, skipping the initial OP_0 and the final redeem script
            for (ScriptChunk chunk : chunks.subList(1, chunks.size() - 1)) {
                if (chunk.isOpCode() || chunk.data.length == 0) {
                    // OP_0, skip
                } else {
                    sigs.add(chunk.data);
                }
            }
            sigs.add(signature.encodeToBitcoin());

            for (byte[] sign : sigs)
                builder.data(sign);
            builder.data(redeemScript.getProgram());

            txin.setScriptSig(builder.build());
        }

        return tx;
    }
}
