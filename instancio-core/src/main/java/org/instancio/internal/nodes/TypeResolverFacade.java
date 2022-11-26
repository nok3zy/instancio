/*
 * Copyright 2022 the original author or authors.
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
package org.instancio.internal.nodes;

import org.instancio.spi.TypeResolver;
import org.instancio.util.ServiceLoaders;

import java.util.List;
import java.util.Optional;

class TypeResolverFacade {

    private static final List<TypeResolver> TYPE_RESOLVERS = ServiceLoaders.loadAll(TypeResolver.class);

    Optional<Class<?>> resolve(final Class<?> typeToResolve) {
        for (TypeResolver resolver : TYPE_RESOLVERS) {
            final Optional<Class<?>> resolved = resolver.resolve(typeToResolve);
            if (resolved.isPresent()) {
                return resolved;
            }
        }

        return Optional.empty();
    }
}
