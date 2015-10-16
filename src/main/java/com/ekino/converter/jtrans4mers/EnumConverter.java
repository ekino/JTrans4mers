/**
 * Copyright (c) 2015 ekino (http://www.ekino.com/)
 */
package com.ekino.converter.jtrans4mers;

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

public class EnumConverter<I, O extends Enum<O>> implements Function<I, O> {

    private final List<Function<I, O>> transformers;

    private List<Function<I, O>> fallbacks = emptyList();

    private EnumConverter(List<Function<I, O>> transformers) {
        this.transformers = requireNonNull(transformers, "The transformers must be defined");
    }

    public EnumConverter(Class<O> targetClass) {
        this(singletonList(defaultTransformer(targetClass)));
    }

    public EnumConverter<I, O> withFallback(Function<I, O> fallbackToApply) {
        withFallbacks(singletonList(requireNonNull(fallbackToApply, "The fallback must be defined")));
        return this;
    }

    public EnumConverter<I, O> withFallbacks(List<Function<I, O>> fallbacksToApply) {
        this.fallbacks = requireNonNull(fallbacksToApply, "The fallbacks must be defined");
        return this;
    }

    public Optional<O> convert(I input) {
        return ofNullable(apply(input));
    }

    @Override
    public O apply(I input) {
        return convert(input, computeFunctions()).orElse(null);
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

    public static <S, T extends Enum<T>> EnumConverter<S, T> to(Class<T> targetClass) {
        return new EnumConverter<>(targetClass);
    }

    public static <S, T extends Enum<T>> EnumConverter<S, T> with(Function<S, T> transformer) {
        return new EnumConverter<>(singletonList(requireNonNull(transformer, "The transformer must be defined")));
    }

    public static <S, T extends Enum<T>> EnumConverter<S, T> with(List<Function<S, T>> transformers) {
        return new EnumConverter<>(transformers);
    }

    public static <S, T extends Enum<T>> Optional<T> convert(S input, Class<T> targetClass) {
        return new EnumConverter<>(targetClass).convert(input);
    }

    public static <S, T extends Enum<T>> Optional<T> convert(S input, List<Function<S, T>> transformers) {
        return new EnumConverter<>(transformers).convert(input);
    }

    public static <S, T extends Enum<T>> Optional<T> convert(S input, Function<S, T> transformer) {
        return convert(input, singletonList(transformer));
    }

    public static <S, T extends Enum<T>> Function<S, T> defaultTransformer(Class<T> targetClass) {
        return Transformers.byEqualNamesIgnoringCase(targetClass);
    }

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

        public static <S, T extends Enum<T>> Function<S, T> firstMatchingAgainstEnumValues(Class<T> targetClass,
                                                                                           BiPredicate<String, String> matcher) {
            return new FirstMatchingAgainstEnumValues<>(targetClass, matcher);
        }

        public static <S, T extends Enum<T>> Function<S, T> byEqualNames(Class<T> targetClass) {
            return firstMatchingAgainstEnumValues(
                    targetClass,
                    (sourceName, targetName) -> targetName.equals(sourceName)
            );
        }

        public static <S, T extends Enum<T>> Function<S, T> byEqualNamesIgnoringCase(Class<T> targetClass) {
            return firstMatchingAgainstEnumValues(
                    targetClass,
                    (sourceName, targetName) -> targetName.equalsIgnoreCase(sourceName)
            );
        }

        public static <S, T extends Enum<T>> Function<S, T> byExplicitMapping(Map<S, T> explicitMapping) {
            return source -> ofNullable(source)
                    .map(explicitMapping::get)
                    .orElse(null);
        }
    }
}
