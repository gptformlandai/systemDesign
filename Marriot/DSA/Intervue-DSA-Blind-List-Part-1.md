# Intervue DSA Blind List - Part 1

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

## Problem 1: Group Anagrams

### Problem Statement

Given an array of strings, group words that are anagrams of each other.

Two strings are anagrams if they contain the same characters with the same frequencies, but possibly in different order.

Return a list of groups. The order of groups and words inside each group usually does not matter unless the interviewer explicitly says so.

### Examples

#### Example 1

Input:

```text
["eat", "tea", "tan", "ate", "nat", "bat"]
```

Output:

```text
[["eat", "tea", "ate"], ["tan", "nat"], ["bat"]]
```

Explanation:
- `eat`, `tea`, and `ate` have the same letters: `a`, `e`, `t`
- `tan` and `nat` have the same letters: `a`, `n`, `t`
- `bat` has no matching anagram

#### Example 2

Input:

```text
["", ""]
```

Output:

```text
[["", ""]]
```

Explanation:
- Empty strings are anagrams of each other.

### Pattern

HashMap + canonical key.

The main idea is:
- Convert every word into a standard representation.
- All anagrams should produce the same key.
- Use that key in a map.

Common key choices:
- Sort the characters: `"eat" -> "aet"`
- Count character frequencies: `"eat" -> "#1#0#0#0#1...#1..."`

### How To Recognize This Pattern

Use this pattern when:
- You need to group items by "same content but different order".
- The problem says anagram, rearrangement, same characters, permutation of a string.
- Order does not matter, frequency matters.

### Hints Before Solution

#### Brute Force Hint

Compare every string with every other string.

For each pair:
- Sort both strings or count characters.
- If they match, put them in the same group.

Why this is not ideal:
- Pairwise comparison becomes expensive.
- For `n` strings, comparing all pairs is roughly `O(n^2)`.

#### Optimized Hint

Create a key for each word.

For each word:
1. Sort characters or build frequency key.
2. Put the word into `map[key]`.
3. Return all map values.

Sorting key complexity:
- If average word length is `k`, sorting each word costs `O(k log k)`.
- Total: `O(n * k log k)`.

Frequency key complexity:
- For lowercase English letters, counting costs `O(k)`.
- Total: `O(n * k)`.

### Solution 1: Sorting Key

```java
import java.util.*;

public class GroupAnagramsSorting {
    public static List<List<String>> groupAnagrams(String[] strs) {
        Map<String, List<String>> map = new HashMap<>();

        for (String word : strs) {
            char[] chars = word.toCharArray();
            Arrays.sort(chars);
            String key = new String(chars);

            map.computeIfAbsent(key, k -> new ArrayList<>()).add(word);
        }

        return new ArrayList<>(map.values());
    }
}
```

### Solution 2: Frequency Key

Use this when strings contain lowercase `a-z`.

```java
import java.util.*;

public class GroupAnagramsFrequency {
    public static List<List<String>> groupAnagrams(String[] strs) {
        Map<String, List<String>> map = new HashMap<>();

        for (String word : strs) {
            int[] count = new int[26];

            for (char ch : word.toCharArray()) {
                count[ch - 'a']++;
            }

            StringBuilder keyBuilder = new StringBuilder();
            for (int freq : count) {
                keyBuilder.append('#').append(freq);
            }

            String key = keyBuilder.toString();
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(word);
        }

        return new ArrayList<>(map.values());
    }
}
```

### Dry Run

Input:

```text
["eat", "tea", "tan", "ate", "nat", "bat"]
```

Using sorting key:

| Word | Sorted Key | Map After Processing |
|---|---|---|
| `eat` | `aet` | `aet -> [eat]` |
| `tea` | `aet` | `aet -> [eat, tea]` |
| `tan` | `ant` | `aet -> [eat, tea]`, `ant -> [tan]` |
| `ate` | `aet` | `aet -> [eat, tea, ate]`, `ant -> [tan]` |
| `nat` | `ant` | `aet -> [eat, tea, ate]`, `ant -> [tan, nat]` |
| `bat` | `abt` | `abt -> [bat]` |

