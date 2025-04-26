package org.pds.util;

import java.util.regex.Pattern;

public class IpAddressValidator {

    private static final String IP_REGEX = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

    private static final Pattern IP_PATTERN = Pattern.compile(IP_REGEX);

    public static boolean isValid(String ip) {
        return IP_PATTERN.matcher(ip).matches();
    }
}
