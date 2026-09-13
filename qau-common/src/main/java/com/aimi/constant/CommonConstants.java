package com.aimi.constant;

public final class CommonConstants {

    private CommonConstants() {}

    public static final class StatusCode {
        public static final Integer SUCCESS = 200;
        public static final Integer BAD_REQUEST = 400;
        public static final Integer UNAUTHORIZED = 401;
        public static final Integer FORBIDDEN = 403;
        public static final Integer NOT_FOUND = 404;
        public static final Integer SERVER_ERROR = 500;
    }
}
