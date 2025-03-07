/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.acquisition.dao;

import com.google.common.collect.Sets;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.ingest.dto.ISipState;
import fr.cnes.regards.modules.ingest.dto.SIPState;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

/**
 * Specification class to filter DAO searches on {@link Product} entities.
 *
 * @author Sébastien Binda
 */
public final class ProductSpecifications {

    private static final String LIKE_CHAR = "%";

    private ProductSpecifications() {
    }

    /**
     * Filter on the given attributes (sessionId, owner, ingestDate and state) and return result ordered by descending ingestDate
     *
     * @param states            {@link ProductState}
     * @param sipStates         {@link SIPState}
     * @param productName       {@link String}
     * @param session           {@link String}
     * @param processingChainId {@likn Long} id of {@link AcquisitionProcessingChain}
     * @param from              {@link OffsetDateTime}
     * @return {@link Specification}<{@link Product}>
     */
    public static Specification<Product> search(List<ProductState> states,
                                                List<ISipState> sipStates,
                                                String productName,
                                                String session,
                                                Long processingChainId,
                                                OffsetDateTime from,
                                                Boolean noSession) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();
            Set<Predicate> statePredicates = Sets.newHashSet();
            if (productName != null) {
                predicates.add(cb.like(root.get("productName"), LIKE_CHAR + productName + LIKE_CHAR));
            }
            if (Boolean.TRUE.equals(noSession)) {
                predicates.add(cb.isNull(root.get("session")));
            } else if (session != null) {
                predicates.add(cb.like(root.get("session"), LIKE_CHAR + session + LIKE_CHAR));
            }
            // Caution : state and sipState are handled as a grouped OR criterion.
            if (states != null && !states.isEmpty()) {
                for (ProductState state : states) {
                    statePredicates.add(cb.equal(root.get("state"), state));
                }
            }
            if (sipStates != null && !sipStates.isEmpty()) {
                for (ISipState state : sipStates) {
                    statePredicates.add(cb.equal(root.get("sipState"), state));
                }
            }
            if (!statePredicates.isEmpty()) {
                predicates.add(cb.or(statePredicates.toArray(new Predicate[statePredicates.size()])));
            }

            if (processingChainId != null) {
                AcquisitionProcessingChain chain = new AcquisitionProcessingChain();
                chain.setId(processingChainId);
                predicates.add(cb.equal(root.get("processingChain"), chain));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("lastUpdate"), from));
            }

            query.orderBy(cb.desc(root.get("lastUpdate")));
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

}
