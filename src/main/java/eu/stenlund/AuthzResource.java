package eu.stenlund;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.inject.Inject;
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
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestHeader;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;

import java.util.Optional;

/**
 * Handles all incoming requests that are supposed to be checked by keycloak authorization service.
 */
@Path("/")
public class AuthzResource {

    /** Logger */
    private static final Logger log = Logger.getLogger(AuthzResource.class);

    /* Get the application */
    @Inject AuthzApplication appl;

    /**
     * Extract the the JWT from the Bearer.
     * @param   bearer  The value of the authroization header.
     * @return          The extracted token.
     */
    private String getJWT (String bearer)
    {
        String jwt = null;

        // Get the JWT
        if (bearer != null) {
            try {
                String s = bearer.substring(0, Math.min(bearer.length(), 7));
                if (s.compareToIgnoreCase("Bearer ") == 0) {
                    jwt = bearer.substring(7).trim();
                }
            } catch (IndexOutOfBoundsException iob) {
                log.error("AUTHZ: Could not extract JWT: " + bearer);
            }
        }
        return jwt;
    }

    /**
     * Performs the authorization check and creates a response.
     * @param   jwt     The token sent by the requestor.
     * @param   uri     The uri the requestor is trying to access
     * @param   scope   The scope the requestor asks for
     * @return          The response sent back to the authorization requestor (istio).
     */
    private RestResponse<String> performAuthzCheck (String jwt, String uri, String scope)
    {
        RestResponse<String> rr = null;

        Optional<AuthorizationResponse> ar = appl.authorize(jwt, uri, scope);
        if (ar.isPresent()) {
            String rpt = ar.get().getToken();
            log.info("AUTHZ: Got an RPT = " + rpt);
            rr = ResponseBuilder.ok("").
                header("Authorization", "Bearer " + rpt).
                header("Cache-Control", "private, no-cache, no-store, must-revalidate").
                header("Expires", "-1").
                header("Pragma", "no-cache").
                build();
        } else {
            log.info ("AUTHZ: Failed to get an RPT, not authorized");
            rr = ResponseBuilder.create(Status.FORBIDDEN, "").
                header("Cache-Control", "private, no-cache, no-store, must-revalidate").
                header("Expires", "-1").
                header("Pragma", "no-cache").
                build();
        }

        return rr;
    }

    /* We always return 200 for OPTION */
    @Path("{:.+}")
    @OPTIONS
    public RestResponse<String> optionsCheck(@RestHeader("Authorization") String bearer, @Context UriInfo uriInfo) {
        log.info ("AUTHZ: Authorization check for URI = " + uriInfo.getPath() + " and scope = OPTIONS");
        return ResponseBuilder.ok("").build();
    }

    /* GET => Scope = GET */
    @Path("{:.+}")
    @GET
    public RestResponse<String> getCheck(HttpHeaders httpHeaders, @RestHeader("Authorization") String bearer, @Context UriInfo uriInfo) {
        log.info ("AUTHZ: Authorization check for URI = " + uriInfo.getPath() + " and scope = GET");
        return performAuthzCheck (getJWT (bearer), uriInfo.getPath(), "GET");
    }

    /* HEAD is the same as GET, so Scope = GET */
    @Path("{:.+}")
    @HEAD
    public RestResponse<String> headCheck(@RestHeader("Authorization") String bearer, @Context UriInfo uriInfo) {
        log.info ("AUTHZ: Authorization check for URI = " + uriInfo.getPath() + " and scope = HEAD");
        return performAuthzCheck (getJWT (bearer), uriInfo.getPath(), "GET");
    }

    /* POST => Scope POST */
    @Path("{:.+}")
    @POST
    public RestResponse<String> postCheck(@RestHeader("Authorization") String bearer, @Context UriInfo uriInfo) {
        log.info ("AUTHZ: Authorization check for URI = " + uriInfo.getPath() + " and scope = POST");
        return performAuthzCheck (getJWT(bearer), uriInfo.getPath(), "POST");
    }

    /* PUT => Scope PUT */
    @Path("{:.+}")
    @PUT
    public RestResponse<String> putCheck(@RestHeader("Authorization") String bearer, @Context UriInfo uriInfo) {
        log.info ("AUTHZ: Authorization check for URI = " + uriInfo.getPath() + " and scope = PUT");
        return performAuthzCheck (getJWT(bearer), uriInfo.getPath(), "PUT");
    }

    /* DELETE => Scope DELETE */
    @Path("{:.+}")
    @DELETE
    public RestResponse<String> deleteCheck(@RestHeader("Authorization") String bearer, @Context UriInfo uriInfo) {
        log.info ("AUTHZ: Authorization check for URI = " + uriInfo.getPath() + " and scope = DELETE");
        return performAuthzCheck (getJWT(bearer), uriInfo.getPath(), "DELETE");
    }

    /* PATCH => Scope PATCH */
    @Path("{:.+}")
    @PATCH
    public RestResponse<String> patchCheck(@RestHeader("Authorization") String bearer, @Context UriInfo uriInfo) {
        log.info ("AUTHZ: Authorization check for URI = " + uriInfo.getPath() + " and scope = PATCH");
        return performAuthzCheck (getJWT(bearer), uriInfo.getPath(), "PATCH");
    }
}
