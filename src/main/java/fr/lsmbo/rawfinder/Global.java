package fr.lsmbo.rawfinder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.net.InetAddress;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

public class Global {

    protected static final Logger logger = LoggerFactory.getLogger(Global.class);
    private static String appName = "RawFinder";
    private static String appSubName = "Search raw files in the archives";
    private static String appVersion = "";
    private static String appDate = "";

    public static File RAW_DATA_DIRECTORY;
    public static File RAW_DATA_ARCHIVES;
    public static File REPORTS_DIRECTORY;
    public static Boolean IS_FOLDER_LIKE;
    public static List<String> FOLDER_LIKE_RAW_DATA_TEMPLATE;
    public static List<String> FOLDER_LIKE_RAW_DATA_EXTENSION;
    public static List<String> FILE_LIKE_RAW_DATA_TEMPLATE;

    public final static String[] MONTH_NAMES = {"janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août", "septembre", "octobre", "novembre", "décembre"};
    private final static String[] units = new String[] { "octets", "ko", "Mo", "Go", "To" };

//    public Global() throws Throwable {
    public static void initialize() throws Throwable {
        final Properties properties = new Properties();
        properties.load(Global.class.getClassLoader().getResourceAsStream("RawFinder.properties"));
        appName = properties.getProperty("name");
        appSubName = properties.getProperty("description");
        appVersion = properties.getProperty("version");
        appDate = properties.getProperty("build-date");

        Gson gson = new Gson();
        JsonReader reader = new JsonReader(new InputStreamReader(
                Objects.requireNonNull(Global.class.getClassLoader().getResourceAsStream("settings.json")),
                StandardCharsets.UTF_8));
        Settings settings = gson.fromJson(reader, Settings.class);

//        RAW_DATA_DIRECTORY = new File(settings.getRawDataDirectory());
//        RAW_DATA_ARCHIVES = new File(settings.getArchiveDirectory());
        RAW_DATA_DIRECTORY = settings.getRawDataDirectory().equals("") ? null : new File(settings.getRawDataDirectory());
        RAW_DATA_ARCHIVES = settings.getArchiveDirectory().equals("") ? null : new File(settings.getArchiveDirectory());
        REPORTS_DIRECTORY = new File(settings.getDefaultReportDirectory());
        IS_FOLDER_LIKE = settings.getFolderLike();
        FOLDER_LIKE_RAW_DATA_TEMPLATE = Arrays.stream(settings.getFolderLikeRawDataTemplate().split(" ")).collect(Collectors.toList());
//        FOLDER_LIKE_RAW_DATA_EXTENSION = Arrays.stream(settings.getFolderLikeRawDataExtension().split(" ")).collect(Collectors.toList());
        FILE_LIKE_RAW_DATA_TEMPLATE = Arrays.stream(settings.getFileLikeRawDataTemplate().split(" ")).collect(Collectors.toList());

        // make sure the mandatory directories are available (if not, maybe the settings file is not encoded in UTF8 ?)
//        if(!RAW_DATA_DIRECTORY.exists() || !RAW_DATA_DIRECTORY.isDirectory()) throw new Exception("Data directory '"+RAW_DATA_DIRECTORY.getAbsolutePath()+"' is not available");
//        if(!RAW_DATA_ARCHIVES.exists() || !RAW_DATA_ARCHIVES.isDirectory()) throw new Exception("Archive directory '"+RAW_DATA_ARCHIVES.getAbsolutePath()+"' is not available");
        if(RAW_DATA_DIRECTORY == null) logger.warn("Data directory is not available");
        if(RAW_DATA_ARCHIVES == null) logger.warn("Archive directory is not available");
        // make sure the report directory exists, otherwise use the home directory
        if(!REPORTS_DIRECTORY.exists() && !REPORTS_DIRECTORY.mkdir()) REPORTS_DIRECTORY = new File(System.getProperty("user.home"));
    }

    public static Boolean areSettingsValid() {
        return (Global.RAW_DATA_DIRECTORY != null && Global.RAW_DATA_DIRECTORY.exists() && Global.RAW_DATA_ARCHIVES != null && Global.RAW_DATA_ARCHIVES.exists());
    }

    private static Boolean matchesAny(String name, List<String> list) {
        for(String item : list) {
            if(name.matches(item)) return true;
        }
        return false;
    }

    private static Boolean endsWithAny(String name, List<String> list) {
        for(String item : list) {
            if(name.endsWith(item)) return true;
        }
        return false;
    }

    public static String getRawFileName(File file) {
        if(IS_FOLDER_LIKE) return getRawParentName(file);
        else return file.getName();
    }

