
FROM openjdk:11.0.7-jre-slim-buster

# Install libsodium
RUN set -eux; \
    apt-get update; \
    apt-get install -y --no-install-recommends curl=7.64.0-4+deb10u2 libsodium23=1.0.17-1; \
    apt-get clean; \
    rm -rf /var/lib/apt/lists/*

# add Orion user
# hadolint ignore=DL3059
RUN adduser --disabled-password --gecos "" --home /opt/orion orion && \
    chown orion:orion /opt/orion

USER orion
WORKDIR /opt/orion

COPY --chown=orion:orion orion /opt/orion/

# Expose services ports
# 8080 Node Port, 8888 Client Port
EXPOSE 8080 8888

ENV PATH="/opt/orion/bin:${PATH}"
ENTRYPOINT ["orion"]

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
      org.label-schema.vendor="PegaSys" \
      org.label-schema.version=$VERSION \
      org.label-schema.schema-version="1.0"
