/**
 * Copyright (c) 2015 ekino (http://www.ekino.com/)
 */
package com.ekino.oss.converter.jtrans4mers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

/**
 * A converter that transforms an input into an {@code Enum} value.
 *
 * <p>This converter will apply a sequence of transformers in order until one of them returns a non-null value.</p>
 *
 * <p>If no transformer returned a non-null value, a list of fallback can be applied then in the same way</p>
 *
 * <p>By default, the converter will give you the first matching {@code Enum} value with a name that
 * is equal ignoring case with the name input</p>
 *
 * @see #defaultTransformer(Class)
 *
 * @param <I> The input type to convert
 * @param <O> The output type to obtain
 *
 * @since 1.0
 * @author Léo Millon
 */
public class EnumConverter<I, O extends Enum<O>> implements Function<I, O> {

    private final List<Function<I, O>> transformers;

    private List<Function<I, O>> fallbacks = emptyList();

    /**
     * Create a converter with a list of custom transformers to apply in order.
     *
     * @since 1.0
     */
    public EnumConverter(List<Function<I, O>> transformers) {
        this.transformers = requireNonNull(transformers, "The transformers must be defined");
    }

    /**
     * Create a converter with the default transformer.
     *
     * @param targetClass The output type to obtain
     *
     * @since 1.0
     */
    public EnumConverter(Class<O> targetClass) {
        this(singletonList(defaultTransformer(targetClass)));
    }

    /**
     * Set a fallback tranformer that will be used if previous transformations returned {@code null}.
     *
     * <p>This will erase any previous fallback set.</p>
     *
     * @since 1.0
     */
    public EnumConverter<I, O> withFallback(Function<I, O> fallbackToApply) {
        withFallbacks(singletonList(requireNonNull(fallbackToApply, "The fallback must be defined")));
        return this;
    }

    /**
     * Set a list of fallback tranformers that will be used in order if previous transformations returned {@code null}.
     *
     * <p>This will erase any previous fallback set.</p>
     *
     * @since 1.0
     */
    public EnumConverter<I, O> withFallbacks(List<Function<I, O>> fallbacksToApply) {
        this.fallbacks = requireNonNull(fallbacksToApply, "The fallbacks must be defined");
        return this;
    }

    /**
     * Create a default converter.
     *
     * @see #EnumConverter(Class)
     *
     * @since 1.0
     */
    public static <S, T extends Enum<T>> EnumConverter<S, T> to(Class<T> targetClass) {
        return new EnumConverter<>(targetClass);
    }

    /**
     * Create a converter with a custom transformer to apply.
     *
     * @see #EnumConverter(List)
     *
     * @since 1.0
     */
    public static <S, T extends Enum<T>> EnumConverter<S, T> with(Function<S, T> transformer) {
        return new EnumConverter<>(singletonList(requireNonNull(transformer, "The transformer must be defined")));
    }

    /**
     * Create a converter with a list of custom transformers to apply in order.
     *
     * @see #EnumConverter(List)
     *
     * @since 1.0
     */
    public static <S, T extends Enum<T>> EnumConverter<S, T> with(List<Function<S, T>> transformers) {
        return new EnumConverter<>(transformers);
    }

    /**
     * Convert the input into a value of {@link O} or {@code null}.
     *
     * @since 1.0
     */
    @Override
    public O apply(I input) {
        return convert(input, computeFunctions()).orElse(null);
    }

    /**
     * Convert the input into an {@link Optional} of {@link O}.
     *
     * @since 1.0
     */
    public Optional<O> convert(I input) {
        return ofNullable(apply(input));
    }

    private Iterable<Function<I, O>> computeFunctions() {
        Collection<Function<I, O>> functionsToApply = new ArrayList<>(transformers);
        functionsToApply.addAll(fallbacks);
        return functionsToApply;
    }

    private Optional<O> convert(I input, Iterable<Function<I, O>> transformersToApply) {

        return ofNullable(toTargetEnum(transformersToApply).apply(input));
    }

    private static <S, T extends Enum<T>> Function<S, T> toTargetEnum(Iterable<Function<S, T>> transformers) {
        return source -> {
            for (Function<S, T> transformer : transformers) {
                T result = transformer.apply(source);
                if (result != null) {
                    return result;
                }
            }
            return null;
        };
    }

    /**
     * Convert the input into a target class enum value with the default transformer applied.
     *
     * @see #EnumConverter(Class)
     * @see #convert(Object)
     *
     * @since 1.0
     */
    public static <S, T extends Enum<T>> Optional<T> convert(S input, Class<T> targetClass) {
        return new EnumConverter<>(targetClass).convert(input);
    }

