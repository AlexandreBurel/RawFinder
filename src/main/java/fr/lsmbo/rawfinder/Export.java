package fr.lsmbo.rawfinder;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.stream.Collectors;

public class Export {

    protected final Logger logger = LoggerFactory.getLogger(Export.class);
    private final Global global;
    private final HashMap<File, File> data;
    private static final HashMap<String, Status> statusPerRawFile = new HashMap<>();

    private enum Status {
        FULLY_ARCHIVED ("Fully archived"),
        PARTIALLY_ARCHIVED ("Partially archived"),
        NOT_ARCHIVED ("Not archived");
        private final String name;
        Status(String status) { name = status; }
        public String toString() { return this.name; }
    }

    public Export(Global _global, HashMap<File, File> _data) {
        global = _global;
        data = _data;
    }

    public void start() throws Throwable {
        // generate excel output
        String host = global.getHostname();
        String time = global.simpleFormatDate(new Date().getTime());
        // generate the output file name
        File excelFile = new File(global.REPORTS_DIRECTORY, "RawFinder-"+host+"-"+time+".xlsx");
        start(excelFile);
    }

    public void start(File excelFile) throws Throwable {
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
        int rowForFullyArchived = rowNum;
        addRow(sheet, rowNum++, new String[] { "Raw data fully archived" });
        addRow(sheet, rowNum++, new String[] { "Raw data of partially archived" });
        addRow(sheet, rowNum++, new String[] { "Raw data not archived" });
        // add an empty line
        addRow(sheet, rowNum++, new String[] {});
        // write the headers
        int headerLine = rowNum;
        String[] headers = new String[] { "Raw file name", "Local file path", "Local file size", "Local file size (bytes)",
                "Local file creation date", "Local file last modification date", "Archived file path", "Archived file size (bytes)",
                "Archived file creation date", "Size match", "Raw file is completely archived", "Raw file status" };
        addRow(sheet, rowNum++, headers, headerStyle);

        // count the number of files per folder-like raw data, so we can tell the status of a raw data
        HashMap<String, Integer> nbFilesPerRawFolder = new HashMap<>();
        // prepare a hashmap to store how many files per raw data are missing or with a wrong size (only useful when folder-like)
        HashMap<String, Integer> missingOrIncorrectArchives = new HashMap<>();

        // write the data content
        String lastRawFileName = "";
        for(File file : data.keySet().stream().sorted().collect(Collectors.toList())) {
            Row row = sheet.createRow(rowNum++);
            int col = 0;
            String currentRawFileName = global.getRawFileName(file);
            if(!missingOrIncorrectArchives.containsKey(currentRawFileName)) missingOrIncorrectArchives.put(currentRawFileName, 0);
            nbFilesPerRawFolder.put(currentRawFileName, nbFilesPerRawFolder.containsKey(currentRawFileName) ? nbFilesPerRawFolder.get(currentRawFileName) + 1 : 1);
            CellStyle style = (global.IS_FOLDER_LIKE && !currentRawFileName.equals(lastRawFileName) ? topStyle : defaultStyle);
            CellStyle dateStyle = (global.IS_FOLDER_LIKE && !currentRawFileName.equals(lastRawFileName) ? topDateStyle : defaultDateStyle);
            addCell(row, col++, currentRawFileName, style); // A
            addCell(row, col++, file.getAbsolutePath(), style); // B
            addCell(row, col++, global.formatSize(file.length()), style); // C
            addCell(row, col++, file.length(), style); // D
            addCell(row, col++, global.getCreationTimeAsDate(file), dateStyle); // E
            addCell(row, col++, new Date(file.lastModified()), dateStyle); // F
            File archive = data.get(file);
            if(archive != null) {
                addCell(row, col++, archive.getAbsolutePath(), style); // G
                addCell(row, col++, archive.length(), style); // H
                addCell(row, col++, global.getCreationTimeAsDate(archive), dateStyle); // I
                if(file.length() == archive.length()) {
                    addCell(row, col, "TRUE", style); //J (using text to avoid a large number of formulas)
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
            if(rowNum % 100 == 0) logger.info(rowNum + " lines written...");
        }

        // second loop to fill column K without using a formula
        rowNum = headerLine + 1;
        lastRawFileName = "";
        logger.info("Add final formulas");
        for(File file : data.keySet().stream().sorted().collect(Collectors.toList())) {
            Row row = sheet.getRow(rowNum++);
            String currentRawFileName = global.getRawFileName(file);
            CellStyle style = (global.IS_FOLDER_LIKE && !currentRawFileName.equals(lastRawFileName) ? topStyle : defaultStyle);
            addCell(row, 10, (missingOrIncorrectArchives.get(currentRawFileName) == 0 ? "TRUE" : "FALSE"), style); //K
            lastRawFileName = currentRawFileName;

            Status status = Status.NOT_ARCHIVED;
            if(missingOrIncorrectArchives.get(currentRawFileName) == 0) {
                status = Status.FULLY_ARCHIVED;
            } else if(!missingOrIncorrectArchives.get(currentRawFileName).equals(nbFilesPerRawFolder.get(currentRawFileName))) {
                status = Status.PARTIALLY_ARCHIVED;
            }
            statusPerRawFile.put(currentRawFileName, status);
            addCell(row, 11, status.toString(), style); //L
        }

        // final loop to display the summary of how many raw data is fully/partially/not archived
        HashMap<String, Integer> countPerStatus = new HashMap<>();
        statusPerRawFile.keySet().forEach( name -> {
            String status = statusPerRawFile.get(name).toString();
            countPerStatus.put(status, countPerStatus.containsKey(status) ? countPerStatus.get(status) + 1 : 1);
        });
        addCell(sheet.getRow(rowForFullyArchived), 1, ""+countPerStatus.get(Status.FULLY_ARCHIVED.toString()), defaultStyle);
        addCell(sheet.getRow(rowForFullyArchived + 1), 1, ""+countPerStatus.get(Status.PARTIALLY_ARCHIVED.toString()), defaultStyle);
        addCell(sheet.getRow(rowForFullyArchived + 2), 1, ""+countPerStatus.get(Status.NOT_ARCHIVED.toString()), defaultStyle);
        logger.info("\n\nRawFinder final summary:\n" +
                "- Number of fully archived raw data: " + countPerStatus.get(Status.FULLY_ARCHIVED.toString()) + "\n" +
                "- Number of partially archived raw data: " + countPerStatus.get(Status.PARTIALLY_ARCHIVED.toString()) + "\n" +
                "- Number of raw data not archived at all: " + countPerStatus.get(Status.NOT_ARCHIVED.toString()) + "\n");

        // add autofilters
        sheet.setAutoFilter(CellRangeAddress.valueOf("A"+(headerLine+1)+":L"+(rowNum-1)));
        // resize all columns to fit the content size
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // add a conditional formatting for the last columns
        SheetConditionalFormatting sheetCF = sheet.getSheetConditionalFormatting();
        ConditionalFormattingRule ruleFalse = getRule(sheetCF, "FALSE", IndexedColors.RED.getIndex());
        ConditionalFormattingRule[] cfRules = { ruleFalse };
        CellRangeAddress[] regions = { CellRangeAddress.valueOf("J"+(headerLine+2)+":K"+(rowNum)) };
        sheetCF.addConditionalFormatting(regions, cfRules);
        // Status column
        sheetCF = sheet.getSheetConditionalFormatting();
        ConditionalFormattingRule ruleFull = getRule(sheetCF, Status.FULLY_ARCHIVED.toString(), IndexedColors.GREEN.getIndex());
        ConditionalFormattingRule rulePart = getRule(sheetCF, Status.PARTIALLY_ARCHIVED.toString(), IndexedColors.LIGHT_ORANGE.getIndex());
        ConditionalFormattingRule ruleNot = getRule(sheetCF, Status.NOT_ARCHIVED.toString(), IndexedColors.RED.getIndex());
        ConditionalFormattingRule[] cfRules2 = { ruleFull, rulePart, ruleNot };
        CellRangeAddress[] regions2 = { CellRangeAddress.valueOf("L"+(headerLine+2)+":L"+(rowNum)) };
        sheetCF.addConditionalFormatting(regions2, cfRules2);

        // write the output to a file
//        File excelFile = new File(global.REPORTS_DIRECTORY, "RawFinder-"+host+"-"+time+".xlsx");
        FileOutputStream fileOut = new FileOutputStream(excelFile);
        workbook.write(fileOut);
        workbook.close();
        fileOut.close();

        logger.info("Excel file "+excelFile.getName()+" has been correctly written");
    }

    private ConditionalFormattingRule getRule(SheetConditionalFormatting sheetCF, String value, short bgColor) {
        ConditionalFormattingRule rule = sheetCF.createConditionalFormattingRule(ComparisonOperator.EQUAL, "\"" + value + "\"");
        FontFormatting fontFmt = rule.createFontFormatting();
        fontFmt.setFontStyle(true, false);
        fontFmt.setFontColorIndex(IndexedColors.WHITE.getIndex());
        PatternFormatting patternFmt = rule.createPatternFormatting();
        patternFmt.setFillBackgroundColor(bgColor);
        return rule;
    }

    private void addCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        if(style != null) cell.setCellStyle(style);
    }
    private void addCell(Row row, int column, Long value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        if(style != null) cell.setCellStyle(style);
    }
    private void addCell(Row row, int column, Date value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        if(style != null) cell.setCellStyle(style);
    }

    private void addRow(Sheet sheet, int rowNumber, String[] items) {
        addRow(sheet, rowNumber, items, null);
    }
    private void addRow(Sheet sheet, int rowNumber, String[] items, CellStyle style) {
        Row row = sheet.createRow(rowNumber);
        for(int i = 0; i < items.length; i++) {
            addCell(row, i, items[i], style);
        }
    }
}
