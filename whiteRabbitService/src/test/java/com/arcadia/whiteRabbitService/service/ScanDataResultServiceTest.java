package com.arcadia.whiteRabbitService.service;

import com.arcadia.whiteRabbitService.model.scandata.ScanDataConversion;
import com.arcadia.whiteRabbitService.repository.ScanDataConversionRepository;
import com.arcadia.whiteRabbitService.repository.ScanDataLogRepository;
import com.arcadia.whiteRabbitService.repository.ScanDataResultRepository;
import com.arcadia.whiteRabbitService.service.response.FileSaveResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static com.arcadia.whiteRabbitService.model.ConversionStatus.COMPLETED;
import static com.arcadia.whiteRabbitService.model.ConversionStatus.FAILED;
import static com.arcadia.whiteRabbitService.service.ScanDataConversionServiceTest.createScanDataConversion;
import static com.arcadia.whiteRabbitService.service.ScanDataResultServiceImpl.DATA_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ExtendWith(SpringExtension.class)
class ScanDataResultServiceTest {
    @Autowired
    ScanDataConversionRepository conversionRepository;

    @Autowired
    ScanDataResultRepository resultRepository;

    @Autowired
    ScanDataLogRepository logRepository;

    @MockBean
    FilesManagerService filesManagerService;

    ScanDataResultService resultService;

    @BeforeEach
    void setUp() {
        resultService = new ScanDataResultServiceImpl(
                conversionRepository,
                resultRepository,
                logRepository
        );
    }

    @Test
    void saveCompletedResult() {
        Long fileId = 1L;
        ScanDataConversion conversion = createScanDataConversion();
        conversionRepository.save(conversion);
        FileSaveResponse fileSaveResponse = new FileSaveResponse(fileId, conversion.getUsername(), DATA_KEY, "test.xlsx");
        Mockito.when(filesManagerService.saveFile(Mockito.any()))
                .thenReturn(fileSaveResponse);
        resultService.saveCompletedResult(fileSaveResponse, conversion.getId());
        conversion = conversionRepository.findById(conversion.getId()).get();

        assertNotNull(conversion.getResult());
        assertEquals(COMPLETED.getCode(), conversion.getStatusCode());
        assertEquals(COMPLETED.getName(), conversion.getStatusName());
        assertEquals(fileId, conversion.getResult().getFileId());
    }

    @Test
    void saveFailedResult() {
        ScanDataConversion conversion = createScanDataConversion();
        conversionRepository.save(conversion);
        resultService.saveFailedResult(conversion.getId(), "Test error");
        conversion = conversionRepository.findById(conversion.getId()).get();

        assertEquals(FAILED.getCode(), conversion.getStatusCode());
        assertEquals(FAILED.getName(), conversion.getStatusName());
    }
}