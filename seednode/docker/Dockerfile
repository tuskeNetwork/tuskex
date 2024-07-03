# docker run -it -p 9050 -p 2002 --restart-policy unless-stopped --name tuskex-seednode tuskex-seednode
# TODO: image very heavy, but it's hard to significantly reduce the size without bins

FROM openjdk:21-jdk-bullseye

RUN set -ex && \
    apt update && \
    apt --no-install-recommends --yes install \
        make \
        git \
        tor

RUN set -ex && adduser --system --group --disabled-password tuskex && \
    mkdir -p /home/tuskex && \
    chown -R tuskex:tuskex /home/tuskex

USER tuskex

WORKDIR /home/tuskex

RUN set -ex && git clone https://github.com/tuskeNetwork/tuskex.git && \
    cd tuskex && \
    make skip-tests

WORKDIR /home/tuskex/tuskex

ENTRYPOINT [ "./tuskex-seednode" ]

CMD ["--baseCurrencyNetwork=TSK_STAGENET", "--useLocalhostForP2P=false", "--useDevPrivilegeKeys=false", "--nodePort=2002", "--appName=tuskex-TSK_STAGENET_Seed_2002" ]
