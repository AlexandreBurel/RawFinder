package fr.lsmbo.rawfinder;

import com.sun.org.apache.xpath.internal.operations.Bool;

public class Settings {

    private String rawDataDirectory;
    private String archiveDirectory;
    private String defaultReportDirectory;
    private Boolean isFolderLike;
    private String folderLikeRawDataTemplate;
    private String folderLikeRawDataExtension;
    private String fileLikeRawDataTemplate;


    public Settings(String rawDataDirectory, String archiveDirectory, String defaultReportDirectory, Boolean isFolderLike, String folderLikeRawDataTemplate, String folderLikeRawDataExtension, String fileLikeRawDataTemplate) {
        this.rawDataDirectory = rawDataDirectory;
        this.archiveDirectory = archiveDirectory;
        this.defaultReportDirectory = defaultReportDirectory;
        this.isFolderLike = isFolderLike;
        this.folderLikeRawDataTemplate = folderLikeRawDataTemplate;
        this.folderLikeRawDataExtension = folderLikeRawDataExtension;
        this.fileLikeRawDataTemplate = fileLikeRawDataTemplate;
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
