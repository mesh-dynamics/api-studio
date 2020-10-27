# Evnoy filters to send data to Meshdynamics services

## Generate Envoy filters

1. Edit `templates/services` file and add the name of services for which you want to generate envoy filters.

2. Install dependencies:  
```
pip3 install -r requirements.txt
```

3. Run `generatefilter.py` to generate envoy filter for services.  
```
./generatefilter.py
```  
This will generate separate file for each envoy filter.


## Apply filters to Envoy proxy  

Add the generate filter to respective service's envoy configuration under `http_filters` section.

Our filters will make http call to MeshDynamics servers, Kindly add the following line under `clusters` section of each envoy configuration to ensure connectivity.  

```
- name: meshdynamics_cluster
  connect_timeout: 0.25s
  type: STRICT_DNS
  lb_policy: ROUND_ROBIN
  load_assignment:
    cluster_name: meshdynamics_cluster
    endpoints:
    - lb_endpoints:
      - endpoint:
          address:
            socket_address:
              address: nutanix.meshdynamics.io
              port_value: 443
  transport_socket:
    name: envoy.transport_sockets.tls
    typed_config:
      "@type": type.googleapis.com/envoy.extensions.transport_sockets.tls.v3.UpstreamTlsContext
      sni: nutanix.meshdynamics.io
      common_tls_context:
        validation_context:
          match_subject_alt_names:
          - exact: "*.meshdynamics.io"
          trusted_ca:
            filename: /etc/ssl/certs/ca-certificates.crt
```
