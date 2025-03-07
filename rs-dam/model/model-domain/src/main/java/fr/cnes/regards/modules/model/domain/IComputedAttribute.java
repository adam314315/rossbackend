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
package fr.cnes.regards.modules.model.domain;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;

/**
 * Plugins of type IComputedAttribute are plugins allowing to calculate the value of an {@link AttributeModel} thanks to
 * a {@link ModelAttrAssoc} We are using the design pattern "Visitor" with {@link IComputedAttributeVisitor}.
 *
 * @param <P> Type of entity on which the attribute will be added
 * @param <R> type of the attribute value
 * @author Sylvain Vissiere-Guerinet
 */
@PluginInterface(description = "Plugins of type IComputedAttribute are plugins allowing to calculate the value of an AttributeModel thanks to a ModelAttrAssoc")
public interface IComputedAttribute<P, R> {

    /**
     * The plugin parameter name for the resulting attribute name
     */
    String RESULT_ATTRIBUTE_NAME = "resultAttributeName";

    /**
     * The plugin parameter name for the resulting attribute fragment name
     */
    String RESULT_FRAGMENT_NAME = "resultAttributeFragmentName";

    /**
     * @return the value computed by the implementation.
     */
    R getResult();

    /**
     * Method responsible for computation of the value.
     *
     * @param pTarget object on which the attribute should be added.
     */
    void compute(P pTarget);

    /**
     * Allows to build the resulting attribute from the computation
     *
     * @return computed attribute
     */
    default <U> U accept(IComputedAttributeVisitor<U> pVisitor) {
        return pVisitor.visit(this);
    }

    /**
     * @return the attribute computed by this plugin
     */
    AttributeModel getAttributeToCompute();
}
