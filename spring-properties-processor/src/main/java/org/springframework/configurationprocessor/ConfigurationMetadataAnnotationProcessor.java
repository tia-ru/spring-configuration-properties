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

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.configurationprocessor.metadata.InvalidConfigurationMetadataException;
import org.springframework.configurationprocessor.metadata.ItemMetadata;
import org.springframework.configurationprocessor.xml.project_layout.GradleProjectLayout;
import org.springframework.configurationprocessor.xml.project_layout.IdeaProjectLayout;
import org.springframework.configurationprocessor.xml.project_layout.MavenProjectLayout;
import org.springframework.configurationprocessor.xml.project_layout.ProjectLayout;

/**
 * Annotation {@link Processor} that writes meta-data file for
 * {@code @ConfigurationProperties}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Kris De Volder
 * @author Jonas Keßler
 * @author Ilia Tugushev
 * @since 1.2.0
 */
/*@SupportedAnnotationTypes(
		{ ConfigurationMetadataAnnotationProcessor.AUTO_CONFIGURATION_ANNOTATION,
		ConfigurationMetadataAnnotationProcessor.CONFIGURATION_PROPERTIES_ANNOTATION,
		ConfigurationMetadataAnnotationProcessor.CONTROLLER_ENDPOINT_ANNOTATION,
		ConfigurationMetadataAnnotationProcessor.ENDPOINT_ANNOTATION,
		ConfigurationMetadataAnnotationProcessor.JMX_ENDPOINT_ANNOTATION,
		ConfigurationMetadataAnnotationProcessor.REST_CONTROLLER_ENDPOINT_ANNOTATION,
		ConfigurationMetadataAnnotationProcessor.SERVLET_ENDPOINT_ANNOTATION,
		ConfigurationMetadataAnnotationProcessor.WEB_ENDPOINT_ANNOTATION,
		"org.springframework.context.annotation.Configuration",

		ConfigurationMetadataAnnotationProcessor.VALUE_ANNOTATION,
		ConfigurationMetadataAnnotationProcessor.PROPERTY_SOURCE_ANNOTATION
})*/

public class ConfigurationMetadataAnnotationProcessor extends AbstractProcessor {

	static final String OPTION_ADDITIONAL_METADATA_LOCATIONS = "org.springframework.configurationprocessor.additionalMetadataLocations";

	private static final Set<String> SUPPORTED_ANNOTATIONS = Set.of(
			"org.springframework.*"
			/*"org.springframework.beans.factory.annotation.Value",
			"org.springframework.context.annotation.PropertySource",
			"org.springframework.scheduling.annotation.Scheduled",
			"org.springframework.web.bind.annotation.PathVariable",
			"org.springframework.web.bind.annotation.RequestAttribute",
			"org.springframework.web.bind.annotation.RequestHeader",
			"org.springframework.web.bind.annotation.RequestParam",
			"org.springframework.web.bind.annotation.MatrixVariable",
			"org.springframework.web.bind.annotation.SessionAttribute",
			"org.springframework.web.bind.annotation.CookieValue",
			"org.springframework.web.bind.annotation.RequestMapping",
			"org.springframework.web.bind.annotation.GetMapping",
			"org.springframework.web.bind.annotation.DeleteMapping",
			"org.springframework.web.bind.annotation.PatchMapping",
			"org.springframework.web.bind.annotation.PostMapping",
			"org.springframework.web.bind.annotation.PutMapping",*/
			);

	private static final Set<String> SUPPORTED_OPTIONS = Set.of(OPTION_ADDITIONAL_METADATA_LOCATIONS);

	private MetadataStore metadataStore;

	private MetadataCollector metadataCollector;

