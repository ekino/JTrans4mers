package com.github.leomillon.converter;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

public class EnumConverter<I, O extends Enum<O>> implements Function<I, O> {

    private final Class<O> targetClass;

    private BiPredicate<I, O> matcher = defaultMatcher();

    public EnumConverter(Class<O> targetClass) {
        this.targetClass = requireNonNull(targetClass, "A target class must be defined");
    }

    public EnumConverter<I, O> withMatcher(BiPredicate<I, O> newMatcher) {
        this.matcher = requireNonNull(newMatcher, "The matcher must be defined");
        return this;
    }

    public Optional<O> convert(I input) {
        return ofNullable(apply(input));
    }

    @Override
    public O apply(I input) {
        if (input == null) {
            return null;
        }

        return convert(input, matcher).orElse(null);
    }

    private Optional<O> convert(I input, BiPredicate<I, O> matcherToApply) {

        return ofNullable(input).map(toTargetEnum(targetClass, matcherToApply));
    }

    public static <S, T extends Enum<T>> EnumConverter<S, T> to(Class<T> targetClass) {
        return new EnumConverter<>(targetClass);
    }

    public static <S, T extends Enum<T>> EnumConverter<S, T> to(Class<T> targetClass, BiPredicate<S, T> matcherToApply) {
        return new EnumConverter<S, T>(targetClass).withMatcher(matcherToApply);
    }

    public static <S, T extends Enum<T>> Optional<T> convert(S input, Class<T> targetClass) {
        return new EnumConverter<>(targetClass).convert(input);
    }

    private static <S, T extends Enum<T>> Function<S, T> toTargetEnum(Class<T> targetClazz, BiPredicate<S, T> matcher) {
        return source -> Arrays.stream(targetClazz.getEnumConstants())
                .filter(Objects::nonNull)
                .filter(enumValue -> matcher.test(source, enumValue))
                .findFirst()
                .orElse(null);
    }

    public static <S, T extends Enum<T>> BiPredicate<S, T> defaultMatcher() {
        return Matchers.<S, T>toNameByEqualsIgnoreCase();
    }

    public static final class Matchers {

        public static <S, T extends Enum<T>> BiPredicate<S, T> toNameByEquals() {
            return (source, target) -> source.equals(target.name());
        }

        public static <S, T extends Enum<T>> BiPredicate<S, T> toNameByEqualsIgnoreCase() {
            return (source, target) -> ofNullable(source)
                    .map(obj -> obj.getClass().isEnum() ? ((Enum) source).name() : source.toString())
                    .filter(stringSource -> stringSource.equalsIgnoreCase(target.name()))
                    .isPresent();
        }
    }
}
