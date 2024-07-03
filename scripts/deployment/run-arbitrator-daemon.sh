#!/bin/bash
#
# Start arbitrator daemon on Monero's stagenet (Tuskex testnet)

runArbitrator() {
    ./tuskex-daemon --baseCurrencyNetwork=TSK_STAGENET \
    --useLocalhostForP2P=false \
    --useDevPrivilegeKeys=false \
    --nodePort=7777 \
    --appName=tuskex-TSK_STAGENET_arbitrator \
    --tskNode=http://127.0.0.1:38088 \
    --tskNodeUsername=admin \
    --tskNodePassword=password
}

cd /home/tuskex/tuskex && \
runArbitrator