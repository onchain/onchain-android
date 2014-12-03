package stamp.app;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.params.MainNetParams;
import com.squareup.okhttp.*;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;

public class OnChainCommands {

    private static final String TAG = "OnChainCommands";

    public static void processSignRequest(final String[] params, final String postBack,
                                          final DeterministicKey ekPrivate, final Activity activity)
            throws Exception {

        FormEncodingBuilder feb = new FormEncodingBuilder();

        for(int i = 3; (i + 1) < params.length; i+= 2) {
            feb.add(params[i], params[i + 1]);
        }
        RequestBody formBody = feb.build();

        Request request = new Request.Builder().url(postBack)
                .post(formBody)
                .build();

        Log.v(TAG, request.toString());

        OkHttpClient client = new OkHttpClient();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, final IOException throwable) {

                makeToast(throwable.toString(), activity);

                Log.e(TAG, throwable.toString());
            }

            @Override
            public void onResponse(final Response response) throws IOException {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                String res = response.toString();
                Log.w("INFO", res);

                // Strip out any meta data.
                String transactionHex = response.toString();
                String metaData = null;
                if(res.indexOf(":", 0) != -1) {
                    transactionHex = res.split(":")[0];
                    metaData = res.substring(res.indexOf(":", 0) + 1, res.length());
                }
                Transaction tx = new Transaction(MainNetParams.get(), Hex.decode(transactionHex));

                if(tx.getOutputs().size() == 0) {

                    String txShort = res.substring(0, 40);
                    makeToast("Invalid TX ? (" + txShort + ")", activity);
                    return;
                }

                try {

                    FormEncodingBuilder callBackParams = new FormEncodingBuilder();
                    if(metaData == null) {
                        tx = MultiSigUtils.signMultiSig(tx, ekPrivate.toECKey());
                    } else {
                        // Is this a JSON payload.
                        if(metaData.startsWith("[")) {
                            // Yes, use the new JSON format.
                            metaData = MultiSigUtils.signSignatureList(metaData, tx, ekPrivate.toECKey());
                            callBackParams.add("meta_data", metaData);
                        }
                        else {
                            tx = MultiSigUtils.signMultiSigFromPath(tx, ekPrivate, metaData);
                        }
                    }

                    callBackParams.add("tx", Utils.bytesToHexString(tx.bitcoinSerialize()));

                    RequestBody formBody = callBackParams.build();

                    Request callBackRequest = new Request.Builder().url(postBack)
                            .post(formBody)
                            .build();

                    // POST it back.

                    OkHttpClient client = new OkHttpClient();
                    client.newCall(callBackRequest).enqueue(new Callback() {
                        @Override
                        public void onFailure(Request request, final IOException throwable) {

                            makeToast(throwable.toString(), activity);

                            Log.e(TAG, throwable.toString());
                        }

                        @Override
                        public void onResponse(final Response response) throws IOException {

                            makeToast(response.toString(), activity);
                            Log.w("INFO", "SUCCESS " + response);
                        }
                    });
                } catch (Exception e) {
                    Log.w("ERROR", e.getMessage());

                    Toast toast = Toast.makeText(activity.getApplicationContext(),
                            e.getMessage(), Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }

                makeToast(tx.getOutputs().get(0).toString(), activity);
            }
        });
    }

    private static void makeToast(final String text, final Activity activity) {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                try {
                    Toast toast = Toast.makeText(activity.getApplicationContext(),
                            text, Toast.LENGTH_SHORT);
                    toast.show();
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
            }
        });
    }

    public static void processMPKRequest(String[] params, String postBack,
                                   final DeterministicKey ekPrivate, final Activity activity)
            throws Exception {

        Toast toast = Toast.makeText(activity.getApplicationContext(),
                ekPrivate.serializePubB58(), Toast.LENGTH_SHORT);
        toast.show();


        OkHttpClient client = new OkHttpClient();

        FormEncodingBuilder feb = new FormEncodingBuilder();

        for(int i = 3; (i + 1) < params.length; i+= 2) {
            feb.add(params[i], params[i + 1]);
        }
        feb.add("mpk", ekPrivate.serializePubB58());
        RequestBody formBody = feb.build();

        Request request = new Request.Builder().url(postBack)
                .post(formBody)
                .build();

        Log.w("INFO", request.toString());

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, final IOException throwable) {

                makeToast(throwable.toString(), activity);

                Log.e(TAG, throwable.toString());
            }

            @Override
            public void onResponse(final Response response) throws IOException {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                makeToast(response.toString(), activity);
                Log.w(TAG, "SUCCESS " + response);
            }
        });
    }
}
