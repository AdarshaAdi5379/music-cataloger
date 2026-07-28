@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper Batch Script
@REM ----------------------------------------------------------------------------
@echo off
setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set BASE_DIR=%DIRNAME%

set WRAPPER_JAR="%BASE_DIR%\.mvn\wrapper\maven-wrapper.jar"
set WRAPPER_PROPERTIES="%BASE_DIR%\.mvn\wrapper\maven-wrapper.properties"

if not exist %WRAPPER_JAR% (
    if not exist "%BASE_DIR%\.mvn\wrapper" mkdir "%BASE_DIR%\.mvn\wrapper"
    powershell -Command "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar' -OutFile %WRAPPER_JAR%"
)

java -classpath %WRAPPER_JAR% org.apache.maven.wrapper.MavenWrapperMain %*
