package fr.cnes.regards.framework.modules.plugins.domain.event;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;

import java.util.Set;

/**
 * This event is raised when the plugin configuration availability changed (from active to unactive or vice versa)
 * Target.ONE_PER_MICROSERVICE_TYPE because only one ingesterService should manage a PluginConf change (Database
 * updated).
 * BROADCAST because UNICAST/ONE_PER_MICROSERVICE_TYPE doesn't exist...
 *
 * @author oroussel
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE)
public class PluginConfEvent extends AbstractPluginConfEvent implements ISubscribable {

    public PluginConfEvent(Long pPluginConfId,
                           String pluginBusinnessId,
                           String label,
                           PluginServiceAction pAction,
                           Set<String> pPluginTypes) {
        super(pPluginConfId, pluginBusinnessId, label, pAction, pPluginTypes);
    }

    public PluginConfEvent() {
    }
}
