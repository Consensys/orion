FROM openjdk:11.0.5-jre-slim-buster

COPY orion /opt/orion/
WORKDIR /opt/orion

# Install latest libsodium
RUN set -eux; \
    apt-get update; \
    apt-get install -y --no-install-recommends libsodium23;



# Expose services ports
# 8080 Node Port, 8888 Client Port
EXPOSE 8080 8888

ENTRYPOINT ["/opt/orion/bin/orion"]

# Build-time metadata as defined at http://label-schema.org
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION
LABEL org.label-schema.build-date=$BUILD_DATE \
      org.label-schema.name="Orion" \
      org.label-schema.description="Private Transaction Manager" \
      org.label-schema.url="https://docs.orion.pegasys.tech/" \
      org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.vcs-url="https://github.com/PegaSysEng/orion.git" \
      org.label-schema.vendor="Pegasys" \
      org.label-schema.version=$VERSION \
      org.label-schema.schema-version="1.0"