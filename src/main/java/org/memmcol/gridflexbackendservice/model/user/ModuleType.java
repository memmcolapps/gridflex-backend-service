package org.memmcol.gridflexbackendservice.model.user;

public enum ModuleType {
    DATA_MANAGEMENT("re12kmd783nd&h#$k4sd7012nh&dk"),
    VENDING("5rtyek&mrtiks5784092nbdhnjk!34@"),
    HES("@478nks#5kljh%ngh&jjkjkjkld##bjk134"),
    BILLING("9gwjk982bhdbgu$gjjksd)hj9bjjj@13bvnb%");

    private final String value;

    ModuleType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
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