# Intervue DSA Blind List - Part 2

Target: Marriott Tech Accelerator first round through Intervue.

How to use this sheet:
- First read the pattern and hints.
- Try to write the Java code without looking at the solution.
- Then compare with the solution.
- Finally, explain the dry run out loud as if the interviewer is listening.

Interview rule:
- Always clarify constraints first.
- State brute force.
- Move to optimized.
- Code cleanly.
- Mention time and space complexity.
- Test edge cases.

---

## Problem 6: Maximum Subarray

### Problem Statement

Given an integer array `nums`, find the contiguous subarray with the largest sum.

Return the maximum sum.

A subarray must be contiguous.

### Examples

#### Example 1

Input:

```text
nums = [-2,1,-3,4,-1,2,1,-5,4]
```

Output:

```text
6
```

Explanation:

The best subarray is:

```text
[4,-1,2,1]
```

Sum:

```text
4 + (-1) + 2 + 1 = 6
```

#### Example 2

Input:

```text
nums = [1]
```

Output:

```text
1
```

Explanation:
- Only one subarray exists: `[1]`.

#### Example 3

Input:

```text
nums = [-3,-2,-5]
```

Output:

```text
-2
```

Explanation:
- When all numbers are negative, choose the least negative single element.

### Pattern

Kadane's algorithm.

The main idea:
- At each index, decide whether to extend the previous subarray or start fresh from the current element.
- If the previous running sum hurts us, drop it.

### How To Recognize This Pattern

Use this pattern when:
- The problem asks for maximum or minimum sum of a contiguous subarray.
- You need the best contiguous segment.
- You are deciding "continue previous segment or start new segment".

### Hints Before Solution

#### Brute Force Hint

Try every possible subarray.

For every start index:
- Extend to every end index.
- Keep calculating sum.
- Track maximum sum.

Basic brute force:
- Time: `O(n^3)` if you recompute each subarray sum.

Improved brute force:
- Time: `O(n^2)` if you maintain running sum for each start index.

Why this is not ideal:
- For large arrays, `O(n^2)` is still too slow.

#### Optimized Hint

At every number, ask:

```text
Is it better to add this number to the previous subarray,
or start a new subarray from this number?
```

Formula:

```text
currentSum = max(nums[i], currentSum + nums[i])
bestSum = max(bestSum, currentSum)
```

### Solution

```java
public class MaximumSubarray {
    public static int maxSubArray(int[] nums) {
        int currentSum = nums[0];
        int bestSum = nums[0];

        for (int i = 1; i < nums.length; i++) {
            currentSum = Math.max(nums[i], currentSum + nums[i]);
            bestSum = Math.max(bestSum, currentSum);
        }

        return bestSum;
    }
}
```

### Dry Run

Input:

```text
nums = [-2,1,-3,4,-1,2,1,-5,4]
```

Initial:

```text
currentSum = -2
bestSum = -2
```

| i | nums[i] | currentSum + nums[i] | New currentSum | New bestSum | Meaning |
|---:|---:|---:|---:|---:|---|
| 1 | 1 | -1 | 1 | 1 | Start fresh at 1 |
| 2 | -3 | -2 | -2 | 1 | Extend: `[1,-3]` |
| 3 | 4 | 2 | 4 | 4 | Start fresh at 4 |
| 4 | -1 | 3 | 3 | 4 | Extend: `[4,-1]` |
| 5 | 2 | 5 | 5 | 5 | Extend: `[4,-1,2]` |
| 6 | 1 | 6 | 6 | 6 | Extend: `[4,-1,2,1]` |
| 7 | -5 | 1 | 1 | 6 | Extend but best stays 6 |
| 8 | 4 | 5 | 5 | 6 | Extend but best stays 6 |

Final:

```text
bestSum = 6
```

Answer:

```text
6
```

### Complexity

- Time: `O(n)`
- Space: `O(1)`

### Edge Cases

- Single element array.
- All negative numbers.
- All positive numbers.
- Zeros in array.
- Maximum subarray at the beginning.
- Maximum subarray at the end.

### Same Mindset Problems

