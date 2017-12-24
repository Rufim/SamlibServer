package ru.samlib.server.util;

import ru.samlib.server.domain.dao.LogEventDao;
import ru.samlib.server.domain.entity.LogEvent;
import ru.samlib.server.domain.entity.ParsingInfo;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by Dmitry on 22.08.13.
 */

public final class Log {

    private static final String TAG = Log.class.getSimpleName();

    public enum LOG_LEVEL {
        /**
         * Priority constant for the println method; use Log.i.
         */

        INFO(5, new Color(83, 175, 255)),

        /**
         * Priority constant for the println method; use Log.d.
         */

        DEBUG(4, Color.YELLOW),

        /**
         * Priority constant for the println method; use Log.w.
         */

        WARN(3, Color.ORANGE),

        /**
         * Priority constant for the println method; use Log.e.
         */

        ERROR(2, Color.RED),

        /**
         * Priority constant for the println method. use Log.f
         */
        FATAL(1, Color.BLACK);

        public final int level_int;
        public final Color color;

        private LOG_LEVEL(int level_int, Color color) {
            this.level_int = level_int;
            this.color = color;
        }

    }

    private static boolean printToConsole = true;
    private static boolean printToBase = true;
    private static boolean printToFile = false;
    private static boolean printToDocument = false;

    private static boolean printToBaseBeforeStop = true;
    private static boolean printToConsoleBeforeStop = true;
    private static boolean printToFileBeforeStop = false;
    private static boolean printToDocumentBeforeStop = false;

