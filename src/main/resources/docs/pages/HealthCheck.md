# Health Check

## Overview
Reports basic JVM health for the extension (heap usage). Useful for monitoring/automation that polls
extension health on a schedule.

## Request Details
- **Area**: Setup
- **Type**: CHANGE_SYSTEM
- **External API**: None.

## Input Parameters
*(none)*

## Output Parameters
| Parameter | Type | Description |
|---|---|---|
| Is Healthy | Boolean | `true` when the extension is responsive. |
| Heap Used MB | Number | Current JVM heap usage, in megabytes. |
| Details | Text | Additional status text. |

## Example
**Output**: Is Healthy=`true`, Heap Used MB=`128`, Details=`"OK"`

## Error Handling
- This request does not call any external system, so it has no failure modes beyond the extension's own
  JVM being unresponsive (in which case the request itself would not return).
