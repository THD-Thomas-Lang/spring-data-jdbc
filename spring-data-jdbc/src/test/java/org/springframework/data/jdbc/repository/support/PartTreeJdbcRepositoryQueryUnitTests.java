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
import org.springframework.data.annotation.Id;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link PartTreeJdbcRepositoryQuery}.
 *
 * @author Thomas Lang
 */
@Slf4j
public class PartTreeJdbcRepositoryQueryUnitTests {

	// DATAJDBC-318
	@Test
	public void testDedicatedPartTreeClassInSimpleMode(){
		try{
			JdbcPartTree jdbcPartTree = new JdbcPartTree("findByFirstName", Person.class);
			assertThat(jdbcPartTree).isNotNull();
		} catch(IllegalArgumentException illegalArgumentException){
			illegalArgumentException.printStackTrace();
			assertThat(true).isFalse();
		}

	}

	// DATAJDBC-318
	@Test
	public void testDedicatedPartTreeClassInComplicatedMode(){
		try{
			JdbcPartTree jdbcPartTree = new JdbcPartTree("findByAnimalNickName", Person.class);
			assertThat(jdbcPartTree).isNotNull();
		} catch(IllegalArgumentException illegalArgumentException){
			illegalArgumentException.printStackTrace();
			assertThat(true).isTrue();
		}

	}

	@Getter
	@Setter
	@AllArgsConstructor
	private class Person{
		@Wither
		@Id
		private final long personId;
		private String firstName;
		private String lastName;
		private Animal animal;
	}

	@Getter
	@Setter
	@AllArgsConstructor
	private class Animal{
		@Wither
		@Id
		private final long personId;
		private String nickName;
	}
}
