package com.arcadia.whiteRabbitService.service;

import com.arcadia.whiteRabbitService.model.scandata.*;
import com.arcadia.whiteRabbitService.service.response.ConversionWithLogsResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ScanDataService {
    ScanDataConversion findConversionById(Long conversionId, String username);

    ScanDataConversion createScanDatabaseConversion(ScanDbSettings settings,
                                                    String username);

    ScanDataConversion createScanFilesConversion(ScanFilesSettings setting,
                                                 List<MultipartFile> files,
                                                 String username,
                                                 String project);

    ConversionWithLogsResponse conversionInfoWithLogs(Long conversionId, String username);

    void abort(Long conversionId, String username);

    ScanDataResult result(Long conversionId, String username);
}
