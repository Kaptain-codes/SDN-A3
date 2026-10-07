
<#
.SYNOPSIS
    Installs everything needed to build and package STEMCraft (Java Swing client).

.DESCRIPTION
    - JDK 21 (Eclipse Temurin). Includes javac and jpackage. The project targets Java 17+.
    - Apache Maven 3.9.x (downloaded and SHA-512 verified)
    - Optional: Git (via winget, use -InstallGit)

    Installs to %LOCALAPPDATA%\Tools. No administrator rights are needed.
    Sets JAVA_HOME and adds the tools to your USER PATH.
    Tools that are already installed and new enough are skipped.

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\setup-tools.ps1

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\setup-tools.ps1 -InstallGit -Force
#>
[CmdletBinding()]
param(
    [string]$ToolsDir     = (Join-Path $env:LOCALAPPDATA 'Tools'),
    [int]$JdkVersion      = 21,
    [string]$MavenVersion = '3.9.9',
    [switch]$InstallGit,
    [switch]$Force
)

$ErrorActionPreference = 'Stop'
$ProgressPreference    = 'SilentlyContinue'   # makes downloads much faster in Windows PowerShell
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

function Write-Step($msg) { Write-Host "`n==> $msg" -ForegroundColor Cyan }
function Write-Ok($msg)   { Write-Host "    OK: $msg" -ForegroundColor Green }
function Write-Skip($msg) { Write-Host "    SKIP: $msg" -ForegroundColor Yellow }

# Run a native command and capture stdout+stderr as text (java -version writes to stderr)
function Invoke-Native([string]$commandLine) {
    $prev = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try { return (& cmd /c "$commandLine 2>&1" | Out-String) }
    finally { $ErrorActionPreference = $prev }
}

function Get-JavaMajor {
    if (-not (Get-Command java -ErrorAction SilentlyContinue)) { return 0 }
    $out = Invoke-Native 'java -version'
    if ($out -match 'version "(\d+)(\.(\d+))?') {
        if ($Matches[1] -eq '1') { return [int]$Matches[3] }   # old style 1.8 -> 8
        return [int]$Matches[1]
    }
    return 0
}

# Add a directory to the USER PATH without expanding existing %VARS% entries
function Add-ToUserPath([string]$dir) {
    $key = [Microsoft.Win32.Registry]::CurrentUser.OpenSubKey('Environment', $true)
    try {
        $current = [string]$key.GetValue('Path', '', [Microsoft.Win32.RegistryValueOptions]::DoNotExpandEnvironmentNames)
        $parts = @($current -split ';' | Where-Object { $_ })
        if ($parts -notcontains $dir) {
            $key.SetValue('Path', (($parts + $dir) -join ';'), [Microsoft.Win32.RegistryValueKind]::ExpandString)
        }
    } finally { $key.Close() }
    if (($env:Path -split ';') -notcontains $dir) { $env:Path = "$dir;$env:Path" }
}

function Download-File([string]$url, [string]$dest) {
    Write-Host "    Downloading $url"
    Invoke-WebRequest -Uri $url -OutFile $dest -UseBasicParsing
}

# Unzip to a temp folder, then move the single top-level folder to the final location
function Expand-ToolZip([string]$zip, [string]$finalDir) {
    $tmp = Join-Path $env:TEMP ("stemcraft-extract-" + [guid]::NewGuid().ToString('N'))
    Expand-Archive -Path $zip -DestinationPath $tmp -Force
    $inner = Get-ChildItem $tmp -Directory | Select-Object -First 1
    if (-not $inner) { throw "Archive $zip did not contain a folder." }
    if (Test-Path $finalDir) { Remove-Item $finalDir -Recurse -Force }
    Move-Item $inner.FullName $finalDir
    Remove-Item $tmp -Recurse -Force -ErrorAction SilentlyContinue
}

New-Item -ItemType Directory -Force -Path $ToolsDir | Out-Null
$downloads = Join-Path $env:TEMP 'stemcraft-downloads'
New-Item -ItemType Directory -Force -Path $downloads | Out-Null

