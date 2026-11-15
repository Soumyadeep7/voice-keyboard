package com.soumyadeep.voicekeyboard;

/** Offline cleanup used when no API key is set: drops fillers and repeated words. */
final class Cleaner {

    private Cleaner() {
    }

    static String basic(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.replaceAll("(?i)\\b(um+|uh+|erm*|hmm+|you know|i mean)\\b,?\\s*", "");
        s = s.replaceAll("(?iu)(?<![\\p{L}\\p{M}\\p{N}])([\\p{L}\\p{M}\\p{N}']+)(\\s+\\1)+(?![\\p{L}\\p{M}\\p{N}])", "$1");
        s = s.replaceAll("[ \\t]+", " ").replaceAll(" ([,.?!])", "$1").trim();
        if (s.isEmpty()) {
            return s;
        }
        s = s.substring(0, 1).toUpperCase() + s.substring(1);
        char lastChar = s.charAt(s.length() - 1);
        boolean asciiWord = (lastChar >= 'a' && lastChar <= 'z') || (lastChar >= 'A' && lastChar <= 'Z')
                || (lastChar >= '0' && lastChar <= '9');
        if (asciiWord) {
            s = s + ".";
        }
        return s;
    }
}