Final map values:

```text
[["eat", "tea", "ate"], ["tan", "nat"], ["bat"]]
```

### Complexity

Sorting key:
- Time: `O(n * k log k)`
- Space: `O(n * k)`

Frequency key:
- Time: `O(n * k)`
- Space: `O(n * k)`

Where:
- `n` is number of strings.
- `k` is average string length.

### Edge Cases

- Empty input array -> empty list.
- Single word -> one group.
- Empty strings -> grouped together.
- Duplicate words -> appear in same group.
- Uppercase letters or Unicode -> sorting key is safer unless constraints are lowercase only.

### Same Mindset Problems

If you get this, you can solve:
- Valid Anagram
- Find All Anagrams in a String
- Group Shifted Strings
- Sort Characters By Frequency
- Check if two strings are permutations

### If You See This Twist

| Twist | What To Do |
|---|---|
| Only lowercase English letters | Use frequency key for better performance |
| Unicode or mixed case | Use sorting key or a map-based frequency key |
| Need stable output order | Use `LinkedHashMap` |
| Need each group sorted | Sort each group before returning |
| Need all anagram indices | Store indices instead of words |

---

## Problem 2: Merge Sorted Array

### Problem Statement

You are given two sorted integer arrays:

```text
nums1
nums2
```

`nums1` has enough extra space at the end to hold all elements of `nums2`.

Given:
- `m`: number of actual elements in `nums1`
- `n`: number of elements in `nums2`

Merge `nums2` into `nums1` so that `nums1` becomes sorted.

You must modify `nums1` in-place.

### Examples

#### Example 1

Input:

```text
nums1 = [1,2,3,0,0,0], m = 3
nums2 = [2,5,6], n = 3
```

Output:

```text
[1,2,2,3,5,6]
```

#### Example 2

Input:

```text
nums1 = [1], m = 1
nums2 = [], n = 0
```

Output:

```text
[1]
```

#### Example 3

Input:

```text
nums1 = [0], m = 0
nums2 = [1], n = 1
```

Output:

```text
[1]
```

### Pattern

Two pointers from the end.

The trick:
- If you merge from the front, you overwrite useful values in `nums1`.
- If you merge from the back, the empty buffer protects you.

### How To Recognize This Pattern

Use this pattern when:
- Two arrays are sorted.
- One array has extra space at the end.
- You need in-place merge.
- Overwriting from the front is dangerous.

### Hints Before Solution

#### Brute Force Hint

Copy the first `m` elements of `nums1` into a temporary array.

Then merge temp and `nums2` from left to right into `nums1`.

This works, but uses extra space.

#### Optimized Hint

Use three pointers:

```text
i = m - 1          last real element in nums1
j = n - 1          last element in nums2
k = m + n - 1      last position in nums1
```

Compare `nums1[i]` and `nums2[j]`.

Place the larger one at `nums1[k]`.

Move the corresponding pointer backward.

### Solution

```java
public class MergeSortedArray {
    public static void merge(int[] nums1, int m, int[] nums2, int n) {
        int i = m - 1;
        int j = n - 1;
        int k = m + n - 1;

        while (i >= 0 && j >= 0) {
            if (nums1[i] > nums2[j]) {
                nums1[k] = nums1[i];
                i--;
            } else {
                nums1[k] = nums2[j];
                j--;
            }
            k--;
        }

        while (j >= 0) {
            nums1[k] = nums2[j];
            j--;
            k--;
        }
    }
}
```

### Why We Do Not Need To Copy Remaining `nums1`

If elements remain in `nums1`, they are already in the correct place.

Example:

```text
nums1 = [1,2,3,0,0,0]
nums2 = [4,5,6]
```

After placing `6`, `5`, `4`, the original `1,2,3` are already correct.

### Dry Run

Input:

```text
nums1 = [1,2,3,0,0,0], m = 3
nums2 = [2,5,6], n = 3
```

Initial:

```text
i = 2 -> nums1[i] = 3
j = 2 -> nums2[j] = 6
k = 5
```

