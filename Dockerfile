FROM openjdk:8-jdk

RUN apt-get -qq update && \
    apt-get -qq -y install libsodium-dev

EXPOSE 8080
EXPOSE 8888

ADD . /orion
WORKDIR /orion
RUN ./gradlew build -x test
WORKDIR /orion/build/distributions
RUN tar -xzf orion-*-SNAPSHOT.tar.gz
RUN ln -s /orion/build/distributions/orion-*-SNAPSHOT/bin/orion /usr/local/bin/orion

CMD orion
