/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.caching.configuration;

import org.gradle.api.Action;
import org.gradle.api.Incubating;
import org.gradle.caching.BuildCacheServiceFactory;
import org.gradle.caching.local.DirectoryBuildCache;
import org.gradle.internal.HasInternalProtocol;

/**
 * Configuration for the build cache for an entire Gradle build.
 *
 * <p>
 * Gradle supports a {@link #local(Action)} and a {@link #remote(Class, Action)} build cache that can be configured separately.
 * When both build caches are enabled, Gradle tries to load build outputs from the local build cache first and then tries the remote build cache if no build outputs are found.
 * Gradle pushes build outputs to any build cache that is enabled and has {@link BuildCache#isPush()} set to true.
 * </p>
 *
 * <p>
 * By default, the local build cache has push enabled, and the remote build cache has push disabled.
 * </p>
 *
 * <p>The local build cache is pre-configured to be a {@link DirectoryBuildCache} and enabled by default. The remote build cache can be configured by specifying
 * the type of build cache to use ({@link #remote(Class)}). Custom remote build cache types can be registered via {@link #registerBuildCacheService(Class, Class)}.</p>
 *
 * <p>Gradle ships with a built-in remote build cache implementation that works via HTTP and can be configured as follows in a build's {@code settings.gradle}:</p>
 *
 * <pre>
 *     buildCache {
 *         remote(HttpBuildCache) {
 *             url = "http://example.com:8123/gradle-cache/"
 *         }
 *     }
 * </pre>
 *
 * @since 3.5
 */
@Incubating
@HasInternalProtocol
public interface BuildCacheConfiguration {

    /**
     * Registers a custom build cache type.
     *
     * @param configurationType Configuration type used to provide parameters to a {@link org.gradle.caching.BuildCacheService}
     * @param buildCacheServiceFactoryType Implementation type of {@link BuildCacheServiceFactory} that is used to create a {@code BuildCacheService}
     */
    <T extends BuildCache> void registerBuildCacheService(Class<T> configurationType, Class<? extends BuildCacheServiceFactory<? super T>> buildCacheServiceFactoryType);

    /**
     * Returns the local cache configuration.
     */
    DirectoryBuildCache getLocal();

    /**
     * Executes the given action against the local configuration.
     *
     * @param configuration the action to execute against the local cache configuration.
     */
    void local(Action<? super DirectoryBuildCache> configuration);

    /**
     * Returns the remote cache configuration.
     */
    BuildCache getRemote();

    /**
     * Configures a remote cache with the given type.
     * <p>
     * If a remote build cache has already been configured with a different type, this method replaces it.
     * </p>
     * <p>
     * Push is disabled by default for the remote cache.
     * </p>
     * @param type the type of remote cache to configure.
     *
     */
    <T extends BuildCache> T remote(Class<T> type);

    /**
     * Configures a remote cache with the given type.
     * <p>
     * If a remote build cache has already been configured with a <b>different</b> type, this method replaces it.
     * </p>
     * <p>
     * If a remote build cache has already been configured with the <b>same</b>, this method configures it. It behaves effectively the same as {@link #remote(Action)}.
     * </p>
     * <p>
     * Push is disabled by default for the remote cache.
     * </p>
     * @param type the type of remote cache to configure.
     * @param configuration the configuration to execute against the remote cache.
     *
     */
    <T extends BuildCache> T remote(Class<T> type, Action<? super T> configuration);

    /**
     * Executes the given action against the currently configured remote cache.
     *
     * @param configuration the action to execute against the currently configured remote cache.
     *
     * @throws IllegalStateException If no remote cache has been assigned yet
     */
    void remote(Action<? super BuildCache> configuration);
}
