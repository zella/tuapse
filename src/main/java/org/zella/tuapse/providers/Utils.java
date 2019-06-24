package org.zella.tuapse.providers;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Utils {

    /**
     * Attempts to calculate the size of a file or directory.
     *
     * <p>
     * Since the operation is non-atomic, the returned value may be inaccurate.
     * However, this method is quick and does its best.
     */
    public static long size(Path path) {

        final AtomicLong size = new AtomicLong(0);

        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {

                    size.addAndGet(attrs.size());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    // Skip folders that can't be traversed
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {

                    if (exc != null)
                        System.out.println("had trouble traversing: " + dir + " (" + exc + ")");
                    // Ignore errors traversing a folder
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new AssertionError("walkFileTree will not throw IOException if the FileVisitor does not");
        }

        return size.get();
    }

    public static int recursiveDeleteFilesAcessedOlderThanNDays(int days, Path dirPath) throws IOException {
        AtomicInteger count = new AtomicInteger(0);
        long cutOff = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000);
        Files.list(dirPath)
                .forEach(path -> {
                    if (Files.isDirectory(path)) {
                        try {
                            recursiveDeleteFilesAcessedOlderThanNDays(days, path);
                        } catch (IOException e) {
                            // log here and move on
                        }
                    } else {
                        try {
                            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                            FileTime time = attrs.lastAccessTime();

                            if (time.toMillis() < cutOff) {
                                Files.delete(path);
                                count.incrementAndGet();
                            }
                        } catch (IOException ex) {
                            // log here and move on
                        }
                    }
                });
        return count.get();
    }
}
