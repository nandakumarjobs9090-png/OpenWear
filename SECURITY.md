# Security Policy

## Supported Versions

| Version | Supported |
|---------|-----------|
| main (latest) | ✅ |

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub Issues.**

If you discover a security vulnerability in OpenWear, please email the maintainers privately. Include:

1. A description of the vulnerability.
2. Steps to reproduce it.
3. Potential impact assessment.

We will acknowledge your report within 48 hours and aim to release a fix within 14 days for critical issues.

## Scope

This project communicates with personal health devices over Bluetooth. Security-relevant areas include:

- BLE communication security (data integrity, spoofing prevention)
- Local database encryption of health data
- Permissions handling (Bluetooth, Location)

We take the privacy of health data very seriously. Any issues in these areas will be treated as high priority.
