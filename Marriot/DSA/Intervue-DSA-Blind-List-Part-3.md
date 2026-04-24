# Intervue DSA Blind List - Part 3

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

## Problem 11: Container With Most Water

### Problem Statement

You are given an integer array `height`.

Each element represents the height of a vertical line drawn at that index.

Choose two lines such that together with the x-axis they form a container that holds the maximum amount of water.

Return the maximum water area.

### Examples

#### Example 1

Input:

```text
height = [1,8,6,2,5,4,8,3,7]
```

Output:

```text
49
```

Explanation:
- Choose height `8` at index `1`.
- Choose height `7` at index `8`.
- Width = `8 - 1 = 7`.
- Effective height = `min(8, 7) = 7`.
- Area = `7 * 7 = 49`.

#### Example 2

Input:

```text
height = [1,1]
```

Output:

```text
1
```

Explanation:
- Width = `1`.
- Height = `1`.
- Area = `1`.

### Pattern

Two pointers.

The main idea:
- Start with the widest possible container.
- Move the pointer with the smaller height.
- Moving the taller pointer cannot improve area because width reduces and the limiting height remains the smaller one.

### How To Recognize This Pattern

Use this pattern when:
- You need to choose two positions from an array.
- Area or sum depends on left and right boundaries.
- The array is not necessarily sorted, but moving one side can eliminate bad choices.

### Hints Before Solution

#### Brute Force Hint

Try all pairs:

```text
for i from 0 to n - 1:
    for j from i + 1 to n - 1:
        area = min(height[i], height[j]) * (j - i)
```

Track maximum area.

Time:
- `O(n^2)`

Why this is not ideal:
- Too many pairs for large input.

#### Optimized Hint

Use two pointers:

```text
left = 0
right = n - 1
```

At each step:
1. Calculate area.
2. Update max area.
3. Move the pointer with smaller height.

Why move smaller height?
- Width always decreases.
- To get a bigger area, the limiting height must increase.

### Solution

```java
public class ContainerWithMostWater {
    public static int maxArea(int[] height) {
        int left = 0;
        int right = height.length - 1;
        int maxArea = 0;

        while (left < right) {
            int width = right - left;
            int currentHeight = Math.min(height[left], height[right]);
            int area = width * currentHeight;

            maxArea = Math.max(maxArea, area);

            if (height[left] < height[right]) {
                left++;
            } else {
                right--;
            }
        }

        return maxArea;
    }
}
```

### Dry Run

Input:

```text
height = [1,8,6,2,5,4,8,3,7]
```

Initial:

```text
left = 0
right = 8
maxArea = 0
```

| Step | left | right | height[left] | height[right] | Area | maxArea | Move |
|---:|---:|---:|---:|---:|---:|---:|---|
| 1 | 0 | 8 | 1 | 7 | `8 * 1 = 8` | 8 | left++ |
| 2 | 1 | 8 | 8 | 7 | `7 * 7 = 49` | 49 | right-- |
| 3 | 1 | 7 | 8 | 3 | `6 * 3 = 18` | 49 | right-- |
| 4 | 1 | 6 | 8 | 8 | `5 * 8 = 40` | 49 | right-- |
| 5 | 1 | 5 | 8 | 4 | `4 * 4 = 16` | 49 | right-- |
| 6 | 1 | 4 | 8 | 5 | `3 * 5 = 15` | 49 | right-- |
| 7 | 1 | 3 | 8 | 2 | `2 * 2 = 4` | 49 | right-- |
| 8 | 1 | 2 | 8 | 6 | `1 * 6 = 6` | 49 | right-- |

Stop:

```text
left = right = 1
```

Answer:

```text
49
```

### Complexity

- Time: `O(n)`
- Space: `O(1)`

### Edge Cases

- Only two lines.
- All heights equal.
- Increasing heights.
- Decreasing heights.
- Very small height at one side.
- Large values where area can be high.

### Same Mindset Problems

If you get this, you can solve:
- Trapping Rain Water, with a stronger two-pointer variant
- Two Sum II - Input Array Is Sorted
- Valid Palindrome
- Squares of a Sorted Array
- 3Sum, after sorting