# ---------------------------------------------------------------- JDK
Write-Step "JDK $JdkVersion (Temurin)"
$jdkHome = Join-Path $ToolsDir "jdk-$JdkVersion"
$major   = Get-JavaMajor
$hasJpackage = [bool](Get-Command jpackage -ErrorAction SilentlyContinue)

if (-not $Force -and $major -ge 17 -and $hasJpackage) {
    Write-Skip "Java $major with jpackage already available."
} else {
    $zip = Join-Path $downloads "temurin-$JdkVersion.zip"
    $url = "https://api.adoptium.net/v3/binary/latest/$JdkVersion/ga/windows/x64/jdk/hotspot/normal/eclipse"
    Download-File $url $zip
    Expand-ToolZip $zip $jdkHome
    [Environment]::SetEnvironmentVariable('JAVA_HOME', $jdkHome, 'User')
    $env:JAVA_HOME = $jdkHome
    Add-ToUserPath (Join-Path $jdkHome 'bin')
    Write-Ok "JDK installed at $jdkHome"
}

# ---------------------------------------------------------------- Maven
Write-Step "Apache Maven $MavenVersion"
$mavenHome = Join-Path $ToolsDir "apache-maven-$MavenVersion"

if (-not $Force -and (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Skip "Maven already available."
} else {
    $zip     = Join-Path $downloads "apache-maven-$MavenVersion-bin.zip"
    $baseUrl = "https://archive.apache.org/dist/maven/maven-3/$MavenVersion/binaries/apache-maven-$MavenVersion-bin.zip"
    Download-File $baseUrl $zip

    Write-Host "    Verifying SHA-512 checksum"
    $expected = ((Invoke-WebRequest -Uri "$baseUrl.sha512" -UseBasicParsing).Content -as [string]).Trim().Split(' ')[0].ToLower()
    $actual   = (Get-FileHash -Path $zip -Algorithm SHA512).Hash.ToLower()
    if ($expected -ne $actual) { throw "Maven checksum mismatch. Expected $expected but got $actual." }

    Expand-ToolZip $zip $mavenHome
    [Environment]::SetEnvironmentVariable('MAVEN_HOME', $mavenHome, 'User')
    $env:MAVEN_HOME = $mavenHome
    Add-ToUserPath (Join-Path $mavenHome 'bin')
    Write-Ok "Maven installed at $mavenHome"
}

# ---------------------------------------------------------------- Git (optional)
if ($InstallGit) {
    Write-Step "Git"
    if (Get-Command git -ErrorAction SilentlyContinue) {
        Write-Skip "Git already installed."
    } elseif (Get-Command winget -ErrorAction SilentlyContinue) {
        winget install --id Git.Git -e --source winget --accept-package-agreements --accept-source-agreements
        Write-Ok "Git installed (open a new terminal to use it)."
    } else {
        Write-Warning "winget not found. Install Git manually from https://git-scm.com/download/win"
    }
}

# ---------------------------------------------------------------- Verify
Write-Step "Verifying tools"
Write-Host (Invoke-Native 'java -version').Trim()
if (Get-Command jpackage -ErrorAction SilentlyContinue) {
    Write-Ok ("jpackage " + (Invoke-Native 'jpackage --version').Trim())
} else {
    Write-Warning "jpackage not found. Make sure the JDK (not a JRE) is first on PATH."
}
if (Get-Command mvn -ErrorAction SilentlyContinue) {
    Write-Host ((Invoke-Native 'mvn -version').Trim().Split("`n") | Select-Object -First 3 | Out-String).TrimEnd()
} else {
    Write-Warning "mvn not found on PATH in this session. Open a new terminal and try again."
}

Write-Host "`nDone. Close and reopen your terminal (and VS Code / IntelliJ) so the new PATH is picked up." -ForegroundColor Green
Write-Host "Then build with:" -ForegroundColor Green
Write-Host "    cd client"
Write-Host "    mvn clean package"
Write-Host "    java -jar target\stemmate-client-0.1.0-SNAPSHOT.jar"