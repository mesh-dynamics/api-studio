Setting up Fluentd in the cluster
---------------------------------

Fluentd will run as a daemonset. Use `fluentd-daemonset.yaml` to set it up.
Also need: `ServiceAccount`, `ClusterRole`, `ClusterRoleBinding`

Config
------
Use the ConfigMap: `fluentd-conf.cm.yaml`
The config map will be mounted in each pod of the DaemonSet.

Things to configure/keep in mind:
- The `path` parameter in the `source` section should pattern match the namespace and containers you want to tail logs of. Example: `/var/log/containers/*staging-sm_istio-proxy*.log` matches log files of `istio-proxy` containers in the `staging-sm` namespace.

- To capture from multiple sources, you can add more `source` plugins, `filter` plugins as needed and tag them appropriately to define a flow path.
- The `endpoint` parameter in the `match` section (`http` output plugin) should point to a Cube instance that's appropriate.
- The `namespace` of the fluentd daemonset and configmap.