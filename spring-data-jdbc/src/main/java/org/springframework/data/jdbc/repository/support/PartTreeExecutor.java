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
package org.springframework.data.jdbc.repository.support;

import org.springframework.data.jdbc.core.SqlGenerator;
import org.springframework.data.relational.core.sql.Expressions;
import org.springframework.data.relational.core.sql.Functions;
import org.springframework.data.relational.core.sql.Select;
import org.springframework.data.relational.core.sql.SelectBuilder;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.util.Lazy;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

/**
 * @author Jens Schauder
 */
public class PartTreeExecutor implements QueryExecutor<Object> {

	private final PartTree tree;
	private final RowMapper<?> rowMapper;
	private final NamedParameterJdbcOperations operations;

	public PartTreeExecutor(PartTree tree, RowMapper<?> rowMapper, NamedParameterJdbcOperations operations) {

		// select * form sometable where name = ? or lastname = ?
		// select * form sometable where name = :name or lastname = :name

		this.tree = tree;
		this.rowMapper = rowMapper;
		this.operations = operations;
	}

	@Override
	public Object execute(MapSqlParameterSource parameters) {
		return operations.query(createQuery(), parameters, rowMapper);
	}

	String createQuery() {

			SelectBuilder.SelectAndFrom select;
		if (tree.isCountProjection()) {
			select = Select.builder().select(Functions.count(Expressions.asterisk()));
		} else {
			new SqlGenerator(...).selectBuilder()
		}

		return null;
	}


}
