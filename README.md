# Istio extension provider for authorization with keycloak
This project creates a standalone external authorization provider (envoyExtAuthzHttp). It uses Keycloaks authorization services for fine grained authorization through istios CUSTOM action for authorization policies. It is deployed as a standalone service in kubernetes.

**NOTE!** This is work in progress and have some thing that needs to be done and quirks to solve before it is production ready. But it is fully functional for experimental use for now.

## Introduction
The functional idea is that it is used by istios authorization policy as a CUSTOM action and acts as a private keycloak client. For each HTTP request it uses the uri to look upp the resource in keycloak. Then it queries keycloak for authorization using the incoming token (**Authorization: Bearer xxxx** header), the looked up resource and the HTTP method as scope. Based on the response from keycloak it responds to istio with either OK and the RPT in the authorization and the x-authz-rpt headers, or with a FORBIDDEN if keycloak denies it.

### Runtime and development versions
The following versions are used for runtime, development and testing. It might work perfectly fine with other versions as well but it has not been verified.
* Keycloak 21.1.1 (keycloak-authz-client, keycloak-core)
* Istio 1.17.2
* Quarkus 3.2.0

### Things to look into
* Caching of uri-to-resource mapping instead of asking Keycloak for every request.
* Better error handling when a request fails for reasons such as Keycloak not respondning/down, not authorized JWT etc.
* Performance tuning and deployment scenarios.
* gRPC support.

## Kubernetes setup

### External extension provider
Istio has to be configured with the extension provider to be able to use it as a CUSTOM action.

```
kubectl edit configmap istio -n istio-system
```
Add the extension provider as shown below and include the authorization header in the check and rewrite it on OK from the provider (it contains the RPT) .If you do not want it to rewrite the authorisation header for the upstream remove it from the array below. The RPT is also sent by the provider in the x-authz-rpt header that you can forward. Save the config. 
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
This example shows how to use an authorization policy for the app **simple** using a CUSTOM action that specifies the previous added extension provider to use for authorization. The provider has to be set up in advance. See previous chapter.
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
The provider is stateless and can be setup with a straightforward deployment. A service must also be registered to
be used for the configuration of the extension provider. You need to provide an application.properties for configuration. If you are using SSL to connecto to the keycloak endpoint you also need to add a truststore. The liveness probe is needed for kubernetes to restart a failed pod and the readiness probe to tell kubernetes that it is accepting requests.
```
apiVersion: v1
kind: Service
metadata:
  name: authz
  namespace: authz
  labels:
    app: authz
spec:
  ports:
  - name: http
    port: 8080
    targetPort: 8080
  selector:
    app: authz
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: authz
  namespace: authz
spec:  
  replicas: 1
  selector:
    matchLabels:
      app: authz
  template:
    metadata:
      labels:
        app: authz
        version: 0.0.56
    spec:
      containers:
      - image: dnulnets/authz:0.0.56
        imagePullPolicy: IfNotPresent
        name: authz
        env:
          - name: QUARKUS_LOG_LEVEL
            value: "INFO"
          - name: JAVA_OPTS_APPEND
            value: " -Djavax.net.ssl.trustStore=/cert/stenlund.jks -Djavax.net.ssl.trustStorePassword=changeme"
        ports:
        - containerPort: 8080
        livenessProbe:
          httpGet:
            path: /q/health/live
            port: 8080
          initialDelaySeconds: 10
          timeoutSeconds: 1
        readinessProbe:
          httpGet:
            path: /q/health/ready
            port: 8080
          initialDelaySeconds: 15 
          timeoutSeconds: 1
        volumeMounts:
        - mountPath: /cert/stenlund.jks
          name: stenlund
          subPath: stenlund.jks
          readOnly: true
        - mountPath: /deployments/config/application.properties
          name: stenlund
          subPath: application.properties
          readOnly: true
      volumes:
      - name: stenlund
        configMap: 
          name: stenlund
          items:
            - key: stenlund.jks
              path: stenlund.jks
            - key: application.properties
              path: application.properties
```
Example application.properties.
```
authz.keycloak.server=https://keycloak.home/auth
authz.keycloak.realm=quarkus
authz.keycloak.client=simple
authz.keycloak.secret=changeme
quarkus.http.cors=true
quarkus.http.cors.origins=/.*/
```

## Setup in keycloak
You need to create a confidential client (**Client authentication. ON**) in your realm and enable fine grained
authorization (**Authentication: ON**) for the client. You will then get a "Authorization" tab in the configuration
pages for the client. You need to add the following scopes: GET,POST,PUT,DELETE and PATCH. Then you can start adding your
protected resources, make sure the URI matches the URI of the requests you want to protect. Then you have to decide which policies and permissions to add to fit your application needs.

## How to build it

### Building the docker image
It is published on docker hub as dnulnets/authz, but if you want to build it on your own it can be done with the following command.

```
quarkus build -Dquarkus.container-image.build=true
```
