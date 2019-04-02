/*
 * Copyright 2017-2019 the original author or authors.
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
package org.springframework.data.jdbc.core;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Wither;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.core.convert.PartTreeExecutor;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.data.jdbc.repository.support.JdbcQueryMethod;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link PartTreeExecutor}.
 *
 * @author Thomas Lang
 */
public class PartTreeExecutorUnitTests {

    private RelationalMappingContext context = new JdbcMappingContext();
    private NamedParameterJdbcOperations jdbcOperations = mock(NamedParameterJdbcOperations.class);
    private RowMapper<?> defaultRowMapper = mock(RowMapper.class);
    private JdbcQueryMethod queryMethod = mock(JdbcQueryMethod.class);
    private ProjectionFactory projectionFactory = mock(ProjectionFactory.class);
    private RepositoryMetadata repositoryMetadata = mock(RepositoryMetadata.class);

    private JdbcQueryMethod createQueryMethod(String name, Class<?>... parameters) {

        Method method = ReflectionUtils.findMethod(PartTreeExecutorUnitTests.class, name, parameters);
        assert method != null;
        return new JdbcQueryMethod(method, repositoryMetadata, projectionFactory);
    }

    // test a simple sql query.
    // one property to be queried
    // no AND connectors
    // expecting it to be like:
    // SELECT person.person_id AS person_id, person.last_name AS last_name, person.first_name AS first_name,
    // animal.nick_name AS animal_nick_name, animal.person_id AS animal_person_id
    // FROM person LEFT OUTER JOIN animal AS animal ON animal.person = person.person_id WHERE person.firstName = :firstName
    @Test
    public void testSimpleOnePropertyNoConnectorSqlQuery() {
        String queryName = "findByFirstName";
        when(repositoryMetadata.getReturnedDomainClass(any(Method.class))).thenReturn((Class) Person.class);
        when(queryMethod.getName()).thenReturn(queryName);
        JdbcQueryMethod queryMethod = createQueryMethod(queryName, String.class);
        PartTree tree = new PartTree(queryMethod.getName(), queryMethod.getResultProcessor().getReturnedType().getDomainType());
        PartTreeExecutor partTreeExecutor = new PartTreeExecutor(tree, defaultRowMapper, jdbcOperations, context,
                queryMethod.getResultProcessor().getReturnedType().getDomainType());

        String sql = partTreeExecutor.createQuery();

        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(sql) //
                .startsWith("SELECT") //
                .contains("person.person_id AS person_id,") //
                .contains("person.last_name AS last_name,") //
                .contains("person.first_name AS first_name,") //
                .contains("animal.nick_name AS animal_nick_name,") //
                .contains("animal.person_id AS animal_person_id") //
                .contains("FROM person LEFT OUTER JOIN animal AS animal ON animal.person = person.person_id WHERE person.firstName = :firstName"); //
        softAssertions.assertAll();

    }

    // test a more complex sql query.
    // expecting it to be like:
    // SELECT
    // person.person_id AS person_id,
    // person.last_name AS last_name,
    // person.first_name AS first_name,
    // animal.nick_name AS animal_nick_name,
    // animal.person_id AS animal_person_id
    // FROM person LEFT OUTER JOIN animal AS animal ON animal.person = person.person_id WHERE person.firstName = :firstName AND person.lastName = :lastName;
    @Test
    public void testMultiPropertyAndConnectorSqlQuery() {
        String queryName = "findByFirstNameAndLastName";
        when(repositoryMetadata.getReturnedDomainClass(any(Method.class))).thenReturn((Class) Person.class);
        when(queryMethod.getName()).thenReturn(queryName);
        JdbcQueryMethod queryMethod = createQueryMethod(queryName, String.class, String.class);
        PartTree tree = new PartTree(queryMethod.getName(), queryMethod.getResultProcessor().getReturnedType().getDomainType());
        PartTreeExecutor partTreeExecutor = new PartTreeExecutor(tree, defaultRowMapper, jdbcOperations, context,
                queryMethod.getResultProcessor().getReturnedType().getDomainType());

        String sql = partTreeExecutor.createQuery();
        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(sql) //
                .startsWith("SELECT") //
                .contains("person.person_id AS person_id,") //
                .contains("person.last_name AS last_name,") //
                .contains("person.first_name AS first_name,") //
                .contains("animal.nick_name AS animal_nick_name,") //
                .contains("animal.person_id AS animal_person_id") //
                .contains("FROM person LEFT OUTER JOIN animal AS animal ON animal.person = person.person_id WHERE person.firstName = :firstName AND person.lastName = :lastName"); //
        softAssertions.assertAll();
    }

    List<Person> findByFirstName(String firstName) {
        return null;
    }

    List<Person> findByFirstNameAndLastName(String firstName, String lastName) {
        return null;
    }

    List<Person> findByFirstNameAndLastNameAndEmail(String firstName, String lastName, String email) {
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
