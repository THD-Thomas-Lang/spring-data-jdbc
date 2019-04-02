package org.springframework.data.jdbc.repository.support;

import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.event.AfterLoadEvent;
import org.springframework.data.relational.core.mapping.event.Identifier;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Constructor;
import java.util.List;

final class QueryExecutorImpl implements QueryExecutor<Object> {
	private static final String PARAMETER_NEEDS_TO_BE_NAMED = "For queries with named parameters you need to provide names for method parameters. Use @Param for query method parameters, or when on Java 8+ use the javac flag -parameters.";

	private final QueryExecutor<Object> executor;
	private final JdbcQueryMethod queryMethod;
	private final ResultSetExtractor extractor;
	private final RowMapper rowMapper;
	private final NamedParameterJdbcOperations operations;
	private RelationalMappingContext context;
	private ApplicationEventPublisher publisher;

	private static boolean isUnconfigured(@Nullable Class<?> configuredClass, Class<?> defaultClass) {
		return configuredClass == null || configuredClass == defaultClass;
	}

	private String determineQuery() {

		String query = queryMethod.getAnnotatedQuery();

		if (StringUtils.isEmpty(query)) {
			throw new IllegalStateException(String.format("No query specified on %s", queryMethod.getName()));
		}

		return query;
	}

	private QueryExecutor<Object> createObjectRowMapperQueryExecutor(String query, RowMapper<?> rowMapper) {
		return parameters -> operations.queryForObject(query, parameters, rowMapper);
	}

	private <T> void publishAfterLoad(Iterable<T> all) {

		for (T e : all) {
			publishAfterLoad(e);
		}
	}

	private <T> void publishAfterLoad(@Nullable T entity) {

		if (entity != null && context.hasPersistentEntityFor(entity.getClass())) {

			RelationalPersistentEntity<?> e = context.getRequiredPersistentEntity(entity.getClass());
			Object identifier = e.getIdentifierAccessor(entity).getIdentifier();

			if (identifier != null) {
				publisher.publishEvent(new AfterLoadEvent(Identifier.of(identifier), entity));
			}
		}

	}

	private QueryExecutor<Object> createCollectionQueryExecutor(QueryExecutor<Object> executor) {

		return parameters -> {

			List<?> result = (List<?>) executor.execute(parameters);

			Assert.notNull(result, "A collection valued result must never be null.");

			publishAfterLoad(result);

			return result;
		};
	}

	private QueryExecutor<Object> createObjectQueryExecutor(QueryExecutor executor) {

		return parameters -> {

			try {

				Object result;

				result = executor.execute(parameters);

				publishAfterLoad(result);

				return result;

			} catch (EmptyResultDataAccessException e) {
				return null;
			}
		};
	}

	// QueryExecutor = Innenleben (Query)
	// kriegt Parameter, unterschiedlich
	// 3 Varianten
	private QueryExecutor<Object> createListRowMapperQueryExecutor(String query, RowMapper<?> rowMapper) {
		return parameters -> operations.query(query, parameters, rowMapper);
	}

	private QueryExecutor<Object> createResultSetExtractorQueryExecutor(String query,
			ResultSetExtractor<?> resultSetExtractor) {
		return parameters -> operations.query(query, parameters, resultSetExtractor);
	}

	@Nullable
	private ResultSetExtractor determineResultSetExtractor(@Nullable RowMapper<?> rowMapper) {

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

	private QueryExecutor<Object> createExecutor(JdbcQueryMethod queryMethod, @Nullable ResultSetExtractor extractor,
			RowMapper rowMapper) {

		String query = determineQuery();

		if (queryMethod.isCollectionQuery() || queryMethod.isStreamQuery()) {
			QueryExecutor<Object> innerExecutor = extractor != null ? createResultSetExtractorQueryExecutor(query, extractor)
					: createListRowMapperQueryExecutor(query, rowMapper);
			return createCollectionQueryExecutor(innerExecutor);
		}

		QueryExecutor<Object> innerExecutor = extractor != null ? createResultSetExtractorQueryExecutor(query, extractor)
				: createObjectRowMapperQueryExecutor(query, rowMapper);
		return createObjectQueryExecutor(innerExecutor);
	}

	QueryExecutorImpl(JdbcQueryMethod queryMethod, ResultSetExtractor extractor, RowMapper rowMapper,
			NamedParameterJdbcOperations operations, RelationalMappingContext context, ApplicationEventPublisher publisher,
			RowMapper<?> defaultRowMapper) {
		this.queryMethod = queryMethod;
		this.extractor = extractor;
		this.rowMapper = rowMapper;
		this.operations = operations;
		this.context = context;
		this.publisher = publisher;
		this.executor = createExecutor( //
				queryMethod, //
				determineResultSetExtractor(rowMapper != defaultRowMapper ? rowMapper : null), //
				rowMapper); //

	}

	@Override
	public Object execute(MapSqlParameterSource parameter) {
		return executor.execute(parameter);
	}
}
