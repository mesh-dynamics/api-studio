set -o errexit

VERSION="demo-1.0"
SCRIPTDIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

# to build and tag restwrapjdbc and sample_apps-mirest
mvn package

pushd "$SCRIPTDIR/details"
  #plain build -- no calling external book service to fetch topics
  docker build -t "istio/examples-bookinfo-details-v1:${VERSION}"  --build-arg service_version=v1 .
  #with calling external book service to fetch topic for the book
  #docker build -t "istio/examples-bookinfo-details-v2:${VERSION}"  --build-arg service_version=v2 \
  #       --build-arg enable_external_book_service=true .
popd

pushd "$SCRIPTDIR/reviews"
  #java build the app.
  docker run --rm -u root -v "$(pwd)":/home/gradle/project -w /home/gradle/project gradle:4.8.1 gradle clean build
  pushd reviews-wlpcfg
    #plain build -- no ratings
    docker build -t "istio/examples-bookinfo-reviews-v1:${VERSION}" --build-arg service_version=v1 .
    #with ratings black stars
    docker build -t "istio/examples-bookinfo-reviews-v2:${VERSION}" --build-arg service_version=v2 \
           --build-arg enable_ratings=true .
    #with ratings red stars
    docker build -t "istio/examples-bookinfo-reviews-v3:${VERSION}" --build-arg service_version=v3 \
           --build-arg enable_ratings=true --build-arg star_color=red .
  popd
popd

pushd "$SCRIPTDIR/ratings"
  docker build -t "istio/examples-bookinfo-ratings-v1:${VERSION}" --build-arg service_version=v1 .
  #docker build -t "istio/examples-bookinfo-ratings-v2:${VERSION}" --build-arg service_version=v2 .
popd
