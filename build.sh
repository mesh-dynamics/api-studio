RELEASE=1.0.0-SNAPSHOT
PHASE=package
BUILD_DOCKER=-Ddockerfile.skip=true

function usage() {
    echo "USAGE: $0 <OPTIONS>"
    echo "OPTIONS:"
    echo "-h|--help             Usage help"
    echo "--install             Run install phase (DEFAULT package)"
    echo "--deploy              Push built artifacts to github (DEFAULT package)"
    echo "--buildDocker         Build docker images (DEFAULT jars-only)"
    echo "-r|release=<version>  Version of the release (DEFAULT 1.0)"
    exit 1
}

for i in "$@"
do
case $i in
    -h|--help)
    usage
    shift
    ;;
    --deploy)
    PHASE=deploy
    shift
    ;;
    --install)
    INSTALL=install
    shift
    ;;
    --buildDocker)
    BUILD_DOCKER=
    shift
    ;;
    -r=*|--release=*)
    RELEASE="${i#*=}"
    shift
    ;;
    *)
    echo "Invalid Option"
    usage
    ;;
esac
done

STANDALONE_GATEWAY_SOURCE_JAR_PATH=services/gateway/target/gateway-$RELEASE.jar
STANDALONE_GATEWAY_TARGET_JAR_PATH=ui/bin/gateway-standalone.jar

STANDALONE_CORE_SOURCE_JAR_PATH=services/core/target/core-$RELEASE.jar
STANDALONE_CORE_TARGET_JAR_PATH=ui/bin/core-standalone.jar

mvn $PHASE $INSTALL -Drevision=$RELEASE -DskipTests $BUILD_DOCKER

cp $STANDALONE_GATEWAY_SOURCE_JAR_PATH $STANDALONE_GATEWAY_TARGET_JAR_PATH
cp $STANDALONE_CORE_SOURCE_JAR_PATH $STANDALONE_CORE_TARGET_JAR_PATH


