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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

public class DataParser {
    protected static final Logger logger = LoggerFactory.getLogger(DataParser.class);

    private enum Policy { CREATION_DATE, NEXT_MONTH, MODIFICATION_DATE }
    private final HashMap<File, File> data = new HashMap<>();
    private final HashMap<String, RawData> rawData = new HashMap<>();
    private final File parentDirectory;

    public DataParser(File _parentDirectory) {
        parentDirectory = _parentDirectory;
    }

    public void start() {
        logger.info("Parsing data directory");
        readDirectory(parentDirectory);
        HashMap<String, Integer> countPerStatus = getCountPerStatus();
        logger.info("Data directory is parsed\n\nRawFinder search summary:\n" +
        "- Number of fully archived raw data: " + countPerStatus.get(Status.FULLY_ARCHIVED.toString()) + "\n" +
        "- Number of partially archived raw data: " + countPerStatus.get(Status.PARTIALLY_ARCHIVED.toString()) + "\n" +
        "- Number of raw data not archived at all: " + countPerStatus.get(Status.NOT_ARCHIVED.toString()) + "\n");
    }

    private void readDirectory(File directory) {
        // read raw-like directories first (recursive call)
        Arrays.stream(Objects.requireNonNull(directory.listFiles())).filter(File::isDirectory).forEach(this::readDirectory);

        // read raw-like files then (search archive files)
        Arrays.stream(Objects.requireNonNull(directory.listFiles())).filter(file -> file.isFile() && Global.IsRawData(file)).forEach(item -> {
            File archive = findArchive(item);
            data.put(item, archive);

            // also feed the rawData hashmap
            String currentRawFileName = Global.getRawFileName(item);
            if(!rawData.containsKey(currentRawFileName)) rawData.put(currentRawFileName, new RawData(new File(currentRawFileName)));
            rawData.get(currentRawFileName).addFile(item, archive != null);

            if(rawData.size() % 100 == 0) logger.info(rawData.size() + " raw files found...");
        });
    }

    private File getArchive(File file, Policy policy) {
        String path = "";
        try {
            if(policy.equals(Policy.MODIFICATION_DATE)) {
                LocalDateTime localDateTime = LocalDateTime.ofEpochSecond(file.lastModified(), 0, ZoneOffset.MIN);
                path = Global.RAW_DATA_ARCHIVES.getAbsolutePath() + "/" + localDateTime.getYear() + "/" + Global.MONTH_NAMES[localDateTime.getMonthValue() - 1];
            } else {
                DosFileAttributes attributes = Files.readAttributes(file.toPath(), DosFileAttributes.class);
                LocalDateTime localDateTime = LocalDateTime.ofInstant(attributes.creationTime().toInstant(), ZoneId.systemDefault());
                int year = localDateTime.getYear();
                int month = localDateTime.getMonthValue();
                if (policy.equals(Policy.CREATION_DATE)) {
                    path = Global.RAW_DATA_ARCHIVES.getAbsolutePath() + "/" + year + "/" + Global.MONTH_NAMES[month - 1];
                } else if (policy.equals(Policy.NEXT_MONTH)) {
                    if (month < Global.MONTH_NAMES.length) // January 2021 -> February 2021
                        path = Global.RAW_DATA_ARCHIVES.getAbsolutePath() + "/" + year + "/" + Global.MONTH_NAMES[month];
                    else // december 2020 -> january 2021
                        path = Global.RAW_DATA_ARCHIVES.getAbsolutePath() + "/" + (year + 1) + "/" + Global.MONTH_NAMES[0];
                }
            }
        } catch (IOException ioe) {
            logger.warn("File " + file.getAbsolutePath() + ": creation date could not be read, can't retrieve archived version", ioe);
        }
        return new File(path, Global.RAW_DATA_DIRECTORY.toURI().relativize(file.toURI()).getPath());
    }

    private File findArchive(File file) {
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

    public HashMap<File, File> getData() {
        return data;
    }

    public HashMap<String, RawData> getAsRawData() {
        return rawData;
    }

    public HashMap<String, Integer> getCountPerStatus() {
        HashMap<String, Integer> countPerStatus = new HashMap<>();
        rawData.keySet().forEach( name -> {
            String status = rawData.get(name).getStatus();
            countPerStatus.put(status, countPerStatus.containsKey(status) ? countPerStatus.get(status) + 1 : 1);
        });
        return countPerStatus;
    }
}
