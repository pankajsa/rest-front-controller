# rest-front-controller

## Environment Setup
Switch a VPN in Solace Broker to Gateway mode and create the following queues
- pregetQ
- getQ

Setup the REST API Microgateway configuration and have the GET/> and POST/> messages mapped to *pregetQ*

## How to Run
gradle bootRun --args "hostname:portnum userid@vpnname"
e.g.
gradle bootRun --args "localhost:55555 default@default"




