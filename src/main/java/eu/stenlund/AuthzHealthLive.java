package eu.stenlund;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/*
 * The liveness check.
 */
@Liveness
@ApplicationScoped
public class AuthzHealthLive implements HealthCheck {
  
    /* The application */
    @Inject AuthzApplication appl;
    
    /*
     * Performs the liveness check for the application, i.e. it is up and running and everyhting is fine.
     * 
     * @return  An up or down response based on liveness.
     */
    @Override
    public HealthCheckResponse call() {
        if (appl.live())
            return HealthCheckResponse.up("Healthy state");
        else
            return HealthCheckResponse.down("Not healty state");
    }  
}