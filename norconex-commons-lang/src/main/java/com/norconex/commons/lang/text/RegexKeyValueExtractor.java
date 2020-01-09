/* Copyright 2017-2020 Norconex Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.norconex.commons.lang.text;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Matcher;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.map.PropertySetter;

/**
 * <p>
 * Simplify extraction of key/value pairs from text using regular expression.
 * Match groups can be used to identify the key and values.
 * Key matching is optional and can be set explicitly instead. If
 * both a key and a key group are provided, the key act as a default
 * when no keys could be obtained from matching.
 * At least one of "key" or "keyGroup" must be specified. If keyGroup
 * is specified without a "key" and finds no matches, the matching
 * of the value is ignored.
 * If no value group is provided, it assumes the entire regex match is the
 * value.
 * If more than one value is extracted for a given key, they will be
 * available as a list.
 * </p>
 * <p>
 * When initialized with a "pattern" only instead of passing or configuring
 * a {@link Regex} instance, a default one will be created, assuming
 * case insensitivity and dots matching any character.
 * </p>
 *
 * @author Pascal Essiembre
 * @since 2.0.0 (moved from Norconex Importer RegexFieldExtractor)
 */
public class RegexKeyValueExtractor {

    private static final Logger LOG =
            LoggerFactory.getLogger(RegexKeyValueExtractor.class);

    public static final RegexKeyValueExtractor[] EMPTY_ARRAY =
            new RegexKeyValueExtractor[] {};

    private Regex regex;
    private String key;
    private int keyGroup = -1;
    private int valueGroup = -1;
    private PropertySetter onSet;

    public RegexKeyValueExtractor() {
        super();
        this.regex = defaultRegex(null);
    }
    public RegexKeyValueExtractor(String pattern) {
        this(defaultRegex(pattern), null);
    }
    public RegexKeyValueExtractor(String pattern, String key) {
        this(defaultRegex(pattern), key, -1);
    }
    public RegexKeyValueExtractor(String pattern, String key, int valueGroup) {
        this(defaultRegex(pattern), key, valueGroup);
    }
    public RegexKeyValueExtractor(
            String pattern, int keyGroup, int valueGroup) {
        this(defaultRegex(pattern), keyGroup, valueGroup);
    }
    public RegexKeyValueExtractor(Regex regex) {
        this(regex, null);
    }
    public RegexKeyValueExtractor(Regex regex, String key) {
        this(regex, key, -1);
    }
    public RegexKeyValueExtractor(Regex regex, String key, int valueGroup) {
        this.regex = regex;
        this.key = key;
        this.valueGroup = valueGroup;
    }
    public RegexKeyValueExtractor(Regex regex, int keyGroup, int valueGroup) {
        super();
        this.regex = regex;
        this.keyGroup = keyGroup;
        this.valueGroup = valueGroup;
    }

    public Regex getRegex() {
        return regex;
    }
    public RegexKeyValueExtractor setRegex(Regex regex) {
        Objects.requireNonNull(regex, "'regex' must not be null.");
        this.regex = regex;
        return this;
    }

    // get/setPattern for convenience: really have them??
    public String getPattern() {
        return regex.getPattern();
    }
    public RegexKeyValueExtractor setPattern(String pattern) {
        this.regex.setPattern(pattern);
        return this;
    }

    // Keey for convenience
    public boolean isCaseSensitive() {
        return !regex.isCaseInsensitive();
    }
    public RegexKeyValueExtractor setCaseSensitive(boolean caseSensitive) {
        regex.setCaseInsensitive(!caseSensitive);
        return this;
    }

    public int getKeyGroup() {
        return keyGroup;
    }
    public RegexKeyValueExtractor setKeyGroup(int keyGroup) {
        this.keyGroup = keyGroup;
        return this;
    }
    public int getValueGroup() {
        return valueGroup;
    }
    public RegexKeyValueExtractor setValueGroup(int valueGroup) {
        this.valueGroup = valueGroup;
        return this;
    }
    public String getKey() {
        return key;
    }
    public RegexKeyValueExtractor setKey(String key) {
        this.key = key;
        return this;
    }

