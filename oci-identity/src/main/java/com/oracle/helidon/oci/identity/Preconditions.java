/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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

package com.oracle.helidon.oci.identity;

/** Specialized runtime precondition checks. */
// inspired by https://bitbucket.oci.oraclecorp.com/projects/PEG/repos/oci-netty/browse/oci-service-common/src/main/java/com/oracle/oci/sfw/netty/common/util/Preconditions.java
class Preconditions {

  /**
   * Checks the boolean value of the specified expression. If the expression is false, throws {@link
   * IllegalArgumentException} along with an errorMessage.
   *
   * @param expression The specified boolean expression.
   * @param errorMessage The error message to be displayed.
   * @throws IllegalArgumentException IFF the expression is false.
   */
  public static void checkArgument(boolean expression, String errorMessage) {
    if (!expression) {
      throw new IllegalArgumentException(errorMessage);
    }
  }

  /**
   * Checks the boolean value of the specified expression. If the expression is false, throws {@link
   * IllegalArgumentException} along with an errorMessage.
   *
   * @param expression The specified boolean expression.
   * @param errorMessageTemplate A template for the exception message should the check fail. The
   *     message is formed by replacing each {@code %s} placeholder in the template with an
   *     argument. These are matched by position - the first {@code %s} gets {@code
   *     errorMessageArgs[0]}, etc. Unmatched arguments will be appended to the formatted message in
   *     square braces. Unmatched placeholders will be left as-is.
   * @param errorMessageArgs The arguments to be substituted into the message template. Arguments
   *     are converted to strings using {@link String#valueOf(Object)}.
   * @throws IllegalArgumentException if {@code expression} is false
   */
  public static void checkArgument(
      boolean expression, String errorMessageTemplate, Object... errorMessageArgs) {
    if (!expression) {
      throw new IllegalArgumentException(lenientFormat(errorMessageTemplate, errorMessageArgs));
    }
  }

  /**
   * Checks the boolean value of the specified expression. If the expression is false, throws {@link
   * IllegalArgumentException} along with a default errorMessage.
   *
   * @param expression The specified boolean value.
   * @throws IllegalArgumentException IFF the expression is false.
   */
  public static void checkArgument(boolean expression) {
    checkArgument(expression, "Illegal Argument");
  }

  /**
   * Checks the value of specified param is positive. If the value is negative, throws {@link
   * IllegalArgumentException} along with an errorMessage.
   *
   * @param val The specified arguments value.
   * @param paramName The name of the specified argument.
   * @throws IllegalArgumentException IFF the expression is false.
   */
  public static void checkPositive(int val, final String paramName) {
    if (val <= 0) {
      throw new IllegalArgumentException("Param '" + paramName + "' must be positive");
    }
  }

  /**
   * Checks the value of specified param is positive. If the value is negative, throws {@link
   * IllegalArgumentException} along with an errorMessage.
   *
   * @param val The specified arguments value.
   * @param paramName The name of the specified argument.
   * @throws IllegalArgumentException IFF the expression is false.
   */
  public static void checkPositive(long val, final String paramName) {
    if (val <= 0) {
      throw new IllegalArgumentException("Param '" + paramName + "' must be positive");
    }
  }

  /**
   * Checks the boolean value of the specified expression. If the expression is false, throws {@link
   * IllegalArgumentException} along with an errorMessage.
   *
   * @param val The specified arguments value.
   * @param paramName The name of the specified argument.
   * @throws IllegalArgumentException IFF the expression is false.
   */
  public static void checkNonNegative(int val, final String paramName) {
    if (val < 0) {
      throw new IllegalArgumentException("Param '" + paramName + "' must be non-negative");
    }
  }

  /**
   * Checks the boolean value of the specified expression. If the expression is false, throws {@link
   * IllegalArgumentException} along with an errorMessage.
   *
   * @param val The specified arguments value.
   * @param paramName The name of the specified argument.
   * @throws IllegalArgumentException IFF the expression is false.
   */
  public static void checkNonNegative(long val, final String paramName) {
    if (val < 0) {
      throw new IllegalArgumentException("Param '" + paramName + "' must be non-negative");
    }
  }

  /**
   * Checks that the given argument is not null. If it is, throws {@link NullPointerException}.
   * Otherwise, returns the argument.
   *
   * @param arg The specified arguments value.
   * @param errorMessage The error message to be displayed.
   * @throws NullPointerException IFF the specified argument is null.
   */
  public static <T> T checkNotNull(T arg, String errorMessage) {
    if (arg == null) {
      throw new NullPointerException(errorMessage);
    }
    return arg;
  }