### If You See This Twist

| Twist | What To Do |
|---|---|
| Need max container area | Two pointers from both ends |
| Need trapped rain water | Track left max and right max |
| Array is sorted and pair sum needed | Two pointers based on sum |
| Need closest pair sum | Sort, then two pointers |
| Need actual pair indices | Store best left and right when max improves |

### Variant: Return Best Pair Indices

```java
public class ContainerWithMostWaterIndices {
    public static int[] maxAreaIndices(int[] height) {
        int left = 0;
        int right = height.length - 1;
        int maxArea = 0;
        int bestLeft = 0;
        int bestRight = 0;

        while (left < right) {
            int area = (right - left) * Math.min(height[left], height[right]);

            if (area > maxArea) {
                maxArea = area;
                bestLeft = left;
                bestRight = right;
            }

            if (height[left] < height[right]) {
                left++;
            } else {
                right--;
            }
        }

        return new int[] {maxArea, bestLeft, bestRight};
    }
}
```

---

## Problem 12: Meeting Rooms II

### Problem Statement

Given an array of meeting time intervals, return the minimum number of conference rooms required.

Each interval is represented as:

```text
[start, end]
```

If one meeting ends at time `10` and another starts at time `10`, they can use the same room.

### Examples

#### Example 1

Input:

```text
intervals = [[0,30], [5,10], [15,20]]
```

Output:

```text
2
```

Explanation:
- Meeting `[0,30]` overlaps with `[5,10]`.
- Meeting `[0,30]` also overlaps with `[15,20]`.
- `[5,10]` and `[15,20]` do not overlap.
- Minimum rooms needed = `2`.

#### Example 2

Input:

```text
intervals = [[7,10], [2,4]]
```

Output:

```text
1
```

Explanation:
- The meetings do not overlap.
- One room is enough.

#### Example 3

Input:

```text
intervals = [[1,5], [5,8], [8,10]]
```

Output:

```text
1
```

Explanation:
- A meeting ending at `5` frees the room for a meeting starting at `5`.

### Pattern

Intervals + min heap.

The main idea:
- Sort meetings by start time.
- Track the earliest ending meeting using a min heap.
- If the earliest ending room is free, reuse it.
- Otherwise, allocate a new room.

### How To Recognize This Pattern

Use this pattern when:
- You need minimum resources to handle overlapping intervals.
- Keywords include meeting rooms, servers, machines, classrooms, platforms.
- You need to know what is currently active.

### Hints Before Solution

#### Brute Force Hint

For every meeting:
- Compare it with every other meeting.
- Count how many meetings overlap with it.

Time:
- `O(n^2)`

Why this is incomplete:
- Pairwise overlap count is not always the cleanest way to calculate minimum rooms.
- You need maximum simultaneous active meetings.

#### Optimized Hint

Sort meetings by start time.

Use a min heap of end times.

For each meeting:
1. If the earliest end time is `<= current start`, that room is free. Remove it.
2. Add current meeting end time.
3. Heap size represents active rooms.

Maximum heap size is the answer.

### Solution

```java
import java.util.*;

public class MeetingRoomsII {
    public static int minMeetingRooms(int[][] intervals) {
        if (intervals == null || intervals.length == 0) {
            return 0;
        }

        Arrays.sort(intervals, Comparator.comparingInt(a -> a[0]));

        PriorityQueue<Integer> minHeap = new PriorityQueue<>();

        for (int[] interval : intervals) {
            int start = interval[0];
            int end = interval[1];

            if (!minHeap.isEmpty() && minHeap.peek() <= start) {
                minHeap.poll();
            }

            minHeap.offer(end);
        }

        return minHeap.size();
    }
}
```

### Alternative Solution: Start And End Arrays