    /**
     * Convert the input into a target class enum value with a list of custom transformers to apply in order.
     *
     * @see #EnumConverter(List)
     * @see #convert(Object)
     *
     * @since 1.0
     */
    public static <S, T extends Enum<T>> Optional<T> convert(S input, List<Function<S, T>> transformers) {
        return new EnumConverter<>(transformers).convert(input);
    }

    /**
     * Convert the input into a target class enum value with a custom transformer to apply.
     *
     * @see #EnumConverter(List)
     * @see #convert(Object)
     *
     * @since 1.0
     */
    public static <S, T extends Enum<T>> Optional<T> convert(S input, Function<S, T> transformer) {
        return convert(input, singletonList(transformer));
    }

    /**
     * The default transformer used by the converter.
     *
     * @see EnumConverter.Transformers#byEqualNamesIgnoringCase(Class)
     * @since 1.0
     */
    public static <S, T extends Enum<T>> Function<S, T> defaultTransformer(Class<T> targetClass) {
        return Transformers.byEqualNamesIgnoringCase(targetClass);
    }

    /**
     * A factory of prepared transformers.
     *
     * @since 1.0
     * @author Léo Millon
     */
    public static final class Transformers {

        private Transformers() {
            // Prevent instanciation
        }

        private static String getSourceName(Object obj) {
            if (obj == null) {
                return null;
            }
            return obj.getClass().isEnum() ? ((Enum) obj).name() : obj.toString();
        }

        private static class FirstMatchingAgainstEnumValues<S, T extends Enum<T>> implements Function<S, T> {

            private final Class<T> targetClass;
            private final BiPredicate<String, String> matcher;

            public FirstMatchingAgainstEnumValues(Class<T> targetClass, BiPredicate<String, String> matcher) {
                this.targetClass = requireNonNull(targetClass);
                this.matcher = requireNonNull(matcher);
            }

            @Override
            public T apply(S source) {
                return ofNullable(source)
                        .map(Transformers::getSourceName)
                        .map(sourceName -> ofNullable(targetClass)
                                .map(firstMatching(sourceName, matcher))
                                .orElse(null))
                        .orElse(null);
            }


            private Function<Class<T>, T> firstMatching(String source, BiPredicate<String, String> enumMatcher) {
                return enumClass -> Arrays.stream(enumClass.getEnumConstants())
                        .filter(enumValue -> enumMatcher.test(source, enumValue.name()))
                        .findFirst()
                        .orElse(null);
            }
        }

        /**
         * A transformer that will return the first matching combo of an input ({@link S}) name compared
         * to a target ({@link T}) value name.
         *
         * @param matcher the matcher between input and target names.
         *                Usage : <pre>(sourceName, targetName) -> targetName.equalsIgnoreCase(sourceName)}</pre>
         *
         * @since 1.0
         */
        public static <S, T extends Enum<T>> Function<S, T> firstMatchingAgainstEnumValues(Class<T> targetClass,
                                                                                           BiPredicate<String, String> matcher) {
            return new FirstMatchingAgainstEnumValues<>(targetClass, matcher);
        }

        /**
         * A transformer that will return the first matching {@link T} value with a name that
         * is equal to the input ({@link S}) name. Else returns {@code null}.
         *
         * <p>The input ({@link S}) name is determined by</p>
         * <ul>
         *     <li>the {@link T#name()} if the input is an {@link Enum}</li>
         *     <li>the {@link Object#toString()} if the input is anything else</li>
         * </ul>
         *
         * @since 1.0
         */
        public static <S, T extends Enum<T>> Function<S, T> byEqualNames(Class<T> targetClass) {
            return firstMatchingAgainstEnumValues(
                    targetClass,
                    (sourceName, targetName) -> targetName.equals(sourceName)
            );
        }

        /**
         * A transformer that will return the first matching {@link T} value with a name that
         * is equal ignoring case to the input ({@link S}) name. Else returns {@code null}.
         *
         * <p>The input ({@link S}) name is determined by</p>
         * <ul>
         *     <li>the {@link T#name()} if the input is an {@link Enum}</li>
         *     <li>the {@link Object#toString()} if the input is anything else</li>
         * </ul>
         *
         * @since 1.0
         */
        public static <S, T extends Enum<T>> Function<S, T> byEqualNamesIgnoringCase(Class<T> targetClass) {
            return firstMatchingAgainstEnumValues(
                    targetClass,
                    (sourceName, targetName) -> targetName.equalsIgnoreCase(sourceName)
            );
        }

        /**
         * A transformer that uses a map to get corresponding {@link T} value for a given input ({@link S}).
         *
         * <p>This matcher simply performs a {@link Map#get(Object)}.</p>
         *
         * @since 1.0
         */
        public static <S, T extends Enum<T>> Function<S, T> byExplicitMapping(Map<S, T> explicitMapping) {
            return source -> ofNullable(source)
                    .map(explicitMapping::get)
                    .orElse(null);
        }
    }
}