    private static LOG_LEVEL current_level = LOG_LEVEL.INFO;
    private static File logFile;
    private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    static {
        if (printToFile) {
            try {
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
                Calendar cal = Calendar.getInstance();
                logFile = new File("Log - " + dateFormat.format(cal.getTime()) + ".txt");
                if (!logFile.createNewFile()) {
                    Log.e(TAG, "Cannot create log file!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static JTextPane outputField;


    /**
     * Send a DEBUG log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int d(Object tag, String msg) {
        return log_writer(LOG_LEVEL.DEBUG, tag, msg);
    }

    /**
     * Send a DEBUG log message and log the exception.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static int d(Object tag, String msg, Throwable tr) {
        return log_writer(LOG_LEVEL.DEBUG, tag, msg + '\n' + getStackTraceString(tr));
    }

    /**
     * Send an INFO log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int i(Object tag, String msg) {
        return log_writer(LOG_LEVEL.INFO, tag, msg);
    }

    /**
     * Send a INFO log message and log the exception.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static int i(Object tag, String msg, Throwable tr) {
        return log_writer(LOG_LEVEL.INFO, tag, msg + '\n' + getStackTraceString(tr));
    }

    /**
     * Send a WARN log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int w(Object tag, String msg) {
        return log_writer(LOG_LEVEL.WARN, tag, msg);
    }

    /**
     * Send a WARN log message and log the exception.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static int w(Object tag, String msg, Throwable tr) {
        return log_writer(LOG_LEVEL.WARN, tag, msg + '\n' + getStackTraceString(tr));
    }

    /**
     * Send a WARN log message and log the exception.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param tr  An exception to log
     */
    public static int w(Object tag, Throwable tr) {
        return log_writer(LOG_LEVEL.WARN, tag, getStackTraceString(tr));
    }

    /**
     * Send an ERROR log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int e(Object tag, String msg) {
        return log_writer(LOG_LEVEL.ERROR, tag, msg);
    }

    /**
     * Send a ERROR log message and log the exception.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static int e(Object tag, String msg, Throwable tr) {
        return log_writer(LOG_LEVEL.ERROR, tag, msg + '\n' + getStackTraceString(tr));
    }

    /**
     * Send an FATAL log message.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    public static int f(Object tag, String msg) {
        return log_writer(LOG_LEVEL.FATAL, tag, msg);
    }

    /**
     * Send a FATAL log message and log the exception.
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     * @param tr  An exception to log
     */
    public static int f(Object tag, String msg, Throwable tr) {
        return log_writer(LOG_LEVEL.FATAL, tag, msg + '\n' + getStackTraceString(tr));
    }

    /**
     * Handy function to get a loggable stack trace from a Throwable
     *
     * @param tr An exception to log
     */
    public static String getStackTraceString(Throwable tr) {
        if (tr == null) {
            return "";
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        tr.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }

    /**
     * Handy function to get a loggable stack trace from a Throwable
     *
     * @param stackTrace An trace to log
     */
    public static String getStackTraceString(StackTraceElement[] stackTrace) {
        if (stackTrace == null) {
            return "";
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        for (StackTraceElement ste : stackTrace) {
            pw.println(ste);
        }
        pw.close();
        return sw.toString();
    }

    /**
     * Low-level logging call.
     *
     * @param level The level of this log message
     * @param tag   Used to identify the source of a log message.  It usually identifies
     *              the class or activity where the log call occurs.
     * @param msg   The message you would like logged.
     * @return The number of bytes written.
     */
    public static int println(LOG_LEVEL level, Object tag, String msg) {
        return log_writer(level, tag, msg);
    }

    public static void println(Object object) {
        String record = object.toString();
        if (printToConsole) {
            System.out.println(record);
        }
        if (printToFile && logFile != null && logFile.canWrite()) {
            try {
                FileWriter writer = new FileWriter(logFile, true);
                PrintWriter printWriter = new PrintWriter(writer);
                printWriter.println(record);
                printWriter.close();
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
                System.out.println(getStackTraceString(ex));
            }
        }
    }

    public static void println(Object object, Color color) {
        println(object);
        if (printToDocument && outputField != null) {
            StyledDocument document = (StyledDocument) outputField.getDocument();
            AttributeSet attr = null;
            try {
                StyleContext cont = StyleContext.getDefaultStyleContext();
                attr = cont.addAttribute(cont.getEmptySet(), StyleConstants.Foreground, color);
                // add some data to the document
                document.insertString(document.getLength(), object.toString() + "\n", attr);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
            outputField.setCaretPosition(document.getLength());
            outputField.invalidate();
        }
    }

    public static String writeln() {
        BufferedReader rdr = new BufferedReader(new InputStreamReader(System.in));
        try {
            return rdr.readLine();
        } catch (IOException ex) {
            w(Log.class.getSimpleName(), ex);
        }
        return "Error while getting text from console";
    }

    /**
     * @hide
     */
    public static int log_writer(LOG_LEVEL level, Object tag, String msg) {
        if (current_level.level_int >= level.level_int) {
            Calendar cal = Calendar.getInstance();
            String record;
            if (tag instanceof Class) {
                record = dateFormat.format(cal.getTime()) + " " + tag + "  " + level.name() + "/" + ((Class) tag).getSimpleName() + ": " + msg;
            } else {
                record = dateFormat.format(cal.getTime()) + "  " + level.name() + "/" + tag + ": " + msg;
            }
            println(record, level.color);
        }
        return 0;
    }

    public static File getLogFile() {
        return logFile;
    }

    public static void printToConsole(boolean printToConsole) {
        Log.printToConsole = printToConsole;
    }

    public static void setLogFile(File log) {
        logFile = log;
    }

    public static void setTextPane(JTextPane textPane) {
        Log.outputField = textPane;
    }

    public static void setLogLevel(LOG_LEVEL current_level) {
        Log.current_level = current_level;
    }

    public static LOG_LEVEL getLogLevel() {
        return Log.current_level;
    }

    public static void saveLogEvent(Log.LOG_LEVEL logLevel, Exception ex, String corruptedData, LogEventDao dao) {
        saveLogEvent(logLevel, ex, corruptedData, dao, null);
    }

    public static void saveLogEvent(Log.LOG_LEVEL logLevel, Exception ex, String corruptedData, LogEventDao dao, ParsingInfo info) {
        if (current_level.level_int >= logLevel.level_int) {
            String tag = "MESSAGE";
            if (ex != null && ex.getStackTrace().length > 0) {
                try {
                    StackTraceElement traceElement = ex.getStackTrace()[0];
                    tag = Class.forName(traceElement.getClassName()).getSimpleName() + "." + traceElement.getMethodName() + "[" + traceElement.getLineNumber() + "]" + ex != null ? '\n' + getStackTraceString(ex) : "";
                } catch (ClassNotFoundException ignored) {
                }
            }
            Log.println(logLevel, tag, ex != null ? (ex.getMessage() + " corruptedData: " + corruptedData) : corruptedData);
            LogEvent logEvent = new LogEvent();
            logEvent.setMessage(ex != null ? ex.getMessage() : "");
            logEvent.setCorruptedData(corruptedData);
            logEvent.setLogLevel(logLevel);
            if (ex != null) logEvent.setTrace(Log.getStackTraceString(ex));
            if (info != null) {
                logEvent.setParsingInfo(info);
            }
            try {
                dao.save(logEvent);
            } catch (Throwable ignore) {
            }
        }
    }

    public static void stopLogging() {
        printToConsoleBeforeStop = printToConsole;
        printToFileBeforeStop = printToFile;
        printToDocumentBeforeStop = printToDocument;
        printToBaseBeforeStop = printToBase;

        printToBase = false;
        printToConsole = false;
        printToDocument = false;
        printToFile = false;
    }

    public static void startLogging() {
        printToConsole = printToConsoleBeforeStop;
        printToDocument = printToFileBeforeStop;
        printToFile = printToDocumentBeforeStop;
        printToBase = printToBaseBeforeStop;
    }
}
