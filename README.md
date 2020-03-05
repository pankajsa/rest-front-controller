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


The microservice forwards the request to getQ if the authorization check is successful. RDP can use this queue to forward to a backend REST service

Absence of 'AuthHeader' in the HTTP REST request trigger the failure of authorization check and returns a error back to the caller

curl -v -X GET http://localhost:9000/api/a
will give a 401 Unauthorized Error

curl -v -X GET -H "Authorization: TESTING" http://localhost:9000/api/a
will forward the request to getQ, from where the RDP can take over and send the request downstream