If you get this, you can solve:
- Best Time to Buy and Sell Stock
- Maximum Product Subarray
- Maximum Sum Circular Subarray
- Longest Turbulent Subarray
- Minimum Size Subarray Sum, with a different sliding window mindset

### If You See This Twist

| Twist | What To Do |
|---|---|
| Need maximum sum only | Kadane's algorithm |
| Need subarray indices also | Track temporary start and best start/end |
| Need maximum product | Track both max and min because negative flips sign |
| Circular array | Compare normal Kadane with total sum minus minimum subarray |
| Need length constraint | Use prefix sum / sliding window depending on constraints |

### Variant: Return Start And End Index

```java
public class MaximumSubarrayWithIndices {
    public static int[] maxSubArrayWithIndices(int[] nums) {
        int currentSum = nums[0];
        int bestSum = nums[0];

        int tempStart = 0;
        int bestStart = 0;
        int bestEnd = 0;

        for (int i = 1; i < nums.length; i++) {
            if (nums[i] > currentSum + nums[i]) {
                currentSum = nums[i];
                tempStart = i;
            } else {
                currentSum += nums[i];
            }

            if (currentSum > bestSum) {
                bestSum = currentSum;
                bestStart = tempStart;
                bestEnd = i;
            }
        }

        return new int[] {bestSum, bestStart, bestEnd};
    }
}
```

---

## Problem 7: Longest Substring Without Repeating Characters

### Problem Statement

Given a string `s`, find the length of the longest substring without repeating characters.

A substring must be contiguous.

### Examples

#### Example 1

Input:

```text
s = "abcabcbb"
```

Output:

```text
3
```

Explanation:
- The answer is `"abc"`.
- Length is `3`.

#### Example 2

Input:

```text
s = "bbbbb"
```

Output:

```text
1
```

Explanation:
- The answer is `"b"`.
- Every longer substring has repeated `b`.

#### Example 3

Input:

```text
s = "pwwkew"
```

Output:

```text
3
```

Explanation:
- The answer is `"wke"`.
- `"pwke"` is not valid because it is not contiguous.

### Pattern

Sliding window + HashSet or HashMap.

The main idea:
- Maintain a window with no duplicate characters.
- Expand right pointer.
- If duplicate appears, move left pointer until the window becomes valid again.

### How To Recognize This Pattern

Use this pattern when:
- The problem asks for longest or shortest contiguous substring/subarray.
- There is a condition the window must satisfy.
- You can move left and right pointers in one pass.

### Hints Before Solution

#### Brute Force Hint

Try every substring.

For every start index:
- Extend end index.
- Check if substring has duplicates.
- Track maximum valid length.

Time:
- `O(n^3)` if duplicate check scans each substring.
- `O(n^2)` if using a set for each start.

Why this is not ideal:
- Rechecking the same characters repeatedly wastes time.

#### Optimized Hint

Use a sliding window.

Window rule:

```text
All characters inside window must be unique.
```

Use a map from character to latest index.

When you see a repeated character:
- Move `left` to one position after the previous occurrence.
- But only move `left` forward, never backward.

Formula:

```text
left = Math.max(left, lastSeen[ch] + 1)
```

### Solution 1: HashMap Latest Index

```java
import java.util.*;

public class LongestSubstringWithoutRepeating {
    public static int lengthOfLongestSubstring(String s) {
        Map<Character, Integer> lastSeen = new HashMap<>();
        int left = 0;
        int best = 0;

        for (int right = 0; right < s.length(); right++) {
            char ch = s.charAt(right);

            if (lastSeen.containsKey(ch)) {
                left = Math.max(left, lastSeen.get(ch) + 1);
            }

            lastSeen.put(ch, right);
            best = Math.max(best, right - left + 1);
        }

        return best;
    }
}
```

### Solution 2: HashSet Window

```java
import java.util.*;

public class LongestSubstringWithSet {
    public static int lengthOfLongestSubstring(String s) {
        Set<Character> window = new HashSet<>();
        int left = 0;
        int best = 0;

        for (int right = 0; right < s.length(); right++) {
            char ch = s.charAt(right);

            while (window.contains(ch)) {
                window.remove(s.charAt(left));
                left++;
            }

            window.add(ch);
            best = Math.max(best, right - left + 1);
        }

        return best;
    }
}
```

