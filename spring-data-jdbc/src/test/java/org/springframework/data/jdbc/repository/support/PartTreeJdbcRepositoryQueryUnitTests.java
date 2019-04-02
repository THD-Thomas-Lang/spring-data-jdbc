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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Wither;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PartTreeJdbcRepositoryQuery}.
 *
 * @author Thomas Lang
 */
@Slf4j
public class PartTreeJdbcRepositoryQueryUnitTests {

    private ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private RelationalMappingContext context = new JdbcMappingContext();
    private NamedParameterJdbcOperations jdbcOperations = mock(NamedParameterJdbcOperations.class);
    private RowMapper<?> defaultRowMapper = mock(RowMapper.class);
    private ProjectionFactory projectionFactory = mock(ProjectionFactory.class);
    private RepositoryMetadata repositoryMetadata = mock(RepositoryMetadata.class);

    @Test // DATAJDBC-318
    public void canInstantiateWithSimplePropertyReference() {

        when(repositoryMetadata.getReturnedDomainClass(any(Method.class))).thenReturn((Class) Person.class);

        JdbcQueryMethod queryMethod = createQueryMethod("findByFirstName");
        new PartTreeJdbcRepositoryQuery(publisher, context, queryMethod, jdbcOperations, defaultRowMapper);

    }

    @Test // DATAJDBC-318
    public void canNotInstantiateWithPathPropertyReference() {

        when(repositoryMetadata.getReturnedDomainClass(any(Method.class))).thenReturn((Class) Person.class);

        JdbcQueryMethod queryMethod = createQueryMethod("findByAnimalNickName");

        assertThatThrownBy(
                () -> new PartTreeJdbcRepositoryQuery(publisher, context, queryMethod, jdbcOperations, defaultRowMapper))
                .isInstanceOf(IllegalStateException.class);

    }

    // DATAJDBC-318
    // try findByFirstname first
    // this means i need a part "Firstname" which is the column
    // and i need a parameter
    @Test
    public void testSimpleFindDerivedQuery() {
        when(repositoryMetadata.getReturnedDomainClass(any(Method.class))).thenReturn((Class) Person.class);
        JdbcQueryMethod queryMethod = createQueryMethod("findByFirstName");
        final PartTreeJdbcRepositoryQuery partTreeJdbcRepositoryQuery = new PartTreeJdbcRepositoryQuery(publisher, context,
                queryMethod, jdbcOperations, defaultRowMapper);
        assertThat(partTreeJdbcRepositoryQuery).isNotNull();
        Object execute = partTreeJdbcRepositoryQuery.execute(new Object[]{"Franz"});
        assertThat(execute).isNotNull();
    }

    // DATAJDBC-318
    // try findByFirstNameOrLastName Or connectors which is not allowed.
    @Test
    public void testComplexOrFindDerivedQueryShouldThrowException() {
        when(repositoryMetadata.getReturnedDomainClass(any(Method.class))).thenReturn((Class) Person.class);
        JdbcQueryMethod queryMethod = createQueryMethod("findByFirstNameOrLastName", String.class, String.class);

        assertThatThrownBy(
                () -> new PartTreeJdbcRepositoryQuery(publisher, context, queryMethod, jdbcOperations, defaultRowMapper))
                .isInstanceOf(IllegalStateException.class);
    }

    // DATAJDBC-318
    // try findByFirstNameAndLastName And connectors which is not allowed.
    @Test
    public void testComplexAndFindDerivedQueryShouldWork() {
        when(repositoryMetadata.getReturnedDomainClass(any(Method.class))).thenReturn((Class) Person.class);
        JdbcQueryMethod queryMethod = createQueryMethod("findByFirstNameAndLastName", String.class, String.class);

        PartTreeJdbcRepositoryQuery partTreeJdbcRepositoryQuery = new PartTreeJdbcRepositoryQuery(publisher, context,
                queryMethod, jdbcOperations, defaultRowMapper);

        assertThat(partTreeJdbcRepositoryQuery).isNotNull();
    }

    // DATAJDBC-318
    // This test should fail because there are more than two property being queried.
    // This is not supported yet.
    @Test
    public void testInvalidMultiPropertyAndConnectorSqlQuery() {
        when(repositoryMetadata.getReturnedDomainClass(any(Method.class))).thenReturn((Class) Person.class);
        JdbcQueryMethod queryMethod = createQueryMethod("findByFirstNameAndLastNameAndEmail", String.class, String.class, String.class);
        assertThatThrownBy(() -> new PartTreeJdbcRepositoryQuery(publisher, context,
                queryMethod, jdbcOperations, defaultRowMapper)).isInstanceOf(IllegalArgumentException.class);
    }

    private JdbcQueryMethod createQueryMethod(String name) {

        Method method = ReflectionUtils.findMethod(PartTreeJdbcRepositoryQueryUnitTests.class, name, String.class);
        assert method != null;
        return new JdbcQueryMethod(method, repositoryMetadata, projectionFactory);
    }

    private JdbcQueryMethod createQueryMethod(String name, Class<?>... parameters) {

        Method method = ReflectionUtils.findMethod(PartTreeJdbcRepositoryQueryUnitTests.class, name, parameters);
        assert method != null;
        return new JdbcQueryMethod(method, repositoryMetadata, projectionFactory);
    }

    List<Person> findByFirstName(String firstName) {
        return null;
    }

    List<Person> findByFirstNameOrLastName(String firstName, String lastName) {
        return null;
    }

    List<Person> findByFirstNameAndLastName(String firstName, String lastName) {
        return null;
    }

    List<Person> findByFirstNameAndLastNameAndEmail(String firstName, String lastName, String email) {
        return null;
    }

    List<Person> findByAnimalNickName(String nickName) {
        return null;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    private class Person {
        @Wither
        @Id
        private final long personId;
        private String firstName;
        private String lastName;
        private String email;
        private Animal animal;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    private class Animal {
        @Wither
        @Id
        private final long personId;
        private String nickName;
    }
}
