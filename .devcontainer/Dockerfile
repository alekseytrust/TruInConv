# Starts from the Java 21 dev container image
FROM mcr.microsoft.com/devcontainers/java:21

# Downloads & unpacks JavaFX SDK
RUN wget -q https://download2.gluonhq.com/openjfx/21.0.3/openjfx-21.0.3_linux-x64_bin-sdk.zip -O /tmp/javafx.zip \
    && unzip -q /tmp/javafx.zip -d /usr/local \
    && rm /tmp/javafx.zip

# Installs any additional system deps (unzip used by your conversions)
RUN apt-get update && apt-get install -y unzip && \
    rm -rf /var/lib/apt/lists/*

# Drops back to the non-root vscode user
USER vscode