	MetadataGenerationEnvironment metadataEnv;


	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		return SUPPORTED_ANNOTATIONS;
	}

	@Override
	public Set<String> getSupportedOptions() {
		return SUPPORTED_OPTIONS;
	}

	@Override
	public synchronized void init(ProcessingEnvironment env) {
		super.init(env);

		this.metadataStore = new MetadataStore(env);
		this.metadataCollector = new MetadataCollector(env, this.metadataStore.readMetadata());
		this.metadataEnv = new MetadataGenerationEnvironment(env);
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

		this.metadataCollector.processing(roundEnv);

		if (!annotations.isEmpty()) {
			/*Set<TypeElement> supportedAnnotationElements = metadataEnv.getSupportedAnnotationElements();
			supportedAnnotationElements.retainAll(annotations);*/
			for (TypeElement annotationElement : annotations) {
				Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotationElement);
				for (Element element : elements) {
					processValueElement(element, annotationElement);
				}
			}
		}

		if (roundEnv.processingOver()) {
			//processXml();
			try {
				//Set<ItemMetadata> groups = metadataCollector.generateGroups();
				Set<ItemMetadata> groups = metadataCollector.generateBlankGroups();
				metadataCollector.add(groups);
				writeMetadata();
			}
			catch (Exception ex) {
				throw new IllegalStateException("Failed to write metadata", ex);
			}
		}
		return false;
	}

	private Map<Element, List<Element>> getElementsAnnotatedOrMetaAnnotatedWith(RoundEnvironment roundEnv,
			TypeElement annotation) {
		Map<Element, List<Element>> result = new LinkedHashMap<>();
		for (Element element : roundEnv.getRootElements()) {
			List<Element> annotations = this.metadataEnv.getElementsAnnotatedOrMetaAnnotatedWith(element, annotation);
			if (!annotations.isEmpty()) {
				result.put(element, annotations);
			}
		}
		return result;
	}


	private void processValueElement(Element element, TypeElement annotationElement) {
		try {

			for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
				Element elementAnnotation = annotation.getAnnotationType().asElement();
				if (elementAnnotation.equals(annotationElement)) {
					metadataEnv.extractDescriptors(element, annotation).forEach( descriptor -> {
						ItemMetadata metadata = descriptor.resolveItemMetadata("", this.metadataEnv);
						if (metadata != null) {
							this.metadataCollector.add(metadata);
						}
					});
				}
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException("Error processing configuration meta-data on " + element, ex);
		}
	}

	/*
	private void processAnnotatedTypeElement(String prefix, TypeElement element, Stack<TypeElement> seen) {
		String type = this.metadataEnv.getTypeUtils().getQualifiedName(element);
		this.metadataCollector.add(ItemMetadata.newGroup(prefix, type, type, null));
		processTypeElement(prefix, element, null, seen);
	}

	private void processExecutableElement(String prefix, ExecutableElement element, Stack<TypeElement> seen) {
		if ((!element.getModifiers().contains(Modifier.PRIVATE))
				&& (TypeKind.VOID != element.getReturnType().getKind())) {
			Element returns = this.processingEnv.getTypeUtils().asElement(element.getReturnType());
			if (returns instanceof TypeElement) {
				ItemMetadata group = ItemMetadata.newGroup(prefix,
						this.metadataEnv.getTypeUtils().getQualifiedName(returns),
						this.metadataEnv.getTypeUtils().getQualifiedName(element.getEnclosingElement()),
						element.toString());
				if (this.metadataCollector.hasSimilarGroup(group)) {
					this.processingEnv.getMessager()
						.printMessage(Kind.ERROR,
								"Duplicate @ConfigurationProperties definition for prefix '" + prefix + "'", element);
				}
				else {
					this.metadataCollector.add(group);
					processTypeElement(prefix, (TypeElement) returns, element, seen);
				}
			}
		}
	}

	private void processTypeElement(String prefix, TypeElement element, ExecutableElement source,
			Stack<TypeElement> seen) {
		if (!seen.contains(element)) {
			seen.push(element);
			new PropertyDescriptorResolver(this.metadataEnv).resolve(element, source).forEach((descriptor) -> {
				this.metadataCollector.add(descriptor.resolveItemMetadata(prefix, this.metadataEnv));
				if (descriptor.isNested(this.metadataEnv)) {
					TypeElement nestedTypeElement = (TypeElement) this.metadataEnv.getTypeUtils()
						.asElement(descriptor.getType());
					String nestedPrefix = ConfigurationMetadata.nestedPrefix(prefix, descriptor.getName());
					processTypeElement(nestedPrefix, nestedTypeElement, source, seen);
				}
			});
			seen.pop();
		}
	}

	private void processEndpoint(Element element, List<Element> annotations) {
		try {
			String annotationName = this.metadataEnv.getTypeUtils().getQualifiedName(annotations.get(0));
			AnnotationMirror annotation = this.metadataEnv.getAnnotation(element, annotationName);
			if (element instanceof TypeElement) {
				processEndpoint(annotation, (TypeElement) element);
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException("Error processing configuration meta-data on " + element, ex);
		}
	}

	private void processEndpoint(AnnotationMirror annotation, TypeElement element) {
		Map<String, Object> elementValues = this.metadataEnv.getAnnotationElementValues(annotation);
		String endpointId = (String) elementValues.get("id");
		if (endpointId == null || endpointId.isEmpty()) {
			return; // Can't process that endpoint
		}
		String endpointKey = ItemMetadata.newItemMetadataPrefix("management.endpoint.", endpointId);
		Boolean enabledByDefault = (Boolean) elementValues.get("enableByDefault");
		String type = this.metadataEnv.getTypeUtils().getQualifiedName(element);
		this.metadataCollector.add(ItemMetadata.newGroup(endpointKey, type, type, null));
		this.metadataCollector.add(ItemMetadata.newProperty(endpointKey, "enabled", Boolean.class.getName(), type, null,
				String.format("Whether to enable the %s endpoint.", endpointId),
				(enabledByDefault != null) ? enabledByDefault : true, null));
		if (hasMainReadOperation(element)) {
			this.metadataCollector.add(ItemMetadata.newProperty(endpointKey, "cache.time-to-live",
					Duration.class.getName(), type, null, "Maximum time that a response can be cached.", "0ms", null));
		}
	}*/

