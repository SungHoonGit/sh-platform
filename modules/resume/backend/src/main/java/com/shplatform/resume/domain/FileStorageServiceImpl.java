package com.shplatform.resume.domain;

import com.shplatform.common.exception.BusinessException;
import com.shplatform.common.exception.ErrorCode;
import com.shplatform.resume.api.dto.FileUploadResponse;
import com.shplatform.resume.infrastructure.entity.ResumeFileEntity;
import com.shplatform.resume.infrastructure.repository.ResumeFileRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "pptx", "ppt", "docx", "png", "jpg", "jpeg");
    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;
    private static final DateTimeFormatter MONTH_BUCKET = DateTimeFormatter.ofPattern("yyyyMM");

    private final ResumeFileRepository resumeFileRepository;

    @Value("${app.resume.upload-dir:./uploads/resume}")
    private String baseDir;

    @Override
    @Transactional
    public FileUploadResponse upload(Long userId, String originalFilename, String contentType, byte[] data) {
        if (data == null || data.length == 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (data.length > MAX_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.PAYLOAD_TOO_LARGE);
        }
        String extension = extractExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        String storedPath = buildStoredPath(userId, extension);
        Path target = Paths.get(baseDir, storedPath);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, data);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }

        ResumeFileEntity saved = resumeFileRepository.save(
                ResumeFileEntity.create(userId, originalFilename, storedPath, contentType, data.length));
        return new FileUploadResponse(saved.getId(), saved.getOriginalName(),
                saved.getContentType(), saved.getSizeBytes());
    }

    @Override
    @Transactional(readOnly = true)
    public DownloadedFile download(Long userId, Long fileId) {
        ResumeFileEntity entity = resumeFileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!entity.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        byte[] data;
        try {
            data = Files.readAllBytes(Paths.get(baseDir, entity.getStoredPath()));
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return new DownloadedFile(entity.getOriginalName(), entity.getContentType(), data);
    }

    private String extractExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase();
    }

    private String buildStoredPath(Long userId, String extension) {
        String bucket = LocalDateTime.now().format(MONTH_BUCKET);
        return userId + "/" + bucket + "/" + UUID.randomUUID() + "." + extension;
    }
}
