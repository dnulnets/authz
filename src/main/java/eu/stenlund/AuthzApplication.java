package eu.stenlund;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.keycloak.authorization.client.AuthorizationDeniedException;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.authorization.client.resource.ProtectedResource;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Contains the generic functionality for the extension provider. Such as connectivity to keycloak
 * regarding authroization and resource mapping.
 */
@ApplicationScoped
public class AuthzApplication {

    /* Logger */
    private static final Logger log = Logger.getLogger(AuthzApplication.class);

    /* Configuration that points out the keycloak server, realm and client */
    @ConfigProperty(name = "authz.keycloak.server") String kcServer;
    @ConfigProperty(name = "authz.keycloak.realm") String kcRealm;
    @ConfigProperty(name = "authz.keycloak.client") String kcClient;
    @ConfigProperty(name = "authz.keycloak.secret") String kcSecret;

    /* The keycloak authorization clients */
    private AuthzClient authzClient = null;

    /* The keycloak resource client */
    private ProtectedResource resourceClient = null;
    
    /* Initialize the application */
    @PostConstruct 
    void init() {
        log.info ("AUTHZ: Initializing the application");

        /* Create the clients */
        try {
            Configuration cfg = new Configuration(kcServer, kcRealm, kcClient, Map.of ("secret", kcSecret), null);
            authzClient = AuthzClient.create (cfg);
            if (authzClient != null)
                resourceClient = authzClient.protection().resource();
        } catch (RuntimeException re) {
            log.error ("AUTHZ: " + re.getLocalizedMessage());
        }
    }

    /* Destroys the application */
    @PreDestroy 
    void destroy() {
        log.info ("AUTHZ: Destroying the application");
    }

    /* We are alive and healthy */
    public Boolean live()
    {
       return (authzClient != null) && (resourceClient != null);
    }

    /* We are ready to receive requests from istio */
    public Boolean ready ()
    {
        /* In the future we might need to wait for the resource caching has finished first */
        return (authzClient != null) && (resourceClient != null);
    }

    /**
     * Check the authorization with keycloak based on the token, uri and scope.
     * 
     * @param   jwt     The token to be used during authroization.
     * @param   uri     The uri that points to the resource that is protected.
     * @param   scope   The wanted scope for the resource.
     * @return          The authorization response if we are authroized.
     */
    public Optional<AuthorizationResponse> authorize (String jwt, String uri, String scope)
    {
        Optional<AuthorizationResponse> ar = Optional.empty();

        /* Are we connected */
        if (authzClient != null && resourceClient != null) {

            try {

                // Get the resources associated with the URI
                log.info ("AUTHZ: Getting resources for URI="+ uri);
                List<ResourceRepresentation> lrr = resourceClient.findByUri (uri);
                log.info ("AUTHZ: Length of resourcelist = " + lrr.size());
                lrr.forEach((n) -> log.info("AUTHZ: Resource = " + n.getName()));

                // Create an authorization request containing all the resources and thescope
                log.info ("AUTHZ: Checking authorization");
                AuthorizationRequest request = new AuthorizationRequest();
                lrr.forEach((n) -> {
                    log.info ("AUTHZ: Adding " + n.getName() + " and scope " + scope);
                    request.addPermission(n.getName(), scope); 
                });

                // Try to get us authorized
                ar = Optional.of(authzClient.authorization(jwt).authorize(request));

            } catch (AuthorizationDeniedException ade) {
                log.info ("AUTHZ: ADE " + ade.getMessage());
            } catch (RuntimeException re) {
                log.info ("AUTHZ: RE " + re.getMessage());
            }
        }

        return ar;
    }

}
