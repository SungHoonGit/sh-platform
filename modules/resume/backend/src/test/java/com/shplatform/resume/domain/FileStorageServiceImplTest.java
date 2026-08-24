package com.shplatform.resume.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import com.shplatform.resume.infrastructure.entity.ResumeFileEntity;
import com.shplatform.resume.infrastructure.repository.ResumeFileRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long FILE_ID = 100L;
    private static final byte[] DATA = "hello portfolio".getBytes();

    @Mock
    private ResumeFileRepository resumeFileRepository;

    @InjectMocks
    private FileStorageServiceImpl fileStorageService;

    @TempDir
    Path tempDir;

    private void givenBaseDir() {
        ReflectionTestUtils.setField(fileStorageService, "baseDir", tempDir.toString());
    }

    private ResumeFileEntity savedEntity() {
        return ResumeFileEntity.create(USER_ID, "기획서.pptx", "1/202608/uuid.pptx",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation", DATA.length);
    }

    @Test
    @DisplayName("upload: 허용 확장자 파일을 저장하고 메타데이터를 반환한다")
    void upload_success() throws Exception {
        givenBaseDir();
        given(resumeFileRepository.save(any(ResumeFileEntity.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        var response = fileStorageService.upload(USER_ID, "기획서.pptx", null, DATA);

        assertThat(response.originalName()).isEqualTo("기획서.pptx");
        assertThat(response.sizeBytes()).isEqualTo(DATA.length);
        try (var paths = Files.walk(tempDir)) {
            assertThat(paths.filter(p -> p.getFileName().toString().endsWith(".pptx")).count())
                    .isEqualTo(1);
        }
    }

    @Test
    @DisplayName("upload: 허용되지 않은 확장자면 INVALID_INPUT 예외가 발생한다")
    void upload_invalidExtension() {
        givenBaseDir();

        assertThatThrownBy(() -> fileStorageService.upload(USER_ID, "악성코드.exe", null, DATA))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("upload: 10MB 초과면 PAYLOAD_TOO_LARGE 예외가 발생한다")
    void upload_tooLarge() {
        givenBaseDir();
        byte[] big = new byte[10 * 1024 * 1024 + 1];

        assertThatThrownBy(() -> fileStorageService.upload(USER_ID, "big.pdf", null, big))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PAYLOAD_TOO_LARGE);
    }

    @Test
    @DisplayName("download: 소유자는 파일을 다운로드할 수 있다")
    void download_success() throws Exception {
        givenBaseDir();
        ResumeFileEntity entity = savedEntity();
        Path stored = tempDir.resolve(entity.getStoredPath());
        Files.createDirectories(stored.getParent());
        Files.write(stored, DATA);
        given(resumeFileRepository.findById(FILE_ID)).willReturn(java.util.Optional.of(entity));

        var downloaded = fileStorageService.download(USER_ID, FILE_ID);

        assertThat(downloaded.originalName()).isEqualTo("기획서.pptx");
        assertThat(downloaded.data()).isEqualTo(DATA);
    }

    @Test
    @DisplayName("download: 다른 사용자의 파일이면 FORBIDDEN 예외가 발생한다")
    void download_forbidden() {
        givenBaseDir();
        given(resumeFileRepository.findById(FILE_ID)).willReturn(java.util.Optional.of(savedEntity()));

        assertThatThrownBy(() -> fileStorageService.download(999L, FILE_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }
}
