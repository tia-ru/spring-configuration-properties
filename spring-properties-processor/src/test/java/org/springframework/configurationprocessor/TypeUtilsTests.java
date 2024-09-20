/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.configurationprocessor;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.configurationprocessor.TypeUtils.TypeDescriptor;
import org.springframework.configurationprocessor.test.RoundEnvironmentTester;
import org.springframework.configurationprocessor.test.TestableAnnotationProcessor;
import org.springframework.configurationsample.generic.AbstractGenericProperties;
import org.springframework.configurationsample.generic.AbstractIntermediateGenericProperties;
import org.springframework.configurationsample.generic.SimpleGenericProperties;
import org.springframework.testsupport.compiler.TestCompiler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TypeUtils}.
 *
 * @author Stephane Nicoll
 */
class TypeUtilsTests {

	@TempDir
	File tempDir;

	@Test
	void resolveTypeDescriptorOnConcreteClass() throws IOException {
		process(SimpleGenericProperties.class, (roundEnv, typeUtils) -> {
			TypeDescriptor typeDescriptor = typeUtils
				.resolveTypeDescriptor(roundEnv.getRootElement(SimpleGenericProperties.class));
			assertThat(typeDescriptor.getGenerics().keySet().stream().map(Object::toString)).containsOnly("A", "B",
					"C");
			assertThat(typeDescriptor.resolveGeneric("A")).hasToString(String.class.getName());
			assertThat(typeDescriptor.resolveGeneric("B")).hasToString(Integer.class.getName());
			assertThat(typeDescriptor.resolveGeneric("C")).hasToString(Duration.class.getName());

		});
	}

	@Test
	void resolveTypeDescriptorOnIntermediateClass() throws IOException {
		process(AbstractIntermediateGenericProperties.class, (roundEnv, typeUtils) -> {
			TypeDescriptor typeDescriptor = typeUtils
				.resolveTypeDescriptor(roundEnv.getRootElement(AbstractIntermediateGenericProperties.class));
			assertThat(typeDescriptor.getGenerics().keySet().stream().map(Object::toString)).containsOnly("A", "B",
					"C");
			assertThat(typeDescriptor.resolveGeneric("A")).hasToString(String.class.getName());
			assertThat(typeDescriptor.resolveGeneric("B")).hasToString(Integer.class.getName());
			assertThat(typeDescriptor.resolveGeneric("C")).hasToString("C");
		});
	}

	@Test
	void resolveTypeDescriptorWithOnlyGenerics() throws IOException {
		process(AbstractGenericProperties.class, (roundEnv, typeUtils) -> {
			TypeDescriptor typeDescriptor = typeUtils
				.resolveTypeDescriptor(roundEnv.getRootElement(AbstractGenericProperties.class));
			assertThat(typeDescriptor.getGenerics().keySet().stream().map(Object::toString)).containsOnly("A", "B",
					"C");

		});
	}

	private void process(Class<?> target, BiConsumer<RoundEnvironmentTester, TypeUtils> consumer) throws IOException {
		TestableAnnotationProcessor<TypeUtils> processor = new TestableAnnotationProcessor<>(consumer, TypeUtils::new);
		TestCompiler compiler = new TestCompiler(this.tempDir);
		compiler.getTask(target).call(processor);
	}

}
