Write-Host "========================================"
Write-Host " Downloading Gradle Wrapper Files"
Write-Host "========================================"

$gradleVersion = "8.7"
$targetDir = "d:\The_Watch_Project\OpenWear"

# Create directories
$wrapperDir = Join-Path $targetDir "gradle/wrapper"
if (-not (Test-Path $wrapperDir)) {
    New-Item -ItemType Directory -Force -Path $wrapperDir | Out-Null
}

# Source URLs on GitHub
$gradlewUrl = "https://raw.githubusercontent.com/gradle/gradle/v$gradleVersion.0/gradlew"
$gradlewBatUrl = "https://raw.githubusercontent.com/gradle/gradle/v$gradleVersion.0/gradlew.bat"
$jarUrl = "https://raw.githubusercontent.com/gradle/gradle/v$gradleVersion.0/gradle/wrapper/gradle-wrapper.jar"

try {
    Write-Host "Downloading gradlew..."
    Invoke-WebRequest -Uri $gradlewUrl -OutFile (Join-Path $targetDir "gradlew") -UseBasicParsing

    Write-Host "Downloading gradlew.bat..."
    Invoke-WebRequest -Uri $gradlewBatUrl -OutFile (Join-Path $targetDir "gradlew.bat") -UseBasicParsing

    Write-Host "Downloading gradle-wrapper.jar..."
    Invoke-WebRequest -Uri $jarUrl -OutFile (Join-Path $wrapperDir "gradle-wrapper.jar") -UseBasicParsing

    Write-Host "`n[OK] Gradle wrapper files downloaded successfully!"
    Write-Host "Now you can run the build command:"
    Write-Host "  .\gradlew assembleDebug"
}
catch {
    Write-Error "Failed to download Gradle wrapper. Error: $_"
    Write-Host "Please check your internet connection and try again."
}
Write-Host "========================================"
