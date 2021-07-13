package fr.lsmbo.rawfinder;

import javafx.concurrent.Task;

import java.io.File;

public class DataParserThread extends Task<DataParser> {

    private File parentDirectory;

    public void setParentDirectory(File _parentDirectory) {
        parentDirectory = _parentDirectory;
    }

    public DataParser call() throws Exception {
        DataParser parser = new DataParser(parentDirectory);
        parser.start();
        return parser;
    }
}
