package com.psehrawa.oppfinder.common.enums;

import lombok.Getter;

@Getter
public enum CompanySize {
    STARTUP("Startup", "1-10 employees", 1, 10),
    SMALL("Small", "11-50 employees", 11, 50),
    MEDIUM("Medium", "51-200 employees", 51, 200),
    LARGE("Large", "201-1000 employees", 201, 1000),
    ENTERPRISE("Enterprise", "1000+ employees", 1000, Integer.MAX_VALUE),
    UNKNOWN("Unknown", "Company size not specified", 0, Integer.MAX_VALUE);

    private final String displayName;
    private final String description;
    private final int minEmployees;
    private final int maxEmployees;

    CompanySize(String displayName, String description, int minEmployees, int maxEmployees) {
        this.displayName = displayName;
        this.description = description;
        this.minEmployees = minEmployees;
        this.maxEmployees = maxEmployees;
    }

    public static CompanySize fromEmployeeCount(int employeeCount) {
        for (CompanySize size : values()) {
            if (employeeCount >= size.minEmployees && employeeCount <= size.maxEmployees && size != UNKNOWN) {
                return size;
            }
        }
        return UNKNOWN;
    }
}