| Step | Compare | Action | nums1 |
|---|---|---|---|
| 1 | 3 vs 6 | place 6 at k=5 | `[1,2,3,0,0,6]` |
| 2 | 3 vs 5 | place 5 at k=4 | `[1,2,3,0,5,6]` |
| 3 | 3 vs 2 | place 3 at k=3 | `[1,2,3,3,5,6]` |
| 4 | 2 vs 2 | place nums2's 2 at k=2 | `[1,2,2,3,5,6]` |

Now:

```text
j = -1
```

No remaining `nums2`.

Final:

```text
[1,2,2,3,5,6]
```

### Complexity

- Time: `O(m + n)`
- Space: `O(1)`

### Edge Cases

- `nums2` empty -> do nothing.
- `nums1` has no real elements -> copy all `nums2`.
- Duplicates.
- Negative numbers.
- All elements in `nums2` smaller than `nums1`.
- All elements in `nums2` larger than `nums1`.

### Same Mindset Problems

If you get this, you can solve:
- Merge Two Sorted Lists
- Merge k Sorted Lists
- Sorted Squares of a Sorted Array
- Remove Duplicates from Sorted Array
- Move Zeroes

### If You See This Twist

| Twist | What To Do |
|---|---|
| Arrays sorted and extra buffer exists | Merge from the end |
| Linked lists instead of arrays | Merge from front using dummy node |
| Need merge k sorted arrays | Use min heap |
| Need remove duplicates | Use slow-fast pointer |
| Need stable preference for first array on equal values | Use `>=` carefully depending on expected order |

---

## Problem 3: Subdomain Visit Count

### Problem Statement

You are given a list of domain visit counts.

Each item looks like:

```text
"9001 discuss.leetcode.com"
```

This means:
- `discuss.leetcode.com` was visited 9001 times.
- `leetcode.com` was also visited 9001 times.
- `com` was also visited 9001 times.

Return total visit counts for every subdomain.

### Examples

#### Example 1

Input:

```text
["9001 discuss.leetcode.com"]
```

Output:

```text
["9001 discuss.leetcode.com", "9001 leetcode.com", "9001 com"]
```

#### Example 2

Input:

```text
[
  "900 google.mail.com",
  "50 yahoo.com",
  "1 intel.mail.com",
  "5 wiki.org"
]
```

Output:

```text
[
  "901 mail.com",
  "50 yahoo.com",
  "900 google.mail.com",
  "5 wiki.org",
  "5 org",
  "1 intel.mail.com",
  "951 com"
]
```

Explanation:
- `google.mail.com` contributes 900 to:
  - `google.mail.com`
  - `mail.com`
  - `com`
- `intel.mail.com` contributes 1 to:
  - `intel.mail.com`
  - `mail.com`
  - `com`
- So `mail.com = 901`.
- `com = 900 + 50 + 1 = 951`.

### Pattern

String parsing + HashMap counting.

This is a frequency aggregation problem.

### How To Recognize This Pattern

Use this pattern when:
- Input has encoded data in strings.
- You need to split and count.
- One item contributes to multiple derived keys.
- The problem asks for total count or frequency.

### Hints Before Solution

#### Brute Force Hint

For every domain:
1. Split into count and domain.
2. Generate all subdomains.
3. Store them in a list.
4. Later count all list entries.

This works but creates unnecessary intermediate data.

#### Optimized Hint

Do counting immediately.

For each input:
1. Parse count.
2. Parse domain.
3. Add count to full domain.
4. Find every dot.
5. Add count to substring after each dot.

### Solution

```java
import java.util.*;

public class SubdomainVisitCount {
    public static List<String> subdomainVisits(String[] cpdomains) {
        Map<String, Integer> countByDomain = new HashMap<>();

        for (String entry : cpdomains) {
            String[] parts = entry.split(" ");
            int count = Integer.parseInt(parts[0]);
            String domain = parts[1];

            countByDomain.put(domain, countByDomain.getOrDefault(domain, 0) + count);

            for (int i = 0; i < domain.length(); i++) {
                if (domain.charAt(i) == '.') {
                    String subdomain = domain.substring(i + 1);
                    countByDomain.put(
                        subdomain,
                        countByDomain.getOrDefault(subdomain, 0) + count
                    );
                }
            }
        }

        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : countByDomain.entrySet()) {
            result.add(entry.getValue() + " " + entry.getKey());
        }

        return result;
    }
}
```

