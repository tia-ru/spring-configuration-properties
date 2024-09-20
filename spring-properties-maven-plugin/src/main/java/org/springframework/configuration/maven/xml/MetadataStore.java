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

package org.springframework.configuration.maven.xml;

import javax.tools.Diagnostic;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.configurationprocessor.metadata.InvalidConfigurationMetadataException;
import org.springframework.configurationprocessor.metadata.JsonMarshaller;

/**
 * A {@code MetadataStore} is responsible for the storage of metadata on the filesystem.
 *
 * @author Andy Wilkinson
 * @since 1.2.2
 */
public class MetadataStore {

	static final String METADATA_PATH = "spring-configuration-metadata.json";

    private final Path outputDir;

    public MetadataStore(Path outputDir) {
        this.outputDir = outputDir;
	}

	public ConfigurationMetadata readMetadata() {
		try {
			BufferedInputStream stream = new BufferedInputStream(Files.newInputStream(getMetadataResource()));
			return readMetadata(stream);
		}
		catch (IOException ex) {
			return null;
		}
	}

	public void writeMetadata(ConfigurationMetadata metadata) throws IOException {
		if (!metadata.getItems().isEmpty()) {
			Path path = getMetadataResource();
			path.getParent().toFile().mkdirs();
			try (OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(path))) {
				new JsonMarshaller().write(metadata, outputStream);
			}
		}
	}

	private ConfigurationMetadata readMetadata(InputStream in) throws IOException {
		try {
			return new JsonMarshaller().read(in);
		}
		catch (IOException ex) {
			return null;
		}
		catch (Exception ex) {
			throw new InvalidConfigurationMetadataException(
					"Invalid additional meta-data in '" + METADATA_PATH + "': " + ex.getMessage(),
					Diagnostic.Kind.ERROR);
		}
		finally {
			in.close();
		}
	}

	private Path getMetadataResource() throws IOException {
		return outputDir.resolve(METADATA_PATH);
	}
}
