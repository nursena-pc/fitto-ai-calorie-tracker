param(
    [string]$Repository = "nursena-pc/fitto-ai-calorie-tracker"
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    throw "GitHub CLI (gh) is required. Install it and run 'gh auth login' first."
}

$labels = @(
    @{ Name = "bug"; Color = "D73A4A"; Description = "Something is not working" },
    @{ Name = "enhancement"; Color = "A2EEEF"; Description = "New feature or improvement" },
    @{ Name = "refactor"; Color = "5319E7"; Description = "Behavior-preserving architecture or code-quality change" },
    @{ Name = "documentation"; Color = "0075CA"; Description = "Documentation-only change" },
    @{ Name = "dependencies"; Color = "0366D6"; Description = "Dependency or build-tool update" },
    @{ Name = "security"; Color = "B60205"; Description = "Security-sensitive work" },
    @{ Name = "needs triage"; Color = "FBCA04"; Description = "Needs initial review and prioritization" },
    @{ Name = "blocked"; Color = "BFD4F2"; Description = "Blocked by another task or decision" },
    @{ Name = "ready for review"; Color = "0E8A16"; Description = "Implementation is ready for review" },
    @{ Name = "risk: low"; Color = "C2E0C6"; Description = "Low regression or operational risk" },
    @{ Name = "risk: medium"; Color = "FEF2C0"; Description = "Moderate regression or operational risk" },
    @{ Name = "risk: high"; Color = "F9D0C4"; Description = "High regression, data, security, or release risk" },
    @{ Name = "tests"; Color = "1D76DB"; Description = "Test coverage or test infrastructure" },
    @{ Name = "area: build"; Color = "D4C5F9"; Description = "Gradle and build configuration" },
    @{ Name = "area: ci"; Color = "D4C5F9"; Description = "GitHub Actions and repository automation" },
    @{ Name = "area: documentation"; Color = "D4C5F9"; Description = "Documentation and project guidance" },
    @{ Name = "area: authentication"; Color = "C5DEF5"; Description = "Authentication and onboarding" },
    @{ Name = "area: daily"; Color = "C5DEF5"; Description = "Daily dashboard and AI summary" },
    @{ Name = "area: diet plans"; Color = "C5DEF5"; Description = "Diet plans and fasting" },
    @{ Name = "area: exercise and health"; Color = "C5DEF5"; Description = "Exercise, Health Connect, sleep, and steps" },
    @{ Name = "area: meals"; Color = "C5DEF5"; Description = "Meals, food search, and camera food entry" },
    @{ Name = "area: profile"; Color = "C5DEF5"; Description = "Profile, settings, measurements, and reminders" },
    @{ Name = "area: recipes"; Color = "C5DEF5"; Description = "Recipes, fridge, and shopping lists" },
    @{ Name = "area: reports"; Color = "C5DEF5"; Description = "Reports and progress analytics" },
    @{ Name = "area: social and messaging"; Color = "C5DEF5"; Description = "Social feed, profiles, groups, and messaging" },
    @{ Name = "area: database"; Color = "C5DEF5"; Description = "Room schema, migrations, and local persistence" }
)

foreach ($label in $labels) {
    gh label create $label.Name `
        --repo $Repository `
        --color $label.Color `
        --description $label.Description `
        --force
}

Write-Host "Configured $($labels.Count) labels for $Repository."