### Dry Run

Input:

```text
["9001 discuss.leetcode.com"]
```

Entry:

```text
count = 9001
domain = discuss.leetcode.com
```

Start map:

```text
{}
```

Add full domain:

```text
discuss.leetcode.com -> 9001
```

Scan domain:

```text
discuss.leetcode.com
       ^
       dot found
```

Substring after dot:

```text
leetcode.com
```

Add:

```text
leetcode.com -> 9001
```

Next dot:

```text
discuss.leetcode.com
                ^
                dot found
```

Substring after dot:

```text
com
```

Add:

```text
com -> 9001
```

Final map:

```text
discuss.leetcode.com -> 9001
leetcode.com -> 9001
com -> 9001
```

Final result:

```text
["9001 discuss.leetcode.com", "9001 leetcode.com", "9001 com"]
```

### Complexity

Let:
- `n` be number of domain strings.
- `k` be average domain length.

Time:
- `O(n * k)`

Space:
- `O(n * k)` in the worst case for all unique subdomains.

### Edge Cases

- Multiple entries for same domain.
- Single-level domain like `com`.
- Large counts.
- Domain with multiple levels.
- Output order usually does not matter.

### Same Mindset Problems

If you get this, you can solve:
- Most Common Word
- Top K Frequent Words
- Log aggregation problems
- Count server requests by endpoint
- Count file extensions from paths
- Parse IP logs and aggregate by subnet

### If You See This Twist

| Twist | What To Do |
|---|---|
| Need output sorted by domain | Sort result keys |
| Need top k subdomains | Use min heap after counting |
| Input has invalid records | Validate split length and count |
| Need aggregate by parent path | Same idea, split by `/` instead of `.` |
| Need case-insensitive domain | Convert domain to lowercase |

---

## Problem 4: Find Rotation Count / Minimum in Rotated Sorted Array

### Problem Statement

You are given a sorted array that has been rotated.

Example sorted array:

```text
[0,1,2,4,5,6,7]
```

After rotation:

```text
[4,5,6,7,0,1,2]
```

Find the rotation count.

The rotation count is the index of the minimum element.

In the above example:

```text
minimum = 0
index = 4
rotation count = 4
```

Usually constraints say:
- No duplicate elements.
- Array length is at least 1.

### Examples

#### Example 1

Input:

```text
[4,5,6,7,0,1,2]
```

Output:

```text
4
```

Explanation:
- Minimum element is `0`.
- It is at index `4`.

#### Example 2

Input:

```text
[3,4,5,1,2]
```

Output:

```text
3
```

Explanation:
- Minimum element is `1`.
- It is at index `3`.

#### Example 3

Input:

```text
[1,2,3,4,5]
```

Output:

```text
0
```

Explanation:
- Array is not rotated.
- Minimum element is already at index `0`.

### Pattern

Binary search on rotated sorted array.

The key observation:
- One side of the array is still sorted.
- The minimum element lies in the unsorted side.
- Compare `nums[mid]` with `nums[right]`.

### How To Recognize This Pattern

Use this pattern when:
- Array is sorted but rotated.
- Need `O(log n)`.
- You are searching for minimum, pivot, or target.

### Hints Before Solution

#### Brute Force Hint

Scan all elements and find the minimum index.

```text
minIndex = 0
for i from 1 to n - 1:
    if nums[i] < nums[minIndex]:
        minIndex = i
```

This is simple and correct.

Time:
- `O(n)`

#### Optimized Hint

Use binary search.

Compare:

```text
nums[mid] vs nums[right]
```

Case 1:

```text
nums[mid] > nums[right]
```

Minimum is to the right of `mid`.

Why?
- `mid` is in the larger sorted part.
- Rotation pivot is after `mid`.

Move:

```text
left = mid + 1
```

Case 2:

```text
nums[mid] <= nums[right]
```

Minimum is at `mid` or to the left of `mid`.

Move:

```text
right = mid
```

### Solution