```java
import java.util.*;

public class MeetingRoomsTwoPointers {
    public static int minMeetingRooms(int[][] intervals) {
        int n = intervals.length;
        int[] starts = new int[n];
        int[] ends = new int[n];

        for (int i = 0; i < n; i++) {
            starts[i] = intervals[i][0];
            ends[i] = intervals[i][1];
        }

        Arrays.sort(starts);
        Arrays.sort(ends);

        int startPointer = 0;
        int endPointer = 0;
        int rooms = 0;
        int maxRooms = 0;

        while (startPointer < n) {
            if (starts[startPointer] < ends[endPointer]) {
                rooms++;
                maxRooms = Math.max(maxRooms, rooms);
                startPointer++;
            } else {
                rooms--;
                endPointer++;
            }
        }

        return maxRooms;
    }
}
```

### Dry Run

Input:

```text
intervals = [[0,30], [5,10], [15,20]]
```

Sorted by start:

```text
[[0,30], [5,10], [15,20]]
```

Initial:

```text
minHeap = []
```

| Meeting | Earliest End | Can Reuse? | Heap After Add | Rooms Active |
|---|---:|---|---|---:|
| `[0,30]` | none | No | `[30]` | 1 |
| `[5,10]` | 30 | No, `30 > 5` | `[10,30]` | 2 |
| `[15,20]` | 10 | Yes, `10 <= 15` | poll 10, add 20 -> `[20,30]` | 2 |

Final heap size:

```text
2
```

Answer:

```text
2
```

### Complexity

- Time: `O(n log n)`
- Space: `O(n)`

Sorting costs `O(n log n)`.

Each heap operation costs `O(log n)`.

### Edge Cases

- Empty intervals -> `0`.
- One meeting -> `1`.
- Meetings touching at boundary like `[1,5]` and `[5,10]`.
- All meetings overlap.
- No meetings overlap.
- Intervals given unsorted.

### Same Mindset Problems

If you get this, you can solve:
- Meeting Rooms I
- Minimum Number of Platforms
- Car Pooling
- Employee Free Time
- Merge Intervals
- Insert Interval
- Task Scheduler, with a different heap use

### If You See This Twist

| Twist | What To Do |
|---|---|
| Need if one person can attend all meetings | Sort and check adjacent overlap |
| Need minimum rooms | Sort + min heap of end times |
| Need merge overlapping intervals | Sort by start and merge ranges |
| Need maximum simultaneous users | Sweep line or start/end arrays |
| Need most booked room | Assign room IDs with two heaps |

### Variant 1: Can Attend All Meetings

```java
import java.util.*;

public class MeetingRoomsI {
    public static boolean canAttendMeetings(int[][] intervals) {
        Arrays.sort(intervals, Comparator.comparingInt(a -> a[0]));

        for (int i = 1; i < intervals.length; i++) {
            if (intervals[i][0] < intervals[i - 1][1]) {
                return false;
            }
        }

        return true;
    }
}
```

### Variant 2: Meeting Rooms III Mindset

If the interviewer asks for the most booked room:
- Keep one heap for available room IDs.
- Keep another heap for busy rooms ordered by end time.
- When a meeting starts, release rooms whose end time is `<= start`.
- If no room is available, delay the meeting until the earliest room is free.

This is still an intervals + heap problem, but with room identity tracking.

---

## Problem 13: Binary Tree Right Side View

### Problem Statement

Given the root of a binary tree, imagine standing on the right side of the tree.

Return the values of the nodes you can see from top to bottom.

### Examples

#### Example 1

Input:

```text
        1
       / \
      2   3
       \   \
        5   4
```

Output:

```text
[1,3,4]
```

Explanation:
- Level 0: visible node is `1`.
- Level 1: visible node is `3`.
- Level 2: visible node is `4`.

#### Example 2

Input:

```text
    1
   /
  2
```

Output:

```text
[1,2]
```

Explanation:
- Even though node `2` is on the left, it is visible because there is no right node at that level.

#### Example 3

Input:

```text
root = null
```

Output:

```text
[]
```

### Pattern

Binary tree BFS level order.

The main idea:
- Process tree level by level.
- The last node processed at each level is visible from the right side.

### How To Recognize This Pattern

Use this pattern when:
- The problem asks for view by level.
- Keywords include right side view, left side view, level order, zigzag level order.
- You need one result per tree depth.

### Hints Before Solution

