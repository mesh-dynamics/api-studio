import  React , { Component, Fragment } from "react";
import DiffResultsFilter from '../../components/DiffResultsFilter/DiffResultsFilter.js';
import DiffResultsList from '../../components/DiffResultsList/DiffResultsList.js';
import { Glyphicon} from 'react-bootstrap';
import {Link} from "react-router-dom";
import _ from 'lodash';
import sortJson from "../../utils/sort-json";
import ReduceDiff from '../../utils/ReduceDiff';
import generator from '../../utils/generator/json-path-generator';
import statusCodeList from "../../StatusCodeList"

const respData = {
    facets: {
        services: [{value: "s1", count: 2}, {value: "s2", count: 2}],
        apiPaths: [{value: "a1", count: 2}, {value: "a2", count: 2}],
        resolutionTypes: [{value: "ERR_", count: 2}],
        pages: 10,
    },
    results: [
    {
        "recordReqId": "gu--1789970137",
        "recordResponse": {
            "body": "[{\"rental_id\":22244}]",
            "hdrs": {
                "date": [
                    "Tue, 18 Feb 2020 09:01:35 GMT"
                ],
                "content-length": [
                    "21"
                ],
                "x-envoy-upstream-service-time": [
                    "148"
                ],
                "content-type": [
                    "application/json"
                ],
                ":status": [
                    "200"
                ]
            },
            "status": 200
        },
        "reqCompDiff": [],
        "respCompResType": "ExactMatch",
        "reqCompResType": "DontCare",
        "replayResponse": {
            "body": "[{\"rental_id\":22244}]",
            "hdrs": {
                "date": [
                    "Tue, 18 Feb 2020 09:01:35 GMT"
                ],
                "content-length": [
                    "21"
                ],
                "x-envoy-upstream-service-time": [
                    "148"
                ],
                "content-type": [
                    "application/json"
                ],
                ":status": [
                    "200"
                ]
            },
            "status": 200
        },
        "recordTraceId": "d9384131fd3e361adfad136c4d52db23",
        "recordRequest": {
            "method": "GET",
            "queryParams": {
                "querystring": [
                    "SELECT rental_id from rental WHERE inventory_id = ? and customer_id = ? and staff_id = ? and return_date is null"
                ],
                "params": [
                    "[{\"index\":1,\"type\":\"integer\",\"value\":840},{\"index\":2,\"type\":\"integer\",\"value\":446},{\"index\":3,\"type\":\"integer\",\"value\":1}]"
                ]
            },
            "formParams": {},
            "body": "",
            "hdrs": {
                "x-request-id": [
                    "b6f1a691-c0c7-4883-a550-49b600041e9b"
                ],
                "x-b3-parentspanid": [
                    "64a9b6df1bd20f1c"
                ],
                "content-length": [
                    "0"
                ],
                "x-forwarded-proto": [
                    "http"
                ],
                "x-b3-sampled": [
                    "0"
                ],
                "x-istio-attributes": [
                    "ClIKGGRlc3RpbmF0aW9uLnNlcnZpY2UuaG9zdBI2EjRyZXN0d3JhcGpkYmMubW92aWVib29rLXJlY29yZC1wcm9kLnN2Yy5jbHVzdGVyLmxvY2FsClAKF2Rlc3RpbmF0aW9uLnNlcnZpY2UudWlkEjUSM2lzdGlvOi8vbW92aWVib29rLXJlY29yZC1wcm9kL3NlcnZpY2VzL3Jlc3R3cmFwamRiYwoqChhkZXN0aW5hdGlvbi5zZXJ2aWNlLm5hbWUSDhIMcmVzdHdyYXBqZGJjCjgKHWRlc3RpbmF0aW9uLnNlcnZpY2UubmFtZXNwYWNlEhcSFW1vdmllYm9vay1yZWNvcmQtcHJvZApQCgpzb3VyY2UudWlkEkISQGt1YmVybmV0ZXM6Ly9tb3ZpZWluZm8tdjEtNjY1YzU5Y2Q4Ny12aHZudy5tb3ZpZWJvb2stcmVjb3JkLXByb2Q="
                ],
                "accept": [
                    "application/json"
                ],
                ":method": [
                    "GET"
                ],
                "x-b3-traceid": [
                    "d9384131fd3e361adfad136c4d52db23"
                ],
                "x-b3-spanid": [
                    "0ee4afa7a2045e2b"
                ],
                ":path": [
                    "/restsql/query?querystring=SELECT+rental_id+from+rental+WHERE+inventory_id+%3D+%3F+and+customer_id+%3D+%3F+and+staff_id+%3D+%3F+and+return_date+is+null&params=%5B%7B%22index%22%3A1%2C%22type%22%3A%22integer%22%2C%22value%22%3A840%7D%2C%7B%22index%22%3A2%2C%22type%22%3A%22integer%22%2C%22value%22%3A446%7D%2C%7B%22index%22%3A3%2C%22type%22%3A%22integer%22%2C%22value%22%3A1%7D%5D"
                ],
                ":authority": [
                    "restwrapjdbc:8080"
                ],
                "user-agent": [
                    "Jersey/2.27 (HttpUrlConnection 11.0.3)"
                ]
            }
        },
        "path": "restsql/query",
        "replayRequest": {
            "method": "GET",
            "queryParams": {
                "querystring": [
                    "SELECT rental_id from rental WHERE inventory_id = ? and customer_id = ? and staff_id = ? and return_date is null"
                ],
                "params": [
                    "[{\"index\":1,\"type\":\"integer\",\"value\":840},{\"index\":2,\"type\":\"integer\",\"value\":446},{\"index\":3,\"type\":\"integer\",\"value\":1}]"
                ]
            },
            "formParams": {},
            "body": "",
            "hdrs": {
                "x-request-id": [
                    "fad54038-944a-98b8-a366-7c77592ccc92"
                ],
                "content-length": [
                    "0"
                ],
                "x-b3-parentspanid": [
                    "8c30c419adddd613"
                ],
                "x-forwarded-proto": [
                    "http"
                ],
                "x-b3-sampled": [
                    "0"
                ],
                "x-forwarded-port": [
                    "443"
                ],
                "x-forwarded-for": [
                    "18.218.226.157,172.20.97.78"
                ],
                "x-envoy-original-path": [
                    "/restsql/query?querystring=SELECT+rental_id+from+rental+WHERE+inventory_id+%3D+%3F+and+customer_id+%3D+%3F+and+staff_id+%3D+%3F+and+return_date+is+null&params=%5B%7B%22index%22%3A1%2C%22type%22%3A%22integer%22%2C%22value%22%3A840%7D%2C%7B%22index%22%3A2%2C%22type%22%3A%22integer%22%2C%22value%22%3A446%7D%2C%7B%22index%22%3A3%2C%22type%22%3A%22integer%22%2C%22value%22%3A1%7D%5D"
                ],
                "accept": [
                    "application/json"
                ],
                "authorization": [
                    "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJNZXNoREFnZW50VXNlciIsInJvbGVzIjpbIlJPTEVfVVNFUiJdLCJ0eXBlIjoicGF0IiwiaWF0IjoxNTc5ODY0MDE3LCJleHAiOjE4OTUyMjQwMTd9.JjTcDlf8EB_iueDWSollLrr1kn7a9e3Yr0kQ2BtdLAk"
                ],
                "x-envoy-external-address": [
                    "172.20.97.78"
                ],
                "x-b3-traceid": [
                    "d9384131fd3e361adfad136c4d52db23"
                ],
                "x-b3-spanid": [
                    "e12d0577a31c74c4"
                ],
                "host": [
                    "cubews-record.cube.svc.cluster.local:8080"
                ],
                "user-agent": [
                    "Jersey/2.27 (HttpUrlConnection 11.0.3)"
                ]
            }
        },
        "respCompDiff": [],
        "service": "restwrapjdbc",
        "replayTraceId": "d9384131fd3e361adfad136c4d52db23",
        "replayReqId": "restwrapjdbc-mock-d87355bc-338c-46d0-a7cf-2d58d9aabb89",
        "numMatch": 1,
        "reqMatchResType": "ExactMatch"
    },
    {
        "recordReqId": "gu-1272896894",
        "recordResponse": {
            "body": "{\"num_updates\":1}",
            "hdrs": {
                "date": [
                    "Tue, 18 Feb 2020 09:01:35 GMT"
                ],
                "content-length": [
                    "17"
                ],
                "x-envoy-upstream-service-time": [
                    "149"
                ],
                "content-type": [
                    "application/json"
                ],
                ":status": [
                    "200"
                ]
            },
            "status": 200
        },
        "reqCompDiff": [],
        "respCompResType": "ExactMatch",
        "reqCompResType": "DontCare",
        "replayResponse": {
            "body": "{\"num_updates\":1}",
            "hdrs": {
                "date": [
                    "Tue, 18 Feb 2020 09:01:35 GMT"
                ],
                "content-length": [
                    "17"
                ],
                "x-envoy-upstream-service-time": [
                    "149"
                ],
                "content-type": [
                    "application/json"
                ],
                ":status": [
                    "200"
                ]
            },
            "status": 200
        },
        "recordTraceId": "d9384131fd3e361adfad136c4d52db23",
        "recordRequest": {
            "method": "POST",
            "queryParams": {},
            "formParams": {},
            "body": "{\"query\":\"UPDATE rental SET return_date = ? WHERE rental_id = ?\",\"params\":[{\"index\":1,\"type\":\"string\",\"value\":\"2020-02-18 09:01:35\"},{\"index\":2,\"type\":\"integer\",\"value\":22244}]}",
            "hdrs": {
                "x-request-id": [
                    "ae939dc1-96a5-477b-8f57-6aca447b70aa"
                ],
                "content-length": [
                    "177"
                ],
                "x-b3-parentspanid": [
                    "64a9b6df1bd20f1c"
                ],
                "x-forwarded-proto": [
                    "http"
                ],
                "x-b3-sampled": [
                    "0"
                ],
                "x-istio-attributes": [
                    "CioKGGRlc3RpbmF0aW9uLnNlcnZpY2UubmFtZRIOEgxyZXN0d3JhcGpkYmMKOAodZGVzdGluYXRpb24uc2VydmljZS5uYW1lc3BhY2USFxIVbW92aWVib29rLXJlY29yZC1wcm9kClAKCnNvdXJjZS51aWQSQhJAa3ViZXJuZXRlczovL21vdmllaW5mby12MS02NjVjNTljZDg3LXZodm53Lm1vdmllYm9vay1yZWNvcmQtcHJvZApSChhkZXN0aW5hdGlvbi5zZXJ2aWNlLmhvc3QSNhI0cmVzdHdyYXBqZGJjLm1vdmllYm9vay1yZWNvcmQtcHJvZC5zdmMuY2x1c3Rlci5sb2NhbApQChdkZXN0aW5hdGlvbi5zZXJ2aWNlLnVpZBI1EjNpc3RpbzovL21vdmllYm9vay1yZWNvcmQtcHJvZC9zZXJ2aWNlcy9yZXN0d3JhcGpkYmM="
                ],
                "accept": [
                    "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2"
                ],
                ":method": [
                    "POST"
                ],
                "x-b3-traceid": [
                    "d9384131fd3e361adfad136c4d52db23"
                ],
                "x-b3-spanid": [
                    "2ac7a22c8952d700"
                ],
                ":path": [
                    "/restsql/update"
                ],
                "content-type": [
                    "application/json"
                ],
                ":authority": [
                    "restwrapjdbc:8080"
                ],
                "user-agent": [
                    "Jersey/2.27 (HttpUrlConnection 11.0.3)"
                ]
            }
        },
        "path": "restsql/update",
        "replayRequest": {
            "method": "POST",
            "queryParams": {},
            "formParams": {},
            "body": "{\"query\":\"UPDATE rental SET return_date = ? WHERE rental_id = ?\",\"params\":[{\"index\":1,\"type\":\"string\",\"value\":\"2020-02-19 21:36:37\"},{\"index\":2,\"type\":\"integer\",\"value\":22244}]}",
            "hdrs": {
                "x-request-id": [
                    "86206f1c-a101-9771-a9a6-4d2f3c4c49f1"
                ],
                "content-length": [
                    "177"
                ],
                "x-b3-parentspanid": [
                    "db6e895fdd766ed1"
                ],
                "x-forwarded-proto": [
                    "http"
                ],
                "x-b3-sampled": [
                    "0"
                ],
                "x-forwarded-port": [
                    "443"
                ],
                "x-forwarded-for": [
                    "18.218.226.157,172.20.94.107"
                ],
                "x-envoy-original-path": [
                    "/restsql/update"
                ],
                "accept": [
                    "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2"
                ],
                "authorization": [
                    "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJNZXNoREFnZW50VXNlciIsInJvbGVzIjpbIlJPTEVfVVNFUiJdLCJ0eXBlIjoicGF0IiwiaWF0IjoxNTc5ODY0MDE3LCJleHAiOjE4OTUyMjQwMTd9.JjTcDlf8EB_iueDWSollLrr1kn7a9e3Yr0kQ2BtdLAk"
                ],
                "x-envoy-external-address": [
                    "172.20.94.107"
                ],
                "x-b3-traceid": [
                    "d9384131fd3e361adfad136c4d52db23"
                ],
                "x-b3-spanid": [
                    "5b7fe35db1ee9ec9"
                ],
                "host": [
                    "cubews-record.cube.svc.cluster.local:8080"
                ],
                "content-type": [
                    "application/json"
                ],
                "user-agent": [
                    "Jersey/2.27 (HttpUrlConnection 11.0.3)"
                ]
            }
        },
        "respCompDiff": [],
        "service": "restwrapjdbc",
        "replayTraceId": "d9384131fd3e361adfad136c4d52db23",
        "replayReqId": "restwrapjdbc-mock-da95fd31-d2c9-4f00-b905-a284d76c07d2",
        "numMatch": 1,
        "reqMatchResType": "ExactMatch"
    },
    {
        "recordReqId": "gu-567961596",
        "recordResponse": {
            "body": "{\"num_updates\":1}",
            "hdrs": {
                "date": [
                    "Tue, 18 Feb 2020 09:01:35 GMT"
                ],
                "content-length": [
                    "17"
                ],
                "x-envoy-upstream-service-time": [
                    "150"
                ],
                "content-type": [
                    "application/json"
                ],
                ":status": [
                    "200"
                ]
            },
            "status": 200
        },
        "reqCompDiff": [],
        "respCompResType": "ExactMatch",
        "reqCompResType": "DontCare",
        "replayResponse": {
            "body": "{\"num_updates\":1}",
            "hdrs": {
                "date": [
                    "Tue, 18 Feb 2020 09:01:35 GMT"
                ],
                "content-length": [
                    "17"
                ],
                "x-envoy-upstream-service-time": [
                    "150"
                ],
                "content-type": [
                    "application/json"
                ],
                ":status": [
                    "200"
                ]
            },
            "status": 200
        },
        "recordTraceId": "d9384131fd3e361adfad136c4d52db23",
        "recordRequest": {
            "method": "POST",
            "queryParams": {},
            "formParams": {},
            "body": "{\"query\":\"INSERT INTO payment (customer_id, staff_id, rental_id, amount, payment_date) VALUES (?, ?, ?, ?, ?)\",\"params\":[{\"index\":1,\"type\":\"integer\",\"value\":446},{\"index\":2,\"type\":\"integer\",\"value\":1},{\"index\":3,\"type\":\"integer\",\"value\":22244},{\"index\":4,\"type\":\"double\",\"value\":5.98},{\"index\":5,\"type\":\"string\",\"value\":\"2020-02-18 09:01:35\"}]}",
            "hdrs": {
                "x-request-id": [
                    "81f9fd12-4d2d-4bc5-94ab-b1567241958e"
                ],
                "content-length": [
                    "344"
                ],
                "x-b3-parentspanid": [
                    "64a9b6df1bd20f1c"
                ],
                "x-forwarded-proto": [
                    "http"
                ],
                "x-b3-sampled": [
                    "0"
                ],
                "x-istio-attributes": [
                    "ClIKGGRlc3RpbmF0aW9uLnNlcnZpY2UuaG9zdBI2EjRyZXN0d3JhcGpkYmMubW92aWVib29rLXJlY29yZC1wcm9kLnN2Yy5jbHVzdGVyLmxvY2FsClAKF2Rlc3RpbmF0aW9uLnNlcnZpY2UudWlkEjUSM2lzdGlvOi8vbW92aWVib29rLXJlY29yZC1wcm9kL3NlcnZpY2VzL3Jlc3R3cmFwamRiYwoqChhkZXN0aW5hdGlvbi5zZXJ2aWNlLm5hbWUSDhIMcmVzdHdyYXBqZGJjCjgKHWRlc3RpbmF0aW9uLnNlcnZpY2UubmFtZXNwYWNlEhcSFW1vdmllYm9vay1yZWNvcmQtcHJvZApQCgpzb3VyY2UudWlkEkISQGt1YmVybmV0ZXM6Ly9tb3ZpZWluZm8tdjEtNjY1YzU5Y2Q4Ny12aHZudy5tb3ZpZWJvb2stcmVjb3JkLXByb2Q="
                ],
                "accept": [
                    "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2"
                ],
                ":method": [
                    "POST"
                ],
                "x-b3-traceid": [
                    "d9384131fd3e361adfad136c4d52db23"
                ],
                "x-b3-spanid": [
                    "6c133f3ae7886768"
                ],
                ":path": [
                    "/restsql/update"
                ],
                "content-type": [
                    "application/json"
                ],
                ":authority": [
                    "restwrapjdbc:8080"
                ],
                "user-agent": [
                    "Jersey/2.27 (HttpUrlConnection 11.0.3)"
                ]
            }
        },
        "path": "restsql/update",
        "replayRequest": {
            "method": "POST",
            "queryParams": {},
            "formParams": {},
            "body": "{\"query\":\"INSERT INTO payment (customer_id, staff_id, rental_id, amount, payment_date) VALUES (?, ?, ?, ?, ?)\",\"params\":[{\"index\":1,\"type\":\"integer\",\"value\":446},{\"index\":2,\"type\":\"integer\",\"value\":1},{\"index\":3,\"type\":\"integer\",\"value\":22244},{\"index\":4,\"type\":\"double\",\"value\":5.98},{\"index\":5,\"type\":\"string\",\"value\":\"2020-02-19 21:36:37\"}]}",
            "hdrs": {
                "x-request-id": [
                    "d5c9f78e-ee3d-9a1d-b425-5fb5e9079d8a"
                ],
                "content-length": [
                    "344"
                ],
                "x-b3-parentspanid": [
                    "f6c78f1a66270365"
                ],
                "x-forwarded-proto": [
                    "http"
                ],
                "x-b3-sampled": [
                    "0"
                ],
                "x-forwarded-port": [
                    "443"
                ],
                "x-forwarded-for": [
                    "18.218.226.157,100.96.6.1"
                ],
                "x-envoy-original-path": [
                    "/restsql/update"
                ],
                "accept": [
                    "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2"
                ],
                "authorization": [
                    "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJNZXNoREFnZW50VXNlciIsInJvbGVzIjpbIlJPTEVfVVNFUiJdLCJ0eXBlIjoicGF0IiwiaWF0IjoxNTc5ODY0MDE3LCJleHAiOjE4OTUyMjQwMTd9.JjTcDlf8EB_iueDWSollLrr1kn7a9e3Yr0kQ2BtdLAk"
                ],
                "x-envoy-external-address": [
                    "100.96.6.1"
                ],
                "x-b3-traceid": [
                    "d9384131fd3e361adfad136c4d52db23"
                ],
                "x-b3-spanid": [
                    "3a257d7ffcf1a146"
                ],
                "host": [
                    "cubews-record.cube.svc.cluster.local:8080"
                ],
                "content-type": [
                    "application/json"
                ],
                "user-agent": [
                    "Jersey/2.27 (HttpUrlConnection 11.0.3)"
                ]
            }
        },
        "respCompDiff": [],
        "service": "restwrapjdbc",
        "replayTraceId": "d9384131fd3e361adfad136c4d52db23",
        "replayReqId": "restwrapjdbc-mock-c25e1587-f5e5-487d-91fa-810852bce343",
        "numMatch": 1,
        "reqMatchResType": "ExactMatch"
    },
    {
        "recordReqId": "gu-1572639919",
        "recordResponse": {
            "body": "[{\"actors_lastnames\":\"CRONYN,TANDY,CRUZ,PECK,AKROYD,WAHLBERG,HARRIS,JOHANSSON\",\"film_id\":207,\"title\":\"DANGEROUS UPTOWN\",\"actors_firstnames\":\"ANNE,MARY,RALPH,SPENCER,KIRSTEN,DARYL,CATE,ALBERT\",\"film_counts\":\"27,31,28,21,34,31,28,33\"}]",
            "hdrs": {
                "date": [
                    "Tue, 18 Feb 2020 09:01:35 GMT"
                ],
                "content-length": [
                    "233"
                ],
                "x-envoy-upstream-service-time": [
                    "197"
                ],
                "content-type": [
                    "application/json"
                ],
                ":status": [
                    "200"
                ]
            },
            "status": 200
        },
        "reqCompDiff": [],
        "respCompResType": "ExactMatch",
        "reqCompResType": "DontCare",
        "replayResponse": {
            "body": "[{\"actors_lastnames\":\"CRONYN,TANDY,CRUZ,PECK,AKROYD,WAHLBERG,HARRIS,JOHANSSON\",\"film_id\":207,\"title\":\"DANGEROUS UPTOWN\",\"actors_firstnames\":\"ANNE,MARY,RALPH,SPENCER,KIRSTEN,DARYL,CATE,ALBERT\",\"film_counts\":\"27,31,28,21,34,31,28,33\"}]",
            "hdrs": {
                "date": [
                    "Tue, 18 Feb 2020 09:01:35 GMT"
                ],
                "content-length": [
                    "233"
                ],
                "x-envoy-upstream-service-time": [
                    "197"
                ],
                "content-type": [
                    "application/json"
                ],
                ":status": [
                    "200"
                ]
            },
            "status": 200
        },
        "recordTraceId": "cf96c788bcb4994320d26a2d2f506247",
        "recordRequest": {
            "method": "GET",
            "queryParams": {
                "querystring": [
                    "select film.film_id as film_id, film.title as title, group_concat(actor_film_count.first_name) as actors_firstnames, group_concat(actor_film_count.last_name) as actors_lastnames, group_concat(actor_film_count.film_count) as film_counts from film, film_actor, actor_film_count  where film.film_id = film_actor.film_id and film_actor.actor_id = actor_film_count.actor_id  and title = ? group by film.film_id, film.title"
                ],
                "params": [
                    "[{\"index\":1,\"type\":\"string\",\"value\":\"DANGEROUS UPTOWN\"}]"
                ]
            },
            "formParams": {},
            "body": "",
            "hdrs": {
                "x-request-id": [
                    "8827890d-c4c6-46bf-a92d-91252de36e87"
                ],
                "x-b3-parentspanid": [
                    "33110a76fa4ebc40"
                ],
                "content-length": [
                    "0"
                ],
                "x-forwarded-proto": [
                    "http"
                ],
                "x-b3-sampled": [
                    "0"
                ],
                "x-istio-attributes": [
                    "ClIKGGRlc3RpbmF0aW9uLnNlcnZpY2UuaG9zdBI2EjRyZXN0d3JhcGpkYmMubW92aWVib29rLXJlY29yZC1wcm9kLnN2Yy5jbHVzdGVyLmxvY2FsClAKF2Rlc3RpbmF0aW9uLnNlcnZpY2UudWlkEjUSM2lzdGlvOi8vbW92aWVib29rLXJlY29yZC1wcm9kL3NlcnZpY2VzL3Jlc3R3cmFwamRiYwoqChhkZXN0aW5hdGlvbi5zZXJ2aWNlLm5hbWUSDhIMcmVzdHdyYXBqZGJjCjgKHWRlc3RpbmF0aW9uLnNlcnZpY2UubmFtZXNwYWNlEhcSFW1vdmllYm9vay1yZWNvcmQtcHJvZApQCgpzb3VyY2UudWlkEkISQGt1YmVybmV0ZXM6Ly9tb3ZpZWluZm8tdjEtNjY1YzU5Y2Q4Ny12aHZudy5tb3ZpZWJvb2stcmVjb3JkLXByb2Q="
                ],
                "accept": [
                    "application/json"
                ],
                ":method": [
                    "GET"
                ],
                "x-b3-traceid": [
                    "cf96c788bcb4994320d26a2d2f506247"
                ],
                "x-b3-spanid": [
                    "39971c2a11754270"
                ],
                ":path": [
                    "/restsql/query?querystring=select+film.film_id+as+film_id%2C+film.title+as+title%2C+group_concat%28actor_film_count.first_name%29+as+actors_firstnames%2C+group_concat%28actor_film_count.last_name%29+as+actors_lastnames%2C+group_concat%28actor_film_count.film_count%29+as+film_counts+from+film%2C+film_actor%2C+actor_film_count++where+film.film_id+%3D+film_actor.film_id+and+film_actor.actor_id+%3D+actor_film_count.actor_id++and+title+%3D+%3F+group+by+film.film_id%2C+film.title&params=%5B%7B%22index%22%3A1%2C%22type%22%3A%22string%22%2C%22value%22%3A%22DANGEROUS%20UPTOWN%22%7D%5D"
                ],
                ":authority": [
                    "restwrapjdbc:8080"
                ],
                "user-agent": [
                    "Jersey/2.27 (HttpUrlConnection 11.0.3)"
                ]
            }
        },
        "path": "restsql/query",
        "replayRequest": {
            "method": "GET",
            "queryParams": {
                "querystring": [
                    "select film.film_id as film_id, film.title as title, group_concat(actor_film_count.first_name) as actors_firstnames, group_concat(actor_film_count.last_name) as actors_lastnames, group_concat(actor_film_count.film_count) as film_counts from film, film_actor, actor_film_count  where film.film_id = film_actor.film_id and film_actor.actor_id = actor_film_count.actor_id  and title = ? group by film.film_id, film.title"
                ],
                "params": [
                    "[{\"index\":1,\"type\":\"string\",\"value\":\"DANGEROUS UPTOWN\"}]"
                ]
            },
            "formParams": {},
            "body": "",
            "hdrs": {
                "x-request-id": [
                    "f10822e6-fe51-9bf7-9d52-820a24159f2a"
                ],
                "content-length": [
                    "0"
                ],
                "x-b3-parentspanid": [
                    "53e8d95c145e5f97"
                ],
                "x-forwarded-proto": [
                    "http"
                ],
                "x-b3-sampled": [
                    "0"
                ],
                "x-forwarded-port": [
                    "443"
                ],
                "x-forwarded-for": [
                    "18.218.226.157,172.20.97.78"
                ],
                "accept": [
                    "application/json"
                ],
                "x-envoy-original-path": [
                    "/restsql/query?querystring=select+film.film_id+as+film_id%2C+film.title+as+title%2C+group_concat%28actor_film_count.first_name%29+as+actors_firstnames%2C+group_concat%28actor_film_count.last_name%29+as+actors_lastnames%2C+group_concat%28actor_film_count.film_count%29+as+film_counts+from+film%2C+film_actor%2C+actor_film_count++where+film.film_id+%3D+film_actor.film_id+and+film_actor.actor_id+%3D+actor_film_count.actor_id++and+title+%3D+%3F+group+by+film.film_id%2C+film.title&params=%5B%7B%22index%22%3A1%2C%22type%22%3A%22string%22%2C%22value%22%3A%22DANGEROUS%20UPTOWN%22%7D%5D"
                ],
                "authorization": [
                    "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJNZXNoREFnZW50VXNlciIsInJvbGVzIjpbIlJPTEVfVVNFUiJdLCJ0eXBlIjoicGF0IiwiaWF0IjoxNTc5ODY0MDE3LCJleHAiOjE4OTUyMjQwMTd9.JjTcDlf8EB_iueDWSollLrr1kn7a9e3Yr0kQ2BtdLAk"
                ],
                "x-envoy-external-address": [
                    "172.20.97.78"
                ],
                "x-b3-traceid": [
                    "cf96c788bcb4994320d26a2d2f506247"
                ],
                "x-b3-spanid": [
                    "da365f9b89c89d9d"
                ],
                "host": [
                    "cubews-record.cube.svc.cluster.local:8080"
                ],
                "user-agent": [
                    "Jersey/2.27 (HttpUrlConnection 11.0.3)"
                ]
            }
        },
        "respCompDiff": [],
        "service": "restwrapjdbc",
        "replayTraceId": "cf96c788bcb4994320d26a2d2f506247",
        "replayReqId": "restwrapjdbc-mock-3e4380ce-cc81-4060-8d37-f955bc78e072",
        "numMatch": 1,
        "reqMatchResType": "ExactMatch"
    },
    {
        "recordReqId": "gu-1881980466",
        "recordResponse": {
            "body": "{\"id\":\"207\",\"reviews\":[{\"reviewer\":\"Reviewer1\",\"text\":\"An extremely entertaining play by Shakespeare. The slapstick humour is refreshing!\",\"rating\":{\"stars\":5,\"color\":\"red\"}},{\"reviewer\":\"Reviewer2\",\"text\":\"Absolutely fun and entertaining. The play lacks thematic depth when compared to other plays by Shakespeare.\",\"rating\":{\"stars\":4,\"color\":\"red\"}}]}",
            "hdrs": {
                "date": [
                    "Tue, 18 Feb 2020 09:01:36 GMT"
                ],
                "content-length": [
                    "377"
                ],
                "x-envoy-upstream-service-time": [
                    "252"
                ],
                "x-powered-by": [
                    "Servlet/3.1"
                ],
                "content-type": [
                    "application/json"
                ],
                ":status": [
                    "200"
                ],
                "content-language": [
                    "en-US"
                ]
            },
            "status": 200
        },
        "reqCompDiff": [],
        "respCompResType": "ExactMatch",
        "reqCompResType": "DontCare",
        "replayResponse": {
            "body": "{\"id\":\"207\",\"reviews\":[{\"reviewer\":\"Reviewer1\",\"text\":\"An extremely entertaining play by Shakespeare. The slapstick humour is refreshing!\",\"rating\":{\"stars\":5,\"color\":\"red\"}},{\"reviewer\":\"Reviewer2\",\"text\":\"Absolutely fun and entertaining. The play lacks thematic depth when compared to other plays by Shakespeare.\",\"rating\":{\"stars\":4,\"color\":\"red\"}}]}",
            "hdrs": {
                "date": [
                    "Tue, 18 Feb 2020 09:01:36 GMT"
                ],
                "content-length": [
                    "377"
                ],
                "x-envoy-upstream-service-time": [
                    "252"
                ],
                "x-powered-by": [
                    "Servlet/3.1"
                ],
                "content-type": [
                    "application/json"
                ],
                ":status": [
                    "200"
                ],
                "content-language": [
                    "en-US"
                ]
            },
            "status": 200
        },
        "recordTraceId": "cf96c788bcb4994320d26a2d2f506247",
        "recordRequest": {
            "method": "GET",
            "queryParams": {},
            "formParams": {},
            "body": "",
            "hdrs": {
                "x-request-id": [
                    "f0c96a4b-e6d7-41d2-a250-f0083a9271e7"
                ],
                "x-b3-parentspanid": [
                    "33110a76fa4ebc40"
                ],
                "content-length": [
                    "0"
                ],
                "x-forwarded-proto": [
                    "http"
                ],
                "x-b3-sampled": [
                    "0"
                ],
                "x-istio-attributes": [
                    "CiUKGGRlc3RpbmF0aW9uLnNlcnZpY2UubmFtZRIJEgdyZXZpZXdzCjgKHWRlc3RpbmF0aW9uLnNlcnZpY2UubmFtZXNwYWNlEhcSFW1vdmllYm9vay1yZWNvcmQtcHJvZApQCgpzb3VyY2UudWlkEkISQGt1YmVybmV0ZXM6Ly9tb3ZpZWluZm8tdjEtNjY1YzU5Y2Q4Ny12aHZudy5tb3ZpZWJvb2stcmVjb3JkLXByb2QKTQoYZGVzdGluYXRpb24uc2VydmljZS5ob3N0EjESL3Jldmlld3MubW92aWVib29rLXJlY29yZC1wcm9kLnN2Yy5jbHVzdGVyLmxvY2FsCksKF2Rlc3RpbmF0aW9uLnNlcnZpY2UudWlkEjASLmlzdGlvOi8vbW92aWVib29rLXJlY29yZC1wcm9kL3NlcnZpY2VzL3Jldmlld3M="
                ],
                "accept": [
                    "application/json"
                ],
                ":method": [
                    "GET"
                ],
                "x-b3-traceid": [
                    "cf96c788bcb4994320d26a2d2f506247"
                ],
                "x-b3-spanid": [
                    "4881e93dfe17d5a5"
                ],
                ":path": [
                    "/reviews/207"
                ],
                ":authority": [
                    "reviews:9080"
                ],
                "user-agent": [
                    "Jersey/2.27 (HttpUrlConnection 11.0.3)"
                ]
            }
        },
        "path": "reviews/207",
        "replayRequest": {
            "method": "GET",
            "queryParams": {},
            "formParams": {},
            "body": "",
            "hdrs": {
                "x-request-id": [
                    "8ab05411-6ebe-9c8b-993a-61f64b68ff9b"
                ],
                "content-length": [
                    "0"
                ],
                "x-b3-parentspanid": [
                    "4fb8f96cb18c6de8"
                ],
                "x-forwarded-proto": [
                    "http"
                ],
                "x-b3-sampled": [
                    "0"
                ],
                "x-forwarded-port": [
                    "443"
                ],
                "x-forwarded-for": [
                    "18.218.226.157,172.20.97.78"
                ],
                "accept": [
                    "application/json"
                ],
                "x-envoy-original-path": [
                    "/reviews/207"
                ],
                "authorization": [
                    "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJNZXNoREFnZW50VXNlciIsInJvbGVzIjpbIlJPTEVfVVNFUiJdLCJ0eXBlIjoicGF0IiwiaWF0IjoxNTc5ODY0MDE3LCJleHAiOjE4OTUyMjQwMTd9.JjTcDlf8EB_iueDWSollLrr1kn7a9e3Yr0kQ2BtdLAk"
                ],
                "x-envoy-external-address": [
                    "172.20.97.78"
                ],
                "x-b3-traceid": [
                    "cf96c788bcb4994320d26a2d2f506247"
                ],
                "x-b3-spanid": [
                    "6696d1671cc93748"
                ],
                "host": [
                    "cubews-record.cube.svc.cluster.local:8080"
                ],
                "user-agent": [
                    "Jersey/2.27 (HttpUrlConnection 11.0.3)"
                ]
            }
        },
        "respCompDiff": [],
        "service": "reviews",
        "replayTraceId": "cf96c788bcb4994320d26a2d2f506247",
        "replayReqId": "reviews-mock-1653960e-762b-4f16-aef3-fde424c691ba",
        "numMatch": 1,
        "reqMatchResType": "ExactMatch"
    },
    {
        "recordReqId": "gu--854546068",
        "recordResponse": {
            "body": "{\"return_updates\":1,\"payment_updates\":1,\"rental_id\":22244}",
            "hdrs": {
                "date": [
                    "Tue, 18 Feb 2020 09:01:35 GMT"
                ],
                "content-length": [
                    "58"
                ],
                "x-envoy-upstream-service-time": [
                    "465"
                ],
                "content-type": [
                    "application/json"
                ],
                ":status": [
                    "200"
                ]
            },
            "status": 200
        },
        "reqCompDiff": [],
        "respCompResType": "FuzzyMatch",
        "reqCompResType": "DontCare",
        "replayResponse": {
            "body": "{\"return_updates\":1,\"payment_updates\":1,\"rental_id\":22244}",
            "hdrs": {
                "date": [
                    "Wed, 19 Feb 2020 21:36:37 GMT"
                ],
                "content-length": [
                    "58"
                ],
                "x-envoy-upstream-service-time": [
                    "755"
                ],
                "content-type": [
                    "application/json"
                ],
                ":status": [
                    "200"
                ]
            },
            "status": 200
        },
        "recordTraceId": "d9384131fd3e361adfad136c4d52db23",
        "recordRequest": {
            "method": "POST",
            "queryParams": {},
            "formParams": {},
            "body": "{\"inventoryId\":840,\"rent\":5.98,\"userId\":446,\"staffId\":1}",
            "hdrs": {
                "x-request-id": [
                    "c04b9c24-50d0-4b70-bdcd-36ffa181a6c7"
                ],
                "content-length": [
                    "56"
                ],
                "x-forwarded-proto": [
                    "http"
                ],
                "x-b3-sampled": [
                    "0"
                ],
                "x-forwarded-port": [
                    "443"
                ],
                "x-forwarded-for": [
                    "14.143.179.162,172.20.51.97"
                ],
                "x-istio-attributes": [
                    "Ck0KF2Rlc3RpbmF0aW9uLnNlcnZpY2UudWlkEjISMGlzdGlvOi8vbW92aWVib29rLXJlY29yZC1wcm9kL3NlcnZpY2VzL21vdmllaW5mbwpPChhkZXN0aW5hdGlvbi5zZXJ2aWNlLmhvc3QSMxIxbW92aWVpbmZvLm1vdmllYm9vay1yZWNvcmQtcHJvZC5zdmMuY2x1c3Rlci5sb2NhbAo4Ch1kZXN0aW5hdGlvbi5zZXJ2aWNlLm5hbWVzcGFjZRIXEhVtb3ZpZWJvb2stcmVjb3JkLXByb2QKJwoYZGVzdGluYXRpb24uc2VydmljZS5uYW1lEgsSCW1vdmllaW5mbwpPCgpzb3VyY2UudWlkEkESP2t1YmVybmV0ZXM6Ly9pc3Rpby1pbmdyZXNzZ2F0ZXdheS01ZDQ5Nzk1NTg5LW50azV2LmlzdGlvLXN5c3RlbQ=="
                ],
                "accept": [
                    "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2"
                ],
                "authorization": [
                    ""
                ],
                "x-envoy-external-address": [
                    "172.20.51.97"
                ],
                ":method": [
                    "POST"
                ],
                "x-b3-traceid": [
                    "d9384131fd3e361adfad136c4d52db23"
                ],
                "x-b3-spanid": [
                    "dfad136c4d52db23"
                ],
                ":path": [
                    "/minfo/returnmovie"
                ],
                "content-type": [
                    "application/json"
                ],
                ":authority": [
                    "moviebook.meshdynamics.io"
                ],
                "user-agent": [
                    "Jersey/2.27 (HttpUrlConnection 11.0.4)"
                ]
            }
        },
        "path": "minfo/returnmovie",
        "replayRequest": {
            "method": "POST",
            "queryParams": {},
            "formParams": {},
            "body": "{\"inventoryId\":840,\"rent\":5.98,\"userId\":446,\"staffId\":1}",
            "hdrs": {
                "x-request-id": [
                    "198775cb-6ec1-4e0e-a3cc-51078baf0054"
                ],
                "content-length": [
                    "56"
                ],
                "x-b3-parentspanid": [
                    "3b090af9e0d332f8"
                ],
                "x-forwarded-proto": [
                    "http"
                ],
                "x-b3-sampled": [
                    "0"
                ],
                "x-forwarded-port": [
                    "443"
                ],
                "x-forwarded-for": [
                    "14.143.179.162,172.20.51.97,172.20.40.41"
                ],
                "x-istio-attributes": [
                    "CicKGGRlc3RpbmF0aW9uLnNlcnZpY2UubmFtZRILEgltb3ZpZWluZm8KMQodZGVzdGluYXRpb24uc2VydmljZS5uYW1lc3BhY2USEBIObW92aWVib29rLXByb2QKTwoKc291cmNlLnVpZBJBEj9rdWJlcm5ldGVzOi8vaXN0aW8taW5ncmVzc2dhdGV3YXktNWQ0OTc5NTU4OS1udGs1di5pc3Rpby1zeXN0ZW0KRgoXZGVzdGluYXRpb24uc2VydmljZS51aWQSKxIpaXN0aW86Ly9tb3ZpZWJvb2stcHJvZC9zZXJ2aWNlcy9tb3ZpZWluZm8KSAoYZGVzdGluYXRpb24uc2VydmljZS5ob3N0EiwSKm1vdmllaW5mby5tb3ZpZWJvb2stcHJvZC5zdmMuY2x1c3Rlci5sb2NhbA=="
                ],
                "accept": [
                    "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2"
                ],
                "authorization": [
                    ""
                ],
                "x-envoy-external-address": [
                    "172.20.40.41"
                ],
                ":method": [
                    "POST"
                ],
                "x-b3-traceid": [
                    "d9384131fd3e361adfad136c4d52db23"
                ],
                "x-b3-spanid": [
                    "fbc96ab5fefef1e2"
                ],
                "c-request-id": [
                    "9a437573-caee-4660-bf81-b816a8d8d2d4"
                ],
                ":path": [
                    "/minfo/returnmovie"
                ],
                "content-type": [
                    "application/json"
                ],
                "c-src-request-id": [
                    "gu--854546068"
                ],
                ":authority": [
                    "moviebook-test.prod.meshdynamics.io"
                ],
                "user-agent": [
                    "Jersey/2.27 (HttpUrlConnection 11.0.4)"
                ]
            }
        },
        "respCompDiff": [
            {
                "fromValue": "Tue, 18 Feb 2020 09:01:35 GMT",
                "op": "replace",
                "path": "/hdrs/date/0",
                "value": "Wed, 19 Feb 2020 21:36:37 GMT",
                "resolution": "OK_Ignore"
            },
            {
                "fromValue": "465",
                "op": "replace",
                "path": "/hdrs/x-envoy-upstream-service-time/0",
                "value": "755",
                "resolution": "OK_Ignore"
            }
        ],
        "service": "movieinfo",
        "replayTraceId": "d9384131fd3e361adfad136c4d52db23",
        "replayReqId": "movieinfo-d9384131fd3e361adfad136c4d52db23-22b1e671-b79b-43a1-938e-39ddd522d997",
        "numMatch": 1,
        "reqMatchResType": "ExactMatch"
    },
    {
        "recordReqId": "gu-1329698905",
        "recordResponse": {
            "body": "[{\"actors_lastnames\":[\"CRONYN\",\"TANDY\",\"CRUZ\",\"PECK\",\"AKROYD\",\"WAHLBERG\",\"HARRIS\",\"JOHANSSON\"],\"display_actors\":[\"KIRSTEN AKROYD\",\"ALBERT JOHANSSON\",\"MARY TANDY\",\"DARYL WAHLBERG\"],\"film_id\":207,\"title\":\"DANGEROUS UPTOWN\",\"film_counts\":[27,31,28,21,34,31,28,33],\"timestamp\":14850417013799012,\"book_info\":{\"reviews\":{\"reviews\":[{\"rating\":{\"color\":\"red\",\"stars\":5},\"reviewer\":\"Reviewer1\",\"text\":\"An extremely entertaining play by Shakespeare. The slapstick humour is refreshing!\"},{\"rating\":{\"color\":\"red\",\"stars\":4},\"reviewer\":\"Reviewer2\",\"text\":\"Absolutely fun and entertaining. The play lacks thematic depth when compared to other plays by Shakespeare.\"}],\"id\":\"207\"},\"ratings\":{\"ratings\":{\"Reviewer2\":4,\"Reviewer1\":5},\"id\":207},\"details\":{\"pages\":200,\"year\":1595,\"author\":\"William Shakespeare\",\"ISBN-13\":\"123-1234567890\",\"publisher\":\"PublisherA\",\"ISBN-10\":\"1234567890\",\"language\":\"English\",\"id\":207,\"type\":\"paperback\"}}}]",
            "hdrs": {
                "date": [
                    "Tue, 18 Feb 2020 09:01:35 GMT"
                ],
                "content-length": [
                    "922"
                ],
                "x-envoy-upstream-service-time": [
                    "474"
                ],
                "content-type": [
                    "application/json"
                ],
                ":status": [
                    "200"
                ]
            },
            "status": 200
        },
        "reqCompDiff": [],
        "respCompResType": "NoMatch",
        "reqCompResType": "DontCare",
        "replayResponse": {
            "body": "[{\"display_actors\":[\"AKROYDKIRSTEN\",\"JOHANSSON,ALBERT\",\"WAHLBERG,DARYL\"],\"film_id\":207,\"title\":\"DANGEROUS UPTOWN\",\"actors_firstnames\":[\"ANNE\",\"MARY\",\"RALPH\",\"SPENCER\",\"KIRSTEN\",\"DARYL\",\"CATE\",\"ALBERT\"],\"film_counts\":[\"27\",\"31\",\"28\",\"21\",\"34\",\"31\",\"28\",\"33\"],\"timestamp\":14983270679187135,\"book_info\":{\"reviews\":{\"reviews\":[{\"rating\":{\"color\":\"red\",\"stars\":5},\"reviewer\":\"Reviewer1\",\"text\":\"An extremely entertaining play by Shakespeare. The slapstick humour is refreshing!\"},{\"rating\":{\"color\":\"red\",\"stars\":4},\"reviewer\":\"Reviewer2\",\"text\":\"Absolutely fun and entertaining. The play lacks thematic depth when compared to other plays by Shakespeare.\"}],\"id\":\"207\"},\"ratings\":{\"ratings\":{\"Reviewer2\":4,\"Reviewer1\":5},\"id\":207},\"details\":{\"pages\":200,\"year\":1595,\"author\":\"William Shakespeare\",\"ISBN-13\":\"123-1234567890\",\"publisher\":\"PublisherA\",\"ISBN-10\":\"1234567890\",\"language\":\"English\",\"id\":207,\"type\":\"paperback\"}}}]",
            "hdrs": {
                "date": [
                    "Wed, 19 Feb 2020 21:36:39 GMT"
                ],
                "content-length": [
                    "919"
                ],
                "x-envoy-upstream-service-time": [
                    "1034"
                ],
                "content-type": [
                    "application/json"
                ],
                ":status": [
                    "200"
                ]
            },
            "status": 200
        },
        "recordTraceId": "cf96c788bcb4994320d26a2d2f506247",
        "recordRequest": {
            "method": "GET",
            "queryParams": {
                "filmName": [
                    "DANGEROUS UPTOWN"
                ]
            },
            "formParams": {},
            "body": "",
            "hdrs": {
                "x-request-id": [
                    "c267d3c8-e970-46cc-b5ee-e2b16ccbba33"
                ],
                "content-length": [
                    "0"
                ],
                "x-forwarded-proto": [
                    "http"
                ],
                "x-b3-sampled": [
                    "0"
                ],
                "x-forwarded-port": [
                    "443"
                ],
                "x-forwarded-for": [
                    "14.143.179.162,172.20.40.41"
                ],
                "x-istio-attributes": [
                    "Ck0KF2Rlc3RpbmF0aW9uLnNlcnZpY2UudWlkEjISMGlzdGlvOi8vbW92aWVib29rLXJlY29yZC1wcm9kL3NlcnZpY2VzL21vdmllaW5mbwpPChhkZXN0aW5hdGlvbi5zZXJ2aWNlLmhvc3QSMxIxbW92aWVpbmZvLm1vdmllYm9vay1yZWNvcmQtcHJvZC5zdmMuY2x1c3Rlci5sb2NhbAo4Ch1kZXN0aW5hdGlvbi5zZXJ2aWNlLm5hbWVzcGFjZRIXEhVtb3ZpZWJvb2stcmVjb3JkLXByb2QKJwoYZGVzdGluYXRpb24uc2VydmljZS5uYW1lEgsSCW1vdmllaW5mbwpPCgpzb3VyY2UudWlkEkESP2t1YmVybmV0ZXM6Ly9pc3Rpby1pbmdyZXNzZ2F0ZXdheS01ZDQ5Nzk1NTg5LW50azV2LmlzdGlvLXN5c3RlbQ=="
                ],
                "accept": [
                    "application/json"
                ],
                "authorization": [
                    ""
                ],
                "x-envoy-external-address": [
                    "172.20.40.41"
                ],
                ":method": [
                    "GET"
                ],
                "x-b3-traceid": [
                    "cf96c788bcb4994320d26a2d2f506247"
                ],
                "x-b3-spanid": [
                    "20d26a2d2f506247"
                ],
                ":path": [
                    "/minfo/listmovies?filmName=DANGEROUS+UPTOWN"
                ],
                ":authority": [
                    "moviebook.meshdynamics.io"
                ],
                "user-agent": [
                    "Jersey/2.27 (HttpUrlConnection 11.0.4)"
                ]
            }
        },
        "path": "minfo/listmovies",
        "replayRequest": {
            "method": "GET",
            "queryParams": {
                "filmName": [
                    "DANGEROUS UPTOWN"
                ]
            },
            "formParams": {},
            "body": "",
            "hdrs": {
                "x-request-id": [
                    "c5811590-e1ee-4204-81c2-f35df94c7f80"
                ],
                "content-length": [
                    "0"
                ],
                "x-b3-parentspanid": [
                    "8d6afd245e622b2b"
                ],
                "x-forwarded-proto": [
                    "http"
                ],
                "x-b3-sampled": [
                    "0"
                ],
                "x-forwarded-port": [
                    "443"
                ],
                "x-forwarded-for": [
                    "14.143.179.162,172.20.40.41,172.20.40.41"
                ],
                "x-istio-attributes": [
                    "CicKGGRlc3RpbmF0aW9uLnNlcnZpY2UubmFtZRILEgltb3ZpZWluZm8KMQodZGVzdGluYXRpb24uc2VydmljZS5uYW1lc3BhY2USEBIObW92aWVib29rLXByb2QKTwoKc291cmNlLnVpZBJBEj9rdWJlcm5ldGVzOi8vaXN0aW8taW5ncmVzc2dhdGV3YXktNWQ0OTc5NTU4OS1udGs1di5pc3Rpby1zeXN0ZW0KRgoXZGVzdGluYXRpb24uc2VydmljZS51aWQSKxIpaXN0aW86Ly9tb3ZpZWJvb2stcHJvZC9zZXJ2aWNlcy9tb3ZpZWluZm8KSAoYZGVzdGluYXRpb24uc2VydmljZS5ob3N0EiwSKm1vdmllaW5mby5tb3ZpZWJvb2stcHJvZC5zdmMuY2x1c3Rlci5sb2NhbA=="
                ],
                "accept": [
                    "application/json"
                ],
                "authorization": [
                    ""
                ],
                "x-envoy-external-address": [
                    "172.20.40.41"
                ],
                ":method": [
                    "GET"
                ],
                "x-b3-traceid": [
                    "cf96c788bcb4994320d26a2d2f506247"
                ],
                "x-b3-spanid": [
                    "07addb713231c764"
                ],
                "c-request-id": [
                    "ef723f28-c4f4-4101-94eb-cf6e66041317"
                ],
                ":path": [
                    "/minfo/listmovies?filmName=DANGEROUS%20UPTOWN"
                ],
                "c-src-request-id": [
                    "gu-1329698905"
                ],
                ":authority": [
                    "moviebook-test.prod.meshdynamics.io"
                ],
                "user-agent": [
                    "Jersey/2.27 (HttpUrlConnection 11.0.4)"
                ]
            }
        },
        "respCompDiff": [
            {
                "fromValue": "Tue, 18 Feb 2020 09:01:35 GMT",
                "op": "replace",
                "path": "/hdrs/date/0",
                "value": "Wed, 19 Feb 2020 21:36:39 GMT",
                "resolution": "OK_Ignore"
            },
            {
                "fromValue": "922",
                "op": "replace",
                "path": "/hdrs/content-length/0",
                "value": "919",
                "resolution": "OK_Ignore"
            },
            {
                "fromValue": "474",
                "op": "replace",
                "path": "/hdrs/x-envoy-upstream-service-time/0",
                "value": "1034",
                "resolution": "OK_Ignore"
            },
            {
                "op": "remove",
                "path": "/body/0/actors_lastnames",
                "value": [
                    "CRONYN",
                    "TANDY",
                    "CRUZ",
                    "PECK",
                    "AKROYD",
                    "WAHLBERG",
                    "HARRIS",
                    "JOHANSSON"
                ],
                "resolution": "OK_Optional"
            },
            {
                "fromValue": "KIRSTEN AKROYD",
                "op": "replace",
                "path": "/body/0/display_actors/0",
                "value": "AKROYDKIRSTEN",
                "resolution": "OK_Ignore"
            },
            {
                "fromValue": "ALBERT JOHANSSON",
                "op": "replace",
                "path": "/body/0/display_actors/1",
                "value": "JOHANSSON,ALBERT",
                "resolution": "OK_Ignore"
            },
            {
                "fromValue": "MARY TANDY",
                "op": "replace",
                "path": "/body/0/display_actors/2",
                "value": "WAHLBERG,DARYL",
                "resolution": "OK_Ignore"
            },
            {
                "op": "remove",
                "path": "/body/0/display_actors/3",
                "value": "DARYL WAHLBERG",
                "resolution": "ERR_Required"
            },
            {
                "fromValue": 27,
                "op": "replace",
                "path": "/body/0/film_counts/0",
                "value": "27",
                "resolution": "ERR_ValTypeMismatch"
            },
            {
                "fromValue": 31,
                "op": "replace",
                "path": "/body/0/film_counts/1",
                "value": "31",
                "resolution": "ERR_ValTypeMismatch"
            },
            {
                "fromValue": 28,
                "op": "replace",
                "path": "/body/0/film_counts/2",
                "value": "28",
                "resolution": "ERR_ValTypeMismatch"
            },
            {
                "fromValue": 21,
                "op": "replace",
                "path": "/body/0/film_counts/3",
                "value": "21",
                "resolution": "ERR_ValTypeMismatch"
            },
            {
                "fromValue": 34,
                "op": "replace",
                "path": "/body/0/film_counts/4",
                "value": "34",
                "resolution": "ERR_ValTypeMismatch"
            },
            {
                "fromValue": 31,
                "op": "replace",
                "path": "/body/0/film_counts/5",
                "value": "31",
                "resolution": "ERR_ValTypeMismatch"
            },
            {
                "fromValue": 28,
                "op": "replace",
                "path": "/body/0/film_counts/6",
                "value": "28",
                "resolution": "ERR_ValTypeMismatch"
            },
            {
                "fromValue": 33,
                "op": "replace",
                "path": "/body/0/film_counts/7",
                "value": "33",
                "resolution": "ERR_ValTypeMismatch"
            },
            {
                "fromValue": 14850417013799012,
                "op": "replace",
                "path": "/body/0/timestamp",
                "value": 14983270679187135,
                "resolution": "ERR_ValMismatch"
            },
            {
                "op": "add",
                "path": "/body/0/actors_firstnames",
                "value": [
                    "ANNE",
                    "MARY",
                    "RALPH",
                    "SPENCER",
                    "KIRSTEN",
                    "DARYL",
                    "CATE",
                    "ALBERT"
                ],
                "resolution": "ERR_RequiredGolden"
            }
        ],
        "service": "movieinfo",
        "replayTraceId": "cf96c788bcb4994320d26a2d2f506247",
        "replayReqId": "movieinfo-cf96c788bcb4994320d26a2d2f506247-eab835c7-7a4c-4731-a9a3-892d20773959",
        "numMatch": 1,
        "reqMatchResType": "ExactMatch"
    },
    {
        "recordReqId": "gu-1669709559",
        "recordResponse": {
            "body": "{\"id\":207,\"author\":\"William Shakespeare\",\"year\":1595,\"type\":\"paperback\",\"pages\":200,\"publisher\":\"PublisherA\",\"language\":\"English\",\"ISBN-10\":\"1234567890\",\"ISBN-13\":\"123-1234567890\"}",
            "hdrs": {
                "date": [
                    "Tue, 18 Feb 2020 09:01:36 GMT"
                ],
                "server": [
                    "WEBrick/1.3.1 (Ruby/2.3.7/2018-03-28)"
                ],
                "content-length": [
                    "180"
                ],
                "x-envoy-upstream-service-time": [
                    "1"
                ],
                "content-type": [
                    "application/json"
                ],
                "connection": [
                    "Keep-Alive"
                ],
                ":status": [
                    "200"
                ]
            },
            "status": 200
        },
        "reqCompDiff": [],
        "respCompResType": "ExactMatch",
        "reqCompResType": "DontCare",
        "replayResponse": {
            "body": "{\"id\":207,\"author\":\"William Shakespeare\",\"year\":1595,\"type\":\"paperback\",\"pages\":200,\"publisher\":\"PublisherA\",\"language\":\"English\",\"ISBN-10\":\"1234567890\",\"ISBN-13\":\"123-1234567890\"}",
            "hdrs": {
                "date": [
                    "Tue, 18 Feb 2020 09:01:36 GMT"
                ],
                "server": [
                    "WEBrick/1.3.1 (Ruby/2.3.7/2018-03-28)"
                ],
                "content-length": [
                    "180"
                ],
                "x-envoy-upstream-service-time": [
                    "1"
                ],
                "content-type": [
                    "application/json"
                ],
                "connection": [
                    "Keep-Alive"
                ],
                ":status": [
                    "200"
                ]
            },
            "status": 200
        },
        "recordTraceId": "cf96c788bcb4994320d26a2d2f506247",
        "recordRequest": {
            "method": "GET",
            "queryParams": {},
            "formParams": {},
            "body": "",
            "hdrs": {
                "x-request-id": [
                    "0e2011d8-ab80-4899-aba4-b06cda706399"
                ],
                "x-b3-parentspanid": [
                    "33110a76fa4ebc40"
                ],
                "content-length": [
                    "0"
                ],
                "x-forwarded-proto": [
                    "http"
                ],
                "x-b3-sampled": [
                    "0"
                ],
                "x-istio-attributes": [
                    "CksKF2Rlc3RpbmF0aW9uLnNlcnZpY2UudWlkEjASLmlzdGlvOi8vbW92aWVib29rLXJlY29yZC1wcm9kL3NlcnZpY2VzL2RldGFpbHMKTQoYZGVzdGluYXRpb24uc2VydmljZS5ob3N0EjESL2RldGFpbHMubW92aWVib29rLXJlY29yZC1wcm9kLnN2Yy5jbHVzdGVyLmxvY2FsCiUKGGRlc3RpbmF0aW9uLnNlcnZpY2UubmFtZRIJEgdkZXRhaWxzCjgKHWRlc3RpbmF0aW9uLnNlcnZpY2UubmFtZXNwYWNlEhcSFW1vdmllYm9vay1yZWNvcmQtcHJvZApQCgpzb3VyY2UudWlkEkISQGt1YmVybmV0ZXM6Ly9tb3ZpZWluZm8tdjEtNjY1YzU5Y2Q4Ny12aHZudy5tb3ZpZWJvb2stcmVjb3JkLXByb2Q="
                ],
                "accept": [
                    "application/json"
                ],
                ":method": [
                    "GET"
                ],
                "x-b3-traceid": [
                    "cf96c788bcb4994320d26a2d2f506247"
                ],
                "x-b3-spanid": [
                    "a2f6ecbbbc7b535b"
                ],
                ":path": [
                    "/details/207"
                ],
                ":authority": [
                    "details:9080"
                ],
                "user-agent": [
                    "Jersey/2.27 (HttpUrlConnection 11.0.3)"
                ]
            }
        },
        "path": "details/207",
        "replayRequest": {
            "method": "GET",
            "queryParams": {},
            "formParams": {},
            "body": "",
            "hdrs": {
                "x-request-id": [
                    "e6166264-26b8-9988-a5ae-d8e38351f4be"
                ],
                "content-length": [
                    "0"
                ],
                "x-b3-parentspanid": [
                    "7b10580ae282502b"
                ],
                "x-forwarded-proto": [
                    "http"
                ],
                "x-b3-sampled": [
                    "0"
                ],
                "x-forwarded-port": [
                    "443"
                ],
                "x-forwarded-for": [
                    "18.218.226.157,172.20.94.107"
                ],
                "accept": [
                    "application/json"
                ],
                "x-envoy-original-path": [
                    "/details/207"
                ],
                "authorization": [
                    "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJNZXNoREFnZW50VXNlciIsInJvbGVzIjpbIlJPTEVfVVNFUiJdLCJ0eXBlIjoicGF0IiwiaWF0IjoxNTc5ODY0MDE3LCJleHAiOjE4OTUyMjQwMTd9.JjTcDlf8EB_iueDWSollLrr1kn7a9e3Yr0kQ2BtdLAk"
                ],
                "x-envoy-external-address": [
                    "172.20.94.107"
                ],
                "x-b3-traceid": [
                    "cf96c788bcb4994320d26a2d2f506247"
                ],
                "x-b3-spanid": [
                    "90ccbb230ba9d9f1"
                ],
                "host": [
                    "cubews-record.cube.svc.cluster.local:8080"
                ],
                "user-agent": [
                    "Jersey/2.27 (HttpUrlConnection 11.0.3)"
                ]
            }
        },
        "respCompDiff": [],
        "service": "details",
        "replayTraceId": "cf96c788bcb4994320d26a2d2f506247",
        "replayReqId": "details-mock-d8d229af-e553-4ce6-8795-c0b2561f20ab",
        "numMatch": 1,
        "reqMatchResType": "ExactMatch"
    }
    ]
}

