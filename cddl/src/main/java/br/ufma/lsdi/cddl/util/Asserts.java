package br.ufma.lsdi.cddl.util;

public class Asserts {

    public static void assertNotNull(Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException("Illegal argument exception: null value not allowed here.");
        }
    }
        public static void assertNotNull(Object obj, String message) {
            if (obj == null) {
                throw new IllegalArgumentException("Illegal argument exception: " + message);
            }
        }

}
