package fr.cnes.regards.framework.modules.jobs.domain;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Reliant task. Associated jobs of this task must not be executed while reliants task jobs are not terminated
 * This class is abstract. Please inherit it from your microservice adding your personal informations in order to use it
 * <b>By default, equality between 2 entities uses id if both exist else they are considered as different</b>
 *
 * @author oroussel
 */
@SuppressWarnings({ "rawtypes" })
@Entity
@Table(name = "t_task")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class AbstractReliantTask<K extends AbstractReliantTask> implements IIdentifiable<Long> {

    @Id
    @SequenceGenerator(name = "TaskSequence", initialValue = 1, sequenceName = "seq_task")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "TaskSequence")
    protected Long id;

    @OneToOne(cascade = CascadeType.REMOVE)
    // Using a join table better than a foreign key on t_job_info to avoid adding a dependence. A job/job_info knows
    // nothing (like Job Snow), it just have something to do regardless of everything else
    @JoinTable(name = "ta_task_job_info",
               joinColumns = @JoinColumn(name = "task_id", foreignKey = @ForeignKey(name = "fk_task")),
               inverseJoinColumns = @JoinColumn(name = "job_info_id", foreignKey = @ForeignKey(name = "fk_job_info")))
    protected JobInfo jobInfo;

    // OneToMany and not ManyToMany because a graph can always be transformed into tree using convenient structure
    @OneToMany(targetEntity = AbstractReliantTask.class, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "parent_id", foreignKey = @ForeignKey(name = "fk_task_parent_task"))
    protected Set<K> reliantTasks = new HashSet<>();

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long pId) {
        id = pId;
    }

    public JobInfo getJobInfo() {
        return jobInfo;
    }

    public void setJobInfo(JobInfo jobInfo) {
        this.jobInfo = jobInfo;
    }

    public Set<K> getReliantTasks() {
        return reliantTasks;
    }

    public void addReliantTask(K reliantTask) {
        this.reliantTasks.add(reliantTask);
    }

    /**
     * No Business key ! Id is the only parameter to check equality between reliant tasks
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AbstractReliantTask<?> that = (AbstractReliantTask<?>) o;
        // id1 == id2 else false if id1, id2 or both is (are) null
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
