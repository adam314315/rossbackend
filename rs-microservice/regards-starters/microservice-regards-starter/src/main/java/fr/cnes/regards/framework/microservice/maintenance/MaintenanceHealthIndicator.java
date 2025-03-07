package fr.cnes.regards.framework.microservice.maintenance;

import fr.cnes.regards.framework.amqp.IInstanceSubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.tenant.TenantCreatedEvent;
import fr.cnes.regards.framework.microservice.manager.MaintenanceInfo;
import fr.cnes.regards.framework.microservice.manager.MaintenanceManager;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

/**
 * Health indicator allowing us to know when was the last shift between maintenance mode and standard mode
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public class MaintenanceHealthIndicator extends AbstractHealthIndicator
    implements ApplicationListener<ApplicationReadyEvent> {

    private static final String TENANT = "tenant";

    /**
     * {@link IRuntimeTenantResolver} instance
     */
    private final IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * {@link IInstanceSubscriber} instance
     */
    @Autowired
    private IInstanceSubscriber subscriber;

    /**
     * {@link ITenantResolver} instance
     */
    @Autowired
    private ITenantResolver tenantResolver;

    /**
     * Constructor setting the runtime tenant resolver
     */
    public MaintenanceHealthIndicator(IRuntimeTenantResolver runtimeTenantResolver) {
        super();
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        MaintenanceInfo info = MaintenanceManager.getMaintenanceMap().get(runtimeTenantResolver.getTenant());

        if (info == null) {
            // No a managed tenant ... supervisor request
            builder.up().withDetail(TENANT, "no tenant specified");
            return;
        }
        builder.withDetail("lastUpdate", info.getLastUpdate());
        if (info.getActive()) {
            builder.outOfService().withDetail(TENANT, runtimeTenantResolver.getTenant());
        } else {
            builder.up().withDetail(TENANT, runtimeTenantResolver.getTenant());
        }
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // lets get all the created tenants
        for (String tenant : tenantResolver.getAllTenants()) {
            MaintenanceManager.addTenant(tenant);
        }
        subscriber.subscribeTo(TenantCreatedEvent.class, new TenantCreatedEventHandler());
    }

    /**
     * Amqp event handler
     */
    private static class TenantCreatedEventHandler implements IHandler<TenantCreatedEvent> {

        @Override
        public void handle(TenantWrapper<TenantCreatedEvent> wrapper) {
            MaintenanceManager.addTenant(wrapper.getTenant());
        }

    }
}
