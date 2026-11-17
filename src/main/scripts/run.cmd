
set JAVA_PATH="javaw"
if defined JAVA_HOME (
    set JAVA_PATH=%JAVA_HOME%\bin\javaw
) else (
    set JAVA_PATH="javaw"
)

%JAVA_PATH% -jar take-break-app.jar