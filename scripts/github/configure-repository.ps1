param(
    [string]$Repository = "nursena-pc/fitto-ai-calorie-tracker"
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    throw "GitHub CLI (gh) is required. Install it and run 'gh auth login' first."
}

gh repo edit $Repository `
    --enable-issues=true `
    --enable-auto-merge=true `
    --enable-squash-merge=true `
    --enable-merge-commit=false `
    --enable-rebase-merge=false `
    --delete-branch-on-merge=true `
    --allow-update-branch=true `
    --squash-merge-commit-message=pr-title-description `
    --add-topic=android `
    --add-topic=kotlin `
    --add-topic=firebase `
    --add-topic=room `
    --add-topic=health-connect `
    --add-topic=nutrition `
    --add-topic=clean-architecture

Write-Host "Configured repository merge, branch cleanup, and topic settings for $Repository."
Write-Host "Configure the main branch ruleset manually after the new CI checks have completed once."
