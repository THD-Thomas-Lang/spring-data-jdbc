/*
 * Copyright 2018-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jdbc.repository.support;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.util.Assert;

/**
 * {@link RepositoryQuery} implementation for derived queries.
 *
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Thomas Darimont
 * @author Mark Paluch
 * @author Thomas Lang
 */
class PartTreeJdbcRepositoryQuery extends AbstractRepositoryQuery implements RepositoryQuery {

	private static final Pattern READONLY_PREFIX_TEMPLATE = Pattern
			.compile("^(find|read|get|query|stream|count|exists)((\\p{Lu}.*?))??By");

	private final PartTree tree;
	private final QueryExecutor<Object> executor;

	// checks if the query path is within one aggregate root
	// only attributes within an aggregate root are supported yet
	private boolean isFatherChildMode(Class<?> domainClass, PartTree partTree) {
		// get iterator from getProperty
		// sollte genau ein element haben
		List<? extends Class<?>> classes = partTree.getParts()
				.map(s -> s.getProperty().getLeafProperty().getOwningType().getType()).toList();

		return classes.stream().anyMatch(s -> !s.isAssignableFrom(domainClass));
	}

	// checks if the query is read-only
	private boolean isReadOnlyQuery(String method) {
		Matcher matcher = READONLY_PREFIX_TEMPLATE.matcher(method);
		return matcher.find();
	}

	/**
	 * Creates a new {@link PartTreeJdbcRepositoryQuery} for the given {@link JdbcQueryMethod},
	 * {@link RelationalMappingContext} and {@link RowMapper}.
	 *
	 * @param publisher must not be {@literal null}.
	 * @param context must not be {@literal null}.
	 * @param queryMethod must not be {@literal null}.
	 * @param operations must not be {@literal null}.
	 * @param defaultRowMapper can be {@literal null} (only in case of a modifying query).
	 */
	PartTreeJdbcRepositoryQuery(ApplicationEventPublisher publisher, RelationalMappingContext context,
								JdbcQueryMethod queryMethod, NamedParameterJdbcOperations operations, RowMapper<?> defaultRowMapper) {
		super(queryMethod, publisher, context);
		Assert.notNull(operations, "NamedParameterJdbcOperations must not be null!");
		Assert.notNull(defaultRowMapper, "Mapper must not be null!");

		if (!isReadOnlyQuery(queryMethod.getName()))
			throw new IllegalArgumentException("Yet just read-only queries like (find) or (query) are supported!");

		ResultProcessor processor = queryMethod.getResultProcessor();
		Class<?> domainType = processor.getReturnedType().getDomainType();
		this.tree = new PartTree(queryMethod.getName(), processor.getReturnedType().getDomainType());

		if (this.isFatherChildMode(domainType, this.tree))
			throw new IllegalStateException(
					"Derived Queries do only work in simple mode, which is from Aggregate Root to Child!");

		executor = new PartTreeExecutor(tree, defaultRowMapper);
	}

	/**
	 * Return the {@link PartTree} backing the query.
	 *
	 * @return the tree
	 */
	public PartTree getTree() {
		return tree;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.RepositoryQuery#execute(java.lang.Object[])
	 */
	@Override
	public Object execute(Object[] objects) {

		return executor.execute(bindParameters(objects));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.RepositoryQuery#getQueryMethod()
	 */
	@Override
	public JdbcQueryMethod getQueryMethod() {
		return this.getQueryMethod();
	}

}
