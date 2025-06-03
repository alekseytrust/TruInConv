# Starts from the Java 21 dev container image
FROM mcr.microsoft.com/devcontainers/java:21

# Downloads & unpacks JavaFX SDK into /usr/local/javafx-sdk-21.0.3
RUN wget -qO- https://gluonhq.com/download/javafx-21-0-3-sdk-linux/ | tar xz -C /usr/local && \
    mv /usr/local/javafx-sdk-21.0.3 /usr/local/javafx-sdk-21.0.3

# Installs any additional system deps (unzip used by your conversions)
RUN apt-get update && apt-get install -y unzip && \
    rm -rf /var/lib/apt/lists/*

# Drops back to the non-root vscode user
USER vscode
