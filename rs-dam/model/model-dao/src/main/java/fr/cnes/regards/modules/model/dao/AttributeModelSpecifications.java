package fr.cnes.regards.modules.model.dao;
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

import com.google.common.collect.Sets;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.Set;

/**
 * JPA Repository to manage {@link AttributeModel} entities.
 *
 * @author Sébastien Binda
 */
public final class AttributeModelSpecifications {

    private AttributeModelSpecifications() {
    }

    /**
     * Filter on the given attributes
     *
     * @return {@link Specification}
     */
    public static Specification<AttributeModel> search(PropertyType type, String fragmentName, Set<String> modelNames) {
        return (root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (fragmentName != null) {
                predicates.add(cb.equal(root.get("fragment").get("name"), fragmentName));
            }
            if ((modelNames != null) && !modelNames.isEmpty()) {
                predicates.add(root.get("model").get("name").in(modelNames));
            }
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }

}
