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
package fr.cnes.regards.modules.opensearch.service.builder;

import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.core.builders.QueryTreeBuilder;
import org.apache.lucene.queryparser.flexible.core.messages.QueryParserMessages;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.apache.lucene.queryparser.flexible.messages.MessageImpl;
import org.apache.lucene.queryparser.flexible.standard.parser.EscapeQuerySyntaxImpl;

/**
 * Set this generic builder in a {@link QueryTreeBuilder} constructor for disabling specific functionalities.
 *
 * @author Xavier-Alexandre Brochard
 */
public class UnsupportedQueryNodeBuilder implements ICriterionQueryBuilder {

    /**
     * Systematicly throw an error
     */
    @Override
    public ICriterion build(final QueryNode queryNode) throws QueryNodeException {
        throw new QueryNodeException(new MessageImpl(QueryParserMessages.LUCENE_QUERY_CONVERSION_ERROR,
                                                     queryNode.toQueryString(new EscapeQuerySyntaxImpl()),
                                                     queryNode.getClass().getName()));
    }

}
