## Welcome to the onchain.io mobile app for signing multi sig transactions from a QR code.

### Potential Uses

* Online Wallets. The onchain.io app can be used as a safer form of 2 factor authentication.
* Online Market Places. A new wave of cryptocurrency marketplaces using multi sig transactions need an easy way for the user to sign the transaction.

### How ?

Basically the onchain.io is a Hierarchcal deterministic Bitcoin wallet that doesn't store any Bitcoins. Rather it can issue Master Public Keys and sign P2SH transactions created with those keys.

### Proposed API

Below are some example commands.

#### Get a Master Public Key

    mpk|service-name|Callback URL (POST)|Pipe seperated paramers you supply

e.g.

    mpk|mywallet.com|hxxp://mywallet.com/external_mpk|user|980190962

#### To Sign a TX

    sign|service-name|Callback URL (GET and POST) to get the TX|Pipe seperated paramers you supply

e.g.

    sign|mywallet.com|hxxp://mywallet.com/sign_tx|user|980190962
    
For TX signing, your call back URL will be called twice. Once with a GET operation to get the existing TX. Seconds with a POST operation
to send the signed TX back to your service.

Simply take the command above and create a QR code for the onchain.io app to scan.


### How do I build this ?

Built with Android Studio.





