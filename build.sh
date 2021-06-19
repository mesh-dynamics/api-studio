RELEASE=1.0
STANDALONE_GATEWAY_SOURCE_JAR_PATH=services/gateway/target/gateway-standalone-$RELEASE.war
STANDALONE_GATEWAY_TARGET_JAR_PATH=ui/bin/gateway-standalone.jar

STANDALONE_CORE_SOURCE_JAR_PATH=services/core/target/core-standalone-$RELEASE.war
STANDALONE_CORE_TARGET_JAR_PATH=ui/bin/core-standalone.jar

DEPLOY=false

function usage() {
    echo "USAGE: $0 <OPTIONS>"
    echo "OPTIONS:"
    echo "-h|--help             Usage help"
    echo "--deploy              Push built artifacts to github (DEFAULT package)"
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
    DEPLOY=true
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

if [ "$DEPLOY" = true ] ; then
    mvn deploy -Drevision=$RELEASE -DskipTests
else mvn package -Drevision=$RELEASE -DskipTests
fi

cp $STANDALONE_GATEWAY_SOURCE_JAR_PATH $STANDALONE_GATEWAY_TARGET_JAR_PATH
cp $STANDALONE_CORE_SOURCE_JAR_PATH $STANDALONE_CORE_TARGET_JAR_PATH


