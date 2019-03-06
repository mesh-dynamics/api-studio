#!/bin/bash

#http://redsymbol.net/articles/unofficial-bash-strict-mode


init() {
	kubectl apply -f moviebook.yaml
	kubectl apply -f moviebook-gateway.yaml
}

record() {
	kubectl apply -f moviebook-envoy-cs.yaml
}

stop_record() {
	kubectl delete -f moviebook-envoy-cs.yaml
}

replay() {
	kubectl apply -f moviebook-envoy-replay-cs.yaml
}

stop_replay() {
	kubectl delete -f moviebook-envoy-replay-cs.yaml
}

clean() {
	kubectl delete -f moviebook.yaml
	kubectl delete -f moviebook-gateway.yaml
}

main() {	
  set -eo pipefail; [[ "$TRACE" ]] && set -x
  case "$1" in
    init) shift; init "$@";;
    record) shift; record "@";;
    stop_recording) shift; stop_record "@";;
    replay) shift; replay "@";;
    stop_replay) shift; stop_replay "@";;
    clean) shift; clean "$@";;
    *) echo "This script expect one of these system argument(init, record, stop_recording, replay, stop_replay, clean).";;
  esac
}

main "$@"
