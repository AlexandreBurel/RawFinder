@echo off
title RawFinder
jre\bin\java -Xmx2G -cp "lib/*;config;RawFinder-1.1.jar" -Dlog4j.configurationFile=config/log4j.xml fr.lsmbo.rawfinder.Main %*

