/*
 * Copyright 2021 OPPO ESA Stack Project
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
package io.esastack.codec.commons.pool;

import java.util.concurrent.CompletableFuture;

/**
 * The {@link MultiplexPool} element factory.
 */
public interface PooledObjectFactory<T> {

    /**
     * Async create pool element.
     *
     * @return CompletableFuture —— result of create
     */
    CompletableFuture<T> create();

    /**
     * Async destroy the given element object.
     *
     * @param object object you want to destroy
     * @return CompletableFuture —— result of destroy
     */
    CompletableFuture<Void> destroy(T object);

    /**
     * Check the element is valid or not.
     *
     * @param object object that you want to validate
     * @return true if object is validated
     */
    Boolean validate(T object);
}
