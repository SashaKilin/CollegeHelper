# Security Specification — СтудПульс (StudPulse)

## 1. Data Invariants
1. **User Isolation**: Every document in `/users/{userId}/subjects/{subjectId}` and `/users/{userId}/assignments/{assignmentId}` belongs exclusively to `userId == request.auth.uid`. Cross-user reads, lists, creates, updates, and deletes are strictly forbidden.
2. **Schema & Bounds Integrity**: Every string field has strict `.size()` bounds; `progressPercent` is bounded `0..100`; `workType`, `stage`, and `priority` are constrained to explicit enums; `checklistItems` is a bounded list (`size() <= 25`).
3. **Temporal & Identity Immutability**: `userId` and `createdAt` are immutable on `update`. `createdAt` and `updatedAt` must be valid Firestore `timestamp` objects `<= request.time`.

## 2. The "Dirty Dozen" Payloads
1. **Unauthenticated Read**: `unauthDb.collection("users/alice/assignments").where("userId", "==", "alice").get()` -> Reject
2. **Cross-User Read**: `bobDb.doc("users/alice/assignments/task1").get()` -> Reject
3. **Cross-User List Scraping**: `bobDb.collection("users/alice/assignments").where("userId", "==", "alice").get()` -> Reject
4. **Unfiltered List Query**: `aliceDb.collection("users/alice/assignments").get()` (without `where("userId", "==", "alice")`) -> Reject
5. **Identity Spoofing on Create**: `aliceDb` creates `/users/alice/assignments/task1` with `userId: "bob"` -> Reject
6. **Shadow Field Injection on Create**: Payload includes undeclared `"isAdmin": true` -> Reject via `keys().hasOnly(...)`
7. **Shadow Field Injection on Update**: Update payload adds `"verified": true` -> Reject via `affectedKeys().hasOnly(...)`
8. **Immutable Field Mutation (`createdAt`)**: Update modifies `createdAt` -> Reject
9. **Immutable Field Mutation (`userId`)**: Update modifies `userId` -> Reject
10. **Out-of-Bounds Progress**: `progressPercent: 150` or `-10` -> Reject
11. **Invalid Enum State**: `stage: "HACKED_STAGE"` or `workType: "UNKNOWN"` -> Reject
12. **Oversized String / Array Exhaustion**: `title` > 160 chars or `checklistItems` > 25 items -> Reject

## 3. Red Team Audit Conflict Report
- **Identity Spoofing**: Blocked (`incoming().userId == request.auth.uid` and `request.auth.uid == userId` on create; `incoming().userId == existing().userId` on update).
- **State Shortcutting / Enum Poisoning**: Blocked (`stage` and `workType` validated against strict allowlists in `isValidAssignment`).
- **Resource Poisoning**: Blocked (`isValidId()` on path IDs and `.size()` limits on all strings and lists).
- **Value Poisoning on Update**: Blocked (`isValidSubject(incoming())` and `isValidAssignment(incoming())` wrap the update blocks).
