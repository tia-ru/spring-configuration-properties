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

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.configurationprocessor.fieldvalues.FieldValuesParser;
import org.springframework.configurationprocessor.fieldvalues.javac.JavaCompilerFieldValuesParser;
import org.springframework.configurationprocessor.helpers.PropertyPlaceholderHelper;
import org.springframework.configurationprocessor.metadata.ItemDeprecation;

/**
 * Provide utilities to detect and validate configuration properties.
 *
 * @author Stephane Nicoll
 */
class MetadataGenerationEnvironment {

	private static final String NULLABLE_ANNOTATION = "org.springframework.lang.Nullable";

	private static final String VALUE_SEPARATOR = ":";
	private static final PropertyPlaceholderHelper PROPERTY_HELPER = new PropertyPlaceholderHelper("${", "}", VALUE_SEPARATOR, true);

	private static final Set<String> TYPE_EXCLUDES;
	static {
		Set<String> excludes = new HashSet<>();
		excludes.add("com.zaxxer.hikari.IConnectionCustomizer");
		excludes.add("groovy.lang.MetaClass");
		excludes.add("groovy.text.markup.MarkupTemplateEngine");
		excludes.add("java.io.Writer");
		excludes.add("java.io.PrintWriter");
		excludes.add("java.lang.ClassLoader");
		excludes.add("java.util.concurrent.ThreadFactory");
		excludes.add("javax.jms.XAConnectionFactory");
		excludes.add("javax.sql.DataSource");
		excludes.add("javax.sql.XADataSource");
		excludes.add("org.apache.tomcat.jdbc.pool.PoolConfiguration");
		excludes.add("org.apache.tomcat.jdbc.pool.Validator");
		excludes.add("org.flywaydb.core.api.callback.FlywayCallback");
		excludes.add("org.flywaydb.core.api.resolver.MigrationResolver");
		TYPE_EXCLUDES = Collections.unmodifiableSet(excludes);
	}

	private final TypeUtils typeUtils;

	private final Elements elements;

	private final Messager messager;

	private final FieldValuesParser fieldValuesParser;

	private final Map<TypeElement, Map<String, Object>> defaultValues = new HashMap<>();


    MetadataGenerationEnvironment(ProcessingEnvironment environment) {
		this.typeUtils = new TypeUtils(environment);
		this.elements = environment.getElementUtils();
		this.messager = environment.getMessager();
		this.fieldValuesParser = resolveFieldValuesParser(environment);
    }

	private static FieldValuesParser resolveFieldValuesParser(ProcessingEnvironment env) {
		try {
			return new JavaCompilerFieldValuesParser(env);
		}
		catch (Throwable ex) {
			return FieldValuesParser.NONE;
		}
	}

	TypeUtils getTypeUtils() {
		return this.typeUtils;
	}

	Messager getMessager() {
		return this.messager;
	}

	/**
	 * Return the default value of the field with the specified {@code name}.
	 * @param type the type to consider
	 * @param name the name of the field
	 * @return the default value or {@code null} if the field does not exist or no default
	 * value has been detected
	 */
	Object getFieldDefaultValue(TypeElement type, String name) {
		return this.defaultValues.computeIfAbsent(type, this::resolveFieldValues).get(name);
	}

	boolean isExcluded(TypeMirror type) {
		if (type == null) {
			return false;
		}
		String typeName = type.toString();
		if (typeName.endsWith("[]")) {
			typeName = typeName.substring(0, typeName.length() - 2);
		}
		return TYPE_EXCLUDES.contains(typeName);
	}

	boolean isDeprecated(Element element) {
		if (isElementDeprecated(element)) {
			return true;
		}
		if (element instanceof VariableElement || element instanceof ExecutableElement) {
			return isElementDeprecated(element.getEnclosingElement());
		}
		return false;
	}

