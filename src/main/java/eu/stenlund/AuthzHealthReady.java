package eu.stenlund;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Readiness check for the application.
 */
@Readiness
@ApplicationScoped
public class AuthzHealthReady implements HealthCheck {
  
    @Inject AuthzApplication appl;

    /*
     * Performs the readiness check for the application, i.e. it can start receiveing requests.
     * 
     * @return  An up or down response based on liveness.
     */
    @Override
    public HealthCheckResponse call() {
        if (appl.ready ())
            return HealthCheckResponse.up("Ready to receive requests");
        else
            return HealthCheckResponse.down("Not ready to receive requests");
    }  
}