### Dry Run

Input:

```text
s = "abcabcbb"
```

Using HashMap latest index:

Initial:

```text
left = 0
best = 0
lastSeen = {}
```

| right | ch | Duplicate? | left update | Window | best |
|---:|---|---|---:|---|---:|
| 0 | a | No | 0 | `a` | 1 |
| 1 | b | No | 0 | `ab` | 2 |
| 2 | c | No | 0 | `abc` | 3 |
| 3 | a | Yes, last at 0 | 1 | `bca` | 3 |
| 4 | b | Yes, last at 1 | 2 | `cab` | 3 |
| 5 | c | Yes, last at 2 | 3 | `abc` | 3 |
| 6 | b | Yes, last at 4 | 5 | `cb` | 3 |
| 7 | b | Yes, last at 6 | 7 | `b` | 3 |

Final:

```text
best = 3
```

Answer:

```text
3
```

### Why `Math.max` Is Important

Input:

```text
"abba"
```

When right reaches the last `a`:
- Previous `a` was at index `0`.
- But `left` is already at index `2`.

If we blindly do:

```java
left = lastSeen.get(ch) + 1;
```

Then `left` becomes `1`, which moves backward and breaks the window.

Correct:

```java
left = Math.max(left, lastSeen.get(ch) + 1);
```

### Complexity

- Time: `O(n)`
- Space: `O(min(n, characterSetSize))`

### Edge Cases

- Empty string -> `0`.
- One character -> `1`.
- All same characters -> `1`.
- All unique characters -> length of string.
- Repeated character outside current window.

### Same Mindset Problems

If you get this, you can solve:
- Longest Repeating Character Replacement
- Minimum Window Substring
- Permutation in String
- Find All Anagrams in a String
- Maximum Number of Vowels in a Substring of Given Length

### If You See This Twist

| Twist | What To Do |
|---|---|
| Need longest substring without duplicates | Sliding window + last seen map |
| Need exactly k distinct chars | Sliding window + frequency map |
| Need at most k distinct chars | Sliding window + frequency map and shrink when size > k |
| Need fixed window size | Move both pointers in fixed steps |
| Need actual substring | Track best start and best length |

### Variant: Return Actual Substring

```java
import java.util.*;

public class LongestSubstringValue {
    public static String longestSubstring(String s) {
        Map<Character, Integer> lastSeen = new HashMap<>();
        int left = 0;
        int bestStart = 0;
        int bestLength = 0;

        for (int right = 0; right < s.length(); right++) {
            char ch = s.charAt(right);

            if (lastSeen.containsKey(ch)) {
                left = Math.max(left, lastSeen.get(ch) + 1);
            }

            lastSeen.put(ch, right);

            int currentLength = right - left + 1;
            if (currentLength > bestLength) {
                bestLength = currentLength;
                bestStart = left;
            }
        }

        return s.substring(bestStart, bestStart + bestLength);
    }
}
```

---

## Problem 8: Count Vowels, Consonants, And Duplicate Characters

### Problem Statement

This problem commonly appears in multiple small variants.

Variant A:

Given a string, count the number of vowels and consonants.

Variant B:

Given a string, return duplicate characters and their frequencies.

These are basic string frequency problems. They test whether you can parse characters cleanly and use a frequency array or HashMap.

### Examples

#### Example 1: Count Vowels And Consonants

Input:

```text
s = "Interview"
```

Output:

```text
vowels = 4
consonants = 5
```

Explanation:
- Vowels: `I`, `e`, `i`, `e`
- Consonants: `n`, `t`, `r`, `v`, `w`

#### Example 2: Duplicate Characters

Input:

```text
s = "programming"
```

Output:

```text
r -> 2
g -> 2
m -> 2
```

Explanation:
- `r`, `g`, and `m` appear more than once.

#### Example 3: Ignore Spaces And Punctuation

Input:

```text
s = "Hello, World!"
```

Output:

```text
vowels = 3
consonants = 7
```

Explanation:
- Vowels: `e`, `o`, `o`
- Consonants: `h`, `l`, `l`, `w`, `r`, `l`, `d`
- Comma, space, and exclamation mark are ignored.

