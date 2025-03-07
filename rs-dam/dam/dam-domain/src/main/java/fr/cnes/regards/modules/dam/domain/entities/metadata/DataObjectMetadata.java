package fr.cnes.regards.modules.dam.domain.entities.metadata;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * DataObject metadata. This object is only used by Elasticsearch
 *
 * @author oroussel
 */
public class DataObjectMetadata {

    @SuppressWarnings("unused")
    private static final class DatasetAccessRight {

        private String dataset;

        /**
         * <b>Data</b> access right
         */
        private boolean dataAccessRight;

        public DatasetAccessRight() {
        }

        public DatasetAccessRight(String dataset, boolean accessRight) {
            this.dataset = dataset;
            this.dataAccessRight = accessRight;
        }

        public String getDataset() {
            return dataset;
        }

        public void setDataset(String dataset) {
            this.dataset = dataset;
        }

        public boolean isAccessRight() {
            return dataAccessRight;
        }

        public void setAccessRight(boolean accessRight) {
            this.dataAccessRight = accessRight;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if ((o == null) || (getClass() != o.getClass())) {
                return false;
            }
            DatasetAccessRight that = (DatasetAccessRight) o;
            return Objects.equals(dataset, that.dataset);
        }

        @Override
        public int hashCode() {

            return Objects.hash(dataset);
        }
    }

    /**
     * Multimap { groupName, (datasetIpIds) }
     */
    private final Multimap<String, DatasetAccessRight> groups = HashMultimap.create();

    /**
     * Multimap { modelName, (datasetIpIds) }
     */
    private final Multimap<String, String> modelNames = HashMultimap.create();

    /**
     * @param fileAccessGranted true if data access is granted for (group, dataset ip id)
     */
    public void addGroup(String groupName, String datasetIpId, boolean fileAccessGranted) {
        groups.put(groupName, new DatasetAccessRight(datasetIpId, fileAccessGranted));
    }

    public void removeGroup(String groupName, String datasetIpId) {
        // accessRight value doesn't count, DatasetAccessRight only use datasetIpId for equality
        groups.remove(groupName, new DatasetAccessRight(datasetIpId, true));
    }

    /**
     * Remove given ipId from all values (groups multimap AND modelNames multimap)
     */
    public void removeDatasetIpId(String datasetIpId) {
        for (Iterator<DatasetAccessRight> i = groups.values().iterator(); i.hasNext(); ) {
            if (i.next().getDataset().equals(datasetIpId)) {
                i.remove();
            }
        }
        for (Iterator<String> i = modelNames.values().iterator(); i.hasNext(); ) {
            if (i.next().equals(datasetIpId)) {
                i.remove();
            }
        }

    }

    public Set<String> getGroups() {
        return groups.keySet();
    }

    /**
     * Retrieve a map of { group -> data access}. data access is a true boolean if at least one associated dataset
     * grants
     * data access, false otherwise
     *
     * @return {@link Map} bteween group name and associated Access
     */
    public Map<String, Boolean> getGroupsAccessRightsMap() {
        return Maps.transformValues(groups.asMap(),
                                    rights -> rights.stream()
                                                    .anyMatch(datasetAccessRight -> datasetAccessRight.isAccessRight()));
    }

    public void addModelName(String modelName, String datasetIpId) {
        modelNames.put(modelName, datasetIpId);
    }

    public void removeModelName(String modelName, String datasetIpId) {
        modelNames.remove(modelName, datasetIpId);
    }

    public Set<String> getModelNames() {
        return modelNames.keySet();
    }
}
