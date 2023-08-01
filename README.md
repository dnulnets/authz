# Istio extension provider for authorization with keycloak
This project creates a standalone external authorization provider (envoyExtAuthzHttp). It uses Keycloaks authorization services for fine grained authorization through istios CUSTOM action for authorization policies. It is deployed as a standalone service in kubernetes.

**NOTE!** This is work in progress and have some thing that needs to be done and quirks to solve before it is production ready. But it is fully functional for experimental use for now.

## Introduction
The functional idea is that it is used by istios authorization policy as a CUSTOM action and acts as a private keycloak client. For each HTTP request it uses the uri to look upp the resource in keycloak. Then it queries keycloak for authorization using the incoming token (**Authorization: Bearer xxxx** header), the looked up resource and the HTTP method as scope. Based on the response from keycloak it responds to istio with either OK and the RPT in the authorization and the x-authz-rpt header, or with a FORBIDDEN if keycloak denies it.

It runs as a stateless pod (Deployment) and can be run in multiple instances to achieve performance and robustness. But of course adds response time to the protected resource.

### Versions
The following versions are used for development and testing, it might work perfectly fine with other versions as well but it has not been tested.
* Keycloak 21.1.1
* Istio 1.17.2
* Quarkus 3.2.0

### Things to do
* Caching of resource/URI mapping instead of asking Keycloak for every request.
* Error handling when a request fails for reasons such as Keycloak not respondning/down, not authorized JWT etc.
* Health endpoints for the server to be used by kubernetes.
* This README with examples for setup etc.
* Performance tuning and deployment scenarios.
* gRPC support.

## Setup in kubernetes

### External extension provider
Istio has to be configured with the extension provider to be able to use it as a CUSTOM action.

```
kubectl edit configmap istio -n istio-system
```
Add the extensionprovider as shown below and include the authorization header in the check and rewrite it on OK from the provider (it contains the RPT) .If you do not want it to rewrite the authorisation header for the upstream remove it from the array below. The RPT is also sent by the provider in the x-authz-rpt header that you can forward. Save the config. 
```
data:
  mesh: |-
    defaultConfig:
      discoveryAddress: istiod.istio-system.svc:15012
      proxyMetadata: {}
      tracing:
        zipkin:
          address: zipkin.istio-system:9411
    enablePrometheusMerge: true
    rootNamespace: istio-system
    trustDomain: cluster.local
    extensionProviders:
    - name: "simple-ext-authz-http"
      envoyExtAuthzHttp:
        service: "authz.simple.svc.cluster.local"
        port: "8080"
        includeHeadersInCheck: ["authorization"]
        headersToUpstreamOnAllow: ["authorization", "x-authz-rpt"]
  meshNetworks: 'networks: {}'
```

### Example AuthorizationPolicy
This example shows how to use an authorization policy using a CUSTOM action and specifies which provider to use for authorization. The provider has to be set up in advance. See previous chapter.
```
apiVersion: security.istio.io/v1
kind: AuthorizationPolicy
metadata:
  name: ext-authz
spec:
  selector:
    matchLabels:
      app: simple
  action: CUSTOM
  provider:
    name: simple-ext-authz-http
  rules:
  - to:
    - operation:
        paths: ["*"]
```

## Setup for the provider

## Setup in keycloak

## How to build it

### Building docker image
quarkus build -Dquarkus.container-image.build=true
