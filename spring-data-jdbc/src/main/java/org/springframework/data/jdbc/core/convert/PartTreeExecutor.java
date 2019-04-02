/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jdbc.core.convert;

import org.springframework.data.jdbc.repository.support.QueryExecutor;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.domain.Identifier;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link QueryExecutor}. Translates derived queries (findByName etc.) to SQL.
 *
 * @author Jens Schauder
 * @author Thomas Lang
 */
public class PartTreeExecutor implements QueryExecutor<Object> {

    // logic to work with method names like "findByFirstName" or "findFirstByFirstName"
    private final PartTree tree;
    // maps the raw sql result set to the given format
    private final RowMapper<?> rowMapper;
    // handling of query parameters
    // select * form sometable where name = ? or lastname = ?
    // select * form sometable where name = :name or lastname = :name
    private final NamedParameterJdbcOperations operations;
    private final Class<?> domainType;
    private final SqlGeneratorSource sqlGeneratorSource;
    private final List<Part> parts;

    /**
     * Overloaded constructor. Builds the needed references.
     *
     * @param tree       a given {@link PartTree} reference
     * @param rowMapper  a given {@link RowMapper} reference
     * @param operations a given {@link NamedParameterJdbcOperations} reference
     * @param context    a given {@link RelationalMappingContext} reference
     * @param domainType a given domain type
     */
    public PartTreeExecutor(PartTree tree, RowMapper<?> rowMapper, NamedParameterJdbcOperations operations,
                            RelationalMappingContext context, Class<?> domainType) {
        Assert.notNull(tree, "Parameter tree of type PartTree cannot be null!");
        Assert.notNull(rowMapper, "Parameter rowMapper of type RowMapper cannot be null!");
        Assert.notNull(operations, "Parameter operations of type NamedParameterJdbcOperations cannot be null!");
        Assert.notNull(context, "Parameter context of type RelationalMappingContext cannot be null!");
        Assert.notNull(domainType, "Parameter domainType of type Class cannot be null!");
        this.parts = tree.getParts().toList();

        this.tree = tree;
        this.rowMapper = rowMapper;
        this.operations = operations;
        this.domainType = domainType;
        this.sqlGeneratorSource = new SqlGeneratorSource(context);
    }

    /**
     * Executes the query.
     *
     * @param parameters given {@link MapSqlParameterSource} to be mapped
     * @return Object
     */
    @Override
    public Object execute(MapSqlParameterSource parameters) {
        return operations.query(createQuery(), parameters, rowMapper);
    }

    public String createQuery() {


        if (this.tree.isCountProjection()) {
            return this.sqlGeneratorSource.getSqlGenerator(domainType).getCount();
        }

        // If the query is a one property one like
        // findByFirstName
        if (this.parts.size() == 1) {
            String column = parts.get(0).getProperty().getSegment();
            Class<?> type = parts.get(0).getProperty().getType();
            return this.sqlGeneratorSource.getSqlGenerator(this.domainType)
                    .getFindAllByProperty(Identifier.of(column, null, type), null, false);
        }
        // this part is relevant if the query has more than one parts aka properties
        // remember: one has come to this point if there is only allowed connectors in the query
        // as of now (03.05.2019) only AND is allowed
        //String columnOne = parts.get(0).getProperty().getSegment();
        //Class<?> typeOne = parts.get(0).getProperty().getType();
        //String columnTwo = parts.get(1).getProperty().getSegment();
        //Class<?> typeTwo = parts.get(0).getProperty().getType();

        Map<String, Object> propertyMap = new HashMap<>();
        this.parts.forEach(part -> propertyMap.put(part.getProperty().getSegment(), null));


        //Identifier identifier = Identifier.of(columnOne, null, typeOne).withPart(columnTwo, null, typeTwo);
        Identifier identifier = Identifier.from(propertyMap);
        return this.sqlGeneratorSource.getSqlGenerator(this.domainType).getFindAllByProperty(identifier, null, false);

    }

}
