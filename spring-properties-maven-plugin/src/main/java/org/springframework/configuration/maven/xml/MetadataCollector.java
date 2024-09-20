/*
 * Copyright 2012-2020 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.configurationprocessor.metadata.ItemMetadata;

/**
 * Used by {@ link ConfigurationMetadataAnnotationProcessor} to collect
 * {@ link ConfigurationMetadata}.
 *
 * @author Andy Wilkinson
 * @author Kris De Volder
 * @since 1.2.2
 */
public class MetadataCollector {

	private final Set<ItemMetadata> metadataItems = new LinkedHashSet<>();

	private final ConfigurationMetadata previousMetadata;


	/**
	 * Creates a new {@code MetadataProcessor} instance.
	 * @param previousMetadata any previous metadata or {@code null}
	 */
	public MetadataCollector( ConfigurationMetadata previousMetadata) {
		this.previousMetadata = previousMetadata;
	}


	public void add(ItemMetadata metadata) {
		this.metadataItems.add(metadata);
	}

	public void add(Collection<ItemMetadata> metadata) {
		this.metadataItems.addAll(metadata);
	}

	public boolean hasSimilarGroup(ItemMetadata metadata) {
		if (!metadata.isOfItemType(ItemMetadata.ItemType.GROUP)) {
			throw new IllegalStateException("item " + metadata + " must be a group");
		}
		for (ItemMetadata existing : this.metadataItems) {
			if (existing.isOfItemType(ItemMetadata.ItemType.GROUP) && existing.getName().equals(metadata.getName())
					&& existing.getType().equals(metadata.getType())) {
				return true;
			}
		}
		return false;
	}

	public ConfigurationMetadata getMetadata() {
		ConfigurationMetadata metadata = new ConfigurationMetadata();
		for (ItemMetadata item : this.metadataItems) {
			metadata.add(item);
		}
		if (this.previousMetadata != null) {
			List<ItemMetadata> items = this.previousMetadata.getItems();
			for (ItemMetadata item : items) {
				if (shouldBeMerged(item)) {
					metadata.addIfMissing(item);
				}
			}
		}
		return metadata;
	}

	public Set<ItemMetadata> generateGroups(){
		Map<String,Set<ItemMetadata>> byClass = new HashMap<>(32);
		for (ItemMetadata metadata : this.metadataItems) {
			if (metadata.isOfItemType(ItemMetadata.ItemType.PROPERTY)) {
				byClass.computeIfAbsent(metadata.getSourceType(), k -> new HashSet<>()).add(metadata);
			}
		}
		Set<ItemMetadata> groupsMetadata = new HashSet<>();
		for (Set<ItemMetadata> byClassItems : byClass.values()) {
			String sourceType = byClassItems.iterator().next().getSourceType();
			Set<String> commonPrefixes = calculateCommonPrefixes(byClassItems);
			for (String prefix : commonPrefixes) {
				ItemMetadata group = ItemMetadata.newGroup(prefix, sourceType, sourceType, null);
				groupsMetadata.add(group);
			}
		}
		return groupsMetadata;
	}

	public Set<ItemMetadata> generateBlankGroups(){
		Set<ItemMetadata> groupsMetadata = new HashSet<>();
		Set<String> seenGroups = new HashSet<>(32);
		for (ItemMetadata metadata : this.metadataItems) {
			if (metadata.isOfItemType(ItemMetadata.ItemType.PROPERTY)) {
				String sourceType = metadata.getSourceType();
				if (seenGroups.add(sourceType)){
					ItemMetadata group = ItemMetadata.newGroup("", sourceType, sourceType, null);
					groupsMetadata.add(group);
				}
			}
		}
		return groupsMetadata;
	}

	public Set<ItemMetadata> generateUntypedGroups(){
		Set<ItemMetadata> groupsMetadata = new HashSet<>();
		Set<String> seenGroups = new HashSet<>(32);
		for (ItemMetadata metadata : this.metadataItems) {
			if (metadata.isOfItemType(ItemMetadata.ItemType.PROPERTY)) {
				String sourceType = metadata.getSourceType();
				if (seenGroups.add(sourceType)){
					ItemMetadata group = ItemMetadata.newGroup(sourceType, sourceType, ".", null);
					groupsMetadata.add(group);
				}
			}
		}
		return groupsMetadata;
	}

	private Set<String> calculateCommonPrefixes(Set<ItemMetadata> byClassItems) {
		Set<String> commonPrefixes = new HashSet<>();
		Map<String, List<String>> by1lvl = new HashMap<>(4);
		for (ItemMetadata item : byClassItems) {
			String lelev1Group = item.getName();
			int idx = lelev1Group.indexOf('.');
            if (idx < 0) {
                lelev1Group = "";
            } else {
                lelev1Group = lelev1Group.substring(0, idx);
            }
            by1lvl.computeIfAbsent(lelev1Group, k -> new ArrayList<>()).add(item.getName());
		}
		for (Map.Entry<String, List<String>> entry : by1lvl.entrySet()) {
			commonPrefixes.add(collapse(entry.getValue()));
		}

		return commonPrefixes;
	}

	private String collapse(List<String> prefixes) {
		String longestPefix = prefixes.get(0);
        for (int i = 1; i < prefixes.size(); i++) {
            longestPefix = intersect(longestPefix, prefixes.get(i));
        }
		return longestPefix;
	}

	private String intersect(String s1, String s2) {
		s1 += '.';
		s2 += '.';
		int len = Math.min(s1.length(), s2.length());
		int lastDotIdx = 0;
		for (int i = 0; i < len; i++) {
			if (s1.charAt(i) != s2.charAt(i)) {
				break;
			}
			if (s1.charAt(i) == '.') {
				lastDotIdx = i;
			}
		}
		return s1.substring(0, lastDotIdx);
	}

	private boolean shouldBeMerged(ItemMetadata itemMetadata) {
		String sourceType = itemMetadata.getSourceType();
		//return (sourceType != null && !deletedInCurrentBuild(sourceType) && !processedInCurrentBuild(sourceType));
		return !sourceType.endsWith(".xml");
	}

}
