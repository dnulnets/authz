package eu.stenlund;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.keycloak.authorization.client.AuthorizationDeniedException;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.resource.ProtectedResource;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.AuthorizationResponse;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AuthzApplication {

    /** Logger */
    private static final Logger log = Logger.getLogger(AuthzApplication.class);

    /* Configuration that points out the keycloak.json file */
    @ConfigProperty(name = "authz.keycloak") String keycloakConfigFile;

    /* The keycloak authorization clients */
    private AuthzClient authzClient = null;
    private ProtectedResource resourceClient = null;
    
    /* Set up the application */
    @PostConstruct 
    void init() {
        log.info ("AUTHZ: Initializing the application");

        /* Create the clients */
        try {
            InputStream input = new FileInputStream(keycloakConfigFile);
            authzClient = AuthzClient.create (input);
            if (authzClient != null)
                resourceClient = authzClient.protection().resource();
        } catch (FileNotFoundException fe) {
            log.error ("AUTHZ: Cannot locate keycloak configuration file: " + keycloakConfigFile);
        } catch (RuntimeException re) {
            log.error ("AUTHZ: " + re.getLocalizedMessage());
        }
    }

    @PreDestroy 
    void destroy() {
        log.info ("AUTHZ: Destroying the application");
    }

    /* We are alive and healthy */
    public Boolean live()
    {
       return (authzClient != null) && (resourceClient != null);
    }

    /* We are ready the receive requests from istio */
    public Boolean ready ()
    {
        return (authzClient != null) && (resourceClient != null);
    }

    /* Check the authroization with keycloak based on the token, uri and scope */
    public Optional<AuthorizationResponse> authorize (String jwt, String uri, String scope)
    {
        Optional<AuthorizationResponse> ar = Optional.empty();

        // The resource lookup and the authorize request can cause exceptions and
        // when they occur we will deny all by default.
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
