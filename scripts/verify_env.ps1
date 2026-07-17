param (
    [switch]$InstallMissing = $false
)

Write-Host "========================================"
Write-Host " OpenWear Environment Verification Tool"
Write-Host "========================================"

# 1. Check JAVA_HOME
if ([string]::IsNullOrEmpty($env:JAVA_HOME)) {
    Write-Warning "JAVA_HOME is not set."
    Write-Host "Please install JDK 17 or 22 and set JAVA_HOME."
} else {
    Write-Host "[OK] JAVA_HOME is set to: $env:JAVA_HOME"
    $javaVersion = & java -version 2>&1 | Select-Object -First 1
    Write-Host "     Found: $javaVersion"
}

# 2. Check ANDROID_HOME and find SDK
$sdkFoundPath = ""
$commonSdkPaths = @(
    "C:\Android-SDK",
    "$env:LOCALAPPDATA\Android\Sdk",
    "$env:USERPROFILE\AppData\Local\Android\Sdk",
    "C:\Program Files (x86)\Android\android-sdk",
    "C:\Program Files\Android\android-sdk",
    "C:\Android\sdk",
    "D:\Android\sdk"
)

if ([string]::IsNullOrEmpty($env:ANDROID_HOME)) {
    Write-Warning "ANDROID_HOME is not set."
    Write-Host "Searching common locations for Android SDK..."
    
    foreach ($path in $commonSdkPaths) {
        if (Test-Path $path) {
            $sdkFoundPath = $path
            break
        }
    }

    if ($sdkFoundPath -ne "") {
        Write-Host "[INFO] Found Android SDK at: $sdkFoundPath"
        Write-Host "Please set the ANDROID_HOME environment variable to this path."
        Write-Host "PowerShell command to set it for current session:"
        Write-Host "  `$env:ANDROID_HOME = `"$sdkFoundPath`""
        Write-Host "Command to set it permanently:"
        Write-Host "  [System.Environment]::SetEnvironmentVariable('ANDROID_HOME', '$sdkFoundPath', 'User')"
    } else {
        Write-Error "Could not find Android SDK in common locations."
        Write-Host "Please install it or locate it and set ANDROID_HOME."
    }
} else {
    Write-Host "[OK] ANDROID_HOME is set to: $env:ANDROID_HOME"
    $sdkFoundPath = $env:ANDROID_HOME
}

# 3. Check for adb
$adbPath = Get-Command "adb" -ErrorAction SilentlyContinue
if ($null -eq $adbPath) {
    Write-Warning "adb is not on PATH."
    
    # Try to find adb in the SDK path
    if ($sdkFoundPath -ne "" -and (Test-Path "$sdkFoundPath\platform-tools\adb.exe")) {
        $foundAdb = "$sdkFoundPath\platform-tools"
        Write-Host "[INFO] Found adb in: $foundAdb"
        Write-Host "Please add this folder to your PATH environment variable."
        Write-Host "PowerShell command to add for current session:"
        Write-Host "  `$env:Path += `";$foundAdb`""
        Write-Host "Command to set it permanently (User PATH):"
        Write-Host "  `$userPath = [System.Environment]::GetEnvironmentVariable('Path', 'User')"
        Write-Host "  [System.Environment]::SetEnvironmentVariable('Path', `"`$userPath;$foundAdb`", 'User')"
    } else {
        Write-Host "Could not find adb.exe. Please ensure platform-tools are installed in the Android SDK."
    }
} else {
    Write-Host "[OK] adb found at: $($adbPath.Source)"
    $adbVersion = & adb version | Select-Object -First 1
    Write-Host "     Found: $adbVersion"
}

# 4. Connected Devices
if ($null -ne $adbPath) {
    Write-Host "`nConnected Devices:"
    & adb devices
} elseif ($sdkFoundPath -ne "" -and (Test-Path "$sdkFoundPath\platform-tools\adb.exe")) {
    Write-Host "`nChecking devices using found adb..."
    & "$sdkFoundPath\platform-tools\adb.exe" devices
}

Write-Host "`nTo build the project:"
Write-Host "1. Navigate to d:\The_Watch_Project\OpenWear"
Write-Host "2. Run: gradle wrapper (if gradlew is missing)"
Write-Host "3. Run: .\gradlew assembleDebug"
Write-Host "4. Run: adb install -r app\build\outputs\apk\debug\app-debug.apk"
Write-Host "========================================"