### Pattern

String traversal + frequency counting.

The main idea:
- Normalize the character if needed.
- Check whether it is a letter.
- Count it based on condition.

### How To Recognize This Pattern

Use this pattern when:
- The problem asks for character count.
- The problem asks for duplicates.
- The problem asks for frequency.
- The input is a string and order is not the main concern.

### Hints Before Solution

#### Brute Force Hint

For every character:
- Scan the whole string again to count occurrences.

Time:
- `O(n^2)`

Why this is not ideal:
- Same counts are recomputed repeatedly.

#### Optimized Hint

Use one pass.

For vowel/consonant count:
- Convert each character to lowercase.
- Ignore non-letters.
- If vowel, increment vowel count.
- Else increment consonant count.

For duplicate characters:
- Use a frequency map.
- After building the map, print entries with count greater than `1`.

### Solution 1: Count Vowels And Consonants

```java
public class VowelConsonantCount {
    public static int[] countVowelsAndConsonants(String s) {
        int vowels = 0;
        int consonants = 0;

        for (int i = 0; i < s.length(); i++) {
            char ch = Character.toLowerCase(s.charAt(i));

            if (!Character.isLetter(ch)) {
                continue;
            }

            if (isVowel(ch)) {
                vowels++;
            } else {
                consonants++;
            }
        }

        return new int[] {vowels, consonants};
    }

    private static boolean isVowel(char ch) {
        return ch == 'a'
            || ch == 'e'
            || ch == 'i'
            || ch == 'o'
            || ch == 'u';
    }
}
```

### Solution 2: Duplicate Characters

```java
import java.util.*;

public class DuplicateCharacters {
    public static Map<Character, Integer> duplicateCharacters(String s) {
        Map<Character, Integer> frequency = new LinkedHashMap<>();

        for (int i = 0; i < s.length(); i++) {
            char ch = Character.toLowerCase(s.charAt(i));

            if (!Character.isLetterOrDigit(ch)) {
                continue;
            }

            frequency.put(ch, frequency.getOrDefault(ch, 0) + 1);
        }

        Map<Character, Integer> duplicates = new LinkedHashMap<>();
        for (Map.Entry<Character, Integer> entry : frequency.entrySet()) {
            if (entry.getValue() > 1) {
                duplicates.put(entry.getKey(), entry.getValue());
            }
        }

        return duplicates;
    }
}
```

### Dry Run

Input:

```text
s = "Interview"
```

Normalize characters to lowercase:

```text
"interview"
```

Initial:

```text
vowels = 0
consonants = 0
```

| Character | Letter? | Vowel? | vowels | consonants |
|---|---|---|---:|---:|
| i | Yes | Yes | 1 | 0 |
| n | Yes | No | 1 | 1 |
| t | Yes | No | 1 | 2 |
| e | Yes | Yes | 2 | 2 |
| r | Yes | No | 2 | 3 |
| v | Yes | No | 2 | 4 |
| i | Yes | Yes | 3 | 4 |
| e | Yes | Yes | 4 | 4 |
| w | Yes | No | 4 | 5 |

Final:

```text
vowels = 4
consonants = 5
```

Duplicate dry run:

Input:

```text
s = "programming"
```

Frequency map:

| Character | Count |
|---|---:|
| p | 1 |
| r | 2 |
| o | 1 |
| g | 2 |
| a | 1 |
| m | 2 |
| i | 1 |
| n | 1 |

Duplicates:

```text
r -> 2
g -> 2
m -> 2
```

### Complexity

Vowel/consonant count:
- Time: `O(n)`
- Space: `O(1)`

Duplicate characters:
- Time: `O(n)`
- Space: `O(k)`

Where:
- `n` is string length.
- `k` is number of unique characters.

### Edge Cases

- Empty string.
- String with only spaces/punctuation.
- Uppercase and lowercase letters.
- Digits in string.
- Unicode characters.
- Whether `y` is considered vowel should be clarified.

### Same Mindset Problems

If you get this, you can solve:
- First Non-Repeating Character
- Valid Anagram
- Ransom Note
- Jewels and Stones
- Sort Characters By Frequency
- Most Common Word

