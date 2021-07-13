package fr.lsmbo.rawfinder;

import javafx.concurrent.Task;

import java.io.File;

public class ExportThread extends Task<Export> {

    private DataParser parser;
    private File outputFile;

    public void initialize(DataParser _parser, File _outputFile) {
        parser = _parser;
        outputFile = _outputFile;
    }

    public Export call() throws Exception {
        Export export = new Export(parser);
        try {
            export.start(outputFile);
        } catch (Throwable t) {
            throw  new Exception(t);
        }
        return export;
    }
}
