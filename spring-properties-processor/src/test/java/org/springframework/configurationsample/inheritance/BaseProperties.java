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

package org.springframework.configurationsample.inheritance;

public class BaseProperties {

	private boolean boolValue;

	private int intValue;

	private final Nest nest = new Nest();

	public boolean isBoolValue() {
		return this.boolValue;
	}

	public void setBoolValue(boolean boolValue) {
		this.boolValue = boolValue;
	}

	public int getIntValue() {
		return this.intValue;
	}

	public void setIntValue(int intValue) {
		this.intValue = intValue;
	}

	public Nest getNest() {
		return this.nest;
	}

	public static class Nest {

		private boolean boolValue;

		private int intValue;

		public boolean isBoolValue() {
			return this.boolValue;
		}

		public void setBoolValue(boolean boolValue) {
			this.boolValue = boolValue;
		}

		public int getIntValue() {
			return this.intValue;
		}

		public void setIntValue(int intValue) {
			this.intValue = intValue;
		}

	}

}