### If You See This Twist

| Twist | What To Do |
|---|---|
| Only lowercase `a-z` | Use `int[26]` frequency array |
| ASCII characters | Use `int[128]` or `int[256]` |
| Need insertion order | Use `LinkedHashMap` |
| Need sorted output | Use `TreeMap` or sort entries |
| Need first non-repeating char | Build frequency, then scan string again |

### Variant: First Non-Repeating Character

```java
import java.util.*;

public class FirstNonRepeatingCharacter {
    public static Character firstNonRepeating(String s) {
        Map<Character, Integer> frequency = new LinkedHashMap<>();

        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            frequency.put(ch, frequency.getOrDefault(ch, 0) + 1);
        }

        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (frequency.get(ch) == 1) {
                return ch;
            }
        }

        return null;
    }
}
```

---

## Problem 9: Valid Parentheses

### Problem Statement

Given a string `s` containing only these characters:

```text
'(', ')', '{', '}', '[' and ']'
```

Determine if the input string is valid.

A string is valid if:
- Open brackets are closed by the same type of brackets.
- Open brackets are closed in the correct order.
- Every closing bracket has a matching opening bracket.

### Examples

#### Example 1

Input:

```text
s = "()"
```

Output:

```text
true
```

#### Example 2

Input:

```text
s = "()[]{}"
```

Output:

```text
true
```

Explanation:
- Each opening bracket is closed correctly.

#### Example 3

Input:

```text
s = "(]"
```

Output:

```text
false
```

Explanation:
- `(` cannot be closed by `]`.

#### Example 4

Input:

```text
s = "([)]"
```

Output:

```text
false
```

Explanation:
- Brackets are closed in the wrong order.

#### Example 5

Input:

```text
s = "{[]}"
```

Output:

```text
true
```

### Pattern

Stack.

The main idea:
- Opening brackets wait to be closed.
- The most recently opened bracket must be closed first.
- This is Last In, First Out, so use a stack.

### How To Recognize This Pattern

Use this pattern when:
- The problem involves nested structure.
- The most recent item must be resolved first.
- You need matching pairs.
- Keywords include parentheses, brackets, tags, expression validation.

### Hints Before Solution

#### Brute Force Hint

Repeatedly remove valid adjacent pairs:

```text
()
{}
[]
```

Continue until no more removals are possible.

If final string is empty, it is valid.

Why this is not ideal:
- Repeated string modification is expensive.
- Time can become `O(n^2)`.

#### Optimized Hint

Use a stack.

For each character:
- If opening bracket, push it.
- If closing bracket:
  - Stack must not be empty.
  - Top of stack must be matching opening bracket.
  - Pop it.

At the end:
- Stack must be empty.

### Solution

```java
import java.util.*;

public class ValidParentheses {
    public static boolean isValid(String s) {
        Deque<Character> stack = new ArrayDeque<>();

        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);

            if (ch == '(' || ch == '{' || ch == '[') {
                stack.push(ch);
            } else {
                if (stack.isEmpty()) {
                    return false;
                }

                char open = stack.pop();
                if (!isMatching(open, ch)) {
                    return false;
                }
            }
        }

        return stack.isEmpty();
    }

    private static boolean isMatching(char open, char close) {
        return (open == '(' && close == ')')
            || (open == '{' && close == '}')
            || (open == '[' && close == ']');
    }
}
```

### Alternative Solution: Push Expected Closing Bracket

This version is cleaner in interviews.

```java
import java.util.*;

public class ValidParenthesesExpectedClose {
    public static boolean isValid(String s) {
        Deque<Character> stack = new ArrayDeque<>();

        for (char ch : s.toCharArray()) {
            if (ch == '(') {
                stack.push(')');
            } else if (ch == '{') {
                stack.push('}');
            } else if (ch == '[') {
                stack.push(']');
            } else {
                if (stack.isEmpty() || stack.pop() != ch) {
                    return false;
                }
            }
        }

        return stack.isEmpty();
    }
}
```

### Dry Run

Input:

```text
s = "{[]}"
```

Initial:

```text
stack = []
```

