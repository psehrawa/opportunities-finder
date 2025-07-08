package com.psehrawa.oppfinder.common.enums;

import lombok.Getter;

@Getter
public enum Industry {
    FINTECH("Fintech", "Financial Technology"),
    HEALTHTECH("HealthTech", "Healthcare Technology"),
    EDTECH("EdTech", "Educational Technology"),
    ENTERPRISE_SOFTWARE("Enterprise Software", "B2B Software Solutions"),
    CONSUMER_SOFTWARE("Consumer Software", "B2C Software Applications"),
    ARTIFICIAL_INTELLIGENCE("Artificial Intelligence", "AI and Machine Learning"),
    CYBERSECURITY("Cybersecurity", "Information Security"),
    BLOCKCHAIN("Blockchain", "Distributed Ledger Technology"),
    IOT("Internet of Things", "Connected Devices and IoT"),
    CLOUD_COMPUTING("Cloud Computing", "Cloud Infrastructure and Services"),
    DEVOPS("DevOps", "Development and Operations Tools"),
    DATA_ANALYTICS("Data Analytics", "Big Data and Analytics"),
    MOBILE_TECHNOLOGY("Mobile Technology", "Mobile Apps and Platforms"),
    WEB_DEVELOPMENT("Web Development", "Web Technologies and Frameworks"),
    GAMING("Gaming", "Video Games and Interactive Entertainment"),
    ECOMMERCE("E-commerce", "Online Retail and Marketplaces"),
    LOGISTICS("Logistics", "Supply Chain and Transportation"),
    RENEWABLE_ENERGY("Renewable Energy", "Clean Energy and Sustainability"),
    BIOTECHNOLOGY("Biotechnology", "Life Sciences and Biotech"),
    ROBOTICS("Robotics", "Automation and Robotics"),
    AUTONOMOUS_VEHICLES("Autonomous Vehicles", "Self-driving and Transportation"),
    VIRTUAL_REALITY("Virtual Reality", "VR and Augmented Reality"),
    REAL_ESTATE("Real Estate", "PropTech and Real Estate"),
    AGRICULTURE("Agriculture", "AgTech and Farming Technology"),
    MANUFACTURING("Manufacturing", "Industrial Technology"),
    TELECOMMUNICATIONS("Telecommunications", "Telecom and Networking"),
    AEROSPACE("Aerospace", "Space Technology and Aviation"),
    MEDIA_ENTERTAINMENT("Media & Entertainment", "Digital Media and Content"),
    TRAVEL_HOSPITALITY("Travel & Hospitality", "Tourism and Hospitality Tech"),
    FOOD_BEVERAGE("Food & Beverage", "FoodTech and Beverage Innovation");

    private final String displayName;
    private final String description;

    Industry(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}