export default class DiffResults extends Component {
    constructor(props) {
        super(props);
        this.state = {
            filter : {
                selectedService: "s1",
                selectedAPI: "a1",
                selectedReqRespMatchType: "responseMismatch",
                selectedResolutionType: "All",
                currentPageNumber: 1,
            },
            diffLayoutData : [],
            facetListData: {},
        }
    }

    componentDidMount = () => {
        console.log("aaa")
        this.fetchResults();
    }

    handleFilterChange = (metaData, value) => {
        console.log("filter changed " + metaData + " : " + value)
        this.setState({
            filter : {
                ...this.state.filter,
                [metaData] : value,
            }
        })
        this.fetchResults();
    }

    cleanEscapedString = (str) => {
        // preserve newlines, etc - use valid JSON
        str = str.replace(/\\n/g, "\\n")
            .replace(/\\'/g, "\\'")
            .replace(/\\"/g, '\\"')
            .replace(/\\&/g, "\\&")
            .replace(/\\r/g, "\\r")
            .replace(/\\t/g, "\\t")
            .replace(/\\b/g, "\\b")
            .replace(/\\f/g, "\\f");
        // remove non-printable and other non-valid JSON chars
        str = str.replace(/[\u0000-\u0019]+/g, "");
        return str;
    }

    validateAndCleanHTTPMessageParts = (messagePart) => {
        let cleanedMessagepart = "";
        if (messagePart &&_.isObject(messagePart)) {
            cleanedMessagepart = messagePart;
        } else if (messagePart) {
            try {
                cleanedMessagepart = JSON.parse(messagePart);
            } catch (e) {
                cleanedMessagepart = JSON.parse('"' + this.cleanEscapedString(_.escape(messagePart)) + '"')
            }
        } else {
            cleanedMessagepart = JSON.parse('""');
        }

        return cleanedMessagepart;
    }

    getDiffForMessagePart = (replayedPart, recordedPart, serverSideDiff, prefix, service, path) => {
        if (!serverSideDiff || serverSideDiff.length === 0) return null; 
        let actpart = JSON.stringify(replayedPart, undefined, 4);
        let expPart = JSON.stringify(recordedPart, undefined, 4);
        let reducedDiffArrayMsgPart = new ReduceDiff(prefix, actpart, expPart, serverSideDiff);
        let reductedDiffArrayMsgPart = reducedDiffArrayMsgPart.computeDiffArray()
        let updatedReductedDiffArrayMsgPart = reductedDiffArrayMsgPart && reductedDiffArrayMsgPart.map((eachItem) => {
            return {
                ...eachItem,
                service,
                app: this.state.app,
                templateVersion: this.state.templateVersion,
                apiPath: path,
                replayId: this.state.replayId,
                recordingId: this.state.recordingId
            }
        });
        return updatedReductedDiffArrayMsgPart;
    }

    validateAndCreateDiffLayoutData = (replayList) => {
        let diffLayoutData = replayList.map((item, index) => {
            let recordedData, replayedData, recordedResponseHeaders, replayedResponseHeaders, prefix = "/body",
                recordedRequestHeaders, replayedRequestHeaders, recordedRequestQParams, replayedRequestQParams, recordedRequestFParams, replayedRequestFParams,recordedRequestBody, replayedRequestBody, reductedDiffArrayReqHeaders, reductedDiffArrayReqBody, reductedDiffArrayReqQParams, reductedDiffArrayReqFParams;
            let isJson = true;
            // processing Response    
            // recorded response body and headers
            if (item.recordResponse) {
                recordedResponseHeaders = item.recordResponse.hdrs ? item.recordResponse.hdrs : [];
                // check if the content type is JSON and attempt to parse it
                let recordedResponseMime = recordedResponseHeaders["content-type"][0];
                isJson = recordedResponseMime.toLowerCase().indexOf("json") > -1;
                if (item.recordResponse.body && isJson) {
                    try {
                        recordedData = JSON.parse(item.recordResponse.body);
                    } catch (e) {
                        recordedData = JSON.parse('"' + this.cleanEscapedString(_.escape(item.recordResponse.body)) + '"')
                    }
                }
                else {
                    // in case the content type isn't json, display the entire body if present, or else an empty string
                    recordedData = item.recordResponse.body ? item.recordResponse.body : '""';
                }
            } else {
                recordedResponseHeaders = "";
                recordedData = "";
            }   

            // same as above but for replayed response
            if (item.replayResponse) {
                replayedResponseHeaders = item.replayResponse.hdrs ? item.replayResponse.hdrs : [];
                // check if the content type is JSON and attempt to parse it
                let replayedResponseMime = replayedResponseHeaders["content-type"][0];
                isJson = replayedResponseMime.toLowerCase().indexOf("json") > -1;
                if (item.replayResponse.body && isJson) {
                    try {
                        replayedData = JSON.parse(item.replayResponse.body);
                    } catch (e) {
                        replayedData = JSON.parse('"' + this.cleanEscapedString(_.escape(item.replayResponse.body)) + '"')
                    }
                }
                else {
                    // in case the content type isn't json, display the entire body if present, or else an empty string
                    replayedData = item.replayResponse.body ? item.replayResponse.body : '""';
                }
            } else {
                replayedResponseHeaders = "";
                replayedData = "";
            }
            let diff;
            
            if (item.respCompDiff && item.respCompDiff.length !== 0) {
                diff = item.respCompDiff;
            } else {
                diff = [];
            }
            let actJSON = JSON.stringify(sortJson(replayedData), undefined, 4),
                expJSON = JSON.stringify(sortJson(recordedData), undefined, 4);
            let reductedDiffArray = null, missedRequiredFields = [], reducedDiffArrayRespHdr = null;

            let actRespHdrJSON = JSON.stringify(replayedResponseHeaders, undefined, 4);
            let expRespHdrJSON = JSON.stringify(recordedResponseHeaders, undefined, 4);
            

            // use the backend diff and the two JSONs to generate diff array that will be passed to the diff renderer
            if (diff && diff.length > 0) {
                // skip calculating the diff array in case of non json data 
                // pass diffArray as null so that the diff library can render it directly
                if (isJson) { 
                    let reduceDiff = new ReduceDiff(prefix, actJSON, expJSON, diff);
                    reductedDiffArray = reduceDiff.computeDiffArray();
                }
                let expJSONPaths = generator(recordedData, "", "", prefix);
                missedRequiredFields = diff.filter((eachItem) => {
                    return eachItem.op === "noop" && eachItem.resolution.indexOf("ERR_REQUIRED") > -1 && !expJSONPaths.has(eachItem.path);
                })

                let reduceDiffHdr = new ReduceDiff("/hdrs", actRespHdrJSON, expRespHdrJSON, diff);
                reducedDiffArrayRespHdr = reduceDiffHdr.computeDiffArray();

            } else if (diff && diff.length == 0) {
                if (_.isEqual(expJSON, actJSON)) {
                    let reduceDiff = new ReduceDiff("/body", actJSON, expJSON, diff);
                    reductedDiffArray = reduceDiff.computeDiffArray();
                }
            }
            let updatedReductedDiffArray = reductedDiffArray && reductedDiffArray.map((eachItem) => {
                return {
                    ...eachItem,
                    service: item.service,
                    app: this.state.app,
                    templateVersion: this.state.templateVersion,
                    apiPath: item.path,
                    replayId: this.state.replayId,
                    recordingId: this.state.recordingId
                }
            });

            let updatedReducedDiffArrayRespHdr = reducedDiffArrayRespHdr && reducedDiffArrayRespHdr.map((eachItem) => {
                return {
                    ...eachItem,
                    service: item.service,
                    app: this.state.app,
                    templateVersion: this.state.templateVersion,
                    apiPath: item.path,
                    replayId: this.state.replayId,
                    recordingId: this.state.recordingId
                }
            });

            // process Requests
            // recorded request header and body
            // parse and clean up body string
            if (item.recordRequest) {
                recordedRequestHeaders = this.validateAndCleanHTTPMessageParts(item.recordRequest.hdrs);
                recordedRequestBody = this.validateAndCleanHTTPMessageParts(item.recordRequest.body);
                recordedRequestQParams = this.validateAndCleanHTTPMessageParts(item.recordRequest.queryParams);
                recordedRequestFParams = this.validateAndCleanHTTPMessageParts(item.recordRequest.formParams);
            } else {
                recordedRequestHeaders = "";
                recordedRequestBody = "";
                recordedRequestQParams = "";
                recordedRequestFParams = "";
            }

            // replayed request header and body
            // same as above
            if (item.replayRequest) {
                replayedRequestHeaders = this.validateAndCleanHTTPMessageParts(item.replayRequest.hdrs);
                replayedRequestBody = this.validateAndCleanHTTPMessageParts(item.replayRequest.body);
                replayedRequestQParams = this.validateAndCleanHTTPMessageParts(item.replayRequest.queryParams);
                replayedRequestFParams = this.validateAndCleanHTTPMessageParts(item.replayRequest.formParams);
            } else {
                replayedRequestHeaders = "";
                replayedRequestBody = "";
                replayedRequestQParams = "";
                replayedRequestFParams = "";
            }

            reductedDiffArrayReqHeaders = this.getDiffForMessagePart(replayedRequestHeaders, recordedRequestHeaders, item.reqCompDiff, "/hdrs", item.service, item.path);
            reductedDiffArrayReqQParams = this.getDiffForMessagePart(replayedRequestQParams, recordedRequestQParams, item.reqCompDiff, "/queryParams", item.service, item.path);
            reductedDiffArrayReqFParams = this.getDiffForMessagePart(replayedRequestFParams, recordedRequestFParams, item.reqCompDiff, "/queryParams", item.service, item.path);
            reductedDiffArrayReqBody = this.getDiffForMessagePart(replayedRequestBody, recordedRequestBody, item.reqCompDiff, "/body", item.service, item.path);

            return {
                ...item,
                recordedResponseHeaders,
                replayedResponseHeaders,
                recordedData,
                replayedData,
                actJSON,
                expJSON,
                parsedDiff: diff,
                reductedDiffArray: updatedReductedDiffArray,
                missedRequiredFields,
                show: true,
                recordedRequestHeaders,
                replayedRequestHeaders,
                recordedRequestQParams,
                replayedRequestQParams,
                recordedRequestFParams,
                replayedRequestFParams,
                recordedRequestBody,
                replayedRequestBody,
                updatedReducedDiffArrayRespHdr,
                reductedDiffArrayReqHeaders,
                reductedDiffArrayReqQParams,
                reductedDiffArrayReqFParams,
                reductedDiffArrayReqBody
            }
        });
        return diffLayoutData;
    }

    fetchResults() {
        console.log("fetching replay list")
        // let dataList = {}
        // //let url = "https://app.meshdynamics.io/api/as/analysisResByPath/a48fd5a0-fc01-443b-a2db-685d2cc72b2c-753a5807-84e8-4c00-b3c9-e053bd10ff0f?start=20&includeDiff=true&path=%2A";
        // let url = "http://www.mocky.io/v2/5e5116f23100006400415919";
        // let response = await fetch(url, { 
        //     "credentials": "include", 
        //     "headers": { 
        //         "accept": "application/json, text/plain, */*", 
        //         "accept-language": "en-US,en;q=0.9", 
        //         "authorization": "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkZW1vQGN1YmVjb3JwLmlvIiwicm9sZXMiOlsiUk9MRV9VU0VSIl0sImlhdCI6MTU4MjE3MTA5MSwiZXhwIjoxNTgyNzc1ODkxfQ.N5ZUkK29_B588MWMeezK1bRb_7l26t7ti_2k2T8E0pE", 
        //         "cache-control": "no-cache", 
        //         "sec-fetch-mode": "cors", 
        //         "sec-fetch-site": "same-origin" 
        //     }, 
        //     "referrer": "https://app.meshdynamics.io/shareable_link?replayId=a48fd5a0-fc01-443b-a2db-685d2cc72b2c-753a5807-84e8-4c00-b3c9-e053bd10ff0f&app=MovieInfo&apiPath=minfo/listmovies&service=movieinfo&recordingId=Recording-2098161868&timeStamp=Feb%2020,%202020%203:06%20AM&currentTemplateVer=5da0a9d9-5388-4772-9fd1-6d4eb1e1fb10&selectedReqRespMatchType=responseMismatch&selectedResolutionType=All", 
        //     "referrerPolicy": "no-referrer-when-downgrade", 
        //     "body": null, 
        //     "method": "GET", 
        //     "mode": "cors" 
        // });
        
        // if (response.ok) {
        //     let json = await response.json();
        //     dataList = json;
        //     let diffLayoutData = this.validateAndCreateDiffLayoutData(dataList.data.res);
        //     this.setState({diffLayoutData: diffLayoutData});
        // } else {
        //     // todo
        // }

        console.log(respData.facets)
        let diffLayoutData = this.validateAndCreateDiffLayoutData(respData.results);
        this.setState({diffLayoutData: diffLayoutData, facetListData: respData.facets});
    }

    
    render() {
        return (
            <div className="content-wrapper">
                <div className="back" style={{ marginBottom: "10px", padding: "5px", background: "#454545" }}>
                    <Link to={"/"} onClick={this.handleBackToDashboardClick}><span className="link-alt"><Glyphicon className="font-15" glyph="chevron-left" /> BACK TO DASHBOARD</span></Link>
                    <span className="link-alt pull-right" onClick={this.showSaveGoldenModal}>&nbsp;&nbsp;&nbsp;&nbsp;<i className="fas fa-save font-15"></i>&nbsp;Save Golden</span>
                    <Link to="/review_golden_updates" className="hidden">
                        <span className="link pull-right"><i className="fas fa-pen-square font-15"></i>&nbsp;REVIEW GOLDEN UPDATES</span>
                    </Link>
                </div>
                <div>
                    <DiffResultsFilter filter={this.state.filter} filterChangeHandler={this.handleFilterChange} facetListData={this.state.facetListData} app={"app"}></DiffResultsFilter>
                    <DiffResultsList diffLayoutData={this.state.diffLayoutData}></DiffResultsList>
                </div>
            </div>
        )
    } 
}
