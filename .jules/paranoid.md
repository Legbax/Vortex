## 2024-05-22 - Command Evasion Bypass
**Learning:** `SpoofingUtils.isSensitiveCommand` previously only checked for exact matches of commands in `SENSITIVE_COMMANDS` (Set). Commands wrapped in `sh -c` or with arguments were bypassing the check if they weren't in the specific `contains` block.
**Action:** Always verify command matching logic handles substrings and wrapped commands. Added comprehensive `contains` checks for sensitive properties.

## 2024-05-22 - Lancelot Bootloader Leak
**Learning:** Lancelot devices with unlocked bootloaders leak `androidboot.verifiedbootstate=orange` in `/proc/cmdline`. This is readable by apps even if `Build` props are spoofed.
**Action:** Blocked access to `/proc/cmdline` and `getprop ro.boot.verifiedbootstate`.
