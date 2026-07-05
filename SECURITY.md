# Security Policy

## Supported version

Security fixes are applied to the latest commit on the `main` branch. Older branches and historical builds are not maintained as supported releases.

## Reporting a vulnerability

Do not open a public issue for vulnerabilities, exposed credentials, authentication bypasses, private-data access, Firebase rule problems, or health-data leaks.

Use the repository **Security** tab and create a private vulnerability report:

`Security` → `Advisories` → `Report a vulnerability`

Include:

- A concise description and impact
- Reproduction steps or a proof of concept
- Affected screens, data, API levels, or commits
- Suggested remediation when available

Do not include real user health data, private messages, authentication tokens, API keys, or production credentials. Use synthetic examples.

## Response expectations

Maintainers will acknowledge valid reports, assess severity, coordinate a fix, and disclose the issue only after a safe remediation path exists. Please avoid public disclosure while the report is under review.

## Secrets

The repository must never contain production API keys, Firebase service-account credentials, signing keys, keystores, access tokens, private `google-services.json` files, or personal user data. Revoke and rotate any credential immediately if it is committed accidentally.
