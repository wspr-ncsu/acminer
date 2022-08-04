package org.sag.common.tools;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Operations on Strings that contain words.
 *
 * <p>
 * This class tries to handle {@code null} input gracefully. An exception will not be thrown for a
 * {@code null} input. Each method documents its behavior in more detail.
 * </p>
 *
 * @since 1.1
 */
public class TextUtils {

	/**
	 * Abbreviates the words nicely.
	 *
	 * <p>
	 * This method searches for the first space after the lower limit and abbreviates
	 * the String there. It will also append any String passed as a parameter
	 * to the end of the String. The upper limit can be specified to forcibly
	 * abbreviate a String.
	 * </p>
	 *
	 * @param str		 the string to be abbreviated. If null is passed, null is returned.
	 *					If the empty String is passed, the empty string is returned.
	 * @param lower	   the lower limit; negative value is treated as zero.
	 * @param upper	   the upper limit; specify -1 if no limit is desired.
	 *					If the upper limit is lower than the lower limit, it will be
	 *					adjusted to be the same as the lower limit.
	 * @param appendToEnd String to be appended to the end of the abbreviated string.
	 *					This is appended ONLY if the string was indeed abbreviated.
	 *					The append does not count towards the lower or upper limits.
	 * @return The abbreviated String.
	 *
	 * <pre>
	 * WordUtils.abbreviate("Now is the time for all good men", 0, 40, null));	 = "Now"
	 * WordUtils.abbreviate("Now is the time for all good men", 10, 40, null));	= "Now is the"
	 * WordUtils.abbreviate("Now is the time for all good men", 20, 40, null));	= "Now is the time for all"
	 * WordUtils.abbreviate("Now is the time for all good men", 0, 40, ""));	   = "Now"
	 * WordUtils.abbreviate("Now is the time for all good men", 10, 40, ""));	  = "Now is the"
	 * WordUtils.abbreviate("Now is the time for all good men", 20, 40, ""));	  = "Now is the time for all"
	 * WordUtils.abbreviate("Now is the time for all good men", 0, 40, " ..."));   = "Now ..."
	 * WordUtils.abbreviate("Now is the time for all good men", 10, 40, " ..."));  = "Now is the ..."
	 * WordUtils.abbreviate("Now is the time for all good men", 20, 40, " ..."));  = "Now is the time for all ..."
	 * WordUtils.abbreviate("Now is the time for all good men", 0, -1, ""));	   = "Now"
	 * WordUtils.abbreviate("Now is the time for all good men", 10, -1, ""));	  = "Now is the"
	 * WordUtils.abbreviate("Now is the time for all good men", 20, -1, ""));	  = "Now is the time for all"
	 * WordUtils.abbreviate("Now is the time for all good men", 50, -1, ""));	  = "Now is the time for all good men"
	 * WordUtils.abbreviate("Now is the time for all good men", 1000, -1, ""));	= "Now is the time for all good men"
	 * WordUtils.abbreviate("Now is the time for all good men", 9, -10, null));	= IllegalArgumentException
	 * WordUtils.abbreviate("Now is the time for all good men", 10, 5, null));	 = IllegalArgumentException
	 * </pre>
	 */
	public static String abbreviate(final String str, int lower, int upper, final String appendToEnd) {
		isTrue(upper >= -1, "upper value cannot be less than -1");
		isTrue(upper >= lower || upper == -1, "upper value is less than lower value");
		if (isEmpty(str)) {
			return str;
		}

		// if the lower value is greater than the length of the string,
		// set to the length of the string
		if (lower > str.length()) {
			lower = str.length();
		}

		// if the upper value is -1 (i.e. no limit) or is greater
		// than the length of the string, set to the length of the string
		if (upper == -1 || upper > str.length()) {
			upper = str.length();
		}

		final StringBuilder result = new StringBuilder();
		final int index = indexOf(str, " ", lower);
		if (index == -1) {
			result.append(str, 0, upper);
			// only if abbreviation has occurred do we append the appendToEnd value
			if (upper != str.length()) {
				result.append(Objects.toString(appendToEnd, ""));
			}
		} else if (index > upper) {
			result.append(str, 0, upper);
			result.append(Objects.toString(appendToEnd, ""));
		} else {
			result.append(str, 0, index);
			result.append(Objects.toString(appendToEnd, ""));
		}

		return result.toString();
	}

	/**
	 * Capitalizes all the whitespace separated words in a String.
	 * Only the first character of each word is changed. To convert the
	 * rest of each word to lowercase at the same time,
	 * use {@link #capitalizeFully(String)}.
	 *
	 * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.
	 * A {@code null} input String returns {@code null}.
	 * Capitalization uses the Unicode title case, normally equivalent to
	 * upper case.</p>
	 *
	 * <pre>
	 * WordUtils.capitalize(null)		= null
	 * WordUtils.capitalize("")		  = ""
	 * WordUtils.capitalize("i am FINE") = "I Am FINE"
	 * </pre>
	 *
	 * @param str  the String to capitalize, may be null
	 * @return capitalized String, {@code null} if null String input
	 * @see #uncapitalize(String)
	 * @see #capitalizeFully(String)
	 */
	public static String capitalize(final String str) {
		return capitalize(str, null);
	}

	/**
	 * Capitalizes all the delimiter separated words in a String.
	 * Only the first character of each word is changed. To convert the
	 * rest of each word to lowercase at the same time,
	 * use {@link #capitalizeFully(String, char[])}.
	 *
	 * <p>The delimiters represent a set of characters understood to separate words.
	 * The first string character and the first non-delimiter character after a
	 * delimiter will be capitalized.</p>
	 *
	 * <p>A {@code null} input String returns {@code null}.
	 * Capitalization uses the Unicode title case, normally equivalent to
	 * upper case.</p>
	 *
	 * <pre>
	 * WordUtils.capitalize(null, *)			= null
	 * WordUtils.capitalize("", *)			  = ""
	 * WordUtils.capitalize(*, new char[0])	 = *
	 * WordUtils.capitalize("i am fine", null)  = "I Am Fine"
	 * WordUtils.capitalize("i aM.fine", {'.'}) = "I aM.Fine"
	 * WordUtils.capitalize("i am fine", new char[]{}) = "I am fine"
	 * </pre>
	 *
	 * @param str  the String to capitalize, may be null
	 * @param delimiters  set of characters to determine capitalization, null means whitespace
	 * @return capitalized String, {@code null} if null String input
	 * @see #uncapitalize(String)
	 * @see #capitalizeFully(String)
	 */
	public static String capitalize(final String str, final char... delimiters) {
		if (isEmpty(str)) {
			return str;
		}
		final Set<Integer> delimiterSet = generateDelimiterSet(delimiters);
		final int strLen = str.length();
		final int[] newCodePoints = new int[strLen];
		int outOffset = 0;

		boolean capitalizeNext = true;
		for (int index = 0; index < strLen;) {
			final int codePoint = str.codePointAt(index);

			if (delimiterSet.contains(codePoint)) {
				capitalizeNext = true;
				newCodePoints[outOffset++] = codePoint;
				index += Character.charCount(codePoint);
			} else if (capitalizeNext) {
				final int titleCaseCodePoint = Character.toTitleCase(codePoint);
				newCodePoints[outOffset++] = titleCaseCodePoint;
				index += Character.charCount(titleCaseCodePoint);
				capitalizeNext = false;
			} else {
				newCodePoints[outOffset++] = codePoint;
				index += Character.charCount(codePoint);
			}
		}
		return new String(newCodePoints, 0, outOffset);
	}

