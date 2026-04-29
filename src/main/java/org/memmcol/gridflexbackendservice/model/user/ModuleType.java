package org.memmcol.gridflexbackendservice.model.user;

public enum ModuleType {
//    DATA_MANAGEMENT("re12kmd783nd&h#$k4sd7012nh&dk"),
//    VENDING("5rtyek&mrtiks5784092nbdhnjk!34@"),
//    HES("@478nks#5kljh%ngh&jjkjkjkld##bjk134"),
//    BILLING("9gwjk982bhdbgu$gjjksd)hj9bjjj@13bvnb%");

    DATA_MANAGEMENT("re12kmd783nd&h#$k4sd7012nh&dk", true),
    USER_MANAGEMENT("#zdt00iud789bf&h#$ke49286h@@nq", true),
    VENDING("5rtyek&mrtiks5784092nbdhnjk!34@", false),
    HES("@478nks#5kljh%ngh&jjkjkjkld##bjk134", false),
    BILLING("9gwjk982bhdbgu$gjjksd)hj9bjjj@13bvnb%", false);

    private final String value;
    private final boolean isDefault;

    ModuleType(String value, boolean isDefault) {
        this.value = value;
        this.isDefault = isDefault;
    }

    public String getValue() {
        return value;
    }
//    ModuleType(String value) {
//        this.value = value;
//    }
//
//    public String getValue() {
//        return value;
//    }
    public boolean isDefault() {
        return isDefault;
    }

    public static ModuleType fromName(String name) {
        for (ModuleType type : values()) {
            if (type.name().equals(name)) {
                return type;
            }
        }
        return null;
    }

    public static ModuleType fromValue(String value) {
        for (ModuleType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}