#### Brute Force Hint

Traverse the entire tree and store nodes by level.

For each level:
- Keep a list of values.
- Return the last value from each level.

This works, but stores more values than needed.

#### Optimized Hint

Use BFS.

For each level:
1. Get current queue size.
2. Process exactly that many nodes.
3. When processing the last node of the level, add it to result.

### Solution

```java
import java.util.*;

public class BinaryTreeRightSideView {
    static class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;

        TreeNode(int val) {
            this.val = val;
        }
    }

    public static List<Integer> rightSideView(TreeNode root) {
        List<Integer> result = new ArrayList<>();

        if (root == null) {
            return result;
        }

        Queue<TreeNode> queue = new ArrayDeque<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            int levelSize = queue.size();

            for (int i = 0; i < levelSize; i++) {
                TreeNode node = queue.poll();

                if (i == levelSize - 1) {
                    result.add(node.val);
                }

                if (node.left != null) {
                    queue.offer(node.left);
                }
                if (node.right != null) {
                    queue.offer(node.right);
                }
            }
        }

        return result;
    }
}
```

### Alternative Solution: DFS Right First

```java
import java.util.*;

public class BinaryTreeRightSideViewDFS {
    static class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;

        TreeNode(int val) {
            this.val = val;
        }
    }

    public static List<Integer> rightSideView(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        dfs(root, 0, result);
        return result;
    }

    private static void dfs(TreeNode node, int depth, List<Integer> result) {
        if (node == null) {
            return;
        }

        if (depth == result.size()) {
            result.add(node.val);
        }

        dfs(node.right, depth + 1, result);
        dfs(node.left, depth + 1, result);
    }
}
```

### Dry Run

Input:

```text
        1
       / \
      2   3
       \   \
        5   4
```

Initial:

```text
queue = [1]
result = []
```

Level 0:

```text
levelSize = 1
process 1, it is last in level
result = [1]
queue = [2,3]
```

Level 1:

```text
levelSize = 2
process 2, not last
process 3, last in level
result = [1,3]
queue = [5,4]
```

Level 2:

```text
levelSize = 2
process 5, not last
process 4, last in level
result = [1,3,4]
queue = []
```

Answer:

```text
[1,3,4]
```

### Complexity

- Time: `O(n)`
- Space: `O(w)`

Where:
- `n` is number of nodes.
- `w` is maximum width of the tree.

DFS version:
- Time: `O(n)`
- Space: `O(h)` recursion stack, where `h` is tree height.

### Edge Cases

- Empty tree.
- Single node.
- Only left children.
- Only right children.
- Perfect binary tree.
- Uneven tree.

### Same Mindset Problems

If you get this, you can solve:
- Binary Tree Level Order Traversal
- Binary Tree Left Side View
- Zigzag Level Order Traversal
- Average of Levels in Binary Tree
- Maximum Depth of Binary Tree
- Top View / Bottom View, with extra horizontal distance tracking

### If You See This Twist

| Twist | What To Do |
|---|---|
| Right side view | BFS last node per level or DFS right first |
| Left side view | BFS first node per level or DFS left first |
| Need all nodes by level | Store full level lists |
| Need zigzag order | Reverse alternate levels or use deque |
| Need vertical view | Track horizontal distance with BFS |

---

## Problem 14: Binary Tree Level Order Traversal

### Problem Statement

Given the root of a binary tree, return the level order traversal of its nodes' values.

Level order means:
- Visit nodes from top to bottom.
- Within each level, visit from left to right.

### Examples

#### Example 1

Input:

```text
        3
       / \
      9   20
         /  \
        15   7
```

Output:

```text
[[3], [9,20], [15,7]]
```

#### Example 2

Input:

```text
root = [1]
```

Output:

```text
[[1]]
```

#### Example 3

Input:

```text
root = null
```

Output:

```text
[]
```

### Pattern

Breadth-first search using queue.

The main idea:
- A queue processes nodes in the same order they are discovered.
- Process one level at a time using the queue size.

### How To Recognize This Pattern

