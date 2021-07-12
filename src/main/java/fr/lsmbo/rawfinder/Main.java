package fr.lsmbo.rawfinder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.DosFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;

public class Main {

    protected static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static Global global;
    private static final HashMap<File, File> data = new HashMap<>();
    private enum Policy { CREATION_DATE, NEXT_MONTH, MODIFICATION_DATE }

    public static void main(String[] args) {
        try {
            logger.info("Starting RawFinder");
            // load configuration
            global = new Global();

            // read local directories and fill data
            logger.info("Parsing data directory");
            readDirectory(global.RAW_DATA_DIRECTORY);

            // generate excel output
            Export export = new Export(global, data);
            export.start();

        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            t.printStackTrace();
        }
        logger.info("End of RawFinder");
    }

    private static void readDirectory(File directory) {
        // read raw-like directories first (recursive call)
        Arrays.stream(Objects.requireNonNull(directory.listFiles())).filter(File::isDirectory).forEach(Main::readDirectory);

        // read raw-like files then (search archive files)
        Arrays.stream(Objects.requireNonNull(directory.listFiles())).filter(file -> file.isFile() && global.IsRawData(file)).forEach(item -> {
            // TODO check if the item has a parent matching a raw file (if folder like)
            File archive = findArchive(item);
            data.put(item, archive);
            if(data.size() % 100 == 0) logger.info(data.size() + " raw files found...");
        });
    }

    private static File getArchive(File file, Policy policy) {
        String path = "";
        try {
            if(policy.equals(Policy.MODIFICATION_DATE)) {
                LocalDateTime localDateTime = LocalDateTime.ofEpochSecond(file.lastModified(), 0, ZoneOffset.MIN);
                path = global.RAW_DATA_ARCHIVES.getAbsolutePath() + "/" + localDateTime.getYear() + "/" + global.MONTH_NAMES[localDateTime.getMonthValue() - 1];
            } else {
                DosFileAttributes attributes = Files.readAttributes(file.toPath(), DosFileAttributes.class);
                LocalDateTime localDateTime = LocalDateTime.ofInstant(attributes.creationTime().toInstant(), ZoneId.systemDefault());
                int year = localDateTime.getYear();
                int month = localDateTime.getMonthValue();
                if (policy.equals(Policy.CREATION_DATE)) {
                    path = global.RAW_DATA_ARCHIVES.getAbsolutePath() + "/" + year + "/" + global.MONTH_NAMES[month - 1];
                } else if (policy.equals(Policy.NEXT_MONTH)) {
                    if (month < global.MONTH_NAMES.length) // January 2021 -> February 2021
                        path = global.RAW_DATA_ARCHIVES.getAbsolutePath() + "/" + year + "/" + global.MONTH_NAMES[month];
                    else // december 2020 -> january 2021
                        path = global.RAW_DATA_ARCHIVES.getAbsolutePath() + "/" + (year + 1) + "/" + global.MONTH_NAMES[0];
                }
            }
        } catch (IOException ioe) {
            logger.warn("File " + file.getAbsolutePath() + ": creation date could not be read, can't retrieve archived version", ioe);
        }
        return new File(path, global.RAW_DATA_DIRECTORY.toURI().relativize(file.toURI()).getPath());
    }

    private static File findArchive(File file) {
        File mostProbableArchive = getArchive(file, Policy.CREATION_DATE);
        File possibleArchive = getArchive(file, Policy.MODIFICATION_DATE);
        File lastResortArchive = getArchive(file, Policy.NEXT_MONTH);
        // return the correct match if any
        if(mostProbableArchive.exists() && mostProbableArchive.length() == file.length()) return mostProbableArchive;
        if(possibleArchive.exists() && possibleArchive.length() == file.length()) return possibleArchive;
        if(lastResortArchive.exists() && lastResortArchive.length() == file.length()) return lastResortArchive;
        // if there is no correct match return the latest existing one
        if(lastResortArchive.exists()) return lastResortArchive;
        if(possibleArchive.exists()) return possibleArchive;
        if(mostProbableArchive.exists()) return mostProbableArchive;
        // if nothing exists at all, return null
        return null;
    }

}
