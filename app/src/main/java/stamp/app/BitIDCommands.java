package stamp.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.widget.Toast;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.params.MainNetParams;
import com.squareup.okhttp.*;

import java.io.IOException;
import java.net.URI;

public class BitIDCommands {

    public static final String BIT_ID_PARAM_ADDRESS = "address";
    public static final String BIT_ID_PARAM_SIGNATURE = "signature";
    public static final String BIT_ID_PARAM_URI = "uri";
    private static final String TAG = "BitIDCommands";
    public static final NetworkParameters NETWORK_PARAMETERS = MainNetParams.get();

    /**
     * Take the data from the QR Code and follow the BIT ID spec.
     * https://github.com/bitid/bitid/blob/master/BIP_draft.md
     */
    public static void execute(final String data,
                               final DeterministicKey key,
                               final Activity activity) throws Exception {

        if(! BitID.checkBitidUriValidity(new URI(data)))
            return;

        final URI callback = BitID.buildCallbackUriFromBitidUri(new URI(data));

        final String signed = key.toECKey().signMessage(data);

        final String address = key.toECKey().toAddress(NETWORK_PARAMETERS).toString();

        // Cool let's tell the user what we are doing.

        DialogInterface.OnClickListener dialogClickListener =
                new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        doBitIDCallback(signed, callback.toString(), address, data, activity);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        String message = callback + " is requesting that you identify yourself. " +
                "Do you want to proceed ?";

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(message).setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener)
                .setTitle("BIP ID Sign in request.").show();
    }


    private static void doBitIDCallback(String signed, String postBack, String address,
                                        String data,
                                        final Activity activity) {

        OkHttpClient client = new OkHttpClient();

        RequestBody formBody = new FormEncodingBuilder()
                .add(BIT_ID_PARAM_ADDRESS, address)
                .add(BIT_ID_PARAM_SIGNATURE, signed)
                .add(BIT_ID_PARAM_URI, data)
                .build();

        Request request = new Request.Builder().url(postBack)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException throwable) {
                Log.e(TAG, throwable.toString());
            }

            @Override
            public void onResponse(final Response response) throws IOException {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            Toast toast = Toast.makeText(activity.getApplicationContext(),
                                    response.body().string(), Toast.LENGTH_SHORT);
                            toast.show();
                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
            }
        });
    }
}
