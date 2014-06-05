## Welcome to Stamp a mobile app for signing multi sig transactions from a QR code.

### Potential Uses

* Online Wallets. Stamp can be used as a safer form of 2 factor authentication.
* Online Market Places. A new wave of cryptocurrency marketplaces using multi sig transactions need an easy way for the user to sign the transaction.

### How ?

Basically Stamp is a Hierarchcal deterministic Bitcoin wallet that doesn't store any Bitcoins. Rather it can issue Master Public Keys and sign P2SH transactions created with those keys.

### Plan

1. Send a MPK after reading a QR code. IN PROGRESS.
2. Sign a TX generated from a P2SH using the above key. IN PROGRESS
3. Fix up GUI and look and feel.
4. Test on iOS

### Proposed API

Below are some example Stamp commands.

#### Get a Master Public Key

    mpk|service-name|Callback URL (POST)|Pipe seperated paramers you supply

e.g.

    mpk|mywallet.com|hxxp://mywallet.com/external_mpk|user|980190962

#### To Sign a TX

    sign|service-name|Callback URL (GET) to get the TX|Callback URL (POST) to send the signed TX|Pipe seperated paramers you supply

e.g.

    sign|mywallet.com|hxxp://mywallet.com/get_tx|hxxp://mywallet.com/get_tx|user|980190962

Simply take the Stamp command above and create a QR code for stamp to scan.


### How do I build this ?

Built with Android Studio.





