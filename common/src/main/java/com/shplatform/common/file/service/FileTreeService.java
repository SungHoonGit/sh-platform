package com.shplatform.common.file.service;

import com.shplatform.common.file.model.FileNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class FileTreeService {

    public List<FileNode> scan(String rootPath, String relativePath) {
        Path basePath = Paths.get(rootPath).toAbsolutePath().normalize();
        Path targetPath = relativePath == null || relativePath.isEmpty()
                ? basePath
                : basePath.resolve(relativePath).normalize();

        if (!targetPath.startsWith(basePath)) {
            throw new SecurityException("Path traversal detected: " + relativePath);
        }

        if (!Files.exists(targetPath) || !Files.isDirectory(targetPath)) {
            return Collections.emptyList();
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(targetPath)) {
            List<FileNode> nodes = StreamSupport.stream(stream.spliterator(), false)
                    .map(path -> buildNode(path, basePath, 0))
                    .sorted(Comparator
                            .comparing(FileNode::getType).reversed()
                            .thenComparing(FileNode::getName))
                    .collect(Collectors.toList());

            // Set isLast flag
            for (int i = 0; i < nodes.size(); i++) {
                nodes.get(i).setLast(i == nodes.size() - 1);
            }

            return nodes;
        } catch (IOException e) {
            log.error("Error scanning directory: {}", targetPath, e);
            return Collections.emptyList();
        }
    }

    public List<FileNode> scanTree(String rootPath) {
        Path basePath = Paths.get(rootPath).toAbsolutePath().normalize();
        if (!Files.exists(basePath) || !Files.isDirectory(basePath)) {
            return Collections.emptyList();
        }

        List<FileNode> result = new ArrayList<>();
        buildTreeRecursive(basePath, basePath, result, 0, true);
        return result;
    }

    private void buildTreeRecursive(Path currentPath, Path basePath, List<FileNode> result, int depth, boolean isLast) {
        String name = currentPath.getFileName().toString();
        String relativePath = basePath.relativize(currentPath).toString();
        boolean isDirectory = Files.isDirectory(currentPath);

        LocalDateTime modifiedAt = null;
        try {
            modifiedAt = LocalDateTime.ofInstant(
                    Files.getLastModifiedTime(currentPath).toInstant(),
                    ZoneId.systemDefault());
        } catch (IOException e) {
            log.warn("Cannot read modified time: {}", currentPath);
        }

        FileNode node = FileNode.builder()
                .name(name)
                .path(relativePath.isEmpty() ? "." : relativePath)
                .type(isDirectory ? "directory" : "file")
                .size(isDirectory ? 0 : getFileSize(currentPath))
                .modifiedAt(modifiedAt)
                .depth(depth)
                .isLast(isLast)
                .build();

        if (isDirectory) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentPath)) {
                List<Path> children = StreamSupport.stream(stream.spliterator(), false)
                        .sorted(Comparator
                                .comparing((Path p) -> Files.isDirectory(p) ? 0 : 1)
                                .thenComparing(p -> p.getFileName().toString()))
                        .collect(Collectors.toList());

                node.setChildCount(children.size());
                result.add(node);

                for (int i = 0; i < children.size(); i++) {
                    buildTreeRecursive(children.get(i), basePath, result, depth + 1, i == children.size() - 1);
                }
            } catch (IOException e) {
                log.error("Error reading directory: {}", currentPath, e);
                result.add(node);
            }
        } else {
            result.add(node);
        }
    }

    public List<FileNode> scanRecursive(String rootPath, String relativePath, String extension) {
        Path basePath = Paths.get(rootPath).toAbsolutePath().normalize();
        Path targetPath = relativePath == null || relativePath.isEmpty()
                ? basePath
                : basePath.resolve(relativePath).normalize();

        if (!targetPath.startsWith(basePath)) {
            throw new SecurityException("Path traversal detected: " + relativePath);
        }

        List<FileNode> result = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(targetPath)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> extension == null || p.toString().endsWith(extension))
                    .sorted()
                    .forEach(path -> result.add(buildNode(path, basePath, 0)));
        } catch (IOException e) {
            log.error("Error walking directory: {}", targetPath, e);
        }
        return result;
    }

    private FileNode buildNode(Path path, Path basePath, int depth) {
        String relativePath = basePath.relativize(path).toString();
        String name = path.getFileName().toString();
        boolean isDirectory = Files.isDirectory(path);

        LocalDateTime modifiedAt = null;
        try {
            modifiedAt = LocalDateTime.ofInstant(
                    Files.getLastModifiedTime(path).toInstant(),
                    ZoneId.systemDefault());
        } catch (IOException e) {
            log.warn("Cannot read modified time: {}", path);
        }

        int childCount = 0;
        if (isDirectory) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(path)) {
                childCount = (int) StreamSupport.stream(ds.spliterator(), false).count();
            } catch (IOException e) {
                log.warn("Cannot count children: {}", path);
            }
        }

        return FileNode.builder()
                .name(name)
                .path(relativePath)
                .type(isDirectory ? "directory" : "file")
                .size(isDirectory ? 0 : getFileSize(path))
                .modifiedAt(modifiedAt)
                .childCount(childCount)
                .depth(depth)
                .build();
    }

    private long getFileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0;
        }
    }
}
