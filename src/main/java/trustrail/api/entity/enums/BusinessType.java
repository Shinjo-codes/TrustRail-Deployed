package trustrail.api.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum BusinessType {
    HEALTHCARE,
    EDUCATION,
    FMCG,
    LOGISTICS,
    SUBSCRIPTION_SERVICE,
    OTHER;

//    @JsonCreator
//    public static BusinessType from(String value) {
//        return BusinessType.valueOf(value.toUpperCase());
//    }
}
