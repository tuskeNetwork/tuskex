package tuskex.common.config;

import tuskex.common.TuskexException;

public class ConfigException extends TuskexException {

    public ConfigException(String format, Object... args) {
        super(format, args);
    }
}
