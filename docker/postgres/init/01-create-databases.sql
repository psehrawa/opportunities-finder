-- Create databases for each microservice
CREATE DATABASE oppfinder_discovery;
CREATE DATABASE oppfinder_intelligence;
CREATE DATABASE oppfinder_user;
CREATE DATABASE oppfinder_notification;
CREATE DATABASE oppfinder_analytics;

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE oppfinder_discovery TO oppfinder;
GRANT ALL PRIVILEGES ON DATABASE oppfinder_intelligence TO oppfinder;
GRANT ALL PRIVILEGES ON DATABASE oppfinder_user TO oppfinder;
GRANT ALL PRIVILEGES ON DATABASE oppfinder_notification TO oppfinder;
GRANT ALL PRIVILEGES ON DATABASE oppfinder_analytics TO oppfinder;