  /**
   * Checks that the given argument is not null. If it is, throws {@link IllegalArgumentException}.
   * Otherwise, returns the argument.
   *
   * @param arg The specified arguments value.
   * @param paramName The name of the specified argument.
   * @throws IllegalArgumentException IFF the specified argument is null.
   */
  public static <T> T checkNotNullWithIllegalArgException(final T arg, final String paramName)
      throws IllegalArgumentException {
    if (arg == null) {
      throw new IllegalArgumentException("Param '" + paramName + "' must not be null");
    }
    return arg;
  }

  /**
   * Checks that the given String is not null or empty. If it is, throws {@link
   * IllegalArgumentException}. Otherwise, returns the given String.
   *
   * @param arg The specified arguments value.
   * @param paramName The name of the specified argument.
   * @throws IllegalArgumentException IFF the specified argument is null.
   */
  public static String checkNotNullOrEmptyWithIllegalArgException(
      final String arg, final String paramName) throws IllegalArgumentException {
    if (isNullOrEmpty(arg)) {
      throw new IllegalArgumentException("Param '" + paramName + "' must not be null or empty");
    }
    return arg;
  }

  /**
   * Determine if a string arg is {@code null} or {@link String#isEmpty()} returns {@code true}.
   *
   * @param arg The specified string argument.
   */
  public static boolean isNullOrEmpty(String arg) {
    return arg == null || arg.isEmpty();
  }

  /**
   * Returns the given {@code template} string with each occurrence of {@code "%s"} replaced with
   * the corresponding argument value from {@code args}; or, if the placeholder and argument counts
   * do not match, returns a best-effort form of that string. Will not throw an exception under
   * normal conditions.
   *
   * <p><b>Note:</b> For most string-formatting needs, use {@link String#format String.format},
   * {@link java.io.PrintWriter#format PrintWriter.format}, and related methods. These support the
   * full range of <a
   * href="https://docs.oracle.com/javase/9/docs/api/java/util/Formatter.html#syntax">format
   * specifiers</a>, and alert you to usage errors by throwing {@link
   * java.util.IllegalFormatException}.
   *
   * <p>In certain cases, such as outputting debugging information or constructing a message to be
   * used for another unchecked exception, an exception during string formatting would serve little
   * purpose except to supplant the real information you were trying to provide. These are the cases
   * this method is made for; it instead generates a best-effort string with all supplied argument
   * values present.
   *
   * <p><b>Warning:</b> Only the exact two-character placeholder sequence {@code "%s"} is
   * recognized.
   *
   * @param template a string containing zero or more {@code "%s"} placeholder sequences. {@code
   *     null} is treated as the four-character string {@code "null"}.
   * @param args the arguments to be substituted into the message template. The first argument
   *     specified is substituted for the first occurrence of {@code "%s"} in the template, and so
   *     forth. A {@code null} argument is converted to the four-character string {@code "null"};
   *     non-null values are converted to strings using {@link Object#toString()}.
   */
  private static String lenientFormat(String template, Object... args) {
    template = String.valueOf(template); // null -> "null"

    if (args == null) {
      args = new Object[] {"(Object[])null"};
    } else {
      for (int i = 0; i < args.length; i++) {
        args[i] = lenientToString(args[i]);
      }
    }

    // start substituting the arguments into the '%s' placeholders
    StringBuilder builder = new StringBuilder(template.length() + 16 * args.length);
    int templateStart = 0;
    int i = 0;
    while (i < args.length) {
      int placeholderStart = template.indexOf("%s", templateStart);
      if (placeholderStart == -1) {
        break;
      }
      builder.append(template, templateStart, placeholderStart);
      builder.append(args[i++]);
      templateStart = placeholderStart + 2;
    }
    builder.append(template, templateStart, template.length());

    // if we run out of placeholders, append the extra args in square braces
    if (i < args.length) {
      builder.append(" [");
      builder.append(args[i++]);
      while (i < args.length) {
        builder.append(", ");
        builder.append(args[i++]);
      }
      builder.append(']');
    }

    return builder.toString();
  }

  private static String lenientToString(Object o) {
    if (o == null) {
      return "null";
    }
    try {
      return o.toString();
    } catch (Exception e) {
      String objectToString =
          o.getClass().getName() + '@' + Integer.toHexString(System.identityHashCode(o));
      return "<" + objectToString + " threw exception" + e.getClass().getName() + ">";
    }
  }
}
