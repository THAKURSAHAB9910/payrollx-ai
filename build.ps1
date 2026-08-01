# PayrollX Build and Test Script
$ErrorActionPreference = "Stop"

# Set JDK 17 path (Adoptium package from system)
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

Write-Output "Checking Java version..."
java -version
javac -version

# Setup Maven locally if not present
$mavenBinDir = "c:\Users\Vishe\Downloads\javap\maven\apache-maven-3.9.6\bin"
if (-not (Test-Path $mavenBinDir)) {
    Write-Output "Maven not detected locally. Downloading Apache Maven 3.9.6..."
    $mavenZipUrl = "https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.zip"
    $outputZip = "c:\Users\Vishe\Downloads\javap\maven.zip"
    $extractDir = "c:\Users\Vishe\Downloads\javap\maven"
    
    # Download
    Invoke-WebRequest -Uri $mavenZipUrl -OutFile $outputZip
    Write-Output "Extracting Maven..."
    Expand-Archive -Path $outputZip -DestinationPath $extractDir -Force
    
    # Clean up zip
    Remove-Item $outputZip -Force
}

$env:PATH = "$mavenBinDir;$env:PATH"
Write-Output "Checking Maven version..."
mvn -version

# Build project and run tests
Write-Output "Compiling project and running JUnit tests..."
mvn clean test

Write-Output "Build and test completed successfully!"
