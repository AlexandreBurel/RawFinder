@echo off
title RawFinder
::jre\bin\java -Xmx2G -cp "lib/*;config;RawFinder-${project.version}.jar" -Dlog4j.configurationFile=config/logback.xml fr.lsmbo.rawfinder.Main %*
java -Xmx2G -cp "lib/*;config;RawFinder-${project.version}.jar" -Dlog4j.configurationFile=config/logback.xml fr.lsmbo.rawfinder.Main %*

pause
