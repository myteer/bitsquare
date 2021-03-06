/*
 * This file is part of Bitsquare.
 *
 * Bitsquare is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bitsquare is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bitsquare. If not, see <http://www.gnu.org/licenses/>.
 */

package io.bitsquare.util;

import io.bitsquare.common.handlers.ResultHandler;

import org.bitcoinj.utils.Threading;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.awt.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.net.URI;
import java.net.URLConnection;

import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * General utilities
 */
public class Utilities {
    private static final Logger log = LoggerFactory.getLogger(Utilities.class);
    private static long lastTimeStamp = System.currentTimeMillis();

    public static Timer setTimeout(long delay, ResultHandler handler) {
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                Threading.USER_THREAD.execute(() -> handler.handleResult());
            }
        };
        timer.schedule(task, delay);
        return timer;
    }

    public static Timer setInterval(long delay, ResultHandler handler) {
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                Threading.USER_THREAD.execute(() -> handler.handleResult());
            }
        };
        timer.scheduleAtFixedRate(task, delay, delay);
        return timer;
    }

    public static String objectToJson(Object object) {
        Gson gson =
                new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).setPrettyPrinting().create();
        return gson.toJson(object);
    }

    public static boolean isWindows() {
        return getOSName().contains("win");
    }

    public static boolean isOSX() {
        return getOSName().contains("mac") || getOSName().contains("darwin");
    }

    public static boolean isLinux() {
        return getOSName().contains("linux");
    }

    private static String getOSName() {
        return System.getProperty("os.name").toLowerCase();
    }

    public static void openURI(URI uri) throws IOException {
        if (!isLinux()
                && Desktop.isDesktopSupported()
                && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(uri);
        }
        else {
            // On Linux Desktop is poorly implemented.
            // See https://stackoverflow.com/questions/18004150/desktop-api-is-not-supported-on-the-current-platform
            if (!DesktopUtil.browse(uri))
                throw new IOException("Failed to open URI: " + uri.toString());
        }
    }

    public static void openWebPage(String target) throws Exception {
        openURI(new URI(target));
    }

    public static byte[] concatByteArrays(byte[]... arrays) {
        int totalLength = 0;
        for (int i = 0; i < arrays.length; i++) {
            totalLength += arrays[i].length;
        }

        byte[] result = new byte[totalLength];
        int currentIndex = 0;
        for (int i = 0; i < arrays.length; i++) {
            System.arraycopy(arrays[i], 0, result, currentIndex, arrays[i].length);
            currentIndex += arrays[i].length;
        }
        return result;
    }

    public static <T> T jsonToObject(String jsonString, Class<T> classOfT) {
        Gson gson =
                new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).setPrettyPrinting().create();
        return gson.fromJson(jsonString, classOfT);
    }


    public static Object deserializeHexStringToObject(String serializedHexString) {
        Object result = null;
        try {
            ByteArrayInputStream byteInputStream =
                    new ByteArrayInputStream(org.bitcoinj.core.Utils.parseAsHexOrBase58(serializedHexString));

            try (ObjectInputStream objectInputStream = new ObjectInputStream(byteInputStream)) {
                result = objectInputStream.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                byteInputStream.close();

            }

        } catch (IOException i) {
            i.printStackTrace();
        }
        return result;
    }


    public static String serializeObjectToHexString(Serializable serializable) {
        String result = null;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(serializable);

            result = org.bitcoinj.core.Utils.HEX.encode(byteArrayOutputStream.toByteArray());
            byteArrayOutputStream.close();
            objectOutputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static <T> T byteArrayToObject(byte[] data) {
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        ObjectInput in = null;
        Object result = null;
        try {
            in = new ObjectInputStream(bis);
            result = in.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                bis.close();
            } catch (IOException ex) {
                // ignore close exception
            }
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                // ignore close exception
            }
        }
        return (T) result;
    }

    public static byte[] objectToBytArray(Object object) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        byte[] result = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(object);
            result = bos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ex) {
                // ignore close exception
            }
            try {
                bos.close();
            } catch (IOException ex) {
                // ignore close exception
            }
        }
        return result;
    }

    public static void deleteDirectory(File file) throws IOException {
        if (file.isDirectory()) {
            for (File c : file.listFiles())
                deleteDirectory(c);
        }
        if (!file.delete())
            throw new FileNotFoundException("Failed to delete file: " + file);
    }

    private static void printElapsedTime(String msg) {
        if (!msg.isEmpty()) {
            msg += " / ";
        }
        long timeStamp = System.currentTimeMillis();
        log.debug(msg + "Elapsed: " + String.valueOf(timeStamp - lastTimeStamp));
        lastTimeStamp = timeStamp;
    }

    public static void printElapsedTime() {
        printElapsedTime("");
    }


    public static Object copy(Serializable orig) {
        Object obj = null;
        try {
            // Write the object out to a byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(orig);
            out.flush();
            out.close();

            // Make an input stream from the byte array and read
            // a copy of the object back in.
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
            obj = in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return obj;
    }

    /**
     * Empty and delete a folder (and subfolders).
     *
     * @param folder folder to empty
     */
    public static void removeDirectory(final File folder) {
        // check if folder file is a real folder
        if (folder.isDirectory()) {
            File[] list = folder.listFiles();
            if (list != null) {
                for (File tmpF : list) {
                    if (tmpF.isDirectory()) {
                        removeDirectory(tmpF);
                    }
                    if (!tmpF.delete())
                        log.warn("can't delete file : " + tmpF);
                }
            }
            if (!folder.delete())
                log.warn("can't delete folder : " + folder);
        }
    }

    public static String readTextFileFromServer(String url, String userAgent) throws IOException {
        URLConnection connection = URI.create(url).toURL().openConnection();
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        connection.setConnectTimeout(10 * 1000);
        connection.addRequestProperty("User-Agent", userAgent);
        connection.connect();
        try (InputStream inputStream = connection.getInputStream();) {
            return CharStreams.toString(new InputStreamReader(inputStream, Charsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }
}
