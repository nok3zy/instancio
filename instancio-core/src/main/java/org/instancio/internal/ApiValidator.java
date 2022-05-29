/*
 *  Copyright 2022 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.instancio.internal;

import org.instancio.Generator;
import org.instancio.TypeTokenSupplier;
import org.instancio.exception.InstancioApiException;
import org.instancio.generators.Generators;
import org.instancio.internal.nodes.Node;
import org.instancio.settings.SettingKey;
import org.instancio.util.Format;
import org.instancio.util.ReflectionUtils;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ApiValidator {

    // Note: include nested generic class in the example as it's used as a validation message for this use case
    private static final String CREATE_TYPE_TOKEN_HELP =
            "%nMap<String, List<Integer>> map = Instancio.create(new TypeToken<Map<String, List<Integer>>>(){});" +
                    "%n%nor the builder version:%n" +
                    "%nMap<String, List<Integer>> map = Instancio.of(new TypeToken<Map<String, List<Integer>>>(){}).create();";

    private static final String CREATE_CLASS_HELP =
            "%nPerson person = Instancio.create(Person.class);" +
                    "%n%nor the builder version:%n" +
                    "%nPerson person = Instancio.of(Person.class).create();";

    public static <T> Class<T> validateRootClass(@Nullable final Class<T> klass) {
        isTrue(klass != null,
                "%nClass must not be null."
                        + "%nProvide a valid class, for example:%n"
                        + CREATE_CLASS_HELP);

        return klass;
    }

    public static Type validateTypeToken(@Nullable final TypeTokenSupplier<?> typeTokenSupplier) {
        isTrue(typeTokenSupplier != null,
                "%nType token supplier must not be null."
                        + "%nProvide a valid type token, for example:%n"
                        + CREATE_TYPE_TOKEN_HELP);

        final Type type = typeTokenSupplier.get();
        isTrue(type != null,
                "%nType token supplier must not return a null Type."
                        + "%nProvide a valid Type, for example:%n"
                        + CREATE_TYPE_TOKEN_HELP);

        return type;
    }

    public static void validateTypeParameters(Class<?> rootClass, List<Class<?>> rootTypeParameters) {
        final int typeVarsLength = rootClass.isArray()
                ? rootClass.getComponentType().getTypeParameters().length
                : rootClass.getTypeParameters().length;

        if (typeVarsLength == 0 && !rootTypeParameters.isEmpty()) {
            final String suppliedParams = Format.paramsToCsv(rootTypeParameters);

            throw new InstancioApiException(String.format(
                    "%nClass '%s' is not generic." +
                            "%nSpecifying type parameters 'withTypeParameters(%s)` is not valid for this class.",
                    rootClass.getName(), suppliedParams));
        }

        isTrue(typeVarsLength == rootTypeParameters.size(),
                "%nClass '%s' has %s type parameters: %s."
                        + "%nSpecify the required type parameters using 'withTypeParameters(Class... types)`",
                rootClass.getName(),
                rootClass.getTypeParameters().length,
                Arrays.toString(rootClass.getTypeParameters()));

        for (Class<?> param : rootTypeParameters) {
            if (param.getTypeParameters().length > 0) {

                final String suppliedParams = Format.paramsToCsv(rootTypeParameters);
                final String classWithTypeParams = String.format("%s<%s>",
                        param.getSimpleName(), Format.getTypeVariablesCsv(param));

                throw new InstancioApiException(String.format(
                        "%n%n'withTypeParameters(%s)` contains a generic class: %s'"
                                + "%n%nFor nested generics use the type token API, for example:"
                                + CREATE_TYPE_TOKEN_HELP,
                        suppliedParams, classWithTypeParams));
            }
        }
    }

    public static void validateSubtype(final Class<?> from, final Class<?> to) {
        isTrue(from.isAssignableFrom(to), "Class '%s' is not a subtype of '%s'", to.getName(), from.getName());
        isFalse(from.equals(to), "Invalid subtype mapping from '%s' to '%s'", to.getName(), from.getName());
        validateConcreteClass(to);
    }

    public static void validateConcreteClass(final Class<?> klass) {
        isTrue(ReflectionUtils.isArrayOrConcrete(klass),
                "Class must not be an interface or abstract class: '%s'", klass.getName());
    }

    public static void validateNotNullAndType(@Nullable final SettingKey key, @Nullable final Object value) {
        isTrue(key != null, "Setting key must not be null");
        isTrue(value != null, "Setting value must not be null");
        isTrue(key.type() == value.getClass(),
                "The value '%s' is of unexpected type (%s) for key '%s'",
                value, value.getClass().getSimpleName(), key);
    }

    public static <T> T[] notEmpty(@Nullable final T[] array, final String message, final Object... values) {
        isTrue(array != null && array.length > 0, message, values);
        return array;
    }

    public static <T> Collection<T> notEmpty(@Nullable final Collection<T> collection, final String message, final Object... values) {
        isTrue(collection != null && !collection.isEmpty(), message, values);
        return collection;
    }

    public static void validateGeneratorUsage(Node node, Generator<?> generator) {
        final Optional<String> name = generator.apiMethodName();
        if (!name.isPresent()) return;
        isTrue(generator.supports(node.getTargetClass()),
                () -> "%nGenerator type mismatch:%n"
                        + "Method '" + name.get() + "' cannot be used for type: " + node.getTargetClass().getCanonicalName()
                        + (node.getField() == null ? "" : "%nField: " + node.getField()));
    }

    public static void validateGeneratorFunction(@Nullable final Function<Generators, ?> gen) {
        isTrue(gen != null, "%nThe second argument of 'generate()' method must not be null."
                + "%nTo generate a null value, use 'supply(SelectorGroup, () -> null)"
                + "%nFor example:"
                + "%n\tPerson person = Instancio.of(Person.class)"
                + "%n\t\t.supply(field(\"firstName\"), () -> null)"
                + "%n\t\t.create()");
    }

    public static void validateSupplierOrGenerator(@Nullable final Object obj) {
        isTrue(obj != null, "%nThe second argument of 'supply()' method must not be null."
                + "%nTo generate a null value, use 'supply(SelectorGroup, () -> null)"
                + "%nFor example:"
                + "%n\tPerson person = Instancio.of(Person.class)"
                + "%n\t\t.supply(field(\"firstName\"), () -> null)"
                + "%n\t\t.create()");
    }

    public static int validateSize(final int size) {
        isTrue(size >= 0, "Size must not be negative: %s", size);
        return size;
    }

    public static int validateLength(final int length) {
        isTrue(length >= 0, "Length must not be negative: %s", length);
        return length;
    }

    public static <T> T notNull(@Nullable final T obj, final String message, final Object... values) {
        if (obj == null) throw new InstancioApiException(String.format(message, values));
        return obj;
    }

    public static void isTrue(final boolean condition, final String message, final Object... values) {
        if (!condition) throw new InstancioApiException(String.format(message, values));
    }

    public static void isFalse(final boolean condition, final String message, final Object... values) {
        if (condition) throw new InstancioApiException(String.format(message, values));
    }

    // Suppressed because String.format() translates %n to platform-specific newlines
    @SuppressWarnings("RedundantStringFormatCall")
    private static void isTrue(final boolean condition, final Supplier<String> message) {
        if (!condition) throw new InstancioApiException(String.format(message.get()));
    }

    public static void validateField(final Class<?> declaringClass, final String fieldName, String message) {
        ApiValidator.notNull(declaringClass, message);
        ApiValidator.notNull(fieldName, message);
        isTrue(ReflectionUtils.isValidField(declaringClass, fieldName), message);
    }

    private ApiValidator() {
        // non-instantiable
    }
}