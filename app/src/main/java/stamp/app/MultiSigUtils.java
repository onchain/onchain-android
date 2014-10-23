package stamp.app;

import com.google.bitcoin.core.*;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.google.bitcoin.crypto.TransactionSignature;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.script.ScriptBuilder;
import com.google.bitcoin.script.ScriptChunk;
import com.google.bitcoin.script.ScriptOpCodes;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;

public class MultiSigUtils {

    // From a path string get all the derived keys
    // Path string looks something lile
    // m/0,m/0/1,....
    public static ArrayList<ECKey> deriveKeysFromPath(String path, DeterministicKey privateKey) {

        String[] paths = path.split(",");

        ArrayList<ECKey> keys = new ArrayList<ECKey>();

        DeterministicKey key = privateKey;
        for(String p : paths) {
            String[] indexes = p.split("/");
            for(String i : indexes) {
                if(i.matches("-?\\d+(\\.\\d+)?")) {
                    int keyIndex = Integer.parseInt(i);
                    key = HDKeyDerivation.deriveChildKey(key, keyIndex);
                }
            }
            keys.add(key.toECKey());
        }
        return keys;
    }

    public static Transaction signMultiSigFromPath(Transaction tx, DeterministicKey walletKey, String path)
        throws AddressFormatException {

        ArrayList<ECKey> keys = deriveKeysFromPath(path, walletKey);

        for(ECKey key : keys) {
            tx = signMultiSig(tx, key);
        }
        return tx;
    }

    public static Transaction signMultiSig(Transaction tx, ECKey key) throws AddressFormatException {


        for(int index = 0; index < tx.getInputs().size(); index++) {

            TransactionInput txin = tx.getInput(index);

            List<ScriptChunk> chunks = txin.getScriptSig().getChunks();

            Script redeemScript = (chunks.get(chunks.size() - 1).equalsOpCode(174)) ?
                txin.getScriptSig() : new Script(chunks.get(chunks.size() - 1).data);

            Sha256Hash hash = tx.hashForSignature(index, redeemScript.getProgram(),
                    Transaction.SigHash.ALL, false);

            TransactionSignature signature = new TransactionSignature(key.sign(hash),
                    Transaction.SigHash.ALL, false);

            if (chunks.size() < 2)
                throw new AddressFormatException("incorrect script size");

            ScriptBuilder builder = new ScriptBuilder();
            // OP_0 required for redeeming multisig due to bug in CHECKMULTISIG in bitcoind
            builder.op(ScriptOpCodes.OP_0);


            if(chunks.get(chunks.size() - 1).equalsOpCode(174)) {
                builder.data(signature.encodeToBitcoin());
            }
            else {
                // Collect the signatures
                List<byte[]> sigs = getExistingSignatures(chunks);
                sigs.add(signature.encodeToBitcoin());

                // Collect the public keys, we need to know the order to add signatures
                // in the same order.
                List<byte[]> pubkeys = getPublicKeysFromRedeemScript(redeemScript);

                // Re-insert sigs in the correct order.
                for(byte[] pubk : pubkeys) {
                    for (byte[] sign : sigs) {

                        if(ECKey.verify(hash.getBytes(), sign, pubk)) {
                            builder.data(sign);
                            break;
                        }
                    }
                }
            }
            builder.data(redeemScript.getProgram());

            txin.setScriptSig(builder.build());
        }

        return tx;
    }

    private static List<byte[]> getExistingSignatures(List<ScriptChunk> chunks) {
        List<byte[]> sigs = Lists.newArrayList();

        // collect existing signatures, skipping the initial OP_0 and the final redeem script
        for (ScriptChunk chunk : chunks.subList(1, chunks.size() - 1)) {
            if (chunk.isOpCode() || chunk.data.length == 0) {
                // OP_0, skip
            } else {
                sigs.add(chunk.data);
            }
        }
        return sigs;
    }

    private static List<byte[]> getPublicKeysFromRedeemScript(Script redeemScript) {
        List<byte[]> publicKeys = Lists.newArrayList();
        for(int i = 1; i < redeemScript.getChunks().size() - 2; i++) {
            publicKeys.add(redeemScript.getChunks().get(i).data);
        }
        return publicKeys;
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static int crc16(String serviceName) {
        int crc = 0xFFFF;          // initial value
        int polynomial = 0x1021;   // 0001 0000 0010 0001  (0, 5, 12)

        byte[] bytes = serviceName.getBytes();

        for (byte b : bytes) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b   >> (7-i) & 1) == 1);
                boolean c15 = ((crc >> 15    & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit) crc ^= polynomial;
            }
        }

        crc &= 0xffff;
        return crc;
    }
}
