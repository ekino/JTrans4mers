package com.github.leomillon.converter;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

public class EnumConverter<I, O extends Enum<O>> implements Function<I, O> {

    private final Class<O> targetClass;

    private BiPredicate<String, O> matcher = defaultMatcher();

    public EnumConverter(Class<O> targetClass) {
        this.targetClass = requireNonNull(targetClass, "A target class must be defined");
    }

    public EnumConverter<I, O> withMatcher(BiPredicate<String, O> newMatcher) {
        this.matcher = requireNonNull(newMatcher, "The matcher must be defined");
        return this;
    }

    @Override
    public O apply(I input) {
        if (input == null) {
            return null;
        }

        Optional<O> result;
        if (input.getClass().isEnum()) {
            result = convert((Enum) input, targetClass, matcher);
        }
        else {
            result = convert(input.toString(), targetClass, matcher);
        }
        return result.orElse(null);
    }

    public Optional<O> convert(I input) {
        return ofNullable(apply(input));
    }

    public static <S, T extends Enum<T>> EnumConverter<S, T> to(Class<T> targetClass) {
        return new EnumConverter<>(targetClass);
    }

    public static <T extends Enum<T>> Optional<T> convert(Enum<?> input, Class<T> targetClass) {
        return convert(input, targetClass, defaultMatcher());
    }

    public static <T extends Enum<T>> Optional<T> convert(Enum<?> input,
                                                          Class<T> targetClass,
                                                          BiPredicate<String, T> matcher) {
        return ofNullable(input)
                .map(Enum::name)
                .map(inputName -> convert(inputName, targetClass, matcher).orElse(null));
    }

    public static <T extends Enum<T>> Optional<T> convert(String input, Class<T> targetClass) {
        return convert(input, targetClass, defaultMatcher());
    }

    public static <T extends Enum<T>> Optional<T> convert(String input,
                                                          Class<T> targetClass,
                                                          BiPredicate<String, T> matcher) {

        validateTargetClass(targetClass);
        requireNonNull(matcher, "The matcher must be defined");
        return ofNullable(input)
                .map(toTargetEnum(targetClass, matcher));
    }

    private static <V extends Enum<V>> Class<V> validateTargetClass(Class<V> targetClass) {
        return requireNonNull(targetClass, "A target class must be defined");
    }

    private static <S, T extends Enum<T>> Function<S, T> toTargetEnum(Class<T> targetClazz, BiPredicate<S, T> matcher) {
        return source -> Arrays.stream(targetClazz.getEnumConstants())
                .filter(enumValue -> matcher.test(source, enumValue))
                .findFirst()
                .orElse(null);
    }

    public static <T extends Enum<T>> BiPredicate<String, T> defaultMatcher() {
        return Matchers.byEqualsIgnoreCase();
    }

    public static final class Matchers {

        public static <T extends Enum<T>> BiPredicate<String, T> byEquals() {
            return (source, target) -> source.equals(target.name());
        }

        public static <T extends Enum<T>> BiPredicate<String, T> byEqualsIgnoreCase() {
            return (source, target) -> source.equalsIgnoreCase(target.name());
        }
    }
}
