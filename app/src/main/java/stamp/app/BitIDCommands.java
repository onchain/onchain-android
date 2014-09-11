package stamp.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.widget.Toast;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.params.MainNetParams;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;

import java.net.URI;

public class BitIDCommands {

    public static final String BITID_PARAM_ADDRESS = "address";
    public static final String BITID_PARAM_SIGNATURE = "signature";
    public static final String BITID_PARAM_URI = "uri";
    public static final NetworkParameters NETWORK_PARAMETERS = MainNetParams.get();

    /**
     * Take the data from the QR Code and fiollow the BITID spec.
     * https://github.com/bitid/bitid/blob/master/BIP_draft.md
     */
    public static void execute(final String data,
                               final DeterministicKey key,
                               final Activity activity) throws Exception {

        if(! BitID.checkBitidUriValidity(new URI(data)))
            return;

        final URI callback = BitID.buildCallbackUriFromBitidUri(new URI(data));
        String[] params = data.split(":");


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


    private static void doBitIDCallback(String signed, String post_back, String address,
                                        String data,
                                        final Activity activity) {

        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams rp = new RequestParams();

        rp.put(BITID_PARAM_ADDRESS, address);
        rp.put(BITID_PARAM_SIGNATURE, signed);
        rp.put(BITID_PARAM_URI, data);

        Toast toast = Toast.makeText(activity.getApplicationContext(),
                "Sig " + signed, Toast.LENGTH_SHORT);
        toast.show();

        client.post(post_back, rp, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(String response) {

                Toast toast = Toast.makeText(activity.getApplicationContext(),
                        response, Toast.LENGTH_SHORT);
                toast.show();
            }
            @Override
            public void onFailure(int statusCode, Header[] headers,
                                  byte[] responseBody, Throwable error) {

                Toast toast = Toast.makeText(activity.getApplicationContext(),
                        "" + statusCode, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }
}
