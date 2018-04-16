FROM openjdk:8-jdk

RUN apt-get -qq update && \
    apt-get -qq -y install libsodium-dev

EXPOSE 8080
EXPOSE 8888

ADD build/distributions/orion*.tar.gz /tmp
RUN mv /tmp/orion* /orion

VOLUME /data

CMD /orion/bin/orion /data/orion.conf