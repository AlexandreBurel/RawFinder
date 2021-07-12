package fr.lsmbo.rawfinder;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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
            String host = global.getHostname();
            String time = global.simpleFormatDate(new Date().getTime());
            // creation of the excel object
            logger.info("Writing Excel output file");
            Workbook workbook = new XSSFWorkbook();
            CreationHelper createHelper = workbook.getCreationHelper();
            Sheet sheet = workbook.createSheet(host+" "+time);
            int rowNum = 0;

            // prepare styles
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setBorderTop(BorderStyle.MEDIUM);
            headerStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
            headerStyle.setBorderBottom(BorderStyle.MEDIUM);
            headerStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
            headerStyle.setBorderLeft(BorderStyle.MEDIUM);
            headerStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
            headerStyle.setBorderRight(BorderStyle.MEDIUM);
            headerStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            CellStyle topStyle = workbook.createCellStyle();
            topStyle.setBorderTop(BorderStyle.MEDIUM);
            topStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
            CellStyle topDateStyle = workbook.createCellStyle();
            topDateStyle.cloneStyleFrom(topStyle);
            topDateStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd mmmm yyyy hh:mm:ss"));
            CellStyle defaultDateStyle = workbook.createCellStyle();
            defaultDateStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd mmmm yyyy hh:mm:ss"));
            CellStyle defaultStyle = workbook.createCellStyle();

            // Describe the environment first
            addRow(sheet, rowNum++, new String[] { "Software", global.getAppTitle() });
            Row row1 = sheet.createRow(rowNum++);
            addCell(row1, 0, "Report date", defaultStyle);
            addCell(row1, 1, new Date(), defaultDateStyle);
            addRow(sheet, rowNum++, new String[] { "Host name", host });
            addRow(sheet, rowNum++, new String[] { "User name", global.getUsername() });
            addRow(sheet, rowNum++, new String[] { "Archive directory", global.RAW_DATA_ARCHIVES.getAbsolutePath() });
            addRow(sheet, rowNum++, new String[] { "RAW data directory", global.RAW_DATA_DIRECTORY.getAbsolutePath() });
            if(global.IS_FOLDER_LIKE) {
                addRow(sheet, rowNum++, new String[]{"RAW data type", "Directory"});
                addRow(sheet, rowNum++, new String[]{"RAW data directory template", String.join(", ", global.FOLDER_LIKE_RAW_DATA_TEMPLATE)});
                addRow(sheet, rowNum++, new String[]{"RAW data file extension", String.join(", ", global.FOLDER_LIKE_RAW_DATA_EXTENSION)});
            } else {
                addRow(sheet, rowNum++, new String[]{"RAW data type", "File"});
                addRow(sheet, rowNum++, new String[]{"RAW data file template", String.join(", ", global.FILE_LIKE_RAW_DATA_TEMPLATE)});
            }
            // add an empty line
            addRow(sheet, rowNum++, new String[] {});
            // write the headers
            int headerLine = rowNum;
            String[] headers = new String[] { "Raw file name", "Local file path", "Local file size", "Local file size (bytes)",
                    "Local file creation date", "Local file last modification date", "Archived file path", "Archived file size (bytes)",
                    "Archived file creation date", "Size match", "Raw file is completely archived" };
            addRow(sheet, rowNum++, headers, headerStyle);

            // prepare a hashmap to store how many files per raw data are missing or with a wrong size (only useful when folder-like)
            HashMap<String, Integer> missingOrIncorrectArchives = new HashMap<>();

            // write the data content
            String lastRawFileName = "";
            for(File file : data.keySet().stream().sorted().collect(Collectors.toList())) {
                Row row = sheet.createRow(rowNum++);
                int col = 0;
                String currentRawFileName = global.getRawFileName(file);
                if(!missingOrIncorrectArchives.containsKey(currentRawFileName)) missingOrIncorrectArchives.put(currentRawFileName, 0);
                CellStyle style = (global.IS_FOLDER_LIKE && !currentRawFileName.equals(lastRawFileName) ? topStyle : defaultStyle);
                CellStyle dateStyle = (global.IS_FOLDER_LIKE && !currentRawFileName.equals(lastRawFileName) ? topDateStyle : defaultDateStyle);
                addCell(row, col++, currentRawFileName, style); // A
                addCell(row, col++, file.getAbsolutePath(), style); // B
                addCell(row, col++, global.formatSize(file.length()), style); // C
                addCell(row, col++, file.length(), style); // D
                addCell(row, col++, getCreationTimeAsDate(file), dateStyle); // E
                addCell(row, col++, new Date(file.lastModified()), dateStyle); // F
                File archive = data.get(file);
                if(archive != null) {
                    addCell(row, col++, archive.getAbsolutePath(), style); // G
                    addCell(row, col++, archive.length(), style); // H
                    addCell(row, col++, getCreationTimeAsDate(archive), dateStyle); // I
                    if(file.length() == archive.length()) {
                        addCell(row, col++, "TRUE", style); //J (using text to avoid a large number of formulas)
                    } else {
                        missingOrIncorrectArchives.replace(currentRawFileName, missingOrIncorrectArchives.get(currentRawFileName) + 1);
                        addCell(row, col, "FALSE", style); //J
                    }
                } else {
                    addCell(row, col++, "", style); //G
                    addCell(row, col++, "", style); //H
                    addCell(row, col++, "", style); //I
                    addCell(row, col, "FALSE", style); //J
                    missingOrIncorrectArchives.replace(currentRawFileName, missingOrIncorrectArchives.get(currentRawFileName) + 1);
                }
                // add a formula to check that both sizes are equal
//                addFormula(row, col++, "AND(NOT(ISBLANK(H"+rowNum+")),D"+rowNum+"=H"+rowNum+")", style); // J
                // add a formula to check that all files in a raw file have the same size: NB.SI(A:A;A9)=NB.SI.ENS(A:A;A9;J:J;VRAI)
//                addFormula(row, col, "COUNTIF(A:A,A"+rowNum+")=COUNTIFS(A:A,A"+rowNum+",J:J,TRUE)", style); // K
                lastRawFileName = currentRawFileName;
            }

            // second loop to fill column K without using a formula
            rowNum = headerLine + 1;
            lastRawFileName = "";
            for(File file : data.keySet().stream().sorted().collect(Collectors.toList())) {
                Row row = sheet.getRow(rowNum++);
                String currentRawFileName = global.getRawFileName(file);
                CellStyle style = (global.IS_FOLDER_LIKE && !currentRawFileName.equals(lastRawFileName) ? topStyle : defaultStyle);
                addCell(row, 10, (missingOrIncorrectArchives.get(currentRawFileName) == 0 ? "TRUE" : "FALSE"), style); //K
                lastRawFileName = currentRawFileName;
            }

            // add autofilters
            sheet.setAutoFilter(CellRangeAddress.valueOf("A"+(headerLine+1)+":K"+(rowNum-1)));
            // resize all columns to fit the content size
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // add a conditional formatting for the last column
            SheetConditionalFormatting sheetCF = sheet.getSheetConditionalFormatting();
            ConditionalFormattingRule rule = sheetCF.createConditionalFormattingRule(ComparisonOperator.EQUAL, "\"FALSE\"");
            FontFormatting fontFmt = rule.createFontFormatting();
            fontFmt.setFontStyle(true, false);
            fontFmt.setFontColorIndex(IndexedColors.WHITE.getIndex());
            PatternFormatting patternFmt = rule.createPatternFormatting();
            patternFmt.setFillBackgroundColor(IndexedColors.RED.getIndex());
            ConditionalFormattingRule[] cfRules = { rule };
            CellRangeAddress[] regions = { CellRangeAddress.valueOf("J"+(headerLine+2)+":K"+(rowNum)) };
            sheetCF.addConditionalFormatting(regions, cfRules);

            // write the output to a file
            File excelFile = new File(global.REPORTS_DIRECTORY, "RawFinder-"+host+"-"+time+".xlsx");
            FileOutputStream fileOut = new FileOutputStream(excelFile);
            workbook.write(fileOut);
            workbook.close();
            fileOut.close();

            logger.info("Excel file "+excelFile.getName()+" has been correctly written");

        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            t.printStackTrace();
        }
        logger.info("End of RawFinder");
    }
    private static void addFormula(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellFormula(value);
        if(style != null) cell.setCellStyle(style);
    }
    private static void addCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        if(style != null) cell.setCellStyle(style);
    }
    private static void addCell(Row row, int column, Long value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        if(style != null) cell.setCellStyle(style);
    }
    private static void addCell(Row row, int column, Date value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        if(style != null) cell.setCellStyle(style);
    }

    private static void addRow(Sheet sheet, int rowNumber, String[] items) {
        addRow(sheet, rowNumber, items, null);
    }
    private static void addRow(Sheet sheet, int rowNumber, String[] items, CellStyle style) {
        Row row = sheet.createRow(rowNumber);
        for(int i = 0; i < items.length; i++) {
            addCell(row, i, items[i], style);
        }
    }

    private static void readDirectory(File directory) {
        // read raw-like directories first (recursive call)
        Arrays.stream(Objects.requireNonNull(directory.listFiles())).filter(File::isDirectory).forEach(Main::readDirectory);

        // read raw-like files then (search archive files)
        Arrays.stream(Objects.requireNonNull(directory.listFiles())).filter(file -> file.isFile() && global.IsRawData(file)).forEach(item -> {
            // TODO check if the item has a parent matching a raw file (if folder like)
            File archive = findArchive(item);
            data.put(item, archive);
        });
    }

    private static Date getCreationTimeAsDate(File file) {
        FileTime time = getCreationTime(file);
        if(time != null) return new Date(time.toMillis());
        return null;
    }

    private static FileTime getCreationTime(File file) {
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

    private static String getArchivePath(File file, boolean currentMonth) throws Throwable {
        DosFileAttributes attributes = Files.readAttributes(file.toPath(), DosFileAttributes.class);
        LocalDateTime localDateTime = LocalDateTime.ofInstant(attributes.creationTime().toInstant(), ZoneId.systemDefault());
        if(currentMonth) {
            return global.RAW_DATA_ARCHIVES.getAbsolutePath() + "/" + localDateTime.getYear() + "/" + global.MONTH_NAMES[localDateTime.getMonthValue() - 1];
        } else {
            if(localDateTime.getMonthValue() < global.MONTH_NAMES.length) // January 2021 -> February 2021
                return global.RAW_DATA_ARCHIVES.getAbsolutePath() + "/" + localDateTime.getYear() + "/" + global.MONTH_NAMES[localDateTime.getMonthValue()];
            else // december 2020 -> january 2021
                return global.RAW_DATA_ARCHIVES.getAbsolutePath() + "/" + (localDateTime.getYear() + 1) + "/" + global.MONTH_NAMES[0];
        }
    }

    private static File findArchive1(File file) {
        File archive = null;
        try {
            String path = getArchivePath(file, true);
            File fromArchive = new File(path, global.RAW_DATA_DIRECTORY.toURI().relativize(file.toURI()).getPath());
            if(fromArchive.exists() && file.length() == fromArchive.length()) {
                archive = fromArchive;
            } else {
                // TODO also check at the modification date (before or after ?)
                // search in the next month if the file sizes do not match (or does not exist at all)
                path = getArchivePath(file, false);
                fromArchive = new File(path, global.RAW_DATA_DIRECTORY.toURI().relativize(file.toURI()).getPath());
                if(fromArchive.exists()) archive = fromArchive;
            }
        } catch (IOException ioe) {
            logger.warn("File "+file.getAbsolutePath()+": creation date could not be read, can't retrieve archived version", ioe);
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            t.printStackTrace();
        }
        return archive;
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
