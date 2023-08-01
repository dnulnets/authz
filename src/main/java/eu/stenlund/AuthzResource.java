package eu.stenlund;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.RestResponse.ResponseBuilder;
import org.jboss.resteasy.reactive.RestResponse.Status;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestHeader;
import org.keycloak.authorization.client.AuthorizationDeniedException;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.resource.ProtectedResource;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

@Path("/")
public class AuthzResource {

    /** Logger */
    private static final Logger log = Logger.getLogger(AuthzResource.class);

    /* The keycloak authorization clients */
    private AuthzClient authzClient = null;
    private ProtectedResource resourceClient = null;

    public AuthzResource() {

        String keycloak = ConfigProvider.getConfig().getValue("authz.keycloak", String.class);
        
        /* Create the clients */
        try {
            InputStream input = new FileInputStream(keycloak);
            authzClient = AuthzClient.create (input);
            if (authzClient != null)
                resourceClient = authzClient.protection().resource();
        } catch (FileNotFoundException fe) {
            log.error ("AUTHZ: Cannot locate keycloak configuration file: " + keycloak);
        } catch (RuntimeException re)
        {
            log.error ("AUTHZ: " + re.getLocalizedMessage());
        }
    }

    /* Take out the JWT from the Bearer, if any */
    private String getJWT (String bearer)
    {
        String jwt = "";

        // Get the JWT
        if (bearer != null) {
            String s = bearer.substring(0, Math.min(bearer.length(), 7));
            if (s.compareToIgnoreCase("Bearer ") == 0) {
                jwt = bearer.substring(7).trim();
            }
        }
        return jwt;
    }

    /* Perform the authorization check */
    private RestResponse<String> performAuthzCheck (String jwt, String uri, String scope)
    {
        RestResponse<String> rr = ResponseBuilder.create(Status.FORBIDDEN, "Default deny").build();

        /* Only possible if we got access to keycloak, if not deny all */
        if (authzClient != null && resourceClient != null) {

            // The resource lookup and the authorize request can cause exceptions and
            // when they occur we will deny all by default.
            try {

                // Get the resources associated with the URI
                log.info ("Getting resources for URI="+ uri);
                List<ResourceRepresentation> lrr = resourceClient.findByUri (uri);
                log.info ("Length of resourcelist = " + lrr.size());
                lrr.forEach((n) -> log.info("Resource = " + n.getName()));

                // Create an authorization request containing all the resources and thescope
                log.info ("Checking authorization");
                AuthorizationRequest request = new AuthorizationRequest();
                lrr.forEach((n) -> {
                    log.info ("Adding " + n.getName() + " and scope " + scope);
                    request.addPermission(n.getName(), scope); 
                });

                // Try to get us authorized
                AuthorizationResponse response = authzClient.authorization(jwt).authorize(request);
                String rpt = response.getToken();
                System.out.println("AUTHZ: You got an RPT = " + rpt);
                rr = ResponseBuilder.ok("").header("Authorization", "Bearer " + jwt).build();

            } catch (AuthorizationDeniedException ade) {
                log.info ("AUTHZ: ADE " + ade.getMessage());
                rr = ResponseBuilder.create(Status.FORBIDDEN, ade.getMessage()).build();
            } catch (RuntimeException ade) {
                log.info ("AUTHZ: RE " + ade.getMessage());
                rr = ResponseBuilder.create(Status.FORBIDDEN, ade.getMessage()).build();
            }
        }

        return rr;
    }

    /* We always return 200 for OPTION */
    @Path("{:.+}")
    @OPTIONS
    public RestResponse<String> optionsCheck(@RestHeader("Authorization") String bearer, @Context UriInfo uriInfo) {
        log.info ("Authorization check for URI = " + uriInfo.getPath() + " and scope = OPTIONS");
        return ResponseBuilder.ok("").build();
    }

    /* GET => Scope = GET */
    @Path("{:.+}")
    @GET
    public RestResponse<String> getCheck(HttpHeaders httpHeaders, @RestHeader("Authorization") String bearer, @Context UriInfo uriInfo) {
        httpHeaders.getRequestHeaders().forEach((h,v)->{
            log.info ("SIMPLE: " + h + " = " + v);
        });
        log.info ("Authorization check for URI = " + uriInfo.getPath() + " and scope = GET");
        return performAuthzCheck (getJWT (bearer), uriInfo.getPath(), "GET");
    }

    /* HEAD is the same as GET, so Scope = GET */
    @Path("{:.+}")
    @HEAD
    public RestResponse<String> headCheck(@RestHeader("Authorization") String bearer, @Context UriInfo uriInfo) {
        log.info ("Authorization check for URI = " + uriInfo.getPath() + " and scope = HEAD");
        return performAuthzCheck (getJWT (bearer), uriInfo.getPath(), "GET");
    }

    /* POST => Scope POST */
    @Path("{:.+}")
    @POST
    public RestResponse<String> postCheck(@RestHeader("Authorization") String bearer, @Context UriInfo uriInfo) {
        log.info ("Authorization check for URI = " + uriInfo.getPath() + " and scope = POST");
        return performAuthzCheck (getJWT(bearer), uriInfo.getPath(), "POST");
    }

    /* PUT => Scope PUT */
    @Path("{:.+}")
    @PUT
    public RestResponse<String> putCheck(@RestHeader("Authorization") String bearer, @Context UriInfo uriInfo) {
        log.info ("Authorization check for URI = " + uriInfo.getPath() + " and scope = PUT");
        return performAuthzCheck (getJWT(bearer), uriInfo.getPath(), "PUT");
    }

    /* DELETE => Scope DELETE */
    @Path("{:.+}")
    @DELETE
    public RestResponse<String> deleteCheck(@RestHeader("Authorization") String bearer, @Context UriInfo uriInfo) {
        log.info ("Authorization check for URI = " + uriInfo.getPath() + " and scope = DELETE");
        return performAuthzCheck (getJWT(bearer), uriInfo.getPath(), "DELETE");
    }

    /* PATCH => Scope PATCH */
    @Path("{:.+}")
    @PATCH
    public RestResponse<String> patchCheck(@RestHeader("Authorization") String bearer, @Context UriInfo uriInfo) {
        log.info ("Authorization check for URI = " + uriInfo.getPath() + " and scope = PATCH");
        return performAuthzCheck (getJWT(bearer), uriInfo.getPath(), "PATCH");
    }
}
