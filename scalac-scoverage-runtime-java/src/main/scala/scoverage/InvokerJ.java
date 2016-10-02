package scoverage;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class InvokerJ {

    private static String MeasurementsPrefix = "scoverage.measurements.";

    private static final ThreadLocal<HashMap<String,FileWriter>> threadFiles =
        new ThreadLocal<HashMap<String,FileWriter>>() {
            @Override protected HashMap<String,FileWriter> initialValue() {
                return new   HashMap<String,FileWriter>();
            }
        };

    private static ConcurrentHashMap<String, Boolean> ids = new ConcurrentHashMap<String, Boolean>();

    /**
     * We record that the given id has been invoked by appending its id to the coverage
     * data file.
     *
     * This will happen concurrently on as many threads as the application is using,
     * so we use one file per thread, named for the thread id.
     *
     * This method is not thread-safe if the threads are in different JVMs, because
     * the thread IDs may collide.
     * You may not use `scoverage` on multiple processes in parallel without risking
     * corruption of the measurement file.
     *
     * @param id the id of the statement that was invoked
     * @param dataDir the directory where the measurement data is held
     */
    public static void invokedJ(final Integer id, final String dataDir)throws IOException  {
        String idStr = Integer.toString(id);
        String key = new String(dataDir + idStr);

        if (!ids.containsKey(key)) {
            // Each thread writes to a separate measurement file, to reduce contention
            // and because file appends via FileWriter are not atomic on Windows.
            HashMap<String,FileWriter> files = threadFiles.get();
            if(!files.containsKey(dataDir))
                files.put(dataDir, new FileWriter(measurementFile(dataDir), true));
            FileWriter writer = files.get(dataDir);
            writer.append(idStr + '\n').flush();

            ids.put(key, Boolean.TRUE);
        }
    }

    private static File measurementFile(File dataDir){
        return measurementFile(dataDir.getAbsolutePath());
    }

    private static File measurementFile(String dataDir) {
        StringBuilder sb = new StringBuilder(MeasurementsPrefix);
        String threadId = Long.toString(Thread.currentThread().getId());
        return new File(dataDir, sb.append(threadId).toString());
    }
}