Use this pattern when:
- The problem asks for level by level traversal.
- The problem asks for shortest path in unweighted graph/tree.
- You need top-to-bottom processing.

### Hints Before Solution

#### Brute Force Hint

Use DFS and store each node by depth:

```text
map depth -> list of node values
```

This works, but BFS is more natural because level order is literally breadth-first.

#### Optimized Hint

Use a queue.

For each level:
1. Read `levelSize = queue.size()`.
2. Create a list for current level.
3. Process exactly `levelSize` nodes.
4. Add their children to the queue.
5. Add current level list to result.

### Solution

```java
import java.util.*;

public class BinaryTreeLevelOrderTraversal {
    static class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;

        TreeNode(int val) {
            this.val = val;
        }
    }

    public static List<List<Integer>> levelOrder(TreeNode root) {
        List<List<Integer>> result = new ArrayList<>();

        if (root == null) {
            return result;
        }

        Queue<TreeNode> queue = new ArrayDeque<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            List<Integer> level = new ArrayList<>();

            for (int i = 0; i < levelSize; i++) {
                TreeNode node = queue.poll();
                level.add(node.val);

                if (node.left != null) {
                    queue.offer(node.left);
                }
                if (node.right != null) {
                    queue.offer(node.right);
                }
            }

            result.add(level);
        }

        return result;
    }
}
```

### Dry Run

Input:

```text
        3
       / \
      9   20
         /  \
        15   7
```

Initial:

```text
queue = [3]
result = []
```

Level 0:

```text
levelSize = 1
level = []
process 3
level = [3]
queue = [9,20]
result = [[3]]
```

Level 1:

```text
levelSize = 2
process 9
process 20
level = [9,20]
queue = [15,7]
result = [[3], [9,20]]
```

Level 2:

```text
levelSize = 2
process 15
process 7
level = [15,7]
queue = []
result = [[3], [9,20], [15,7]]
```

Answer:

```text
[[3], [9,20], [15,7]]
```

### Complexity

- Time: `O(n)`
- Space: `O(w)`

Where:
- `n` is number of nodes.
- `w` is maximum width of the tree.

The result itself also stores all node values, so output space is `O(n)`.

### Edge Cases

- Empty tree.
- Single node.
- Left-skewed tree.
- Right-skewed tree.
- Wide tree.
- Duplicate values.

### Same Mindset Problems

If you get this, you can solve:
- Binary Tree Right Side View
- Binary Tree Zigzag Level Order Traversal
- Average of Levels in Binary Tree
- Minimum Depth of Binary Tree
- Rotting Oranges
- Shortest Path in Binary Matrix

### If You See This Twist

| Twist | What To Do |
|---|---|
| Need level order | BFS with queue |
| Need right side view | Add last node of each level |
| Need left side view | Add first node of each level |
| Need zigzag | Reverse alternate level or use deque |
| Need bottom-up level order | Build normally then reverse result |

### Variant: Zigzag Level Order

```java
import java.util.*;

public class BinaryTreeZigzagLevelOrder {
    static class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;

        TreeNode(int val) {
            this.val = val;
        }
    }

    public static List<List<Integer>> zigzagLevelOrder(TreeNode root) {
        List<List<Integer>> result = new ArrayList<>();
        if (root == null) {
            return result;
        }

        Queue<TreeNode> queue = new ArrayDeque<>();
        queue.offer(root);
        boolean leftToRight = true;

        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            LinkedList<Integer> level = new LinkedList<>();

            for (int i = 0; i < levelSize; i++) {
                TreeNode node = queue.poll();

                if (leftToRight) {
                    level.addLast(node.val);
                } else {
                    level.addFirst(node.val);
                }

                if (node.left != null) {
                    queue.offer(node.left);
                }
                if (node.right != null) {
                    queue.offer(node.right);
                }
            }

            result.add(new ArrayList<>(level));
            leftToRight = !leftToRight;
        }

        return result;
    }
}
```

---

## Problem 15: Trim a Binary Search Tree

### Problem Statement

Given the root of a binary search tree and two integers `low` and `high`, trim the tree so that all node values lie within:

```text
[low, high]
```

