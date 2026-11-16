package com.codeoj.judge.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.UUID;

/** 一次领取任务独占一个目录，不写入或清理任何其他尝试的文件。 */
final class AttemptWorkspace implements AutoCloseable {
    private static final long MAX_ARTIFACT_BYTES = 64L * 1024 * 1024;
    private final Path root;

    AttemptWorkspace(Path configuredRoot, long submissionId, String executionToken) throws IOException {
        Path base = configuredRoot.toAbsolutePath().normalize();
        Files.createDirectories(base);
        if (Files.isSymbolicLink(base)) throw new IOException("work root must not be a symbolic link");
        String token = executionToken == null ? UUID.randomUUID().toString()
                : UUID.fromString(executionToken).toString();
        root = Files.createTempDirectory(base, submissionId + "-" + token + "-");
        permissions(root, "rwx------");
    }

    Path createDirectory(String name, boolean writable) throws IOException {
        Path directory = root.resolve(name);
        Files.createDirectory(directory);
        permissions(directory, writable ? "rwxrwxrwx" : "rwxr-xr-x");
        return directory;
    }

    static void writeNew(Path path, String content) throws IOException {
        try (SeekableByteChannel channel = Files.newByteChannel(path,
                Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS))) {
            ByteBuffer buffer = StandardCharsets.UTF_8.encode(content);
            while (buffer.hasRemaining()) channel.write(buffer);
        }
        permissions(path, "r--r--r--");
    }

    /** 编译容器退出后仅接收普通文件，禁止跟随链接或复制任意目录结构。 */
    static void copyArtifact(Path source, Path target, boolean executable) throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(source, BasicFileAttributes.class,
                LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isRegularFile() || attributes.size() > MAX_ARTIFACT_BYTES) {
            throw new IOException("invalid compile artifact: " + source.getFileName());
        }
        try (SeekableByteChannel input = Files.newByteChannel(source,
                Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS));
             SeekableByteChannel output = Files.newByteChannel(target,
                     Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS))) {
            ByteBuffer buffer = ByteBuffer.allocate(8192);
            long total = 0;
            while (input.read(buffer) != -1) {
                total += buffer.position();
                if (total > MAX_ARTIFACT_BYTES) throw new IOException("compile artifact too large");
                buffer.flip();
                while (buffer.hasRemaining()) output.write(buffer);
                buffer.clear();
            }
        }
        permissions(target, executable ? "r-xr-xr-x" : "r--r--r--");
    }

    static void seal(Path directory) throws IOException { permissions(directory, "r-xr-xr-x"); }

    void deleteDirectory(Path directory) throws IOException {
        if (!root.equals(directory.getParent())) throw new IOException("directory outside attempt");
        deleteTree(directory);
    }

    private static void permissions(Path path, String mode) throws IOException {
        if (Files.getFileStore(path).supportsFileAttributeView("posix")) {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(mode));
        }
    }

    @Override
    public void close() throws IOException {
        deleteTree(root);
    }

    private static void deleteTree(Path directory) throws IOException {
        // walkFileTree 默认不跟随符号链接，只删除本次创建的目录。
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                Files.delete(path);
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs) throws IOException {
                permissions(path, "rwx------");
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult postVisitDirectory(Path path, IOException error) throws IOException {
                if (error != null) throw error;
                Files.delete(path);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
