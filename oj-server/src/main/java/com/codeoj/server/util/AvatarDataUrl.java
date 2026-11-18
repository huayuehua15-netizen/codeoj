package com.codeoj.server.util;

import org.springframework.lang.Nullable;

import java.util.Base64;
import java.util.Locale;

/**
 * data URL 形式头像的解析与校验（API.md v1.5b）。
 *
 * <p><b>为什么不能只看 MIME 前缀</b>：{@code data:image/png;base64,xxx} 的前缀
 * 是客户端自己声明的，把任意字节改个前缀就能冒充图片。因此解码后必须再用
 * 魔数（magic bytes）确认真实格式——PNG/JPEG/WebP 各自的文件头是固定的，
 * 改不出来。
 *
 * <p>纯静态工具、无 IO，便于单测覆盖全部拒绝路径。
 */
public final class AvatarDataUrl {

    /** 解码后大小上限（API.md v1.5b：300 KB） */
    public static final int MAX_DECODED_BYTES = 300 * 1024;

    /** 解析成功的结果：真实扩展名（不含点）与图像字节 */
    public record Image(String ext, byte[] bytes) { }

    private AvatarDataUrl() {
    }

    /**
     * 解析并校验 data URL。任何不合规都返回 null（调用方转 40000），
     * 不抛异常——脏输入是常态而不是故障。
     */
    @Nullable
    public static Image parse(@Nullable String dataUrl) {
        if (dataUrl == null) {
            return null;
        }
        // 前缀格式：data:image/{type};base64,{payload}
        if (!dataUrl.startsWith("data:image/")) {
            return null;
        }
        int semi = dataUrl.indexOf(";base64,");
        if (semi < 0) {
            return null;
        }
        String type = dataUrl.substring("data:image/".length(), semi).toLowerCase(Locale.ROOT);
        String ext = switch (type) {
            case "png" -> "png";
            case "jpeg", "jpg" -> "jpg";
            case "webp" -> "webp";
            default -> null;
        };
        if (ext == null) {
            return null;
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(dataUrl.substring(semi + ";base64,".length()));
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (bytes.length == 0 || bytes.length > MAX_DECODED_BYTES) {
            return null;
        }
        if (!magicMatches(ext, bytes)) {
            return null;
        }
        return new Image(ext, bytes);
    }

    /** 魔数校验：声明的类型必须与文件头一致 */
    private static boolean magicMatches(String ext, byte[] b) {
        return switch (ext) {
            case "png" -> b.length >= 8
                    && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
            case "jpg" -> b.length >= 3
                    && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
            case "webp" -> b.length >= 12
                    && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                    && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
            default -> false;
        };
    }
}