```java
public class RotationCount {
    public static int findRotationCount(int[] nums) {
        int left = 0;
        int right = nums.length - 1;

        while (left < right) {
            int mid = left + (right - left) / 2;

            if (nums[mid] > nums[right]) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }

        return left;
    }

    public static int findMinimum(int[] nums) {
        return nums[findRotationCount(nums)];
    }
}
```

### Dry Run

Input:

```text
[4,5,6,7,0,1,2]
```

Initial:

```text
left = 0
right = 6
```

| Step | left | right | mid | nums[mid] | nums[right] | Decision |
|---|---:|---:|---:|---:|---:|---|
| 1 | 0 | 6 | 3 | 7 | 2 | `7 > 2`, minimum right side, `left = 4` |
| 2 | 4 | 6 | 5 | 1 | 2 | `1 <= 2`, minimum at mid or left, `right = 5` |
| 3 | 4 | 5 | 4 | 0 | 1 | `0 <= 1`, minimum at mid or left, `right = 4` |

Stop:

```text
left = right = 4
```

Answer:

```text
rotation count = 4
minimum = nums[4] = 0
```

### Complexity

- Time: `O(log n)`
- Space: `O(1)`

### Edge Cases

- Not rotated array -> return `0`.
- One element -> return `0`.
- Two elements rotated -> works.
- Already sorted -> works because `right` keeps moving left.
- Duplicates -> this exact logic needs a small change.

### Duplicate Variant

If duplicates are allowed:

```java
public class RotationCountWithDuplicates {
    public static int findRotationCount(int[] nums) {
        int left = 0;
        int right = nums.length - 1;

        while (left < right) {
            int mid = left + (right - left) / 2;

            if (nums[mid] > nums[right]) {
                left = mid + 1;
            } else if (nums[mid] < nums[right]) {
                right = mid;
            } else {
                right--;
            }
        }

        return left;
    }
}
```

With duplicates, worst-case time can degrade to `O(n)`.

### Same Mindset Problems

If you get this, you can solve:
- Find Minimum in Rotated Sorted Array
- Search in Rotated Sorted Array
- Find Peak Element
- First Bad Version
- Binary Search on Answer problems

### If You See This Twist

| Twist | What To Do |
|---|---|
| Need minimum value | Return `nums[left]` |
| Need rotation count | Return `left` |
| Need search target | First find which half is sorted, then binary search |
| Duplicates allowed | If `nums[mid] == nums[right]`, do `right--` |
| Array sorted descending then rotated | Logic changes; clarify order first |

---

## Problem 5: Rotate Array by K

### Problem Statement

Given an integer array `nums`, rotate the array to the right by `k` steps.

Right rotation means elements from the end move to the front.

You should modify the array in-place if possible.

### Examples

#### Example 1

Input:

```text
nums = [1,2,3,4,5,6,7], k = 3
```

Output:

```text
[5,6,7,1,2,3,4]
```

Explanation:

```text
rotate 1 step: [7,1,2,3,4,5,6]
rotate 2 steps: [6,7,1,2,3,4,5]
rotate 3 steps: [5,6,7,1,2,3,4]
```

#### Example 2

Input:

```text
nums = [-1,-100,3,99], k = 2
```

Output:

```text
[3,99,-1,-100]
```

### Pattern

Array reversal.

The trick:

For right rotation by `k`:

```text
Original:       [1,2,3,4,5,6,7]
k = 3

Reverse all:    [7,6,5,4,3,2,1]
Reverse first k:[5,6,7,4,3,2,1]
Reverse rest:   [5,6,7,1,2,3,4]
```

### How To Recognize This Pattern

Use this pattern when:
- Array needs rotation.
- In-place solution is requested.
- You need `O(1)` extra space.

### Hints Before Solution

#### Brute Force Hint

Rotate one step at a time.

For each step:
1. Save last element.
2. Shift all elements right by one.
3. Put saved element at index `0`.

Time:
- `O(n * k)`

Too slow when `k` is large.

#### Better Hint With Extra Array

Use another array.

For each index `i`:

```text
newIndex = (i + k) % n
```

Place:

```text
result[newIndex] = nums[i]
```

