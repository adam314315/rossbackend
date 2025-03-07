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
package fr.cnes.regards.modules.indexer.domain.criterion;

import java.util.Collection;

/**
 * String[] specialized AbstractMatchCriterion ie a criterion to test if an attribute belongs to an array of
 * values.<br/>
 * <b>NB : This class is only used if none of provided string from given ones contains a blank character</b>
 *
 * @author oroussel
 */
public class StringMatchAnyCriterion extends AbstractMatchCriterion<String[]> {

    /**
     * See {@link StringMatchType} for explanation
     */
    private final StringMatchType matchType;

    public StringMatchAnyCriterion(String name, StringMatchType matchType, String... value) {
        super(name, MatchType.CONTAINS_ANY, value);
        this.matchType = matchType;
    }

    public StringMatchAnyCriterion(String name, StringMatchType matchType, Collection<String> values) {
        super(name, MatchType.CONTAINS_ANY, values.toArray(new String[0]));
        this.matchType = matchType;
    }

    @Override
    public StringMatchAnyCriterion copy() {
        return new StringMatchAnyCriterion(super.name, matchType, super.value);
    }

    @Override
    public <U> U accept(ICriterionVisitor<U> visitor) {
        return visitor.visitStringMatchAnyCriterion(this);
    }

    public StringMatchType getMatchType() {
        return matchType;
    }
}
