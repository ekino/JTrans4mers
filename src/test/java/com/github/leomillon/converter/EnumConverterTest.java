package com.github.leomillon.converter;

import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

import static com.github.leomillon.converter.EnumConverter.to;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class EnumConverterTest {

    private enum A {
        COMMON_VALUE,
        A_VALUE
    }

    private enum B {
        COMMON_VALUE,
        B_VALUE
    }

    @Test
    public void should_convert_two_enums_by_name() {

        // When
        Optional<B> result = to(B.class).convert(A.COMMON_VALUE);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(B.COMMON_VALUE);
    }

    @Test
    public void should_convert_a_string_to_an_enum_by_name() {

        // When
        Optional<B> result = to(B.class).convert("COMMON_VALUE");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(B.COMMON_VALUE);
    }

    @Test
    public void should_convert_an_object_toString_to_an_enum_by_name() {

        // Given
        Object input = new Object() {
            @Override
            public String toString() {
                return "COMMON_VALUE";
            }
        };

        // When
        Optional<B> result = to(B.class).convert(input);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(B.COMMON_VALUE);
    }

    @Test
    public void should_return_empty_when_to_string_value_does_not_exist_in_the_two_enums() {

        // When
        Optional<B> result = to(B.class).convert(A.A_VALUE);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void should_return_empty_for_null_input() {

        // When
        Optional<B> result = to(B.class).convert(null);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void should_return_empty_for_null_toString() {

        // Given
        Object input = new Object() {
            @Override
            public String toString() {
                return null;
            }
        };

        // When
        Optional<B> result = to(B.class).convert(input);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void static_should_convert_two_enums_by_name() {

        // When
        Optional<B> result = EnumConverter.convert(A.COMMON_VALUE, B.class);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(B.COMMON_VALUE);
    }

    @Test
    public void static_should_convert_a_string_to_an_enum_by_name() {

        // When
        Optional<B> result = EnumConverter.convert("COMMON_VALUE", B.class);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(B.COMMON_VALUE);
    }

    @Test
    public void static_should_return_empty_when_to_string_value_does_not_exist_in_the_two_enums() {

        // When
        Optional<B> result = EnumConverter.convert(A.A_VALUE, B.class);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void use_as_a_function_for_optinal_map() {

        // When
        Optional<B> result = Optional.of(A.COMMON_VALUE)
                .map(to(B.class));

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(B.COMMON_VALUE);
    }

    @Test
    public void use_as_a_function_for_stream_map() {

        // When
        List<B> result = asList(A.COMMON_VALUE, A.A_VALUE)
                .stream()
                .map(to(B.class))
                .collect(toList());

        // Then
        assertThat(result).containsExactly(B.COMMON_VALUE, null);
    }

    @Test
    public void should_pass_with_equals_custom_matcher_because_of_upper_cased_string_input() {

        // When
        Optional<B> result = to(B.class)
                .withMatcher(EnumConverter.Matchers.byEquals())
                .convert("COMMON_VALUE");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(B.COMMON_VALUE);
    }

    @Test
    public void should_fail_with_equals_custom_matcher_because_of_lower_cased_string_input() {

        // When
        Optional<B> result = to(B.class)
                .withMatcher(EnumConverter.Matchers.byEquals())
                .convert("common_value");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void use_with_fallback_on_custom_matcher() {

        // Given
        BiPredicate<String, B> customMatcher = EnumConverter
                .<B>defaultMatcher()
                .or((source, target) -> "A_VALUE".equals(source) && target == B.B_VALUE);

        // When
        List<B> result = asList(A.COMMON_VALUE, A.A_VALUE)
                .stream()
                .map(to(B.class).withMatcher(customMatcher))
                .collect(toList());

        // Then
        assertThat(result).containsExactly(B.COMMON_VALUE, B.B_VALUE);
    }
}