Then copy result back to nums.

Time:
- `O(n)`

Space:
- `O(n)`

#### Optimized Hint

Use reversal.

Steps:
1. `k = k % n`
2. Reverse whole array.
3. Reverse first `k` elements.
4. Reverse remaining `n - k` elements.

Time:
- `O(n)`

Space:
- `O(1)`

### Solution

```java
public class RotateArray {
    public static void rotate(int[] nums, int k) {
        int n = nums.length;
        if (n == 0) {
            return;
        }

        k = k % n;
        if (k == 0) {
            return;
        }

        reverse(nums, 0, n - 1);
        reverse(nums, 0, k - 1);
        reverse(nums, k, n - 1);
    }

    private static void reverse(int[] nums, int left, int right) {
        while (left < right) {
            int temp = nums[left];
            nums[left] = nums[right];
            nums[right] = temp;
            left++;
            right--;
        }
    }
}
```

### Dry Run

Input:

```text
nums = [1,2,3,4,5,6,7]
k = 3
```

Initial:

```text
n = 7
k = 3 % 7 = 3
```

Step 1: Reverse entire array

```text
[1,2,3,4,5,6,7]
 -> [7,6,5,4,3,2,1]
```

Step 2: Reverse first `k = 3` elements

```text
[7,6,5,4,3,2,1]
 -> [5,6,7,4,3,2,1]
```

Step 3: Reverse remaining elements from index `3` to `6`

```text
[5,6,7,4,3,2,1]
 -> [5,6,7,1,2,3,4]
```

Final:

```text
[5,6,7,1,2,3,4]
```

### Complexity

- Time: `O(n)`
- Space: `O(1)`

### Edge Cases

- `k = 0` -> no change.
- `k == n` -> no change.
- `k > n` -> use `k % n`.
- Array length 1 -> no change.
- Empty array -> return safely.
- Negative numbers -> no difference.

### Same Mindset Problems

If you get this, you can solve:
- Rotate String
- Left Rotate Array
- Reverse Words in a String
- Move Zeroes
- Cyclic replacement rotation

### If You See This Twist

| Twist | What To Do |
|---|---|
| Rotate right by `k` | Reverse all, reverse first `k`, reverse rest |
| Rotate left by `k` | Reverse first `k`, reverse rest, reverse all |
| Need extra array allowed | Use `(i + k) % n` mapping |
| Very large `k` | Always do `k = k % n` |
| String rotation | Use substring or reverse depending on constraints |

### Left Rotation Variant

For left rotation by `k`:

```java
public class LeftRotateArray {
    public static void rotateLeft(int[] nums, int k) {
        int n = nums.length;
        if (n == 0) {
            return;
        }

        k = k % n;
        if (k == 0) {
            return;
        }

        reverse(nums, 0, k - 1);
        reverse(nums, k, n - 1);
        reverse(nums, 0, n - 1);
    }

    private static void reverse(int[] nums, int left, int right) {
        while (left < right) {
            int temp = nums[left];
            nums[left] = nums[right];
            nums[right] = temp;
            left++;
            right--;
        }
    }
}
```

---

## Final Rapid Revision Table

| Problem | Core Pattern | Must Say In Interview |
|---|---|---|
| Group Anagrams | HashMap + canonical key | "Same anagrams produce same key" |
| Merge Sorted Array | Two pointers from end | "Backwards avoids overwriting nums1" |
| Subdomain Visit Count | String parsing + HashMap | "Each domain contributes to all parent domains" |
| Rotation Count | Binary search | "Compare mid with right to locate minimum side" |
| Rotate Array | Reverse array | "Use k modulo n, then 3 reversals" |

## Monday Morning Drill

Before the interview, do these in order:

1. Code `Group Anagrams` once using sorting key.
2. Code `Merge Sorted Array` without looking.
3. Code `Subdomain Visit Count` and dry run one domain.
4. Code `Rotation Count` and explain why `nums[mid] > nums[right]` means go right.
5. Code `Rotate Array` and explain the three reversals.

If you can explain these five calmly, you are covering the highest-probability Intervue DSA patterns for this first round.