    public static String getRawParentName(File file) {
        while(file != null && !matchesAny(file.getName(), FOLDER_LIKE_RAW_DATA_TEMPLATE)) {
            file = file.getParentFile();
        }
        return (file != null && matchesAny(file.getName(), FOLDER_LIKE_RAW_DATA_TEMPLATE) ? file.getName() : "");
    }

    public static Boolean IsRawData(File file) {
        if(IS_FOLDER_LIKE) {
            if(file.isDirectory()) {
                return matchesAny(file.getName(), FOLDER_LIKE_RAW_DATA_TEMPLATE);
            } else if(file.isFile()) {
                // avoid obvious Windows Explorer files
                if(file.getName().equals("desktop.ini")) return false;
                // conditions: file name has to match the expected extension, and one parent must match the folder template
//                return endsWithAny(file.getName(), FOLDER_LIKE_RAW_DATA_EXTENSION) &&
//                        !getRawParentName(file).equals("");
                return !getRawParentName(file).equals(""); // allows all files within a raw data directory
            }
        } else if(file.isFile() && matchesAny(file.getName(), FILE_LIKE_RAW_DATA_TEMPLATE)) return false;
        return false;
    }

    public static String formatSize(Long _size) {
        if(_size <= 0) return "0 octets";
        int digitGroups = (int) (Math.log10(_size)/Math.log10(1024));
        return new DecimalFormat("#,##0.##").format(_size/Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static String getHostname() {
        String hostname = "Unknown";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (Throwable t) {
            logger.error("Hostname can not be resolved", t);
        }
        return hostname;
    }

//    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM);
//    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
    private static final SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat("yyyyMMdd-HHmmss");
    private static final SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
//    public  String formatDate(Long _date, boolean humanReadable) { return humanReadable ? dateFormat.format(_date) : simpleDateFormat.format(_date); }
    public static String simpleFormatDate(Long _date) { return simpleDateFormat1.format(_date); }
    public static String simpleFormatDate2(Long _date) { return simpleDateFormat2.format(_date); }

    public static String getUsername() {
        return System.getProperty("user.name");
    }

    public static String getAppTitle() {
        return appName + " " + appVersion + " (" + appDate + ")";
    }

    public static Date getCreationTimeAsDate(File file) {
        FileTime time = getCreationTime(file);
        if(time != null) return new Date(time.toMillis());
        return null;
    }

    public static FileTime getCreationTime(File file) {
        try {
            if(file != null && file.exists()) {
                DosFileAttributes attributes = Files.readAttributes(file.toPath(), DosFileAttributes.class);
                return attributes.creationTime();
            }
        } catch (IOException ioe) {
            logger.warn("File " + file.getAbsolutePath() + ": creation date could not be read, can't retrieve archived version", ioe);
        } catch (Throwable ignored) {}
        return null;
    }

    public static String getLicence() {
        return 	"Copyright 2021 CNRS\n" +
                "Authors: Alexandre BUREL\n" +
                "Corresponding author: Alexandre BUREL\n" +
                "Email: alexandre.burel@unistra.fr\n" +
                "Affiliation: Laboratoire de Spectrométrie de Masse BioOrganique, Université de Strasbourg, CNRS, IPHC, UMR7178, F-67000 Strasbourg, France\n" +
                "Laboratory contact: Christine CARAPITO\n" +
                "Email: ccarapito@unistra.fr\n\n" +

                "This software is a computer program whose purpose is to verify files before deletion.\n\n " +

                "This software is governed by the CeCILL license under French law and " +
                "abiding by the rules of distribution of free software. You can use, " +
                "modify and/ or redistribute the software under the terms of the CeCILL " +
                "license as circulated by CEA, CNRS and INRIA at the following URL " +
                "http://www.cecill.info\n\n " +

                "As a counterpart to the access to the source code and rights to copy, " +
                "modify and redistribute granted by the license, users are provided only " +
                "with a limited warranty and the software's author, the holder of the " +
                "economic rights, and the successive licensors have only limited " +
                "liability. \n\n" +

                "In this respect, the user's attention is drawn to the risks associated " +
                "with loading, using, modifying and/or developing or reproducing the " +
                "software by the user in light of its specific status of free software, " +
                "that may mean that it is complicated to manipulate, and that also " +
                "therefore means that it is reserved for developers and experienced " +
                "professionals having in-depth computer knowledge. Users are therefore " +
                "encouraged to load and test the software's suitability as regards their " +
                "requirements in conditions enabling the security of their systems and/or " +
                "data to be ensured and, more generally, to use and operate it in the " +
                "same conditions as regards security.\n\n" +

                "The fact that you are presently reading this means that you have had " +
                "knowledge of the CeCILL license and that you accept its terms.";
    }

}