The returned tree should still be a valid BST.

You should preserve the relative structure of nodes that remain.

### Examples

#### Example 1

Input:

```text
root = [1,0,2], low = 1, high = 2
```

Tree:

```text
    1
   / \
  0   2
```

Output:

```text
[1,null,2]
```

Trimmed tree:

```text
    1
     \
      2
```

Explanation:
- Node `0` is less than `low = 1`, so remove it.

#### Example 2

Input:

```text
root = [3,0,4,null,2,null,null,1], low = 1, high = 3
```

Tree:

```text
        3
       / \
      0   4
       \
        2
       /
      1
```

Output:

```text
[3,2,null,1]
```

Trimmed tree:

```text
      3
     /
    2
   /
  1
```

Explanation:
- Node `0` is too small, but its right subtree may contain valid nodes.
- Node `4` is too large, so remove it.

### Pattern

BST recursion.

The main idea:
- If node value is below `low`, discard node and its left subtree.
- If node value is above `high`, discard node and its right subtree.
- If node is within range, recursively trim both children.

### How To Recognize This Pattern

Use this pattern when:
- The tree is a BST.
- The problem asks to search, validate, delete, trim, or find range values.
- You can use BST ordering to skip entire subtrees.

### Hints Before Solution

#### Brute Force Hint

Traverse all nodes.

For each node:
- Check whether it is in range.
- Rebuild a new BST using valid values.

This works, but it does extra work and does not preserve the original relative structure as directly.

#### Optimized Hint

Use BST property.

If:

```text
root.val < low
```

Then:
- Root is invalid.
- Everything in root's left subtree is also smaller.
- Only root's right subtree may contain valid values.

Return:

```text
trimBST(root.right, low, high)
```

If:

```text
root.val > high
```

Then:
- Root is invalid.
- Everything in root's right subtree is also larger.
- Only root's left subtree may contain valid values.

Return:

```text
trimBST(root.left, low, high)
```

Otherwise:
- Root is valid.
- Trim left and right children.

### Solution

```java
public class TrimBST {
    static class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;

        TreeNode(int val) {
            this.val = val;
        }
    }

    public static TreeNode trimBST(TreeNode root, int low, int high) {
        if (root == null) {
            return null;
        }

        if (root.val < low) {
            return trimBST(root.right, low, high);
        }

        if (root.val > high) {
            return trimBST(root.left, low, high);
        }

        root.left = trimBST(root.left, low, high);
        root.right = trimBST(root.right, low, high);

        return root;
    }
}
```

### Dry Run

Input:

```text
low = 1
high = 3
```

Tree:

```text
        3
       / \
      0   4
       \
        2
       /
      1
```

Start at root `3`:

```text
3 is within [1,3]
trim left and right
```

Left child `0`:

```text
0 < low
discard 0 and its left subtree
return trimBST(0.right)
```

Now process `2`:

```text
2 is within [1,3]
trim left and right
```

Left child `1`:

```text
1 is within [1,3]
children are null
return 1
```

Right child of `2`:

```text
null -> return null
```

So trimmed left side of `3` becomes:

```text
    2
   /
  1
```

Right child `4`:

```text
4 > high
discard 4 and its right subtree
return trimBST(4.left)
```

`4.left` is null, so right child becomes null.

Final tree:

```text
      3
     /
    2
   /
  1
```

### Complexity

- Time: `O(n)` in the worst case.
- Space: `O(h)` due to recursion stack.

Where:
- `n` is number of nodes.
- `h` is tree height.

Balanced BST:
- `h = O(log n)`

Skewed BST:
- `h = O(n)`

### Edge Cases

- Empty tree.
- All nodes are in range.
- All nodes are below range.
- All nodes are above range.
- Root itself is removed.
- Single node tree.

### Same Mindset Problems

If you get this, you can solve:
- Search in a BST
- Delete Node in a BST
- Validate Binary Search Tree
- Lowest Common Ancestor of a BST
- Range Sum of BST
- Kth Smallest Element in a BST

### If You See This Twist

