---
name: Pull Request
about: Submit a code change
title: ''
labels: ''
assignees: ''
---

## Summary
Brief description of what this PR does.

## Type of Change
- [ ] Bug fix
- [ ] New feature / UI improvement
- [ ] New watch driver
- [ ] Documentation update
- [ ] Refactor / code cleanup

## Changes Made
- 
- 

## How to Test
Steps for the reviewer to verify the changes.

## Watch Driver Checklist (if adding a new driver)
- [ ] BLE service and characteristic UUIDs documented in `BleModels.kt`
- [ ] Status packet layout documented in `README.md`
- [ ] Sample packet CSV log included in `docs/packet-logs/`
- [ ] Driver compiles and connects to the target watch

## Build Verification
- [ ] `.\gradlew clean assembleDebug` passes with no errors
