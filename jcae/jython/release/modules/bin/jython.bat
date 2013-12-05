@echo off
set CLASSPATH=%~dp0\..\ext\jython.jar;%CLASSPATH%
"%~dp0\..\..\..\jre\bin\java" -Dpython.cachedir.skip=false org.python.util.jython %*

