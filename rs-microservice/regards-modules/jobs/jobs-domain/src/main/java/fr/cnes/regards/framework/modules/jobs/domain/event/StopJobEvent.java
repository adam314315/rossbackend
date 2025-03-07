package fr.cnes.regards.framework.modules.jobs.domain.event;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;

import java.util.UUID;

/**
 * AMQP event to notify job should be stopped. This event aims to change the job status, that's why it is not a JobEvent
 *
 * @author oroussel
 * @author Sylvain Vissiere-Guerinet
 */
@Event(target = Target.MICROSERVICE)
public class StopJobEvent implements ISubscribable {

    /**
     * the job id
     */
    protected UUID jobId;

    public StopJobEvent() {
    }

    public StopJobEvent(UUID jobId) {
        this.jobId = jobId;
    }

    /**
     * @return the job id
     */
    public UUID getJobId() {
        return jobId;
    }

    /**
     * Set the job id
     */
    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }
}
