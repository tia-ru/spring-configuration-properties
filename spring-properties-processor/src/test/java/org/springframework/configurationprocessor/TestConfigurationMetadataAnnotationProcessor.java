/*
 * Copyright 2012-2022 the original author or authors.
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

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.configurationprocessor.metadata.JsonMarshaller;

/**
 * Test {@link ConfigurationMetadataAnnotationProcessor}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Kris De Volder
 */
/*@SupportedAnnotationTypes({ TestConfigurationMetadataAnnotationProcessor.CONFIGURATION_PROPERTIES_ANNOTATION,
		TestConfigurationMetadataAnnotationProcessor.CONTROLLER_ENDPOINT_ANNOTATION,
		TestConfigurationMetadataAnnotationProcessor.ENDPOINT_ANNOTATION,
		TestConfigurationMetadataAnnotationProcessor.JMX_ENDPOINT_ANNOTATION,
		TestConfigurationMetadataAnnotationProcessor.REST_CONTROLLER_ENDPOINT_ANNOTATION,
		TestConfigurationMetadataAnnotationProcessor.SERVLET_ENDPOINT_ANNOTATION,
		TestConfigurationMetadataAnnotationProcessor.WEB_ENDPOINT_ANNOTATION,
		TestConfigurationMetadataAnnotationProcessor.VALUE_ANNOTATION,
		TestConfigurationMetadataAnnotationProcessor.PROPERTY_SOURCE_ANNOTATION,
		"org.springframework.context.annotation.Configuration" })*/
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class TestConfigurationMetadataAnnotationProcessor extends ConfigurationMetadataAnnotationProcessor {

	public static final String CONFIGURATION_PROPERTIES_ANNOTATION = "org.springframework.configurationsample.ConfigurationProperties";

	public static final String NESTED_CONFIGURATION_PROPERTY_ANNOTATION = "org.springframework.configurationsample.NestedConfigurationProperty";

	public static final String DEPRECATED_CONFIGURATION_PROPERTY_ANNOTATION = "org.springframework.configurationsample.DeprecatedConfigurationProperty";

	public static final String CONSTRUCTOR_BINDING_ANNOTATION = "org.springframework.configurationsample.ConstructorBinding";

	public static final String DEFAULT_VALUE_ANNOTATION = "org.springframework.configurationsample.DefaultValue";

	public static final String CONTROLLER_ENDPOINT_ANNOTATION = "org.springframework.configurationsample.ControllerEndpoint";

	public static final String ENDPOINT_ANNOTATION = "org.springframework.configurationsample.Endpoint";

	public static final String JMX_ENDPOINT_ANNOTATION = "org.springframework.configurationsample.JmxEndpoint";

	public static final String REST_CONTROLLER_ENDPOINT_ANNOTATION = "org.springframework.configurationsample.RestControllerEndpoint";

	public static final String SERVLET_ENDPOINT_ANNOTATION = "org.springframework.configurationsample.ServletEndpoint";

	public static final String WEB_ENDPOINT_ANNOTATION = "org.springframework.configurationsample.WebEndpoint";

	public static final String READ_OPERATION_ANNOTATION = "org.springframework.configurationsample.ReadOperation";

	public static final String NAME_ANNOTATION = "org.springframework.configurationsample.Name";

	public static final String VALUE_ANNOTATION = "org.springframework.configurationsample.tia.Value";
	public static final String PROPERTY_SOURCE_ANNOTATION = "org.springframework.configurationsample.tia.PropertySource";

	static final Set<String> SUPPORTED_ANNOTATIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
			TestConfigurationMetadataAnnotationProcessor.CONFIGURATION_PROPERTIES_ANNOTATION,
			TestConfigurationMetadataAnnotationProcessor.CONTROLLER_ENDPOINT_ANNOTATION,
			TestConfigurationMetadataAnnotationProcessor.ENDPOINT_ANNOTATION,
			TestConfigurationMetadataAnnotationProcessor.JMX_ENDPOINT_ANNOTATION,
			TestConfigurationMetadataAnnotationProcessor.REST_CONTROLLER_ENDPOINT_ANNOTATION,
			TestConfigurationMetadataAnnotationProcessor.SERVLET_ENDPOINT_ANNOTATION,
			TestConfigurationMetadataAnnotationProcessor.WEB_ENDPOINT_ANNOTATION,
			TestConfigurationMetadataAnnotationProcessor.VALUE_ANNOTATION,
			TestConfigurationMetadataAnnotationProcessor.PROPERTY_SOURCE_ANNOTATION,
			"org.springframework.context.annotation.Configuration",
			"org.springframework.configurationsample.tia.RequestMapping",
			"absent.annotation"
	)));

	private ConfigurationMetadata metadata;

	private final File outputLocation;

	public TestConfigurationMetadataAnnotationProcessor(File outputLocation) {
		this.outputLocation = outputLocation;
	}

	@Override
	public synchronized void init(ProcessingEnvironment env) {
		super.init(env);

		this.metadataEnv = new MetadataGenerationEnvironment(env /*configurationPropertiesAnnotation(),
				nestedConfigurationPropertyAnnotation(), deprecatedConfigurationPropertyAnnotation(),
				constructorBindingAnnotation(), defaultValueAnnotation(), endpointAnnotations(),
				readOperationAnnotation(), nameAnnotation(),*/);
	}

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return SUPPORTED_ANNOTATIONS;
	}

	/*@Override
	protected String configurationPropertiesAnnotation() {
		return CONFIGURATION_PROPERTIES_ANNOTATION;
	}

	@Override
	protected String nestedConfigurationPropertyAnnotation() {
		return NESTED_CONFIGURATION_PROPERTY_ANNOTATION;
	}

	@Override
	protected String deprecatedConfigurationPropertyAnnotation() {
		return DEPRECATED_CONFIGURATION_PROPERTY_ANNOTATION;
	}

	@Override
	protected String constructorBindingAnnotation() {
		return CONSTRUCTOR_BINDING_ANNOTATION;
	}

	@Override
	protected String defaultValueAnnotation() {
		return DEFAULT_VALUE_ANNOTATION;
	}

	@Override
	protected Set<String> endpointAnnotations() {
		return new HashSet<>(Arrays.asList(CONTROLLER_ENDPOINT_ANNOTATION, ENDPOINT_ANNOTATION, JMX_ENDPOINT_ANNOTATION,
				REST_CONTROLLER_ENDPOINT_ANNOTATION, SERVLET_ENDPOINT_ANNOTATION, WEB_ENDPOINT_ANNOTATION));
	}

	@Override
	protected String readOperationAnnotation() {
		return READ_OPERATION_ANNOTATION;
	}

	@Override
	protected String nameAnnotation() {
		return NAME_ANNOTATION;
	}*/

	/*@Override
	protected String valueAnnotation() {
		return VALUE_ANNOTATION;
	}*/

	@Override
	protected ConfigurationMetadata writeMetadata() throws Exception {
		super.writeMetadata();
		try {
			File metadataFile = new File(this.outputLocation, "META-INF/spring-configuration-metadata.json");
			if (metadataFile.isFile()) {
				try (InputStream input = new FileInputStream(metadataFile)) {
					this.metadata = new JsonMarshaller().read(input);
				}
			}
			else {
				this.metadata = new ConfigurationMetadata();
			}
			return this.metadata;
		}
		catch (IOException ex) {
			throw new RuntimeException("Failed to read metadata from disk", ex);
		}
	}

	public ConfigurationMetadata getMetadata() {
		return this.metadata;
	}

}