    /**
     * Gets the property setter to use when a value is set.
     * @return property setter
     * @since 3.0.0
     */
    public PropertySetter getOnSet() {
        return onSet;
    }
    /**
     * Sets the property setter to use when a value is set.
     * @param onSet property setter
     * @return this instance
     * @since 3.0.0
     */
    public RegexKeyValueExtractor setOnSet(PropertySetter onSet) {
        this.onSet = onSet;
        return this;
    }

    public void extractKeyValues(Properties dest, CharSequence text) {
        if (StringUtils.isBlank(key) && !hasKeyGroup()) {
            throw new IllegalArgumentException(
                    "At least one of 'key' or 'keyGroup' expected.");
        }

        Properties extractedKeyValues = new Properties();
        Matcher m = matcher(text);
        while (m.find()) {
            String k = extractKey(m);
            String v = extractValue(m);
            if (StringUtils.isBlank(k)) {
                LOG.debug("No key for value: {}", v);
            } else if (v == null) {
                LOG.debug("Null value for key: {}", k);
            } else {
                extractedKeyValues.add(k, v);
            }
        }
        for (Entry<String,List<String>> en : extractedKeyValues.entrySet()) {
            PropertySetter.orDefault(onSet).apply(
                    dest, en.getKey(), en.getValue());
        }
    }
    public Properties extractKeyValues(CharSequence text) {
        Properties dest = new Properties();
        extractKeyValues(dest, text);
        return dest;
    }

    public static void extractKeyValues(Properties dest,
            CharSequence text, List<RegexKeyValueExtractor> extractors) {
        if (extractors == null) {
            return;
        }
        for (RegexKeyValueExtractor extractor : extractors) {
            extractor.extractKeyValues(dest, text);
        }
    }

    public static Properties extractKeyValues(
            CharSequence text, List<RegexKeyValueExtractor> extractors) {
        Properties dest = new Properties();
        extractKeyValues(dest, text, extractors);
        return dest;
    }

    public static void extractKeyValues(Properties dest,
            CharSequence text, RegexKeyValueExtractor... extractors) {
        if (ArrayUtils.isEmpty(extractors)) {
            return;
        }
        extractKeyValues(dest, text, Arrays.asList(extractors));
    }

    public static Properties extractKeyValues(
            CharSequence text, RegexKeyValueExtractor... extractors) {
        Properties dest = new Properties();
        extractKeyValues(dest, text, extractors);
        return dest;
    }

    private static Regex defaultRegex(String pattern) {
        return new Regex(pattern).dotAll().caseInsensitive();
    }

    private Matcher matcher(CharSequence text) {
        return regex.matcher(text);
    }
    private String extractKey(Matcher m) {
        String f = StringUtils.isNotBlank(getKey()) ? getKey() : "";
        if (hasKeyGroup()) {
            if (m.groupCount() < getKeyGroup()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("No match group {} for key in regex \"{}\""
                            + "for match value \"{}\". Defaulting to key: "
                            + "\"{}\".",
                            getKeyGroup(), getRegex(), m.group(), key);
                }
            } else {
                f = m.group(getKeyGroup());
            }
        }
        return f;
    }
    private String extractValue(Matcher m) {
        if (hasValueGroup()) {
            if (m.groupCount() < getValueGroup()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("No match group {} "
                            + "for value in regex \"{}\" "
                            + "for match value \"{}\". "
                            + "Defaulting to entire match.",
                            getValueGroup(), getRegex(), m.group());
                }
            } else {
                return m.group(getValueGroup());
            }
        }
        return m.group();
    }
    private boolean hasKeyGroup() {
        return getKeyGroup() > -1;
    }
    private boolean hasValueGroup() {
        return getValueGroup() > -1;
    }

    @Override
    public boolean equals(final Object other) {
        return EqualsBuilder.reflectionEquals(this, other);
    }
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
    @Override
    public String toString() {
        return new ReflectionToStringBuilder(
                this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
    }
}