/*	private boolean hasMainReadOperation(TypeElement element) {
		for (ExecutableElement method : ElementFilter.methodsIn(element.getEnclosedElements())) {
			if (this.metadataEnv.getReadOperationAnnotation(method) != null
					&& (TypeKind.VOID != method.getReturnType().getKind()) && hasNoOrOptionalParameters(method)) {
				return true;
			}
		}
		return false;
	}*/

	/*private boolean hasNoOrOptionalParameters(ExecutableElement method) {
		for (VariableElement parameter : method.getParameters()) {
			if (!this.metadataEnv.hasNullableAnnotation(parameter)) {
				return false;
			}
		}
		return true;
	}

	private String getPrefix(AnnotationMirror annotation) {
		Map<String, Object> elementValues = this.metadataEnv.getAnnotationElementValues(annotation);
		Object prefix = elementValues.get("prefix");
		if (prefix != null && !"".equals(prefix)) {
			return (String) prefix;
		}
		Object value = elementValues.get("value");
		if (value != null && !"".equals(value)) {
			return (String) value;
		}
		return null;
	}*/

	protected ConfigurationMetadata writeMetadata() throws Exception {
		ConfigurationMetadata metadata = this.metadataCollector.getMetadata();
		metadata = mergeAdditionalMetadata(metadata);
		if (!metadata.getItems().isEmpty()) {
			this.metadataStore.writeMetadata(metadata);
			return metadata;
		}
		return null;
	}

	private ConfigurationMetadata mergeAdditionalMetadata(ConfigurationMetadata metadata) {
		try {
			ConfigurationMetadata merged = new ConfigurationMetadata(metadata);
			merged.merge(this.metadataStore.readAdditionalMetadata());
			return merged;
		}
		catch (FileNotFoundException ex) {
			// No additional metadata
		}
		catch (InvalidConfigurationMetadataException ex) {
			log(ex.getKind(), ex.getMessage());
		}
		catch (Exception ex) {
			logWarning("Unable to merge additional metadata");
			logWarning(getStackTrace(ex));
		}
		return metadata;
	}

	private String getStackTrace(Exception ex) {
		StringWriter writer = new StringWriter();
		ex.printStackTrace(new PrintWriter(writer, true));
		return writer.toString();
	}

	private void logWarning(String msg) {
		log(Kind.WARNING, msg);
	}

	private void log(Kind kind, String msg) {
		this.processingEnv.getMessager().printMessage(kind, msg);
	}

	private ProjectLayout getProjectLayout(ProcessingEnvironment processingEnv) {
		List<ProjectLayout> projectLayouts = List.of(new MavenProjectLayout(), new GradleProjectLayout(), new IdeaProjectLayout());
		for (ProjectLayout layout : projectLayouts) {
			if(layout.support(processingEnv)) {
				return layout;
			}
		}
		return null;
	}

	public List<String> getGradlePossibleMetadataFilePaths() {
		return List.of(
				"/build/resources/main",
				"/resources/main",
				"/src/main/resources"
		);
		/*return List.of(
				"/build/classes/java/main/META-INF/spring-configuration-metadata.json",
				"/classes/java/main/META-INF/spring-configuration-metadata.json",
				"/java/main/META-INF/spring-configuration-metadata.json",
				"/main/META-INF/spring-configuration-metadata.json",
				"/META-INF/spring-configuration-metadata.json",
				"/spring-configuration-metadata.json"
		);*/
	}

	public List<String> getMavenPossibleMetadataFilePaths() {
		return List.of(
				"/target/classes",
				"/src/main/resources"
		);
		/*return List.of(
				"/target/classes/META-INF/spring-configuration-metadata.json",
				"/classes/META-INF/spring-configuration-metadata.json",
				"/META-INF/spring-configuration-metadata.json",
				"/spring-configuration-metadata.json"
		);*/
	}
}
