# Istio external authorization for keycloak
This project creates a standalone authorization server that uses Keycloaks authorization services for fine grained authroization. It is deployed in kubernetes and used by a CUSTOM action in an istio authorization policy for fine grained authroization of your application via istio.

**NOTE!** This is work in progress and have some thing that needs to be done and quirks to solve before it is production ready. But it is a fully functional for experimental use for now.

## Introduction

## Things that needs to be done

### Setup in kubernetes
### Setup in keycloak

## How to build it

### Building docker image
quarkus build -Dquarkus.container-image.build=true