	/**
	 * Converts all the whitespace separated words in a String into capitalized words,
	 * that is each word is made up of a titlecase character and then a series of
	 * lowercase characters.
	 *
	 * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.
	 * A {@code null} input String returns {@code null}.
	 * Capitalization uses the Unicode title case, normally equivalent to
	 * upper case.</p>
	 *
	 * <pre>
	 * WordUtils.capitalizeFully(null)		= null
	 * WordUtils.capitalizeFully("")		  = ""
	 * WordUtils.capitalizeFully("i am FINE") = "I Am Fine"
	 * </pre>
	 *
	 * @param str  the String to capitalize, may be null
	 * @return capitalized String, {@code null} if null String input
	 */
	public static String capitalizeFully(final String str) {
		return capitalizeFully(str, null);
	}

	/**
	 * Converts all the delimiter separated words in a String into capitalized words,
	 * that is each word is made up of a titlecase character and then a series of
	 * lowercase characters.
	 *
	 * <p>The delimiters represent a set of characters understood to separate words.
	 * The first string character and the first non-delimiter character after a
	 * delimiter will be capitalized.</p>
	 *
	 * <p>A {@code null} input String returns {@code null}.
	 * Capitalization uses the Unicode title case, normally equivalent to
	 * upper case.</p>
	 *
	 * <pre>
	 * WordUtils.capitalizeFully(null, *)			= null
	 * WordUtils.capitalizeFully("", *)			  = ""
	 * WordUtils.capitalizeFully(*, null)			= *
	 * WordUtils.capitalizeFully(*, new char[0])	 = *
	 * WordUtils.capitalizeFully("i aM.fine", {'.'}) = "I am.Fine"
	 * </pre>
	 *
	 * @param str  the String to capitalize, may be null
	 * @param delimiters  set of characters to determine capitalization, null means whitespace
	 * @return capitalized String, {@code null} if null String input
	 */
	public static String capitalizeFully(String str, final char... delimiters) {
		if (isEmpty(str)) {
			return str;
		}
		str = str.toLowerCase();
		return capitalize(str, delimiters);
	}

