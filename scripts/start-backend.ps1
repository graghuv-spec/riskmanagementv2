# PowerShell script to set environment variables and start the backend
# Update the values below as needed

$env:DB_URL = "jdbc:postgresql://localhost:5432/yourdb"
$env:DB_USERNAME = "youruser"
$env:DB_PASSWORD = "yourpassword"

# Navigate to backend directory and start the Spring Boot application
cd "$PSScriptRoot/../backend"
./gradlew.bat bootRun
