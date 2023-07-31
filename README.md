# Istio external authorization for Keycloak
This project creates a standalone authorization server that uses Keycloaks authorization services for fine grained authroization through Istios CUSTOM action for Authorization Policies. It is deployed as a standalone service in kubernetes.

**NOTE!** This is work in progress and have some thing that needs to be done and quirks to solve before it is production ready. But it is fully functional for experimental use for now.

## Introduction

## Versions
The following versions are the one used for development and testing, it might work perfectly fine with other versions as well but it has not been tested.
* Keycloak 21.1.1
* Istio 1.17.2
* Quarkus 3.2.0

## Things on the list
* Caching of resource/URI mapping instead of asking Keycloak for every request.
* Error handling when a request fails for reasons such as Keycloak not respondning/down, not authorized JWT etc.
* Health endpoints for the server to be used by kubernetes.
* This README with examples for setup etc.
* Performance tuning and deployment scenarios.

### Setup in kubernetes
### Setup in keycloak

## How to build it

### Building docker image
quarkus build -Dquarkus.container-image.build=true
