param(
  [string]$RepoRoot = "C:\Users\KIM\git\sh-platform"
)

$Today = Get-Date -Format "yyyy-MM-dd"

function Get-Category {
  param([string]$Path)
  $relative = $Path.Replace($RepoRoot, "").Replace("\", "/").TrimStart("/")
  if ($relative -eq "README.md") { return "readme" }
  if ($relative -eq "AGENTS.md") { return "config" }
  if ($relative -eq "SCRAPER-GUIDE.md") { return "scraper" }
  if ($relative -match "^docs/architecture/") { return "architecture" }
  if ($relative -match "^docs/auth/") { return "auth" }
  if ($relative -match "^docs/scraper/") { return "scraper" }
  if ($relative -match "^docs/guides/") { return "guide" }
  if ($relative -match "^docs/infra(stracture)?/") { return "infra" }
  if ($relative -match "^docs/common/") { return "common" }
  if ($relative -match "^docs/database/") { return "database" }
  if ($relative -match "^docs/development/") { return "development" }
  if ($relative -match "^docs/plans/") { return "plan" }
  if ($relative -match "^docs/saas/") { return "saas" }
  if ($relative -match "^docs/daily/") { return "daily" }
  if ($relative -match "^docs/front/") { return "front" }
  return "general"
}

function Get-Title {
  param([string]$Path)
  $name = [System.IO.Path]::GetFileNameWithoutExtension($Path)
  # Kebab-case to Title Case
  $title = ($name -replace "-", " " -replace "_", " ")
  # Uppercase first letter of each word
  $title = (Get-Culture).TextInfo.ToTitleCase($title.ToLower())
  # Handle special abbreviations
  $title = $title -replace "\bOci\b", "OCI"
  $title = $title -replace "\bSsl\b", "SSL"
  $title = $title -replace "\bDto\b", "DTO"
  $title = $title -replace "\bApi\b", "API"
  $title = $title -replace "\bErd\b", "ERD"
  $title = $title -replace "\bDdl\b", "DDL"
  $title = $title -replace "\bUi\b", "UI"
  $title = $title -replace "\bDb\b", "DB"
  $title = $title -replace "\bSql\b", "SQL"
  $title = $title -replace "\bV2\b", "v2"
  $title = $title -replace "\bV3\b", "v3"
  return $title
}

function Get-Description {
  param([string]$Path)
  $name = [System.IO.Path]::GetFileNameWithoutExtension($Path)
  $title = Get-Title -Path $Path
  $category = Get-Category -Path $Path
  return "$title - $category module documentation"
}

function Get-CreatedDate {
  param([string]$Path)
  $relative = $Path.Replace($RepoRoot, "").Replace("\", "/").TrimStart("/")
  $date = & {
    $d = git -C "$RepoRoot" log --follow --format=%as --reverse -- "$relative" 2>$null | Select-Object -First 1
    if ($d) { return $d }
    return $Today
  }
  return $date
}

function Has-Frontmatter {
  param([string]$Content)
  return $Content.TrimStart().StartsWith("---")
}

function Generate-Frontmatter {
  param([string]$Path)

  $title = Get-Title -Path $Path
  $category = Get-Category -Path $Path
  $description = Get-Description -Path $Path
  $created = Get-CreatedDate -Path $Path

  return @"

---
title: $title
description: $description
category: $category
created: $created
updated: $Today
---

"@
}

# Main
$files = Get-ChildItem -Path $RepoRoot -Recurse -Filter "*.md" -File | Where-Object {
  $_.FullName -notmatch "\\(node_modules|\.git|build|dist|\.gradle|\.bak)\\" -and
  $_.FullName -notmatch "\\frontend\\node_modules\\"
}

$count = 0
$skipped = 0
$errors = @()

foreach ($file in $files) {
  try {
    $content = Get-Content -LiteralPath $file.FullName -Raw -Encoding UTF8
    if ($null -eq $content) { $content = "" }

    if (Has-Frontmatter -Content $content) {
      Write-Host "SKIP (has frontmatter): $($file.Name)" -ForegroundColor Yellow
      $skipped++
      continue
    }

    $frontmatter = Generate-Frontmatter -Path $file.FullName

    # Skip if empty file
    if ($content.Trim().Length -eq 0) {
      $content = $frontmatter
    } else {
      $content = $frontmatter.TrimStart() + "`n" + $content.TrimStart()
    }

    Set-Content -LiteralPath $file.FullName -Value $content -Encoding UTF8 -NoNewline
    Write-Host "OK: $($file.Name)" -ForegroundColor Green
    $count++
  }
  catch {
    Write-Host "ERR: $($file.Name) - $_" -ForegroundColor Red
    $errors += $file.Name
  }
}

Write-Host "`n=== Done ===" -ForegroundColor Cyan
Write-Host "Applied: $count" -ForegroundColor Green
Write-Host "Skipped (already has frontmatter): $skipped" -ForegroundColor Yellow
if ($errors.Count -gt 0) {
  Write-Host "Errors: $($errors.Count)" -ForegroundColor Red
  $errors | ForEach-Object { Write-Host "  - $_" }
}
