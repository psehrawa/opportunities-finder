package com.psehrawa.oppfinder.common.enums;

import lombok.Getter;

@Getter
public enum Country {
    US("United States", "USD", "America/New_York"),
    GB("United Kingdom", "GBP", "Europe/London"),
    CA("Canada", "CAD", "America/Toronto"),
    AU("Australia", "AUD", "Australia/Sydney"),
    DE("Germany", "EUR", "Europe/Berlin"),
    FR("France", "EUR", "Europe/Paris"),
    IN("India", "INR", "Asia/Kolkata"),
    SG("Singapore", "SGD", "Asia/Singapore"),
    JP("Japan", "JPY", "Asia/Tokyo"),
    CN("China", "CNY", "Asia/Shanghai"),
    BR("Brazil", "BRL", "America/Sao_Paulo"),
    MX("Mexico", "MXN", "America/Mexico_City"),
    NL("Netherlands", "EUR", "Europe/Amsterdam"),
    SE("Sweden", "SEK", "Europe/Stockholm"),
    CH("Switzerland", "CHF", "Europe/Zurich"),
    IL("Israel", "ILS", "Asia/Jerusalem"),
    KR("South Korea", "KRW", "Asia/Seoul"),
    IE("Ireland", "EUR", "Europe/Dublin"),
    NO("Norway", "NOK", "Europe/Oslo"),
    DK("Denmark", "DKK", "Europe/Copenhagen");

    private final String displayName;
    private final String currency;
    private final String timezone;

    Country(String displayName, String currency, String timezone) {
        this.displayName = displayName;
        this.currency = currency;
        this.timezone = timezone;
    }
}