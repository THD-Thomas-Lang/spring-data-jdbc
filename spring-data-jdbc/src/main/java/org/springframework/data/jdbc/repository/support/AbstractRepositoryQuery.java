/*
 * Copyright 2018-2019 the original author or authors.
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

import lombok.Getter;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.event.AfterLoadEvent;
import org.springframework.data.relational.core.mapping.event.Identifier;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Constructor;

/**
 * An abstract basis class for reusable {@link org.springframework.data.repository.query.RepositoryQuery}
 * implementations. Offers some handy functions.
 *
 * @author Thomas Lang
 */
abstract class AbstractRepositoryQuery {

	private static final String PARAMETER_NEEDS_TO_BE_NAMED = "For queries with named parameters you need to provide names for method parameters. Use @Param for query method parameters, or when on Java 8+ use the javac flag -parameters.";

	@Getter
	private final JdbcQueryMethod queryMethod;
	private final ApplicationEventPublisher publisher;
	private final RelationalMappingContext context;

	/**
	 * Overloaded constructor. Builds the needed references.
	 * 
	 * @param queryMethod a given {@link JdbcQueryMethod} reference.
	 * @param publisher a given {@link ApplicationEventPublisher} reference.
	 * @param context a given {@link RelationalMappingContext} reference.
	 */
	AbstractRepositoryQuery(JdbcQueryMethod queryMethod, ApplicationEventPublisher publisher,
			RelationalMappingContext context) {
		Assert.notNull(publisher, "Publisher must not be null!");
		Assert.notNull(context, "Context must not be null!");
		Assert.notNull(queryMethod, "Query method must not be null!");
		this.queryMethod = queryMethod;
		this.publisher = publisher;
		this.context = context;
	}

	protected String determineQuery() {

		String query = queryMethod.getAnnotatedQuery();

		if (StringUtils.isEmpty(query)) {
			throw new IllegalStateException(String.format("No query specified on %s", queryMethod.getName()));
		}

		return query;
	}

	protected MapSqlParameterSource bindParameters(Object[] objects) {

		MapSqlParameterSource parameters = new MapSqlParameterSource();

		queryMethod.getParameters().getBindableParameters().forEach(p -> {

			String parameterName = p.getName().orElseThrow(() -> new IllegalStateException(PARAMETER_NEEDS_TO_BE_NAMED));
			parameters.addValue(parameterName, objects[p.getIndex()]);
		});

		return parameters;
	}

	@Nullable
	protected ResultSetExtractor determineResultSetExtractor(@Nullable RowMapper<?> rowMapper) {

		Class<? extends ResultSetExtractor> resultSetExtractorClass = (Class<? extends ResultSetExtractor>) queryMethod
				.getResultSetExtractorClass();

		if (isUnconfigured(resultSetExtractorClass, ResultSetExtractor.class)) {
			return null;
		}

		Constructor<? extends ResultSetExtractor> constructor = ClassUtils
				.getConstructorIfAvailable(resultSetExtractorClass, RowMapper.class);

		if (constructor != null) {
			return BeanUtils.instantiateClass(constructor, rowMapper);
		}

		return BeanUtils.instantiateClass(resultSetExtractorClass);
	}

	protected RowMapper determineRowMapper(RowMapper<?> defaultMapper) {

		Class<?> rowMapperClass = queryMethod.getRowMapperClass();

		if (isUnconfigured(rowMapperClass, RowMapper.class)) {
			return defaultMapper;
		}

		return (RowMapper) BeanUtils.instantiateClass(rowMapperClass);
	}

	protected boolean isUnconfigured(@Nullable Class<?> configuredClass, Class<?> defaultClass) {
		return configuredClass == null || configuredClass == defaultClass;
	}

	protected <T> void publishAfterLoad(Iterable<T> all) {

		for (T e : all) {
			publishAfterLoad(e);
		}
	}

	protected <T> void publishAfterLoad(@Nullable T entity) {

		if (entity != null && context.hasPersistentEntityFor(entity.getClass())) {

			RelationalPersistentEntity<?> e = context.getRequiredPersistentEntity(entity.getClass());
			Object identifier = e.getIdentifierAccessor(entity).getIdentifier();

			if (identifier != null) {
				publisher.publishEvent(new AfterLoadEvent(Identifier.of(identifier), entity));
			}
		}

	}
}