	/**
	 * Checks if the String contains all words in the given array.
	 *
	 * <p>
	 * A {@code null} String will return {@code false}. A {@code null}, zero
	 * length search array or if one element of array is null will return {@code false}.
	 * </p>
	 *
	 * <pre>
	 * WordUtils.containsAllWords(null, *)			= false
	 * WordUtils.containsAllWords("", *)			  = false
	 * WordUtils.containsAllWords(*, null)			= false
	 * WordUtils.containsAllWords(*, [])			  = false
	 * WordUtils.containsAllWords("abcd", "ab", "cd") = false
	 * WordUtils.containsAllWords("abc def", "def", "abc") = true
	 * </pre>
	 *
	 * @param word The CharSequence to check, may be null
	 * @param words The array of String words to search for, may be null
	 * @return {@code true} if all search words are found, {@code false} otherwise
	 */
	public static boolean containsAllWords(final CharSequence word, final CharSequence... words) {
		if (isEmpty(word) || isEmpty(words)) {
			return false;
		}
		for (final CharSequence w : words) {
			if (isBlank(w)) {
				return false;
			}
			final Pattern p = Pattern.compile(".*\\b" + w + "\\b.*");
			if (!p.matcher(word).matches()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Converts an array of delimiters to a hash set of code points. Code point of space(32) is added as the default
	 * value if delimiters is null. The generated hash set provides O(1) lookup time.
	 *
	 * @param delimiters set of characters to determine capitalization, null means whitespace
	 * @return Set<Integer>
	 */
	private static Set<Integer> generateDelimiterSet(final char[] delimiters) {
		final Set<Integer> delimiterHashSet = new HashSet<>();
		if (delimiters == null || delimiters.length == 0) {
			if (delimiters == null) {
				delimiterHashSet.add(Character.codePointAt(new char[] {' '}, 0));
			}

			return delimiterHashSet;
		}

		for (int index = 0; index < delimiters.length; index++) {
			delimiterHashSet.add(Character.codePointAt(delimiters, index));
		}
		return delimiterHashSet;
	}

	/**
	 * Extracts the initial characters from each word in the String.
	 *
	 * <p>All first characters after whitespace are returned as a new string.
	 * Their case is not changed.</p>
	 *
	 * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.
	 * A {@code null} input String returns {@code null}.</p>
	 *
	 * <pre>
	 * WordUtils.initials(null)			 = null
	 * WordUtils.initials("")			   = ""
	 * WordUtils.initials("Ben John Lee")   = "BJL"
	 * WordUtils.initials("Ben J.Lee")	  = "BJ"
	 * </pre>
	 *
	 * @param str  the String to get initials from, may be null
	 * @return String of initial letters, {@code null} if null String input
	 * @see #initials(String,char[])
	 */
	public static String initials(final String str) {
		return initials(str, null);
	}
	
	/**
	 * <p>Checks if a CharSequence is empty ("") or null.</p>
	 *
	 * <pre>
	 * StringUtils.isEmpty(null)	  = true
	 * StringUtils.isEmpty("")		= true
	 * StringUtils.isEmpty(" ")	   = false
	 * StringUtils.isEmpty("bob")	 = false
	 * StringUtils.isEmpty("  bob  ") = false
	 * </pre>
	 *
	 * <p>NOTE: This method changed in Lang version 2.0.
	 * It no longer trims the CharSequence.
	 * That functionality is available in isBlank().</p>
	 *
	 * @param cs  the CharSequence to check, may be null
	 * @return {@code true} if the CharSequence is empty or null
	 * @since 3.0 Changed signature from isEmpty(String) to isEmpty(CharSequence)
	 */
	public static boolean isEmpty(final CharSequence cs) {
		return cs == null || cs.length() == 0;
	}
	
	/**
	 * <p>Finds the first index within a CharSequence, handling {@code null}.
	 * This method uses {@link String#indexOf(String, int)} if possible.</p>
	 *
	 * <p>A {@code null} CharSequence will return {@code -1}.
	 * A negative start position is treated as zero.
	 * An empty ("") search CharSequence always matches.
	 * A start position greater than the string length only matches
	 * an empty search CharSequence.</p>
	 *
	 * <pre>
	 * StringUtils.indexOf(null, *, *)		  = -1
	 * StringUtils.indexOf(*, null, *)		  = -1
	 * StringUtils.indexOf("", "", 0)		   = 0
	 * StringUtils.indexOf("", *, 0)			= -1 (except when * = "")
	 * StringUtils.indexOf("aabaabaa", "a", 0)  = 0
	 * StringUtils.indexOf("aabaabaa", "b", 0)  = 2
	 * StringUtils.indexOf("aabaabaa", "ab", 0) = 1
	 * StringUtils.indexOf("aabaabaa", "b", 3)  = 5
	 * StringUtils.indexOf("aabaabaa", "b", 9)  = -1
	 * StringUtils.indexOf("aabaabaa", "b", -1) = 2
	 * StringUtils.indexOf("aabaabaa", "", 2)   = 2
	 * StringUtils.indexOf("abc", "", 9)		= 3
	 * </pre>
	 *
	 * @param seq  the CharSequence to check, may be null
	 * @param searchSeq  the CharSequence to find, may be null
	 * @param startPos  the start position, negative treated as zero
	 * @return the first index of the search CharSequence (always &ge; startPos),
	 *  -1 if no match or {@code null} string input
	 * @since 2.0
	 * @since 3.0 Changed signature from indexOf(String, String, int) to indexOf(CharSequence, CharSequence, int)
	 */
	public static int indexOf(final String seq, final String searchSeq, final int startPos) {
		if (seq == null || searchSeq == null) {
			return -1;
		}
		return seq.indexOf(searchSeq, startPos);
	}
	
	/**
	 * <p>Checks if a CharSequence is empty (""), null or whitespace only.</p>
	 *
	 * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.</p>
	 *
	 * <pre>
	 * StringUtils.isBlank(null)	  = true
	 * StringUtils.isBlank("")		= true
	 * StringUtils.isBlank(" ")	   = true
	 * StringUtils.isBlank("bob")	 = false
	 * StringUtils.isBlank("  bob  ") = false
	 * </pre>
	 *
	 * @param cs  the CharSequence to check, may be null
	 * @return {@code true} if the CharSequence is null, empty or whitespace only
	 * @since 2.0
	 * @since 3.0 Changed signature from isBlank(String) to isBlank(CharSequence)
	 */
	public static boolean isBlank(final CharSequence cs) {
		final int strLen = ((cs == null) ? 0 : cs.length());
		if (strLen == 0) {
			return true;
		}
		for (int i = 0; i < strLen; i++) {
			if (!Character.isWhitespace(cs.charAt(i))) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Returns the length of the specified array.
	 * This method can deal with {@code Object} arrays and with primitive arrays.
	 * <p>
	 * If the input array is {@code null}, {@code 0} is returned.
	 * </p>
	 * <pre>
	 * ArrayUtils.getLength(null)			= 0
	 * ArrayUtils.getLength([])			  = 0
	 * ArrayUtils.getLength([null])		  = 1
	 * ArrayUtils.getLength([true, false])   = 2
	 * ArrayUtils.getLength([1, 2, 3])	   = 3
	 * ArrayUtils.getLength(["a", "b", "c"]) = 3
	 * </pre>
	 *
	 * @param array  the array to retrieve the length from, may be null
	 * @return The length of the array, or {@code 0} if the array is {@code null}
	 * @throws IllegalArgumentException if the object argument is not an array.
	 * @since 2.1
	 */
	public static int getLength(final Object array) {
		if (array == null) {
			return 0;
		}
		return Array.getLength(array);
	}
	
	/**
	 * Checks if an array of Objects is empty or {@code null}.
	 *
	 * @param array  the array to test
	 * @return {@code true} if the array is empty or {@code null}
	 * @since 2.1
	 */
	public static boolean isEmpty(final Object[] array) {
		return getLength(array) == 0;
	}
	
	/**
	 * <p>Validate that the argument condition is {@code true}; otherwise
	 * throwing an exception with the specified message. This method is useful when
	 * validating according to an arbitrary boolean expression, such as validating a
	 * primitive number or using your own custom validation expression.</p>
	 *
	 * <pre>
	 * Validate.isTrue(i &gt;= min &amp;&amp; i &lt;= max, "The value must be between &#37;d and &#37;d", min, max);
	 * Validate.isTrue(myObject.isOk(), "The object is not okay");</pre>
	 *
	 * @param expression  the boolean expression to check
	 * @param message  the {@link String#format(String, Object...)} exception message if invalid, not null
	 * @param values  the optional values for the formatted exception message, null array not recommended
	 * @throws IllegalArgumentException if expression is {@code false}
	 */
	public static void isTrue(final boolean expression, final String message, final Object... values) {
		if (!expression) {
			throw new IllegalArgumentException(String.format(message, values));
		}
	}
	
	/**
	 * <p>Centers a String in a larger String of size {@code size}
	 * using the space character (' ').</p>
	 *
	 * <p>If the size is less than the String length, the original String is returned.
	 * A {@code null} String returns {@code null}.
	 * A negative size is treated as zero.</p>
	 *
	 * <p>Equivalent to {@code center(str, size, " ")}.</p>
	 *
	 * <pre>
	 * StringUtils.center(null, *)   = null
	 * StringUtils.center("", 4)	 = "	"
	 * StringUtils.center("ab", -1)  = "ab"
	 * StringUtils.center("ab", 4)   = " ab "
	 * StringUtils.center("abcd", 2) = "abcd"
	 * StringUtils.center("a", 4)	= " a  "
	 * </pre>
	 *
	 * @param str  the String to center, may be null
	 * @param size  the int size of new String, negative treated as zero
	 * @return centered String, {@code null} if null String input
	 */
	public static String center(final String str, final int size) {
		return center(str, size, ' ');
	}

	/**
	 * <p>Centers a String in a larger String of size {@code size}.
	 * Uses a supplied character as the value to pad the String with.</p>
	 *
	 * <p>If the size is less than the String length, the String is returned.
	 * A {@code null} String returns {@code null}.
	 * A negative size is treated as zero.</p>
	 *
	 * <pre>
	 * StringUtils.center(null, *, *)	 = null
	 * StringUtils.center("", 4, ' ')	 = "	"
	 * StringUtils.center("ab", -1, ' ')  = "ab"
	 * StringUtils.center("ab", 4, ' ')   = " ab "
	 * StringUtils.center("abcd", 2, ' ') = "abcd"
	 * StringUtils.center("a", 4, ' ')	= " a  "
	 * StringUtils.center("a", 4, 'y')	= "yayy"
	 * </pre>
	 *
	 * @param str  the String to center, may be null
	 * @param size  the int size of new String, negative treated as zero
	 * @param padChar  the character to pad the new String with
	 * @return centered String, {@code null} if null String input
	 * @since 2.0
	 */
	public static String center(String str, final int size, final char padChar) {
		if (str == null || size <= 0) {
			return str;
		}
		final int strLen = str.length();
		final int pads = size - strLen;
		if (pads <= 0) {
			return str;
		}
		str = leftPad(str, strLen + pads / 2, padChar);
		str = rightPad(str, size, padChar);
		return str;
	}

	/**
	 * <p>Centers a String in a larger String of size {@code size}.
	 * Uses a supplied String as the value to pad the String with.</p>
	 *
	 * <p>If the size is less than the String length, the String is returned.
	 * A {@code null} String returns {@code null}.
	 * A negative size is treated as zero.</p>
	 *
	 * <pre>
	 * StringUtils.center(null, *, *)	 = null
	 * StringUtils.center("", 4, " ")	 = "	"
	 * StringUtils.center("ab", -1, " ")  = "ab"
	 * StringUtils.center("ab", 4, " ")   = " ab "
	 * StringUtils.center("abcd", 2, " ") = "abcd"
	 * StringUtils.center("a", 4, " ")	= " a  "
	 * StringUtils.center("a", 4, "yz")   = "yayz"
	 * StringUtils.center("abc", 7, null) = "  abc  "
	 * StringUtils.center("abc", 7, "")   = "  abc  "
	 * </pre>
	 *
	 * @param str  the String to center, may be null
	 * @param size  the int size of new String, negative treated as zero
	 * @param padStr  the String to pad the new String with, must not be null or empty
	 * @return centered String, {@code null} if null String input
	 * @throws IllegalArgumentException if padStr is {@code null} or empty
	 */
	public static String center(String str, final int size, String padStr) {
		if (str == null || size <= 0) {
			return str;
		}
		if (isEmpty(padStr)) {
			padStr = " ";
		}
		final int strLen = str.length();
		final int pads = size - strLen;
		if (pads <= 0) {
			return str;
		}
		str = leftPad(str, strLen + pads / 2, padStr);
		str = rightPad(str, size, padStr);
		return str;
	}
	
	/**
	 * <p>Left pad a String with spaces (' ').</p>
	 *
	 * <p>The String is padded to the size of {@code size}.</p>
	 *
	 * <pre>
	 * StringUtils.leftPad(null, *)   = null
	 * StringUtils.leftPad("", 3)	 = "   "
	 * StringUtils.leftPad("bat", 3)  = "bat"
	 * StringUtils.leftPad("bat", 5)  = "  bat"
	 * StringUtils.leftPad("bat", 1)  = "bat"
	 * StringUtils.leftPad("bat", -1) = "bat"
	 * </pre>
	 *
	 * @param str  the String to pad out, may be null
	 * @param size  the size to pad to
	 * @return left padded String or original String if no padding is necessary,
	 *  {@code null} if null String input
	 */
	public static String leftPad(final String str, final int size) {
		return leftPad(str, size, ' ');
	}

	/**
	 * <p>Left pad a String with a specified character.</p>
	 *
	 * <p>Pad to a size of {@code size}.</p>
	 *
	 * <pre>
	 * StringUtils.leftPad(null, *, *)	 = null
	 * StringUtils.leftPad("", 3, 'z')	 = "zzz"
	 * StringUtils.leftPad("bat", 3, 'z')  = "bat"
	 * StringUtils.leftPad("bat", 5, 'z')  = "zzbat"
	 * StringUtils.leftPad("bat", 1, 'z')  = "bat"
	 * StringUtils.leftPad("bat", -1, 'z') = "bat"
	 * </pre>
	 *
	 * @param str  the String to pad out, may be null
	 * @param size  the size to pad to
	 * @param padChar  the character to pad with
	 * @return left padded String or original String if no padding is necessary,
	 *  {@code null} if null String input
	 * @since 2.0
	 */
	public static String leftPad(final String str, final int size, final char padChar) {
		if (str == null) {
			return null;
		}
		final int pads = size - str.length();
		if (pads <= 0) {
			return str; // returns original String when possible
		}
		if (pads > 8192) {
			return leftPad(str, size, String.valueOf(padChar));
		}
		return repeat(padChar, pads).concat(str);
	}

	/**
	 * <p>Left pad a String with a specified String.</p>
	 *
	 * <p>Pad to a size of {@code size}.</p>
	 *
	 * <pre>
	 * StringUtils.leftPad(null, *, *)	  = null
	 * StringUtils.leftPad("", 3, "z")	  = "zzz"
	 * StringUtils.leftPad("bat", 3, "yz")  = "bat"
	 * StringUtils.leftPad("bat", 5, "yz")  = "yzbat"
	 * StringUtils.leftPad("bat", 8, "yz")  = "yzyzybat"
	 * StringUtils.leftPad("bat", 1, "yz")  = "bat"
	 * StringUtils.leftPad("bat", -1, "yz") = "bat"
	 * StringUtils.leftPad("bat", 5, null)  = "  bat"
	 * StringUtils.leftPad("bat", 5, "")	= "  bat"
	 * </pre>
	 *
	 * @param str  the String to pad out, may be null
	 * @param size  the size to pad to
	 * @param padStr  the String to pad with, null or empty treated as single space
	 * @return left padded String or original String if no padding is necessary,
	 *  {@code null} if null String input
	 */
	public static String leftPad(final String str, final int size, String padStr) {
		if (str == null) {
			return null;
		}
		if (isEmpty(padStr)) {
			padStr = " ";
		}
		final int padLen = padStr.length();
		final int strLen = str.length();
		final int pads = size - strLen;
		if (pads <= 0) {
			return str; // returns original String when possible
		}
		if (padLen == 1 && pads <= 8192) {
			return leftPad(str, size, padStr.charAt(0));
		}

		if (pads == padLen) {
			return padStr.concat(str);
		}
		if (pads < padLen) {
			return padStr.substring(0, pads).concat(str);
		}
		final char[] padding = new char[pads];
		final char[] padChars = padStr.toCharArray();
		for (int i = 0; i < pads; i++) {
			padding[i] = padChars[i % padLen];
		}
		return new String(padding).concat(str);
	}
	
	/**
	 * <p>Right pad a String with spaces (' ').</p>
	 *
	 * <p>The String is padded to the size of {@code size}.</p>
	 *
	 * <pre>
	 * StringUtils.rightPad(null, *)   = null
	 * StringUtils.rightPad("", 3)	 = "   "
	 * StringUtils.rightPad("bat", 3)  = "bat"
	 * StringUtils.rightPad("bat", 5)  = "bat  "
	 * StringUtils.rightPad("bat", 1)  = "bat"
	 * StringUtils.rightPad("bat", -1) = "bat"
	 * </pre>
	 *
	 * @param str  the String to pad out, may be null
	 * @param size  the size to pad to
	 * @return right padded String or original String if no padding is necessary,
	 *  {@code null} if null String input
	 */
	public static String rightPad(final String str, final int size) {
		return rightPad(str, size, ' ');
	}

	/**
	 * <p>Right pad a String with a specified character.</p>
	 *
	 * <p>The String is padded to the size of {@code size}.</p>
	 *
	 * <pre>
	 * StringUtils.rightPad(null, *, *)	 = null
	 * StringUtils.rightPad("", 3, 'z')	 = "zzz"
	 * StringUtils.rightPad("bat", 3, 'z')  = "bat"
	 * StringUtils.rightPad("bat", 5, 'z')  = "batzz"
	 * StringUtils.rightPad("bat", 1, 'z')  = "bat"
	 * StringUtils.rightPad("bat", -1, 'z') = "bat"
	 * </pre>
	 *
	 * @param str  the String to pad out, may be null
	 * @param size  the size to pad to
	 * @param padChar  the character to pad with
	 * @return right padded String or original String if no padding is necessary,
	 *  {@code null} if null String input
	 * @since 2.0
	 */
	public static String rightPad(final String str, final int size, final char padChar) {
		if (str == null) {
			return null;
		}
		final int pads = size - str.length();
		if (pads <= 0) {
			return str; // returns original String when possible
		}
		if (pads > 8192) {
			return rightPad(str, size, String.valueOf(padChar));
		}
		return str.concat(repeat(padChar, pads));
	}

	/**
	 * <p>Right pad a String with a specified String.</p>
	 *
	 * <p>The String is padded to the size of {@code size}.</p>
	 *
	 * <pre>
	 * StringUtils.rightPad(null, *, *)	  = null
	 * StringUtils.rightPad("", 3, "z")	  = "zzz"
	 * StringUtils.rightPad("bat", 3, "yz")  = "bat"
	 * StringUtils.rightPad("bat", 5, "yz")  = "batyz"
	 * StringUtils.rightPad("bat", 8, "yz")  = "batyzyzy"
	 * StringUtils.rightPad("bat", 1, "yz")  = "bat"
	 * StringUtils.rightPad("bat", -1, "yz") = "bat"
	 * StringUtils.rightPad("bat", 5, null)  = "bat  "
	 * StringUtils.rightPad("bat", 5, "")	= "bat  "
	 * </pre>
	 *
	 * @param str  the String to pad out, may be null
	 * @param size  the size to pad to
	 * @param padStr  the String to pad with, null or empty treated as single space
	 * @return right padded String or original String if no padding is necessary,
	 *  {@code null} if null String input
	 */
	public static String rightPad(final String str, final int size, String padStr) {
		if (str == null) {
			return null;
		}
		if (isEmpty(padStr)) {
			padStr = " ";
		}
		final int padLen = padStr.length();
		final int strLen = str.length();
		final int pads = size - strLen;
		if (pads <= 0) {
			return str; // returns original String when possible
		}
		if (padLen == 1 && pads <= 8192) {
			return rightPad(str, size, padStr.charAt(0));
		}

		if (pads == padLen) {
			return str.concat(padStr);
		}
		if (pads < padLen) {
			return str.concat(padStr.substring(0, pads));
		}
		final char[] padding = new char[pads];
		final char[] padChars = padStr.toCharArray();
		for (int i = 0; i < pads; i++) {
			padding[i] = padChars[i % padLen];
		}
		return str.concat(new String(padding));
	}
	
	/**
	 * <p>Returns padding using the specified delimiter repeated
	 * to a given length.</p>
	 *
	 * <pre>
	 * StringUtils.repeat('e', 0)  = ""
	 * StringUtils.repeat('e', 3)  = "eee"
	 * StringUtils.repeat('e', -2) = ""
	 * </pre>
	 *
	 * <p>Note: this method does not support padding with
	 * <a href="http://www.unicode.org/glossary/#supplementary_character">Unicode Supplementary Characters</a>
	 * as they require a pair of {@code char}s to be represented.
	 * If you are needing to support full I18N of your applications
	 * consider using {@link #repeat(String, int)} instead.
	 * </p>
	 *
	 * @param ch  character to repeat
	 * @param repeat  number of times to repeat char, negative treated as zero
	 * @return String with repeated character
	 * @see #repeat(String, int)
	 */
	public static String repeat(final char ch, final int repeat) {
		if (repeat <= 0) {
			return "";
		}
		final char[] buf = new char[repeat];
		Arrays.fill(buf, ch);
		return new String(buf);
	}

	/**
	 * <p>Repeat a String {@code repeat} times to form a
	 * new String.</p>
	 *
	 * <pre>
	 * StringUtils.repeat(null, 2) = null
	 * StringUtils.repeat("", 0)   = ""
	 * StringUtils.repeat("", 2)   = ""
	 * StringUtils.repeat("a", 3)  = "aaa"
	 * StringUtils.repeat("ab", 2) = "abab"
	 * StringUtils.repeat("a", -2) = ""
	 * </pre>
	 *
	 * @param str  the String to repeat, may be null
	 * @param repeat  number of times to repeat str, negative treated as zero
	 * @return a new String consisting of the original String repeated,
	 *  {@code null} if null String input
	 */
	public static String repeat(final String str, final int repeat) {
		// Performance tuned for 2.0 (JDK1.4)
		if (str == null) {
			return null;
		}
		if (repeat <= 0) {
			return "";
		}
		final int inputLength = str.length();
		if (repeat == 1 || inputLength == 0) {
			return str;
		}
		if (inputLength == 1 && repeat <= 8192) {
			return repeat(str.charAt(0), repeat);
		}

		final int outputLength = inputLength * repeat;
		switch (inputLength) {
			case 1 :
				return repeat(str.charAt(0), repeat);
			case 2 :
				final char ch0 = str.charAt(0);
				final char ch1 = str.charAt(1);
				final char[] output2 = new char[outputLength];
				for (int i = repeat * 2 - 2; i >= 0; i--, i--) {
					output2[i] = ch0;
					output2[i + 1] = ch1;
				}
				return new String(output2);
			default :
				final StringBuilder buf = new StringBuilder(outputLength);
				for (int i = 0; i < repeat; i++) {
					buf.append(str);
				}
				return buf.toString();
		}
	}

	/**
	 * <p>Repeat a String {@code repeat} times to form a
	 * new String, with a String separator injected each time. </p>
	 *
	 * <pre>
	 * StringUtils.repeat(null, null, 2) = null
	 * StringUtils.repeat(null, "x", 2)  = null
	 * StringUtils.repeat("", null, 0)   = ""
	 * StringUtils.repeat("", "", 2)	 = ""
	 * StringUtils.repeat("", "x", 3)	= "xxx"
	 * StringUtils.repeat("?", ", ", 3)  = "?, ?, ?"
	 * </pre>
	 *
	 * @param str		the String to repeat, may be null
	 * @param separator  the String to inject, may be null
	 * @param repeat	 number of times to repeat str, negative treated as zero
	 * @return a new String consisting of the original String repeated,
	 *  {@code null} if null String input
	 * @since 2.5
	 */
	public static String repeat(final String str, final String separator, final int repeat) {
		if (str == null || separator == null) {
			return repeat(str, repeat);
		}
		// given that repeat(String, int) is quite optimized, better to rely on it than try and splice this into it
		final String result = repeat(str + separator, repeat);
		return removeEnd(result, separator);
	}
	
	/**
	 * <p>Removes a substring only if it is at the end of a source string,
	 * otherwise returns the source string.</p>
	 *
	 * <p>A {@code null} source string will return {@code null}.
	 * An empty ("") source string will return the empty string.
	 * A {@code null} search string will return the source string.</p>
	 *
	 * <pre>
	 * StringUtils.removeEnd(null, *)	  = null
	 * StringUtils.removeEnd("", *)		= ""
	 * StringUtils.removeEnd(*, null)	  = *
	 * StringUtils.removeEnd("www.domain.com", ".com.")  = "www.domain.com"
	 * StringUtils.removeEnd("www.domain.com", ".com")   = "www.domain"
	 * StringUtils.removeEnd("www.domain.com", "domain") = "www.domain.com"
	 * StringUtils.removeEnd("abc", "")	= "abc"
	 * </pre>
	 *
	 * @param str  the source String to search, may be null
	 * @param remove  the String to search for and remove, may be null
	 * @return the substring with the string removed if found,
	 *  {@code null} if null String input
	 * @since 2.1
	 */
	public static String removeEnd(final String str, final String remove) {
		if (isEmpty(str) || isEmpty(remove)) {
			return str;
		}
		if (str.endsWith(remove)) {
			return str.substring(0, str.length() - remove.length());
		}
		return str;
	}

	/**
	 * Extracts the initial characters from each word in the String.
	 *
	 * <p>All first characters after the defined delimiters are returned as a new string.
	 * Their case is not changed.</p>
	 *
	 * <p>If the delimiters array is null, then Whitespace is used.
	 * Whitespace is defined by {@link Character#isWhitespace(char)}.
	 * A {@code null} input String returns {@code null}.
	 * An empty delimiter array returns an empty String.</p>
	 *
	 * <pre>
	 * WordUtils.initials(null, *)				= null
	 * WordUtils.initials("", *)				  = ""
	 * WordUtils.initials("Ben John Lee", null)   = "BJL"
	 * WordUtils.initials("Ben J.Lee", null)	  = "BJ"
	 * WordUtils.initials("Ben J.Lee", [' ','.']) = "BJL"
	 * WordUtils.initials(*, new char[0])		 = ""
	 * </pre>
	 *
	 * @param str  the String to get initials from, may be null
	 * @param delimiters  set of characters to determine words, null means whitespace
	 * @return String of initial characters, {@code null} if null String input
	 * @see #initials(String)
	 */
	public static String initials(final String str, final char... delimiters) {
		if (isEmpty(str)) {
			return str;
		}
		if (delimiters != null && delimiters.length == 0) {
			return "";
		}
		final Set<Integer> delimiterSet = generateDelimiterSet(delimiters);
		final int strLen = str.length();
		final int[] newCodePoints = new int[strLen / 2 + 1];
		int count = 0;
		boolean lastWasGap = true;
		for (int i = 0; i < strLen;) {
			final int codePoint = str.codePointAt(i);

			if (delimiterSet.contains(codePoint) || (delimiters == null && Character.isWhitespace(codePoint))) {
				lastWasGap = true;
			} else if (lastWasGap) {
				newCodePoints[count++] = codePoint;
				lastWasGap = false;
			}

			i += Character.charCount(codePoint);
		}
		return new String(newCodePoints, 0, count);
	}

	/**
	 * Is the character a delimiter.
	 *
	 * @param ch the character to check
	 * @param delimiters the delimiters
	 * @return true if it is a delimiter
	 * @deprecated as of 1.2 and will be removed in 2.0
	 */
	@Deprecated
	public static boolean isDelimiter(final char ch, final char[] delimiters) {
		if (delimiters == null) {
			return Character.isWhitespace(ch);
		}
		for (final char delimiter : delimiters) {
			if (ch == delimiter) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Is the codePoint a delimiter.
	 *
	 * @param codePoint the codePint to check
	 * @param delimiters the delimiters
	 * @return true if it is a delimiter
	 * @deprecated as of 1.2 and will be removed in 2.0
	 */
	@Deprecated
	public static boolean isDelimiter(final int codePoint, final char[] delimiters) {
		if (delimiters == null) {
			return Character.isWhitespace(codePoint);
		}
		for (int index = 0; index < delimiters.length; index++) {
			final int delimiterCodePoint = Character.codePointAt(delimiters, index);
			if (delimiterCodePoint == codePoint) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Swaps the case of a String using a word based algorithm.
	 *
	 * <ul>
	 *  <li>Upper case character converts to Lower case</li>
	 *  <li>Title case character converts to Lower case</li>
	 *  <li>Lower case character after Whitespace or at start converts to Title case</li>
	 *  <li>Other Lower case character converts to Upper case</li>
	 * </ul>
	 *
	 * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.
	 * A {@code null} input String returns {@code null}.</p>
	 *
	 * <pre>
	 * StringUtils.swapCase(null)				 = null
	 * StringUtils.swapCase("")				   = ""
	 * StringUtils.swapCase("The dog has a BONE") = "tHE DOG HAS A bone"
	 * </pre>
	 *
	 * @param str  the String to swap case, may be null
	 * @return The changed String, {@code null} if null String input
	 */
	public static String swapCase(final String str) {
		if (isEmpty(str)) {
			return str;
		}
		final int strLen = str.length();
		final int[] newCodePoints = new int[strLen];
		int outOffset = 0;
		boolean whitespace = true;
		for (int index = 0; index < strLen;) {
			final int oldCodepoint = str.codePointAt(index);
			final int newCodePoint;
			if (Character.isUpperCase(oldCodepoint) || Character.isTitleCase(oldCodepoint)) {
				newCodePoint = Character.toLowerCase(oldCodepoint);
				whitespace = false;
			} else if (Character.isLowerCase(oldCodepoint)) {
				if (whitespace) {
					newCodePoint = Character.toTitleCase(oldCodepoint);
					whitespace = false;
				} else {
					newCodePoint = Character.toUpperCase(oldCodepoint);
				}
			} else {
				whitespace = Character.isWhitespace(oldCodepoint);
				newCodePoint = oldCodepoint;
			}
			newCodePoints[outOffset++] = newCodePoint;
			index += Character.charCount(newCodePoint);
		}
		return new String(newCodePoints, 0, outOffset);
	}

	/**
	 * Uncapitalizes all the whitespace separated words in a String.
	 * Only the first character of each word is changed.
	 *
	 * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.
	 * A {@code null} input String returns {@code null}.</p>
	 *
	 * <pre>
	 * WordUtils.uncapitalize(null)		= null
	 * WordUtils.uncapitalize("")		  = ""
	 * WordUtils.uncapitalize("I Am FINE") = "i am fINE"
	 * </pre>
	 *
	 * @param str  the String to uncapitalize, may be null
	 * @return uncapitalized String, {@code null} if null String input
	 * @see #capitalize(String)
	 */
	public static String uncapitalize(final String str) {
		return uncapitalize(str, null);
	}

	/**
	 * Uncapitalizes all the whitespace separated words in a String.
	 * Only the first character of each word is changed.
	 *
	 * <p>The delimiters represent a set of characters understood to separate words.
	 * The first string character and the first non-delimiter character after a
	 * delimiter will be uncapitalized.</p>
	 *
	 * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.
	 * A {@code null} input String returns {@code null}.</p>
	 *
	 * <pre>
	 * WordUtils.uncapitalize(null, *)			= null
	 * WordUtils.uncapitalize("", *)			  = ""
	 * WordUtils.uncapitalize(*, null)			= *
	 * WordUtils.uncapitalize(*, new char[0])	 = *
	 * WordUtils.uncapitalize("I AM.FINE", {'.'}) = "i AM.fINE"
	 * WordUtils.uncapitalize("I am fine", new char[]{}) = "i am fine"
	 * </pre>
	 *
	 * @param str  the String to uncapitalize, may be null
	 * @param delimiters  set of characters to determine uncapitalization, null means whitespace
	 * @return uncapitalized String, {@code null} if null String input
	 * @see #capitalize(String)
	 */
	public static String uncapitalize(final String str, final char... delimiters) {
		if (isEmpty(str)) {
			return str;
		}
		final Set<Integer> delimiterSet = generateDelimiterSet(delimiters);
		final int strLen = str.length();
		final int[] newCodePoints = new int[strLen];
		int outOffset = 0;

		boolean uncapitalizeNext = true;
		for (int index = 0; index < strLen;) {
			final int codePoint = str.codePointAt(index);

			if (delimiterSet.contains(codePoint)) {
				uncapitalizeNext = true;
				newCodePoints[outOffset++] = codePoint;
				index += Character.charCount(codePoint);
			} else if (uncapitalizeNext) {
				final int titleCaseCodePoint = Character.toLowerCase(codePoint);
				newCodePoints[outOffset++] = titleCaseCodePoint;
				index += Character.charCount(titleCaseCodePoint);
				uncapitalizeNext = false;
			} else {
				newCodePoints[outOffset++] = codePoint;
				index += Character.charCount(codePoint);
			}
		}
		return new String(newCodePoints, 0, outOffset);
	}

	/**
	 * Wraps a single line of text, identifying words by {@code ' '}.
	 *
	 * <p>New lines will be separated by the system property line separator.
	 * Very long words, such as URLs will <i>not</i> be wrapped.</p>
	 *
	 * <p>Leading spaces on a new line are stripped.
	 * Trailing spaces are not stripped.</p>
	 *
	 * <table border="1">
	 *  <caption>Examples</caption>
	 *  <tr>
	 *   <th>input</th>
	 *   <th>wrapLength</th>
	 *   <th>result</th>
	 *  </tr>
	 *  <tr>
	 *   <td>null</td>
	 *   <td>*</td>
	 *   <td>null</td>
	 *  </tr>
	 *  <tr>
	 *   <td>""</td>
	 *   <td>*</td>
	 *   <td>""</td>
	 *  </tr>
	 *  <tr>
	 *   <td>"Here is one line of text that is going to be wrapped after 20 columns."</td>
	 *   <td>20</td>
	 *   <td>"Here is one line of\ntext that is going\nto be wrapped after\n20 columns."</td>
	 *  </tr>
	 *  <tr>
	 *   <td>"Click here to jump to the commons website - https://commons.apache.org"</td>
	 *   <td>20</td>
	 *   <td>"Click here to jump\nto the commons\nwebsite -\nhttps://commons.apache.org"</td>
	 *  </tr>
	 *  <tr>
	 *   <td>"Click here, https://commons.apache.org, to jump to the commons website"</td>
	 *   <td>20</td>
	 *   <td>"Click here,\nhttps://commons.apache.org,\nto jump to the\ncommons website"</td>
	 *  </tr>
	 * </table>
	 *
	 * (assuming that '\n' is the systems line separator)
	 *
	 * @param str  the String to be word wrapped, may be null
	 * @param wrapLength  the column to wrap the words at, less than 1 is treated as 1
	 * @return a line with newlines inserted, {@code null} if null input
	 */
	public static String wrap(final String str, final int wrapLength) {
		return wrap(str, wrapLength, null, null, 0, false);
	}

	/**
	 * Wraps a single line of text, identifying words by {@code ' '}.
	 *
	 * <p>Leading spaces on a new line are stripped.
	 * Trailing spaces are not stripped.</p>
	 *
	 * <table border="1">
	 *  <caption>Examples</caption>
	 *  <tr>
	 *   <th>input</th>
	 *   <th>wrapLength</th>
	 *   <th>newLineString</th>
	 *   <th>wrapLongWords</th>
	 *   <th>result</th>
	 *  </tr>
	 *  <tr>
	 *   <td>null</td>
	 *   <td>*</td>
	 *   <td>*</td>
	 *   <td>true/false</td>
	 *   <td>null</td>
	 *  </tr>
	 *  <tr>
	 *   <td>""</td>
	 *   <td>*</td>
	 *   <td>*</td>
	 *   <td>true/false</td>
	 *   <td>""</td>
	 *  </tr>
	 *  <tr>
	 *   <td>"Here is one line of text that is going to be wrapped after 20 columns."</td>
	 *   <td>20</td>
	 *   <td>"\n"</td>
	 *   <td>true/false</td>
	 *   <td>"Here is one line of\ntext that is going\nto be wrapped after\n20 columns."</td>
	 *  </tr>
	 *  <tr>
	 *   <td>"Here is one line of text that is going to be wrapped after 20 columns."</td>
	 *   <td>20</td>
	 *   <td>"&lt;br /&gt;"</td>
	 *   <td>true/false</td>
	 *   <td>"Here is one line of&lt;br /&gt;text that is going&lt;
	 *   br /&gt;to be wrapped after&lt;br /&gt;20 columns."</td>
	 *  </tr>
	 *  <tr>
	 *   <td>"Here is one line of text that is going to be wrapped after 20 columns."</td>
	 *   <td>20</td>
	 *   <td>null</td>
	 *   <td>true/false</td>
	 *   <td>"Here is one line of" + systemNewLine + "text that is going"
	 *   + systemNewLine + "to be wrapped after" + systemNewLine + "20 columns."</td>
	 *  </tr>
	 *  <tr>
	 *   <td>"Click here to jump to the commons website - https://commons.apache.org"</td>
	 *   <td>20</td>
	 *   <td>"\n"</td>
	 *   <td>false</td>
	 *   <td>"Click here to jump\nto the commons\nwebsite -\nhttps://commons.apache.org"</td>
	 *  </tr>
	 *  <tr>
	 *   <td>"Click here to jump to the commons website - https://commons.apache.org"</td>
	 *   <td>20</td>
	 *   <td>"\n"</td>
	 *   <td>true</td>
	 *   <td>"Click here to jump\nto the commons\nwebsite -\nhttp://commons.apach\ne.org"</td>
	 *  </tr>
	 * </table>
	 *
	 * @param str  the String to be word wrapped, may be null
	 * @param wrapLength  the column to wrap the words at, less than 1 is treated as 1
	 * @param newLineStr  the string to insert for a new line,
	 *  {@code null} uses the system property line separator
	 * @param wrapLongWords  true if long words (such as URLs) should be wrapped
	 * @return a line with newlines inserted, {@code null} if null input
	 */
	public static String wrap(final String str,
							  final int wrapLength,
							  final String newLineStr,
							  final String newLineSpacer,
							  final int firstLineHeadLength,
							  final boolean wrapLongWords) {
		return wrap(str, wrapLength, newLineStr, newLineSpacer, firstLineHeadLength, wrapLongWords, " ");
	}

	/**
	 * Wraps a single line of text, identifying words by {@code wrapOn}.
	 *
	 * <p>Leading spaces on a new line are stripped.
	 * Trailing spaces are not stripped.</p>
	 *
	 * <table border="1">
	 *  <caption>Examples</caption>
	 *  <tr>
	 *   <th>input</th>
	 *   <th>wrapLength</th>
	 *   <th>newLineString</th>
	 *   <th>wrapLongWords</th>
	 *   <th>wrapOn</th>
	 *   <th>result</th>
	 *  </tr>
	 *  <tr>
	 *   <td>null</td>
	 *   <td>*</td>
	 *   <td>*</td>
	 *   <td>true/false</td>
	 *   <td>*</td>
	 *   <td>null</td>
	 *  </tr>
	 *  <tr>
	 *   <td>""</td>
	 *   <td>*</td>
	 *   <td>*</td>
	 *   <td>true/false</td>
	 *   <td>*</td>
	 *   <td>""</td>
	 *  </tr>
	 *  <tr>
	 *   <td>"Here is one line of text that is going to be wrapped after 20 columns."</td>
	 *   <td>20</td>
	 *   <td>"\n"</td>
	 *   <td>true/false</td>
	 *   <td>" "</td>
	 *   <td>"Here is one line of\ntext that is going\nto be wrapped after\n20 columns."</td>
	 *  </tr>
	 *  <tr>
	 *   <td>"Here is one line of text that is going to be wrapped after 20 columns."</td>
	 *   <td>20</td>
	 *   <td>"&lt;br /&gt;"</td>
	 *   <td>true/false</td>
	 *   <td>" "</td>
	 *   <td>"Here is one line of&lt;br /&gt;text that is going&lt;br /&gt;
	 *   to be wrapped after&lt;br /&gt;20 columns."</td>
	 *  </tr>
	 *  <tr>
	 *   <td>"Here is one line of text that is going to be wrapped after 20 columns."</td>
	 *   <td>20</td>
	 *   <td>null</td>
	 *   <td>true/false</td>
	 *   <td>" "</td>
	 *   <td>"Here is one line of" + systemNewLine + "text that is going"
	 *   + systemNewLine + "to be wrapped after" + systemNewLine + "20 columns."</td>
	 *  </tr>
	 *  <tr>
	 *   <td>"Click here to jump to the commons website - https://commons.apache.org"</td>
	 *   <td>20</td>
	 *   <td>"\n"</td>
	 *   <td>false</td>
	 *   <td>" "</td>
	 *   <td>"Click here to jump\nto the commons\nwebsite -\nhttps://commons.apache.org"</td>
	 *  </tr>
	 *  <tr>
	 *   <td>"Click here to jump to the commons website - https://commons.apache.org"</td>
	 *   <td>20</td>
	 *   <td>"\n"</td>
	 *   <td>true</td>
	 *   <td>" "</td>
	 *   <td>"Click here to jump\nto the commons\nwebsite -\nhttp://commons.apach\ne.org"</td>
	 *  </tr>
	 *  <tr>
	 *   <td>"flammable/inflammable"</td>
	 *   <td>20</td>
	 *   <td>"\n"</td>
	 *   <td>true</td>
	 *   <td>"/"</td>
	 *   <td>"flammable\ninflammable"</td>
	 *  </tr>
	 * </table>
	 * @param str  the String to be word wrapped, may be null
	 * @param wrapLengthTotal  the column to wrap the words at, less than 1 is treated as 1
	 * @param newLineStr  the string to insert for a new line,
	 *  {@code null} uses the system property line separator
	 * @param wrapLongWords  true if long words (such as URLs) should be wrapped
	 * @param wrapOn regex expression to be used as a breakable characters,
	 *			   if blank string is provided a space character will be used
	 * @return a line with newlines inserted, {@code null} if null input
	 */
	public static String wrap(final String str,
							  final int wrapLengthTotal,
							  String newLineStr,
							  String newLineSpacer,
							  int firstLineHeadLength,
							  final boolean wrapLongWords,
							  String wrapOn) {
		if (str == null) {
			return null;
		}
		if (newLineStr == null) {
			newLineStr = System.lineSeparator();
		}
		if(newLineSpacer == null) {
			newLineSpacer = "";
		}
		int wrapLength = wrapLengthTotal;
		if (wrapLength < 1) {
			wrapLength = 1;
		}
		if (firstLineHeadLength < 0) {
			firstLineHeadLength = 0;
		}
		if (isBlank(wrapOn)) {
			wrapOn = " ";
		}
		final Pattern patternToWrapOn = Pattern.compile(wrapOn);
		final int inputLineLength = str.length();
		int offset = 0;
		final StringBuilder wrappedLine = new StringBuilder(inputLineLength + 32);
		int matcherSize = -1;

		boolean first = true;

		while (offset < inputLineLength) {
			if(first) {
				first = false;
				wrapLength = wrapLengthTotal - firstLineHeadLength;
				if(wrapLength < 1) {
					wrapLength = 1;
				}
			} else {
				wrapLength = wrapLengthTotal - newLineSpacer.length();
				if (wrapLength < 1) {
					wrapLength = 1;
				}
			}
			int spaceToWrapAt = -1;
			Matcher matcher = patternToWrapOn.matcher(str.substring(offset,
					Math.min((int) Math.min(Integer.MAX_VALUE, offset + wrapLength + 1L), inputLineLength)));
			if (matcher.find()) {
				if (matcher.start() == 0) {
					matcherSize = matcher.end();
					if (matcherSize != 0) {
						offset += matcher.end();
						continue;
					}
					offset += 1;
				}
				spaceToWrapAt = matcher.start() + offset;
			}

			// only last line without leading spaces is left
			if (inputLineLength - offset <= wrapLength) {
				break;
			}

			while (matcher.find()) {
				spaceToWrapAt = matcher.start() + offset;
			}

			if (spaceToWrapAt >= offset) {
				// normal case
				wrappedLine.append(str, offset, spaceToWrapAt);
				wrappedLine.append(newLineStr);
				wrappedLine.append(newLineSpacer);
				offset = spaceToWrapAt + 1;

			} else {
				// really long word or URL
				if (wrapLongWords) {
					if (matcherSize == 0) {
						offset--;
					}
					// wrap really long word one line at a time
					wrappedLine.append(str, offset, wrapLength + offset);
					wrappedLine.append(newLineStr);
					wrappedLine.append(newLineSpacer);
					offset += wrapLength;
					matcherSize = -1;
				} else {
					// do not wrap really long word, just extend beyond limit
					matcher = patternToWrapOn.matcher(str.substring(offset + wrapLength));
					if (matcher.find()) {
						matcherSize = matcher.end() - matcher.start();
						spaceToWrapAt = matcher.start() + offset + wrapLength;
					}

					if (spaceToWrapAt >= 0) {
						if (matcherSize == 0 && offset != 0) {
							offset--;
						}
						wrappedLine.append(str, offset, spaceToWrapAt);
						wrappedLine.append(newLineStr);
						wrappedLine.append(newLineSpacer);
						offset = spaceToWrapAt + 1;
					} else {
						if (matcherSize == 0 && offset != 0) {
							offset--;
						}
						wrappedLine.append(str, offset, str.length());
						offset = inputLineLength;
						matcherSize = -1;
					}
				}
			}
		}

		if (matcherSize == 0 && offset < inputLineLength) {
			offset--;
		}

		// Whatever is left in line is short enough to just pass through
		wrappedLine.append(str, offset, str.length());

		return wrappedLine.toString();
	}

	/**
	 * {@code WordUtils} instances should NOT be constructed in
	 * standard programming. Instead, the class should be used as
	 * {@code WordUtils.wrap("foo bar", 20);}.
	 *
	 * <p>This constructor is public to permit tools that require a JavaBean
	 * instance to operate.</p>
	 */
	public TextUtils() {}
	
 }