Using expected closing bracket approach:

| Character | Action | Stack |
|---|---|---|
| `{` | Push expected `}` | [`}`] |
| `[` | Push expected `]` | [`]`, `}`] |
| `]` | Pop and match `]` | [`}`] |
| `}` | Pop and match `}` | [] |

End:

```text
stack = []
```

Answer:

```text
true
```

Dry run for invalid case:

Input:

```text
s = "([)]"
```

| Character | Action | Stack |
|---|---|---|
| `(` | Push expected `)` | [`)`] |
| `[` | Push expected `]` | [`]`, `)`] |
| `)` | Top is `]`, but current is `)` | Invalid |

Answer:

```text
false
```

### Complexity

- Time: `O(n)`
- Space: `O(n)`

In the worst case, all characters are opening brackets.

### Edge Cases

- Empty string -> usually valid.
- Single opening bracket -> false.
- Single closing bracket -> false.
- Correct bracket types but wrong order -> false.
- Nested brackets.
- Sequential valid groups like `()[]{}`.

### Same Mindset Problems

If you get this, you can solve:
- Min Add to Make Parentheses Valid
- Remove Invalid Parentheses
- Decode String
- Daily Temperatures
- Next Greater Element
- Evaluate Reverse Polish Notation
- Simplify Path

### If You See This Twist

| Twist | What To Do |
|---|---|
| Need validate brackets | Use stack |
| Need minimum additions | Count imbalance |
| Need remove invalid brackets | Stack/index tracking or BFS depending requirement |
| Need nested decoded string | Stack for count and previous string |
| Need next greater element | Monotonic stack |

### Variant: Only Parentheses Minimum Additions

```java
public class MinAddToMakeParenthesesValid {
    public static int minAddToMakeValid(String s) {
        int open = 0;
        int additions = 0;

        for (char ch : s.toCharArray()) {
            if (ch == '(') {
                open++;
            } else {
                if (open > 0) {
                    open--;
                } else {
                    additions++;
                }
            }
        }

        return additions + open;
    }
}
```

---

## Problem 10: Two Sum / Pair Sum

### Problem Statement

Given an integer array `nums` and an integer `target`, return indices of the two numbers such that they add up to `target`.

Assume:
- Each input has exactly one solution, unless interviewer says otherwise.
- You may not use the same element twice.

### Examples

#### Example 1

Input:

```text
nums = [2,7,11,15], target = 9
```

Output:

```text
[0,1]
```

Explanation:

```text
nums[0] + nums[1] = 2 + 7 = 9
```

#### Example 2

Input:

```text
nums = [3,2,4], target = 6
```

Output:

```text
[1,2]
```

Explanation:

```text
nums[1] + nums[2] = 2 + 4 = 6
```

#### Example 3

Input:

```text
nums = [3,3], target = 6
```

Output:

```text
[0,1]
```

Explanation:
- We need two different indices.
- Same value can appear twice.

### Pattern

HashMap complement lookup.

The main idea:
- For current number `x`, we need `target - x`.
- If that complement was seen earlier, answer found.
- Otherwise store current number and index.

### How To Recognize This Pattern

Use this pattern when:
- You need a pair that sums to a target.
- You need indices.
- Array is unsorted.
- You want better than `O(n^2)`.

### Hints Before Solution

#### Brute Force Hint

Check every pair:

```text
for i from 0 to n - 1:
    for j from i + 1 to n - 1:
        if nums[i] + nums[j] == target:
            return [i, j]
```

Time:
- `O(n^2)`

Space:
- `O(1)`

This is acceptable only for small input.

#### Optimized Hint

Use a HashMap:

```text
value -> index
```

For each number:
1. Calculate complement.
2. If complement exists in map, return previous index and current index.
3. Else store current number and index.

Important:
- Check complement before inserting current number.
- This avoids using the same element twice.

### Solution

```java
import java.util.*;

public class TwoSum {
    public static int[] twoSum(int[] nums, int target) {
        Map<Integer, Integer> indexByValue = new HashMap<>();

        for (int i = 0; i < nums.length; i++) {
            int complement = target - nums[i];

            if (indexByValue.containsKey(complement)) {
                return new int[] {indexByValue.get(complement), i};
            }

            indexByValue.put(nums[i], i);
        }

        return new int[] {-1, -1};
    }
}
```

