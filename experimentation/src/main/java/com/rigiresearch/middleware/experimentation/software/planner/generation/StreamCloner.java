package com.rigiresearch.middleware.experimentation.software.planner.generation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Allows to deep clone an object using Streams. Implements the Singleton pattern.
 *
 * @author lfrivera
 * @version $Id$
 * @since 0.1.0
 */
public final class StreamCloner {

    /**
     * Unique instance of the class.
     */
    private static StreamCloner instance = null;

    /**
     * Private constructor of the class.
     */
    private StreamCloner() {
    }

    /**
     * Allows to obtain the unique instance of the class.
     *
     * @return Unique instance of the class.
     */
    public static StreamCloner getInstance() {

        if (instance == null) {

            instance = new StreamCloner();

        }

        return instance;

    }

    /**
     * Allows to deep clone an object using Streams.
     *
     * @param toClone The object to clone.
     * @return The cloned object.
     */
    protected Object clone(Object toClone) {

        Object response = null;

        try {

            ByteArrayOutputStream boutStream = new ByteArrayOutputStream();
            ObjectOutputStream outStream = new ObjectOutputStream(boutStream);
            outStream.writeObject(toClone);
            outStream.flush();

            ByteArrayInputStream binStream = new ByteArrayInputStream(boutStream.toByteArray());
            ObjectInputStream inStream = new ObjectInputStream(binStream);

            response = inStream.readObject();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return response;

    }

}
