@echo off

set scriptpath=%dp0
set jarname=%scriptpath%\smobs.jar

java -jar "%jarname%" %*
