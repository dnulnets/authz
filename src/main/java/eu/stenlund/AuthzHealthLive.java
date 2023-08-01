package eu.stenlund;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@Liveness
@ApplicationScoped
public class AuthzHealthLive implements HealthCheck {
  
    @Inject AuthzApplication appl;
    
    @Override
    public HealthCheckResponse call() {
        if (appl.live())
            return HealthCheckResponse.up("Healthy state");
        else
            return HealthCheckResponse.down("Not healty state");
    }  
}