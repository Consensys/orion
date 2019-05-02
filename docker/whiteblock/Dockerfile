FROM ubuntu:16.04

RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y build-essential maven libsodium-dev \
    tmux wget iperf3 curl apt-utils iputils-ping expect npm git git-extras \
    software-properties-common openssh-server

# install java
RUN apt-get update && \
	apt-get install -y openjdk-8-jdk && \
	apt-get install -y ant && \
	apt-get clean && \
	rm -rf /var/lib/apt/lists/* && \
	rm -rf /var/cache/oracle-jdk8-installer;

RUN wget -O /root/libsodium.tar.gz "https://download.libsodium.org/libsodium/releases/LATEST.tar.gz"
WORKDIR /root
RUN tar -xzf libsodium.tar.gz
RUN cd libsodium-stable && \
    ./configure && \
    make -j $(nproc) && make check && \
    make install

EXPOSE 8080
EXPOSE 8888

WORKDIR /
RUN git clone --recursive https://github.com/PegaSysEng/orion.git
WORKDIR /orion
RUN ./gradlew build -x test
WORKDIR /orion/build/distributions
RUN tar -xzf orion-*-SNAPSHOT.tar.gz

RUN ln -s /orion/build/distributions/orion-*-SNAPSHOT/bin/orion /usr/local/bin/orion

WORKDIR /

ENTRYPOINT ["/bin/bash"]
