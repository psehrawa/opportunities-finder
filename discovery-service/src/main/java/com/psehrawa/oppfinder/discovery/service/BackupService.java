package com.psehrawa.oppfinder.discovery.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.psehrawa.oppfinder.common.dto.OpportunityDto;
import com.psehrawa.oppfinder.discovery.mapper.OpportunityMapper;
import com.psehrawa.oppfinder.discovery.repository.OpportunityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class BackupService {

    private final OpportunityRepository opportunityRepository;
    private final OpportunityMapper opportunityMapper;
    private final ObjectMapper objectMapper;

    @Value("${backup.directory:./backups}")
    private String backupDirectory;

    @Value("${backup.retention.days:30}")
    private int retentionDays;

    /**
     * Scheduled backup - runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional(readOnly = true)
    public void performScheduledBackup() {
        log.info("Starting scheduled backup");
        try {
            String backupFile = performBackup();
            log.info("Backup completed successfully: {}", backupFile);
            cleanupOldBackups();
        } catch (Exception e) {
            log.error("Backup failed", e);
        }
    }

    /**
     * Perform manual backup
     */
    public String performBackup() throws IOException {
        // Create backup directory if it doesn't exist
        Path backupPath = Paths.get(backupDirectory);
        if (!Files.exists(backupPath)) {
            Files.createDirectories(backupPath);
        }

        // Generate backup filename
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String backupFileName = String.format("oppfinder_backup_%s.json", timestamp);
        String zipFileName = String.format("oppfinder_backup_%s.zip", timestamp);

        // Fetch all opportunities
        List<OpportunityDto> opportunities = opportunityRepository.findAll()
                .stream()
                .map(opportunityMapper::toDto)
                .collect(Collectors.toList());

        // Create backup data structure
        BackupData backupData = new BackupData();
        backupData.setBackupTimestamp(LocalDateTime.now());
        backupData.setVersion("1.0");
        backupData.setOpportunityCount(opportunities.size());
        backupData.setOpportunities(opportunities);

        // Write to JSON file
        File jsonFile = new File(backupPath.toFile(), backupFileName);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, backupData);

        // Compress the backup
        File zipFile = new File(backupPath.toFile(), zipFileName);
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile.toPath()))) {
            ZipEntry zipEntry = new ZipEntry(backupFileName);
            zos.putNextEntry(zipEntry);
            Files.copy(jsonFile.toPath(), zos);
            zos.closeEntry();
        }

        // Delete the uncompressed file
        Files.delete(jsonFile.toPath());

        return zipFile.getAbsolutePath();
    }

    /**
     * Restore from backup
     */
    public void restoreFromBackup(String backupFilePath) throws IOException {
        log.info("Restoring from backup: {}", backupFilePath);
        
        // TODO: Implement restore logic
        // 1. Unzip the backup file
        // 2. Parse JSON data
        // 3. Clear existing data (optional)
        // 4. Import opportunities
        // 5. Rebuild indexes and caches
        
        log.warn("Restore functionality not yet implemented");
    }

    /**
     * Clean up old backups based on retention policy
     */
    private void cleanupOldBackups() {
        try {
            Path backupPath = Paths.get(backupDirectory);
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);

            Files.list(backupPath)
                    .filter(path -> path.toString().endsWith(".zip"))
                    .filter(path -> {
                        try {
                            return Files.getLastModifiedTime(path).toInstant()
                                    .isBefore(cutoffDate.atZone(java.time.ZoneId.systemDefault()).toInstant());
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            log.info("Deleted old backup: {}", path.getFileName());
                        } catch (IOException e) {
                            log.error("Failed to delete backup: {}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.error("Failed to cleanup old backups", e);
        }
    }

    /**
     * Backup data structure
     */
    @lombok.Data
    private static class BackupData {
        private LocalDateTime backupTimestamp;
        private String version;
        private int opportunityCount;
        private List<OpportunityDto> opportunities;
    }
}