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

@Path("/")
public class AuthzResource {

    /** Logger */
    private static final Logger log = Logger.getLogger(AuthzResource.class);

    /* Get the application */
    @Inject AuthzApplication appl;

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
        Optional<AuthorizationResponse> ar = appl.authorize(jwt, uri, scope);
        if (ar.isPresent()) {
                String rpt = ar.get().getToken();
                System.out.println("AUTHZ: RPT = " + rpt);
                rr = ResponseBuilder.ok("").
                    header("authorization", "Bearer " + rpt).
                    header ("x-authz-rpt", "Bearer " + rpt).
                    build();
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
