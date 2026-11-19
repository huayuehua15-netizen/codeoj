package com.codeoj.server.util;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 头像 data URL 校验的拒绝路径（API.md v1.5b）。
 *
 * <p>核心断言：<b>MIME 前缀不是身份证明</b>——冒充的图片必须被魔数拦下。
 */
class AvatarDataUrlTest {

    private static final byte[] PNG_MAGIC = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0
    };
    private static final byte[] JPEG_MAGIC = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0
    };
    private static final byte[] WEBP_MAGIC = {
            'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 0, 0
    };

    private static String asDataUrl(String declaredType, byte[] bytes) {
        return "data:image/" + declaredType + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }

    @Test void pngIsAcceptedWhenMagicMatches() {
        AvatarDataUrl.Image img = AvatarDataUrl.parse(asDataUrl("png", PNG_MAGIC));
        assertNotNull(img);
        assertEquals("png", img.ext());
        assertArrayEquals(PNG_MAGIC, img.bytes());
    }

    @Test void jpgAliasAndWebpAreAccepted() {
        assertNotNull(AvatarDataUrl.parse(asDataUrl("jpeg", JPEG_MAGIC)));
        assertNotNull(AvatarDataUrl.parse(asDataUrl("jpg", JPEG_MAGIC)));
        assertNotNull(AvatarDataUrl.parse(asDataUrl("webp", WEBP_MAGIC)));
    }

    @Test void fakedMimePrefixIsRejectedByMagic() {
        // 把一段纯文本伪装成 png：前缀合法，但魔数对不上——必须拒绝
        byte[] fake = "hello, not an image".getBytes();
        assertNull(AvatarDataUrl.parse(asDataUrl("png", fake)));
        // 反过来也一样：png 字节声明成 jpeg
        assertNull(AvatarDataUrl.parse(asDataUrl("jpeg", PNG_MAGIC)));
    }

    @Test void unsupportedAndMalformedInputsAreRejected() {
        assertNull(AvatarDataUrl.parse(null));
        assertNull(AvatarDataUrl.parse(""));
        // gif 不在白名单（API.md 只放行 png/jpeg/webp）
        assertNull(AvatarDataUrl.parse(asDataUrl("gif", WEBP_MAGIC)));
        // 缺 ;base64, 段
        assertNull(AvatarDataUrl.parse("data:image/png," + Base64.getEncoder().encodeToString(PNG_MAGIC)));
        // base64 载荷损坏
        assertNull(AvatarDataUrl.parse("data:image/png;base64,!!!not-base64!!!"));
        // 空载荷
        assertNull(AvatarDataUrl.parse(asDataUrl("png", new byte[0])));
    }

    @Test void oversizedPayloadIsRejected() {
        byte[] big = new byte[AvatarDataUrl.MAX_DECODED_BYTES + 1];
        assertNull(AvatarDataUrl.parse(asDataUrl("png", big)));
        // 恰好等于上限：放行（边界）
        byte[] exact = new byte[AvatarDataUrl.MAX_DECODED_BYTES];
        System.arraycopy(PNG_MAGIC, 0, exact, 0, PNG_MAGIC.length);
        assertNotNull(AvatarDataUrl.parse(asDataUrl("png", exact)));
    }
}
