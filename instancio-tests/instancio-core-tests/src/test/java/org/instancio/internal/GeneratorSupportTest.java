/*
 * Copyright 2022-2023 the original author or authors.
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
package org.instancio.internal;

import org.instancio.generator.Generator;
import org.instancio.generator.GeneratorContext;
import org.instancio.internal.generator.lang.BooleanGenerator;
import org.instancio.internal.generator.lang.EnumGenerator;
import org.instancio.internal.generator.lang.IntegerGenerator;
import org.instancio.internal.generator.lang.StringGenerator;
import org.instancio.internal.generator.util.CollectionGenerator;
import org.instancio.internal.generator.util.EnumSetGenerator;
import org.instancio.internal.generator.util.MapGenerator;
import org.instancio.internal.random.DefaultRandom;
import org.instancio.settings.Settings;
import org.instancio.test.support.pojo.generics.foobarbaz.Foo;
import org.instancio.test.support.pojo.person.Gender;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.internal.GeneratorSupport.supports;

class GeneratorSupportTest {

    private final GeneratorContext context = new GeneratorContext(Settings.defaults(), new DefaultRandom());
    private final Generator<?> booleanGenerator = new BooleanGenerator(context);
    private final Generator<?> integerGenerator = new IntegerGenerator(context);
    private final Generator<?> stringGenerator = new StringGenerator(context);
    private final Generator<?> enumGenerator = new EnumGenerator<>(context, Gender.class);
    private final Generator<?> enumSetGenerator = new EnumSetGenerator<>(context, Gender.class);
    private final Generator<?> collectionGenerator = new CollectionGenerator<>(context);
    private final MapGenerator<?, ?> mapGenerator = new MapGenerator<>(context);

    @Test
    void supportsIsTrue() {
        assertSupportsAll(booleanGenerator, boolean.class, Boolean.class);
        assertSupportsAll(integerGenerator, int.class, Integer.class);
        assertSupportsAll(enumGenerator, Gender.class);
        assertSupportsAll(enumSetGenerator, EnumSet.class);
        assertSupportsAll(collectionGenerator, Collection.class, List.class, Set.class, SortedSet.class, HashSet.class);
        assertSupportsAll(mapGenerator, Map.class, TreeMap.class, HashMap.class);
        assertSupportsAll(stringGenerator, String.class);
    }

    @Test
    void supportsIsFalse() {
        assertSupportsNone(booleanGenerator, Object.class, boolean[].class);
        assertSupportsNone(integerGenerator, long.class, Number.class);
        assertSupportsNone(enumGenerator, Object.class);
        assertSupportsNone(collectionGenerator, Object.class, Map.class);
        assertSupportsNone(enumSetGenerator, Object.class, Foo.class, Gender.class, Collection.class);
        assertSupportsNone(mapGenerator, Object.class, Collection.class);
        assertSupportsNone(stringGenerator, CharSequence.class, StringBuilder.class);
    }

    private static void assertSupportsAll(final Generator<?> generator, final Class<?>... types) {
        Stream.of(types).forEach(type -> assertThat(supports(generator, type))
                .as("Expected '%s' to support '%s'",
                        generator.getClass().getTypeName(), type.getTypeName())
                .isTrue());
    }

    private static void assertSupportsNone(final Generator<?> generator, final Class<?>... types) {
        Stream.of(types).forEach(type -> assertThat(supports(generator, type))
                .as("Expected '%s' to NOT support '%s'",
                        generator.getClass().getTypeName(), type.getTypeName())
                .isFalse());
    }
}
