package fr.lsmbo.rawfinder;

import java.io.File;
import java.util.List;

public class Settings {

    private String rawDataDirectory;
    private String archiveDirectory;
    private String defaultReportDirectory;
    private Boolean isFolderLike;
    private String folderLikeRawDataTemplate;
    private String folderLikeRawDataExtension;
    private String fileLikeRawDataTemplate;

    public Settings(File rawDataDirectory, File archiveDirectory, Boolean isFolderLike, List<String> folderLikeRawDataTemplate, List<String> fileLikeRawDataTemplate, File defaultReportDirectory) {
        this.rawDataDirectory = rawDataDirectory == null ? "" : rawDataDirectory.getAbsolutePath();
        this.archiveDirectory = archiveDirectory == null ? "" : archiveDirectory.getAbsolutePath();
        this.isFolderLike = isFolderLike;
        this.folderLikeRawDataTemplate = String.join(" ", folderLikeRawDataTemplate);
        this.fileLikeRawDataTemplate = String.join(" ", fileLikeRawDataTemplate);
        this.defaultReportDirectory = defaultReportDirectory == null ? "" : defaultReportDirectory.getAbsolutePath();
    }

    public Settings() {}

    public String getRawDataDirectory() {
        return rawDataDirectory;
    }

    public void setRawDataDirectory(String rawDataDirectory) {
        this.rawDataDirectory = rawDataDirectory;
    }

    public String getArchiveDirectory() {
        return archiveDirectory;
    }

    public void setArchiveDirectory(String archiveDirectory) {
        this.archiveDirectory = archiveDirectory;
    }

    public String getDefaultReportDirectory() {
        return defaultReportDirectory;
    }

    public void setDefaultReportDirectory(String defaultReportDirectory) {
        this.defaultReportDirectory = defaultReportDirectory;
    }

    public Boolean getFolderLike() {
        return isFolderLike;
    }

    public void setFolderLike(Boolean folderLike) {
        isFolderLike = folderLike;
    }

    public String getFolderLikeRawDataTemplate() {
        return folderLikeRawDataTemplate;
    }

    public void setFolderLikeRawDataTemplate(String folderLikeRawDataTemplate) {
        this.folderLikeRawDataTemplate = folderLikeRawDataTemplate;
    }

    public String getFolderLikeRawDataExtension() {
        return folderLikeRawDataExtension;
    }

    public void setFolderLikeRawDataExtension(String folderLikeRawDataExtension) {
        this.folderLikeRawDataExtension = folderLikeRawDataExtension;
    }

    public String getFileLikeRawDataTemplate() {
        return fileLikeRawDataTemplate;
    }

    public void setFileLikeRawDataTemplate(String fileLikeRawDataTemplate) {
        this.fileLikeRawDataTemplate = fileLikeRawDataTemplate;
    }

    public String toString() {
        return  "\nrawDataDirectory: " + rawDataDirectory +
                "\narchiveDirectory: " + archiveDirectory +
                "\ndefaultReportDirectory: " + defaultReportDirectory +
                "\nisFolderLike: " + isFolderLike +
                "\nfolderLikeRawDataTemplate: " + folderLikeRawDataTemplate +
                "\nfolderLikeRawDataExtension: " + folderLikeRawDataExtension +
                "\nfileLikeRawDataTemplate: " + fileLikeRawDataTemplate;
    }
}
