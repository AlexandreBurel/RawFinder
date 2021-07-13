package fr.lsmbo.rawfinder;

import java.io.File;
import java.util.Date;

public class RawData {

    private File file;
    private String name;
    private Double size;
    private Date date;
    private Integer nbFilesArchived;
    private Integer nbFilesTotal;
    private Status status;

    public RawData(File _file) {
        file = _file;
        name = file.getName();
        size = 0d;
        date = Global.getCreationTimeAsDate(file);
        nbFilesArchived = 0;
        nbFilesTotal = 0;
        status = null;
    }

    public void addFile(File _file, Boolean isArchived) {
        size += _file.length();
        if(isArchived) nbFilesArchived += 1;
        nbFilesTotal += 1;
        if(date == null || Global.getCreationTimeAsDate(_file).before(date)) {
            date = Global.getCreationTimeAsDate(_file);
        }
    }

    public String getName() {
        return name;
    }
    public Double getSize() {
        return size;
    }
    public Date getDate() {
        return date;
    }
    public String getStatus() {
        if(status == null) {
            if (nbFilesArchived == 0) { status = Status.NOT_ARCHIVED;
            } else if (nbFilesArchived == nbFilesTotal) { status = Status.FULLY_ARCHIVED;
            } else { status = Status.PARTIALLY_ARCHIVED;
            }
        }
        return status.toString();
    }
}
