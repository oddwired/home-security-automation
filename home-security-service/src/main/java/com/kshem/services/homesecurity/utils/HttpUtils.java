package com.kshem.services.homesecurity.utils;

import java.util.Arrays;

public class HttpUtils {
    public static boolean accepts(String acceptHeader, String toAccept){
        if(acceptHeader.contains("*/*")){
            return true;

        }

        String[] acceptValues = acceptHeader.split("\\s*(,|;)\\s*");
        Arrays.sort(acceptValues);
        return Arrays.binarySearch(acceptValues, toAccept) > -1
                || Arrays.binarySearch(acceptValues, toAccept.replaceAll("/.*$", "/*")) > -1;
    }
}
