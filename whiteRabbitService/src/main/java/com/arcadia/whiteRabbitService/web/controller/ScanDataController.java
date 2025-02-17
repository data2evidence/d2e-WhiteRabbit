package com.arcadia.whiteRabbitService.web.controller;

import com.arcadia.whiteRabbitService.model.scandata.ScanDataConversion;
import com.arcadia.whiteRabbitService.model.scandata.ScanDataResult;
import com.arcadia.whiteRabbitService.model.scandata.ScanDbSettings;
import com.arcadia.whiteRabbitService.model.scandata.ScanFilesSettings;
import com.arcadia.whiteRabbitService.service.FilesManagerService;
import com.arcadia.whiteRabbitService.service.ScanDataConversionService;
import com.arcadia.whiteRabbitService.service.ScanDataService;
import com.arcadia.whiteRabbitService.service.StorageService;
import com.arcadia.whiteRabbitService.service.error.BadRequestException;
import com.arcadia.whiteRabbitService.service.error.InternalServerErrorException;
import com.arcadia.whiteRabbitService.service.response.ConversionWithLogsResponse;
import com.arcadia.whiteRabbitService.service.response.ScanReportResponse;
import com.arcadia.whiteRabbitService.web.context.RequestContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.arcadia.whiteRabbitService.service.WhiteRabbitFacade.MAX_TABLES_COUNT;
import static com.arcadia.whiteRabbitService.util.FileUtil.createDirectory;
import static com.arcadia.whiteRabbitService.util.FileUtil.deleteRecursive;
import static java.lang.String.format;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping("/api/scan-report")
@RequiredArgsConstructor
@Slf4j
public class ScanDataController {
    private final ScanDataService scanDataService;
    private final ScanDataConversionService conversionService;
    private final FilesManagerService filesManagerService;
    private final StorageService storageService;

    @Autowired
    private RequestContext requestContext;

    @PostMapping("/db")
    public ResponseEntity<ScanDataConversion> generate(@RequestHeader("Username") String username,
                                                       @RequestHeader("Authorization") String token,
                                                       @Validated @RequestBody ScanDbSettings dbSetting) {
        log.info("Rest request to generate scan report by database settings");

        requestContext.setToken(token);
        
        ScanDataConversion conversion = scanDataService.createScanDatabaseConversion(dbSetting, username);
        conversionService.runConversion(conversion);
        return ok(conversion);
    }

    @PostMapping("/files")
    public ResponseEntity<ScanDataConversion> generate(@RequestHeader("Username") String username,
                                                       @RequestHeader("Authorization") String token,
                                                       @RequestParam String settings,
                                                       @RequestParam List<MultipartFile> files) {
        log.info("Rest request to generate scan report by files settings");

        requestContext.setToken(token);

        if (files.size() > MAX_TABLES_COUNT) {
            throw new BadRequestException(format("Too many files. Max count is %d.", MAX_TABLES_COUNT));
        }
        ScanFilesSettings scanFilesSettings;
        try {
            ObjectMapper mapper = new ObjectMapper();
            scanFilesSettings = mapper.readValue(settings, ScanFilesSettings.class);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Incorrect Scan Data Params: " + e.getMessage());
        }

        String project = "csv";
        Path csvDirectory = Path.of(username, project);
        createDirectory(csvDirectory);
        List<Path> csvFiles = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                Path csvFilePath = storageService.store(file, csvDirectory, file.getOriginalFilename());
                csvFiles.add(csvFilePath);
            }
        } catch (IOException e) {
            log.error("Could not store CSV file {}. Stack trace: {}", e.getMessage(), e.getStackTrace());
            deleteRecursive(csvDirectory);
            throw new InternalServerErrorException(e.getMessage(), e);
        }

        scanFilesSettings.setCsvDirectory(csvDirectory);
        scanFilesSettings.setCsvFiles(csvFiles);
        ScanDataConversion conversion = scanDataService.createScanFilesConversion(scanFilesSettings, files, username, project);
        conversionService.runConversion(conversion);

        return ok(conversion);
    }

    @GetMapping("/abort/{conversionId}")
    public ResponseEntity<Void> abort(@RequestHeader("Username") String username,
                                      @PathVariable Long conversionId) {
        log.info("Rest request to abort Scan Data conversion with id {}", conversionId);
        scanDataService.abort(conversionId, username);
        return noContent().build();
    }

    @GetMapping("/conversion/{conversionId}")
    public ResponseEntity<ConversionWithLogsResponse> conversionInfoAndLogs(@RequestHeader("Username") String username,
                                                                            @PathVariable Long conversionId) {
        log.info("Rest request to get Scan Data conversion info by id {}", conversionId);
        return ok(scanDataService.conversionInfoWithLogs(conversionId, username));
    }

    @GetMapping("/result/{conversionId}")
    public ResponseEntity<ScanReportResponse> scanResult(@RequestHeader("Username") String username,
                                                         @PathVariable Long conversionId) {
        ScanDataResult result = scanDataService.result(conversionId, username);
        ScanReportResponse response = new ScanReportResponse(result.getFileId(), result.getFileName());
        return ok(response);
    }

    @GetMapping("/result-as-resource/{conversionId}")
    public ResponseEntity<Resource> downloadScanReport(@RequestHeader("Username") String username,
                                                       @RequestHeader("Authorization") String token,
                                                       @PathVariable Long conversionId) {
        log.info("Rest request to download scan report with conversion id {}", conversionId);

        requestContext.setToken(token);

        ScanDataResult result = scanDataService.result(conversionId, username);
        Resource resource = filesManagerService.getFile(result.getFileId());
        return ok()
                .contentType(MediaType.parseMediaType("application/x-xls"))
                .header(CONTENT_DISPOSITION, format("attachment; filename=\"%S\"", result.getFileName()))
                .body(resource);
    }
}
