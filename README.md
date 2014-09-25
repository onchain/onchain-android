## The OnChain.IO wallet. A simple user interface hiding a HD wallet with some advanced features.

| Wallet | BIP 39 | BitID |
|--------|--------|--------|
|![](http://i.imgur.com/pGEmL2B.png)|![](http://i.imgur.com/S6frWkp.png)|![](http://i.imgur.com/qRcBoEn.png)|

### Notes

* Wallet functionality coming soon. i.e. spend and receive via QR code.
* The onchain.io app can be used as a safer form of 2 factor authentication. It is used by the https://www.onchain.io online wallet to split keys across devices.
* The onchain.io protocol for transaction signing has been implemented in the bitwasp project as a multi sig marketplace.
* The landing page for the android app is here [Android Bitcoin Wallet](https://www.onchain.io/android-bitcoin-wallet)

### How ?

Basically the onchain.io is a Hierarchcal deterministic Bitcoin wallet.It can issue Master Public Keys and sign P2SH transactions created with those keys.

### API

Below are some example commands.

#### Get a Master Public Key

    mpk|service-name|Callback URL (POST)|Pipe seperated paramers you supply

e.g.

    mpk|mywallet.com|hxxp://mywallet.com/external_mpk|user|980190962

#### Get a Public Key

    pubkey|service-name|Callback URL (POST)|Pipe seperated paramers you supply

e.g.

    pubkey|mywallet.com|hxxp://mywallet.com/public_key|user|980190963

#### To Sign a TX

    sign|service-name|Callback URL (GET and POST) to get the TX|Pipe seperated paramers you supply

e.g.

    sign|mywallet.com|hxxp://mywallet.com/sign_tx|user|980190962
    
For TX signing, your call back URL will be called twice. Once with a GET operation to get the existing TX. Seconds with a POST operation
to send the signed TX back to your service.

Simply take the command above and create a QR code for the onchain.io app to scan.

### Setting the wallet seed.

The first time it's run the onchain app will generate a BIP32 wallet seed. To backup the seed click on the menu and 
select "Show BIP39 Seed"

To set a seed generate a QR code containing a 24 word BIP39 compatible passphrase. Scan the passphrase in with the onchain app.


### How do I build this ?

Built with Android Studio.




