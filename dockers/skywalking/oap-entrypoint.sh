#!/bin/bash
# Fixed entrypoint for SkyWalking OAP Server
# Downloads MySQL connector and builds classpath

set -e

echo "[Entrypoint] Apache SkyWalking Docker Image"

# Download MySQL connector if not exists
MYSQL_JAR="/skywalking/ext-libs/mysql-connector-j.jar"
if [ ! -f "$MYSQL_JAR" ]; then
    echo "[Entrypoint] Downloading MySQL connector..."
    mkdir -p /skywalking/ext-libs
    curl -sL https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar -o "$MYSQL_JAR"
    echo "[Entrypoint] MySQL connector downloaded: $(ls -lh "$MYSQL_JAR")"
fi

# Build classpath
CLASSPATH="config"
for jar in /skywalking/oap-libs/*.jar; do
    if [ -f "$jar" ]; then
        CLASSPATH="$CLASSPATH:$jar"
    fi
done

# Add ext-libs (including MySQL connector)
if [ -d /skywalking/ext-libs ]; then
    for jar in /skywalking/ext-libs/*.jar; do
        if [ -f "$jar" ]; then
            CLASSPATH="$CLASSPATH:$jar"
        fi
    done
fi

# Override config
if [ -d /skywalking/ext-config ] && [ "$(ls -A /skywalking/ext-config 2>/dev/null)" ]; then
    cp -f /skywalking/ext-config/* /skywalking/config/ 2>/dev/null || true
fi

echo "[Entrypoint] Starting OAP Server..."
echo "[Entrypoint] CLASSPATH length: ${#CLASSPATH}"

exec java ${JAVA_OPTS} -cp "${CLASSPATH}" org.apache.skywalking.oap.server.starter.OAPServerStartUp "$@"