| Twist | What To Do |
|---|---|
| Need trim BST by range | Use BST property to discard subtrees |
| Need range sum | If node below low, go right; if above high, go left |
| Need validate BST | Carry min and max bounds |
| Need delete node | Use BST ordering, then handle 0/1/2 child cases |
| Tree is not BST | Must traverse all nodes; cannot skip subtrees |

### Variant: Range Sum of BST

```java
public class RangeSumBST {
    static class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;

        TreeNode(int val) {
            this.val = val;
        }
    }

    public static int rangeSumBST(TreeNode root, int low, int high) {
        if (root == null) {
            return 0;
        }

        if (root.val < low) {
            return rangeSumBST(root.right, low, high);
        }

        if (root.val > high) {
            return rangeSumBST(root.left, low, high);
        }

        return root.val
            + rangeSumBST(root.left, low, high)
            + rangeSumBST(root.right, low, high);
    }
}
```

---

## Problem 16: Substrings Containing Only Vowels

### Problem Statement

Given a string `s`, count the number of substrings that contain only vowels.

Vowels are:

```text
a, e, i, o, u
```

A substring must be contiguous.

Clarify with interviewer:
- Should uppercase vowels count?
- Should the substring contain only vowels, or all five vowels at least once?

This sheet covers the common variant:

```text
Count all substrings made only of vowels.
```

### Examples

#### Example 1

Input:

```text
s = "abc"
```

Output:

```text
1
```

Explanation:
- Valid vowel-only substrings:

```text
"a"
```

#### Example 2

Input:

```text
s = "aei"
```

Output:

```text
6
```

Explanation:
- All substrings are vowel-only:

```text
"a", "e", "i", "ae", "ei", "aei"
```

Total:

```text
6
```

#### Example 3

Input:

```text
s = "abciiidef"
```

Output:

```text
7
```

Explanation:
- Vowel groups are:

```text
"a" -> 1 substring
"iii" -> 6 substrings
"e" -> 1 substring
```

Total:

```text
1 + 6 + 1 = 8
```

Correct output:

```text
8
```

### Pattern

String scan + contiguous segment counting.

The main idea:
- Every continuous block of vowels contributes multiple substrings.
- A vowel block of length `L` has:

```text
L * (L + 1) / 2
```

substrings.

### How To Recognize This Pattern

Use this pattern when:
- The problem asks for number of substrings/subarrays satisfying a simple continuous condition.
- Invalid characters reset the current streak.
- You can count valid substrings ending at each position.

### Hints Before Solution

#### Brute Force Hint

Generate every substring.

For every start:
- Extend every end.
- Check if all characters are vowels.
- Count valid substrings.

Time:
- `O(n^3)` if checking each substring from scratch.
- `O(n^2)` if you break early when consonant appears.

Why this is not ideal:
- Too slow for large strings.

#### Optimized Hint

Scan once and maintain current vowel streak.

If current character is a vowel:
- `currentStreak++`
- Add `currentStreak` to answer.

Why add `currentStreak`?
- If current vowel streak length is `3`, then vowel-only substrings ending here are:

```text
current char only
last 2 chars
last 3 chars
```

So it contributes `3`.

If current character is not a vowel:
- Reset streak to `0`.

### Solution

