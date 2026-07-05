# Repository Governance

## Default branch

`main` is the protected, releasable branch. Direct pushes should be disabled. Every change should arrive through a focused pull request from a short-lived branch.

## Required pull request checks

The following GitHub Actions checks should be required before merge:

- `Unit tests and lint`
- `Assemble debug artifacts`
- `Review dependency changes` for public-repository pull requests

Instrumentation tests run weekly, manually, and whenever migration or Android-test paths change. Run them manually before merging any change involving Room migrations, lifecycle-sensitive Android behavior, alarms, permissions, or platform integrations.

## Recommended ruleset for `main`

Configure a GitHub branch ruleset with these settings:

- Require a pull request before merging
- Require at least one approving review when another maintainer is available
- Dismiss stale approvals when new commits are pushed
- Require review from Code Owners
- Require all conversations to be resolved
- Require status checks to pass
- Require branches to be up to date before merging
- Require linear history
- Block force pushes
- Block branch deletion
- Do not allow bypass except for emergency repository administration

Use squash merge as the default merge strategy and automatically delete merged branches.

## Pull request scope

A pull request should represent one coherent change. Architecture changes, migrations, UI redesigns, dependency upgrades, and feature work should not be bundled unless they are inseparable and independently verified.

## Labels

Use consistent labels for triage and reporting:

- Type: `bug`, `enhancement`, `refactor`, `documentation`, `dependencies`, `security`
- Status: `needs triage`, `blocked`, `ready for review`
- Risk: `risk: low`, `risk: medium`, `risk: high`
- Area labels are applied automatically from changed paths

## Releases

Create annotated version tags from tested commits on `main`. Release notes should include user-visible changes, fixes, migration notes, known limitations, and verification evidence. Never attach a production APK built from an unreviewed local branch.

## One-time repository setup

After merging the repository-foundation pull request, authenticate GitHub CLI and run:

```powershell
gh auth login
.\scripts\github\setup-labels.ps1
.\scripts\github\configure-repository.ps1
```

These scripts create the labels referenced by issue forms and automated PR labelling, then configure squash merging, automatic branch deletion, update-branch support, and repository topics. Branch rules remain a deliberate manual setting because required check names must exist before they can be selected.
