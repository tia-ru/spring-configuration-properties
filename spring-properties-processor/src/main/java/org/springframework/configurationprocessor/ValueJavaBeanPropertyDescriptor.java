/*
 * Copyright 2012-2019 the original author or authors.
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

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import org.springframework.configurationprocessor.metadata.ItemDeprecation;

/**
 * A {@link PropertyDescriptor} for a standard JavaBean property.
 *
 * @author Stephane Nicoll
 */
class ValueJavaBeanPropertyDescriptor extends PropertyDescriptor<Element> {

	private final String defaultValue;

	protected ValueJavaBeanPropertyDescriptor(String name, String defaultValue, VariableElement vari) {
		super(vari.getEnclosingElement() instanceof TypeElement
						? (TypeElement) vari.getEnclosingElement() // class field
						: (TypeElement) vari.getEnclosingElement().getEnclosingElement(), //method parameter
				null, vari, name, vari.asType(), vari, null, null);
        this.defaultValue = defaultValue;
	}

	protected ValueJavaBeanPropertyDescriptor(String name, String defaultValue, ExecutableElement setter) {
		super((TypeElement) setter.getEnclosingElement(), null, setter, name, setter.getParameters().get(0).asType(),
				setter.getParameters().get(0), setter, setter);
        this.defaultValue = defaultValue;
	}

	protected ValueJavaBeanPropertyDescriptor(String name, String defaultValue, TypeElement clazz, TypeMirror propType) {
		super(clazz, null, clazz, name, propType,
				null, null, null);
		this.defaultValue = defaultValue;
	}


	@Override
	protected boolean isProperty(MetadataGenerationEnvironment env) {
		/*boolean isCollection = env.getTypeUtils().isCollectionOrMap(getType());
		return !env.isExcluded(getType()) && (getSetter() != null || isCollection);*/
		return true;
	}

	@Override
	protected Object resolveDefaultValue(MetadataGenerationEnvironment environment) {
		//return environment.getFieldDefaultValue(getOwnerElement(), getName());
		return defaultValue;
	}

	@Override
	protected ItemDeprecation resolveItemDeprecation(MetadataGenerationEnvironment environment) {
		boolean deprecated = environment.isDeprecated(getGetter()) || environment.isDeprecated(getSetter())
				|| environment.isDeprecated(getField()) || environment.isDeprecated(getFactoryMethod());
		return deprecated ? environment.resolveItemDeprecation(getGetter()) : null;
	}
}
