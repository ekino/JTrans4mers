# JTrans4mers

[![Build Status](https://travis-ci.org/ekino/JTrans4mers.svg?branch=master)](https://travis-ci.org/ekino/JTrans4mers)

A Java tool to easily map an Object to an Enum.

This tool gives you the ability to convert an Object to an Enum value by applying a sequence of transformations 
without throwing any exception.

Requires JDK 1.8 or higher.

## Installation

To add a dependency on JTrans4mers using Maven, use the following:
```xml
<dependency>
  <groupId>com.ekino.oss.converter</groupId>
  <artifactId>jtrans4mers</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

## Usage

Given the two following enumerations :
```java
enum A {
    COMMON_VALUE,
    A_VALUE
}

enum B {
    COMMON_VALUE,
    B_VALUE
}
```

### An `Enum` to another `Enum` value
Here is the simpliest way to convert an Enum value to another one :
```java
// This converts A value to B value by their names
Optional<B> result = EnumConverter.convert(A.COMMON_VALUE, B.class); // result.get() : B.COMMON_VALUE
```
```java
Optional<B> result = EnumConverter.convert(A.A_VALUE, B.class); // result : empty
```

You can also use the fluent way :
```java
import static com.ekino.oss.converter.jtrans4mers.EnumConverter.to;

Optional<B> result = to(B.class).convert(A.COMMON_VALUE); // result.get() : B.COMMON_VALUE
````
```java
import static com.ekino.oss.converter.jtrans4mers.EnumConverter.to;

Optional<B> result = to(B.class).convert(A.A_VALUE); // result : empty
```

### An `Object` to an `Enum` value
This does not only apply on Enum input. An object can be used to match with the target enum name :
- Enum input will use the `Enum.getName()`.
- Any other Object type input will use the `Object.toString()`.

String input
```java
import static com.ekino.oss.converter.jtrans4mers.EnumConverter.to;

Optional<B> result = to(B.class).convert("COMMON_VALUE"); // result.get() : B.COMMON_VALUE
```

Object input
```java
import static com.ekino.oss.converter.jtrans4mers.EnumConverter.to;

Object input = new Object() {
    @Override
    public String toString() {
        return "COMMON_VALUE";
    }
};

Optional<B> result = to(B.class).convert(input); // result.get() : B.COMMON_VALUE
```

Null safe input
```java
Optional<B> result = EnumConverter.convert(null, B.class); // result : empty
````
```java
import static com.ekino.oss.converter.jtrans4mers.EnumConverter.to;

Optional<B> result = to(B.class).convert(null); // result : empty
```

### Transformers
You can apply custom transformation(s) and fallback(s). These are simple Java `Function`.

How to use a custom transformer :
```java
import static com.ekino.oss.converter.jtrans4mers.EnumConverter.convert;

Function<String, B> transformer = input -> "B".equals(input) ? B.B_VALUE : null;

Optional<B> result = convert("B", transformer); // result.get() : B.B_VALUE
```

Or a sequence of transformers :
```java
import static java.util.Arrays.asList;

List<Function<String, B>> transformers = asList(
        input -> "COMMON".equals(input) ? B.COMMON_VALUE : null,
        input -> "B".equals(input) ? B.B_VALUE : null);
````
```java
import static com.ekino.oss.converter.jtrans4mers.EnumConverter.convert;

Optional<B> result = convert("COMMON", transformers); // result.get() : B.COMMON_VALUE
```
```java
import static com.ekino.oss.converter.jtrans4mers.EnumConverter.convert;

Optional<B> result = convert("B", transformers); // result.get() : B.B_VALUE
```
```java
import static com.ekino.oss.converter.jtrans4mers.EnumConverter.convert;

Optional<B> result = convert("Other", transformers); // result : empty
```
The fluent way :
```java
import static com.ekino.oss.converter.jtrans4mers.EnumConverter.with;

Optional<B> result = with(transformers).convert("B"); // result.get() : B.B_VALUE
```

#### Default transformer
By default, the transformer will return the first target enum value name that is equal ignoring case to the input `name`/`toString` value.

See the next section about the default transformer `Transformers.byEqualNamesIgnoringCase`.

#### Prepared transformers
If you want to change the default transformation behavior, you have access at some already prepared for you in the `EnumConverter.Transformers` class:
- `Transformers.byEqualNamesIgnoringCase(Class<T> targetClass)` :
The default transformer that returns the first target enum value name that **is equal ignoring case** to the input `name`/`toString` value.
```java
import static com.ekino.oss.converter.jtrans4mers.EnumConverter.to;

Optional<B> result = to(B.class).convert("COMMON_VALUE"); // result.get() : B.COMMON_VALUE
```
```java
import static com.ekino.oss.converter.jtrans4mers.EnumConverter.to;

Optional<B> result = to(B.class).convert("common_value"); // result.get() : B.COMMON_VALUE
```
- `Transformers.byEqualNames(Class<T> targetClass)` :
A transformer that returns the first target enum value name that **is equal** to the input `name`/`toString` value.
```java
import static com.ekino.oss.converter.jtrans4mers.EnumConverter.convert;

Optional<B> result = convert("COMMON_VALUE", Transformers.byEqualNames(B.class)); // result.get() : B.COMMOM_VALUE
```
```java
import static com.ekino.oss.converter.jtrans4mers.EnumConverter.convert;

Optional<B> result = convert("common_value", Transformers.byEqualNames(B.class)); // result : empty
```
- `Transformers.byExplicitMapping(Map<S, T> explicitMapping)` :
A transformer that uses a map to get corresponding target value for a given input.
```java
import static com.ekino.oss.converter.jtrans4mers.EnumConverter.with;

Map<A, B> mapping = new HashMap<>();
mapping.put(A.COMMON_VALUE, B.COMMON_VALUE);
mapping.put(A.A_VALUE, B.B_VALUE);

Optional<B> result = with(Transformers.byExplicitMapping(mapping)).convert(A.A_VALUE); // result.get() : B.B_VALUE
```
- `Transformers.firstMatchingAgainstEnumValues(Class<T> targetClass, BiPredicate<String, String> matcher)` :
A transformer that test the input against each enum value's name until it matches.
```java
// This does exactly the same transformation as the Transformers#byEqualNamesIgnoringCase
Transformers.firstMatchingAgainstEnumValues(
        B.class,
        (sourceName, targetName) -> targetName.equalsIgnoreCase(sourceName)
);
```

### Converter as a `Function`

You can use the converter as a standard `java.util.function.Function`.

#### For `Optional`
```java
import static com.ekino.oss.converter.jtrans4mers.EnumConverter.to;
import static java.util.Optional.of;

Optional<B> result = of(A.COMMON_VALUE).map(to(B.class)); // result.get() : B.COMMON_VALUE
```

#### For `Stream`
```java
import static com.ekino.oss.converter.jtrans4mers.EnumConverter.to;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

List<B> result = asList(A.COMMON_VALUE, A.A_VALUE)
                .stream()
                .map(to(B.class))
                .collect(toList()); // result = [B.COMMON_VALUE, null]
```

#### Direct result
If you do not want an optional as a result, you can use the fonctional interface of the `Function`:
```java
import static com.ekino.oss.converter.jtrans4mers.EnumConverter.to;

B result = to(B.class).apply("COMMON_VALUE"); // result : B.COMMON_VALUE
```
```java
import static com.ekino.oss.converter.jtrans4mers.EnumConverter.to;

B result = to(B.class).apply("A_VALUE"); // result : null
```