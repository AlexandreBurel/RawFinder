package fr.lsmbo.rawfinder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public class Main {

    protected static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            logger.info("Starting RawFinder");
            // load configuration
            Global.initialize();
            if(args.length > 0 && args[0].toLowerCase(Locale.ROOT).equals("gui")) {
                GuiLauncher.run();
            } else {
                // read local directories and fill data
                DataParser parser = new DataParser(Global.RAW_DATA_DIRECTORY);
                parser.start();

                // generate excel output
                Export export = new Export(parser);
                export.start();
            }

        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            t.printStackTrace();
        }
        logger.info("End of RawFinder");
    }

}