	ItemDeprecation resolveItemDeprecation(Element element) {
		//AnnotationMirror annotation = getAnnotation(element, this.deprecatedConfigurationPropertyAnnotation);
		String reason = null;
		String replacement = null;
		/*if (annotation != null) {
			Map<String, Object> elementValues = getAnnotationElementValues(annotation);
			reason = (String) elementValues.get("reason");
			replacement = (String) elementValues.get("replacement");
		}
		reason = (reason == null || reason.isEmpty()) ? null : reason;
		replacement = (replacement == null || replacement.isEmpty()) ? null : replacement;*/
		return new ItemDeprecation(reason, replacement);
	}



	boolean hasAnnotation(Element element, String type) {
		return getAnnotation(element, type) != null;
	}

	AnnotationMirror getAnnotation(Element element, String type) {
		if (element != null) {
			for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
				if (type.equals(annotation.getAnnotationType().toString())) {
					return annotation;
				}
			}
		}
		return null;
	}

	/**
	 * Collect the annotations that are annotated or meta-annotated with the specified
	 * {@link TypeElement annotation}.
	 * @param element the element to inspect
	 * @param annotationType the annotation to discover
	 * @return the annotations that are annotated or meta-annotated with this annotation
	 */
	List<Element> getElementsAnnotatedOrMetaAnnotatedWith(Element element, TypeElement annotationType) {
		LinkedList<Element> stack = new LinkedList<>();
		stack.push(element);
		collectElementsAnnotatedOrMetaAnnotatedWith(annotationType, stack);
		stack.removeFirst();
		return Collections.unmodifiableList(stack);
	}

	private boolean hasAnnotationRecursive(Element element, String type) {
		return !getElementsAnnotatedOrMetaAnnotatedWith(element, this.elements.getTypeElement(type)).isEmpty();
	}

	private boolean collectElementsAnnotatedOrMetaAnnotatedWith(TypeElement annotationType, LinkedList<Element> stack) {
		Element element = stack.peekLast();
		for (AnnotationMirror annotation : this.elements.getAllAnnotationMirrors(element)) {
			Element annotationElement = annotation.getAnnotationType().asElement();
			if (!stack.contains(annotationElement)) {
				stack.addLast(annotationElement);
				if (annotationElement.equals(annotationType)) {
					return true;
				}
				if (!collectElementsAnnotatedOrMetaAnnotatedWith(annotationType, stack)) {
					stack.removeLast();
				}
			}
		}
		return false;
	}

	Map<String, Object> getAnnotationElementValues(AnnotationMirror annotation) {
		Map<String, Object> values = new LinkedHashMap<>();
		annotation.getElementValues()
			.forEach((name, value) -> values.put(name.getSimpleName().toString(), getAnnotationValue(value)));
		return values;
	}

	private Object getAnnotationValue(AnnotationValue annotationValue) {
		Object value = annotationValue.getValue();
		if (value instanceof List) {
			List<Object> values = new ArrayList<>();
			((List<?>) value).forEach((v) -> values.add(((AnnotationValue) v).getValue()));
			return values;
		}
		return value;
	}


	/*Set<TypeElement> getSupportedAnnotationElements(){
		return supportedAnnotations.stream()
				.map(elements::getTypeElement)
				.collect(Collectors.toSet());
	}*/


	boolean hasNullableAnnotation(Element element) {
		return getAnnotation(element, NULLABLE_ANNOTATION) != null;
	}

	public List<ValueJavaBeanPropertyDescriptor> extractDescriptors(Element element, AnnotationMirror valueAnnotation) {

		List<ValueJavaBeanPropertyDescriptor> result = new ArrayList<>(4);
		for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> elementValues : valueAnnotation.getElementValues().entrySet()) {
			ExecutableElement property = elementValues.getKey();
			Object value = elementValues.getValue().getValue();
			TypeMirror returnType = property.getReturnType();

			Stream<?> stream = value instanceof Collection ? ((Collection<?>) value).stream()
					: value instanceof String ? Stream.of((String) value)
					: Stream.empty();
			stream
					.map(Object::toString)
					//.map(String.class::cast)
					.flatMap(valueString -> extractPlaceholders(valueString).entrySet().stream())
					.forEach(entry -> {
						String placeHolder = entry.getKey();
						String defVal = entry.getValue();
						ValueJavaBeanPropertyDescriptor descriptor = null;
						if (element instanceof VariableElement) {
							descriptor = new ValueJavaBeanPropertyDescriptor(placeHolder, defVal, (VariableElement) element);
						} else if (element instanceof ExecutableElement) {
							ExecutableElement setter = (ExecutableElement) element;
							if (setter.toString().startsWith("set") && setter.getParameters().size() == 1) {
								descriptor = new ValueJavaBeanPropertyDescriptor(placeHolder, defVal, setter);
							} else if (element.getEnclosingElement() instanceof TypeElement) {
								//в том числе и св-во аннотации
								descriptor = new ValueJavaBeanPropertyDescriptor(placeHolder, defVal, (TypeElement) element.getEnclosingElement(), returnType);
							}
						} else if (element instanceof TypeElement) {
							TypeMirror propType = elements.getTypeElement(String.class.getCanonicalName()).asType();
							descriptor = new ValueJavaBeanPropertyDescriptor(placeHolder, defVal, (TypeElement) element, propType);
						}

						if (descriptor != null) {
							result.add(descriptor);
						}
					});
		}

		/*valueAnnotation.getElementValues()
				.values()
				.stream()
				.map(AnnotationValue::getValue)
				.flatMap(v -> v instanceof Collection ?
						((Collection<?>) v).stream() :
						v instanceof String ? Stream.of((String) v) : Stream.empty())
				//.filter(String.class::isInstance)
				.map(Object::toString)
				//.map(String.class::cast)
				.flatMap(valueString -> extractPlaceholders(valueString).entrySet().stream())
				.forEach(entry -> {
					String placeHolder = entry.getKey();
					String defVal = entry.getValue();
					ValueJavaBeanPropertyDescriptor descriptor = null;
					if (element instanceof VariableElement) {
						descriptor = new ValueJavaBeanPropertyDescriptor(placeHolder, defVal, (VariableElement) element);
					} else if (element instanceof ExecutableElement) {
						ExecutableElement setter = (ExecutableElement) element;
						if (setter.getParameters().size() == 1) {
							descriptor = new ValueJavaBeanPropertyDescriptor(placeHolder, defVal, setter);
						}
					} else if (element instanceof TypeElement) {
						TypeMirror propType = elements.getTypeElement(String.class.getCanonicalName()).asType();
						descriptor = new ValueJavaBeanPropertyDescriptor(placeHolder, defVal, (TypeElement) element, propType);
					}

					if (descriptor != null) {
						result.add(descriptor);
					}
				});*/
		return result;
	}

	private boolean isElementDeprecated(Element element) {
		//return hasAnnotation(element, "java.lang.Deprecated") || hasAnnotation(element, this.deprecatedConfigurationPropertyAnnotation);
		return hasAnnotation(element, "java.lang.Deprecated");
	}

	private Map<String, Object> resolveFieldValues(TypeElement element) {
		Map<String, Object> values = new LinkedHashMap<>();
		resolveFieldValuesFor(values, element);
		return values;
	}

	private void resolveFieldValuesFor(Map<String, Object> values, TypeElement element) {
		try {
			this.fieldValuesParser.getFieldValues(element).forEach((name, value) -> {
				if (!values.containsKey(name)) {
					values.put(name, value);
				}
			});
		}
		catch (Exception ex) {
			// continue
		}
		Element superType = this.typeUtils.asElement(element.getSuperclass());
		if (superType instanceof TypeElement && superType.asType().getKind() != TypeKind.NONE) {
			resolveFieldValuesFor(values, (TypeElement) superType);
		}
	}

	private static Map<String, String> extractPlaceholders(String valueString) {
		return PROPERTY_HELPER.extractPlaceholders(valueString);
	}

}
