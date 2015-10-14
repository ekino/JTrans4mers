package com.github.leomillon.converter;

import com.github.leomillon.converter.EnumConverter.Matchers;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;

import static com.github.leomillon.converter.EnumConverter.to;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class EnumConverterTest {

    public static final Map<A, B> EXPLICIT_ENUM_MAPPING = new HashMap<>();
    public static final Map<String, B> EXPLICIT_STRING_MAPPING = new HashMap<>();

    static {
        EXPLICIT_ENUM_MAPPING.put(A.COMMON_VALUE, B.COMMON_VALUE);
        EXPLICIT_ENUM_MAPPING.put(A.A_VALUE, B.B_VALUE);

        EXPLICIT_STRING_MAPPING.put("COMMON_VALUE", B.COMMON_VALUE);
        EXPLICIT_STRING_MAPPING.put("A_VALUE", B.B_VALUE);
    }

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
        Optional<B> result = of(A.COMMON_VALUE)
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
                .withMatcher(Matchers.toNameByEquals())
                .convert("COMMON_VALUE");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(B.COMMON_VALUE);
    }

    @Test
    public void should_fail_with_equals_custom_matcher_because_of_lower_cased_string_input() {

        // When
        Optional<B> result = to(B.class)
                .withMatcher(Matchers.toNameByEquals())
                .convert("common_value");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    public void use_as_a_function_for_stream_map_with_custom_matcher() {

        // Given
        BiPredicate<A, B> customMatcher = EnumConverter
                .<A, B>defaultMatcher()
                .or((source, target) -> source == A.A_VALUE && target == B.B_VALUE);

        // When
        List<B> result = asList(A.COMMON_VALUE, A.A_VALUE)
                .stream()
                .map(to(B.class, customMatcher))
                .collect(toList());

        // Then
        assertThat(result).containsExactly(B.COMMON_VALUE, B.B_VALUE);
    }

    @Test(dataProvider = "explicit_enum_mapping_provider")
    public void should_pass_with_explicit_enum_mapping_matcher(A input, Optional<B> expectedResult) {

        // Given
        BiPredicate<A, B> matcher = Matchers.byExplicitMapping(EXPLICIT_ENUM_MAPPING);

        // When
        Optional<B> result = to(B.class, matcher).convert(input);

        // Then
        assertThat(result).isEqualTo(expectedResult);
    }

    @DataProvider
    private Object[][] explicit_enum_mapping_provider() {
        return new Object[][] {
                // A input, Optional<B> expectedResult
                { A.COMMON_VALUE, of(B.COMMON_VALUE) },
                { A.A_VALUE, of(B.B_VALUE) },
                { null, empty() }
        };
    }

    @Test(dataProvider = "explicit_string_mapping_provider")
    public void should_pass_with_explicit_string_mapping_matcher(String input, Optional<B> expectedResult) {

        // Given
        BiPredicate<String, B> matcher = Matchers.byExplicitMapping(EXPLICIT_STRING_MAPPING);

        // When
        Optional<B> result = to(B.class, matcher).convert(input);

        // Then
        assertThat(result).isEqualTo(expectedResult);
    }

    @DataProvider
    private Object[][] explicit_string_mapping_provider() {
        return new Object[][] {
                // String input, Optional<B> expectedResult
                { "COMMON_VALUE", of(B.COMMON_VALUE) },
                { "A_VALUE", of(B.B_VALUE) },
                { null, empty() }
        };
    }
}