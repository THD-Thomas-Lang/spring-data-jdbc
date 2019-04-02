package org.springframework.data.jdbc.repository.support;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.lang.Nullable;

public interface QueryExecutor<T> {
    @Nullable
    T execute(MapSqlParameterSource parameter);
}