### Dry Run

Input:

```text
nums = [2,7,11,15]
target = 9
```

Initial:

```text
indexByValue = {}
```

| i | nums[i] | complement | Map Before Check | Action |
|---:|---:|---:|---|---|
| 0 | 2 | 7 | `{}` | 7 not found, store `2 -> 0` |
| 1 | 7 | 2 | `{2=0}` | 2 found, return `[0,1]` |

Answer:

```text
[0,1]
```

Dry run with duplicates:

Input:

```text
nums = [3,3]
target = 6
```

| i | nums[i] | complement | Map Before Check | Action |
|---:|---:|---:|---|---|
| 0 | 3 | 3 | `{}` | 3 not found, store `3 -> 0` |
| 1 | 3 | 3 | `{3=0}` | 3 found, return `[0,1]` |

Correct because two different indices are used.

### Complexity

- Time: `O(n)`
- Space: `O(n)`

### Edge Cases

- Duplicate values like `[3,3]`.
- Negative numbers.
- Zero values.
- No solution, if interviewer does not guarantee solution.
- Multiple solutions, if interviewer asks to return all.
- Sorted array variant.

### Same Mindset Problems

If you get this, you can solve:
- Three Sum
- Four Sum
- Two Sum II - Input Array Is Sorted
- Subarray Sum Equals K
- Count Pairs With Given Sum
- Pair Difference problems

### If You See This Twist

| Twist | What To Do |
|---|---|
| Need indices in unsorted array | HashMap complement lookup |
| Array is sorted | Two pointers |
| Need all pairs | Frequency map or sort + two pointers |
| Need count of pairs | HashMap frequency count |
| Need triplets | Sort array, fix one number, then two pointers |
| Need contiguous subarray sum | Prefix sum + HashMap |

### Variant 1: Sorted Array Pair Sum

If the array is sorted and you need values or 1-based indices, use two pointers.

```java
public class TwoSumSorted {
    public static int[] twoSumSorted(int[] nums, int target) {
        int left = 0;
        int right = nums.length - 1;

        while (left < right) {
            int sum = nums[left] + nums[right];

            if (sum == target) {
                return new int[] {left, right};
            } else if (sum < target) {
                left++;
            } else {
                right--;
            }
        }

        return new int[] {-1, -1};
    }
}
```

### Variant 2: Count Pairs With Given Sum

```java
import java.util.*;

public class CountPairsWithSum {
    public static int countPairs(int[] nums, int target) {
        Map<Integer, Integer> frequency = new HashMap<>();
        int count = 0;

        for (int num : nums) {
            int complement = target - num;
            count += frequency.getOrDefault(complement, 0);
            frequency.put(num, frequency.getOrDefault(num, 0) + 1);
        }

        return count;
    }
}
```

Example:

```text
nums = [1,5,7,-1,5]
target = 6
```

Pairs:

```text
(1,5), (1,5), (7,-1)
```

Count:

```text
3
```

---

## Final Rapid Revision Table

| Problem | Core Pattern | Must Say In Interview |
|---|---|---|
| Maximum Subarray | Kadane's algorithm | "At each index, either extend or restart the subarray" |
| Longest Substring Without Repeating Characters | Sliding window | "Move left only forward when duplicate appears" |
| Count Vowels / Duplicate Characters | Frequency counting | "Normalize, filter, then count in one pass" |
| Valid Parentheses | Stack | "Most recent opening bracket must close first" |
| Two Sum / Pair Sum | HashMap complement | "For current number, search for target minus current number" |

## Monday Morning Drill

Before the interview, do these in order:

1. Code `Maximum Subarray` and explain when you restart the subarray.
2. Code `Longest Substring Without Repeating Characters` and explain the `Math.max` left update.
3. Code `Duplicate Characters` using `LinkedHashMap`.
4. Code `Valid Parentheses` using expected closing brackets.
5. Code `Two Sum` and explain why complement check happens before insert.

If you can solve these five without notes, your DSA base becomes much safer for the Intervue first round.