```java
public class VowelOnlySubstrings {
    public static long countVowelOnlySubstrings(String s) {
        long total = 0;
        int currentStreak = 0;

        for (int i = 0; i < s.length(); i++) {
            char ch = Character.toLowerCase(s.charAt(i));

            if (isVowel(ch)) {
                currentStreak++;
                total += currentStreak;
            } else {
                currentStreak = 0;
            }
        }

        return total;
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

### Dry Run

Input:

```text
s = "abciiidef"
```

Initial:

```text
currentStreak = 0
total = 0
```

| i | ch | Vowel? | currentStreak | Add To Total | total |
|---:|---|---|---:|---:|---:|
| 0 | a | Yes | 1 | 1 | 1 |
| 1 | b | No | 0 | 0 | 1 |
| 2 | c | No | 0 | 0 | 1 |
| 3 | i | Yes | 1 | 1 | 2 |
| 4 | i | Yes | 2 | 2 | 4 |
| 5 | i | Yes | 3 | 3 | 7 |
| 6 | d | No | 0 | 0 | 7 |
| 7 | e | Yes | 1 | 1 | 8 |
| 8 | f | No | 0 | 0 | 8 |

Answer:

```text
8
```

### Complexity

- Time: `O(n)`
- Space: `O(1)`

### Edge Cases

- Empty string -> `0`.
- No vowels -> `0`.
- All vowels.
- Uppercase vowels.
- Mixed punctuation.
- Very long string, so use `long` for count.

### Same Mindset Problems

If you get this, you can solve:
- Count Subarrays With All Ones
- Number of Zero-Filled Subarrays
- Count Substrings Containing Only One Character
- Max Consecutive Ones
- Longest Continuous Increasing Subsequence

### If You See This Twist

| Twist | What To Do |
|---|---|
| Count substrings containing only vowels | Maintain vowel streak and add streak |
| Count vowel substrings containing all five vowels | Sliding window / last seen vowels within vowel blocks |
| Need longest vowel-only substring | Track max streak |
| Need all vowel-only substrings printed | Generate from each vowel block |
| Need count all-one subarrays | Same streak formula |

### Variant: Longest Vowel-Only Substring

```java
public class LongestVowelOnlySubstring {
    public static int longestVowelOnlySubstring(String s) {
        int currentStreak = 0;
        int best = 0;

        for (int i = 0; i < s.length(); i++) {
            char ch = Character.toLowerCase(s.charAt(i));

            if (isVowel(ch)) {
                currentStreak++;
                best = Math.max(best, currentStreak);
            } else {
                currentStreak = 0;
            }
        }

        return best;
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

### Variant: Count Vowel Substrings With All Five Vowels

This is a different problem.

Given:

```text
s = "aeiouu"
```

Valid substrings must:
- Contain only vowels.
- Contain all five vowels at least once.

One simple interview-safe approach:
- Split the string into vowel-only blocks.
- For each block, use a sliding window with vowel frequency map.

```java
import java.util.*;

public class VowelSubstringsWithAllFive {
    public static int countVowelSubstrings(String word) {
        int total = 0;
        int start = 0;

        while (start < word.length()) {
            while (start < word.length() && !isVowel(word.charAt(start))) {
                start++;
            }

            int end = start;
            while (end < word.length() && isVowel(word.charAt(end))) {
                end++;
            }

            total += countAllFiveInBlock(word.substring(start, end));
            start = end;
        }

        return total;
    }

    private static int countAllFiveInBlock(String block) {
        int count = 0;

        for (int left = 0; left < block.length(); left++) {
            Set<Character> seen = new HashSet<>();

            for (int right = left; right < block.length(); right++) {
                seen.add(block.charAt(right));

                if (seen.size() == 5) {
                    count++;
                }
            }
        }

        return count;
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

This variant is less likely for the Marriott Intervue round, but it is worth knowing the distinction.

---

## Final Rapid Revision Table

| Problem | Core Pattern | Must Say In Interview |
|---|---|---|
| Container With Most Water | Two pointers | "Move the smaller height because it limits the area" |
| Meeting Rooms II | Intervals + min heap | "Heap tracks earliest room availability" |
| Binary Tree Right Side View | BFS level order | "Take the last node from each level" |
| Binary Tree Level Order Traversal | BFS queue | "Use queue size to process one level at a time" |
| Trim a BST | BST recursion | "Use BST property to discard entire invalid subtrees" |
| Substrings Containing Only Vowels | Streak counting | "Each vowel adds current streak number of substrings" |

## Monday Morning Drill

Before the interview, do these in order:

1. Code `Container With Most Water` and explain why smaller pointer moves.
2. Code `Meeting Rooms II` using min heap.
3. Code `Right Side View` using BFS.
4. Code `Level Order Traversal` using queue size.
5. Code `Trim BST` and explain why `root.val < low` returns right subtree.
6. Code `Vowel-Only Substrings` and explain why answer adds current streak.

If you can solve these six calmly, your full blind DSA list is covered end to end.
