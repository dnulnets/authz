# Istio external authorization for Keycloak
This project creates a standalone authorization server that uses Keycloaks authorization services for fine grained authorization through Istios CUSTOM action for Authorization Policies. It is deployed as a standalone service in kubernetes.

**NOTE!** This is work in progress and have some thing that needs to be done and quirks to solve before it is production ready. But it is fully functional for experimental use for now.

## Introduction
The functional idea is that it is used by istios authorization policy as a CUSTOM action. For each HTTP request it uses the uri to look upp the resource in keycloak. Then it queries keycloak for authorization using the incoming token (**Authorization: Bearer xxxx** header), the looked up resource and the HTTP method as scope. Based on the response from keycloak it responds to istio with either OK and the RPT or a FORBIDDEN.

It runs as a stateless pod (Deployment) and can be run in multiple instances to achieve performance and robustness. But of course adds response time to the protected resource.

## Versions
The following versions are the one used for development and testing, it might work perfectly fine with other versions as well but it has not been tested.
* Keycloak 21.1.1
* Istio 1.17.2
* Quarkus 3.2.0

## Things to do
* Caching of resource/URI mapping instead of asking Keycloak for every request.
* Error handling when a request fails for reasons such as Keycloak not respondning/down, not authorized JWT etc.
* Health endpoints for the server to be used by kubernetes.
* This README with examples for setup etc.
* Performance tuning and deployment scenarios.

### Setup in kubernetes
#### Example AuthroizationPolicy
This example shows how to use an authorization policy using a CUSTOM action and specifies which provider to use for authroization.
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

### Setup in keycloak

## How to build it

### Building docker image
quarkus build -Dquarkus.container-image.build=true
