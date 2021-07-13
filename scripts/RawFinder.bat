@echo off
title RawFinder
start javaw -Xmx2G -cp "lib/*;config;RawFinder-${project.version}.jar" -Dlog4j.configurationFile=config/logback.xml fr.lsmbo.rawfinder.Main gui

