/*
 * Copyright 2017-2018 the original author or authors.
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
package org.springframework.data.jdbc.core;

import static java.util.Arrays.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import junit.framework.AssertionFailedError;

import java.util.Collections;

import org.junit.Test;
import org.springframework.data.jdbc.core.FunctionCollector.CombinedDataAccessException;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;

/**
 * Unit tests for {@link CascadingDataAccessStrategy}.
 *
 * @author Jens Schauder
 */
public class CascadingDataAccessStrategyUnitTests {

    int errorIndex = 1;
    String[] errorMessages = {"Sorry I don't support this method. Please try again later", "Still no luck"};

    DataAccessStrategy alwaysFails = mock(DataAccessStrategy.class, i -> {
        errorIndex++;
        errorIndex %= 2;
        throw new UnsupportedOperationException(errorMessages[errorIndex]);
    });
    DataAccessStrategy succeeds = mock(DataAccessStrategy.class);
    DataAccessStrategy mayNotCall = mock(DataAccessStrategy.class, i -> {
        throw new AssertionFailedError("this shouldn't have get called");
    });

    @Test // DATAJDBC-123
    public void findByReturnsFirstSuccess() {

        doReturn("success").when(succeeds).findById(23L, String.class);
        CascadingDataAccessStrategy access = new CascadingDataAccessStrategy(asList(alwaysFails, succeeds, mayNotCall));

        String byId = access.findById(23L, String.class);

        assertThat(byId).isEqualTo("success");
    }

    @Test // DATAJDBC-123
    public void findByFailsIfAllStrategiesFail() {

        CascadingDataAccessStrategy access = new CascadingDataAccessStrategy(asList(alwaysFails, alwaysFails));

        assertThatExceptionOfType(CombinedDataAccessException.class) //
                .isThrownBy(() -> access.findById(23L, String.class)) //
                .withMessageContaining("Failed to perform data access with all available strategies") //
                .withMessageContaining("Sorry I don't support this method") //
                .withMessageContaining("Still no luck");

    }

    @Test // DATAJDBC-123
    public void findByPropertyReturnsFirstSuccess() {

        doReturn(Collections.singletonList("success")).when(succeeds).findAllByProperty(eq(23L),
                any(RelationalPersistentProperty.class));
        CascadingDataAccessStrategy access = new CascadingDataAccessStrategy(asList(alwaysFails, succeeds, mayNotCall));

        Iterable<Object> findAll = access.findAllByProperty(23L, mock(RelationalPersistentProperty.class));

        assertThat(findAll).containsExactly("success");
    }

}
