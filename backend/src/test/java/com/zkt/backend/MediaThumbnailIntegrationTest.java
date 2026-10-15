package com.zkt.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriComponentsBuilder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class MediaThumbnailIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test void uploadedPhotosAndCoversServeStreamedThumbnailsWithCacheHeaders() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("username", "thumb_owner", "password", "Password123!")))).andExpect(status().isOk());
        String token = data(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("username", "thumb_owner", "password", "Password123!")))).path("accessToken").asText();
        LocalDateTime now = LocalDateTime.now().withNano(0);
        long activityId = data(post("/api/v1/activities").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of(
                        "title", "缩略图", "location", "测试场地", "signupStart", now.minusMinutes(1).toString(),
                        "signupEnd", now.plusDays(1).toString(), "startTime", now.plusDays(2).toString(),
                        "endTime", now.plusDays(3).toString(), "visibility", "PUBLIC")))).path("id").asLong();
        byte[] png = png(2000, 1000);

        JsonNode photo = data(multipart("/api/v1/activities/{id}/photos", activityId)
                .file(new MockMultipartFile("file", "big.png", "image/png", png)).header("Authorization", "Bearer " + token));
        assertThat(photo.path("thumbnailUrl").asText()).contains("variant=thumb").isNotEqualTo(photo.path("url").asText());

        MockHttpServletResponse thumb = fetch(photo.path("thumbnailUrl").asText());
        assertThat(thumb.getContentType()).isEqualTo("image/jpeg");
        assertThat(thumb.getHeader("Cache-Control")).isEqualTo("max-age=300, private");
        BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(thumb.getContentAsByteArray()));
        assertThat(decoded.getWidth()).isEqualTo(720);
        assertThat(decoded.getHeight()).isEqualTo(360);
        assertThat(thumb.getContentAsByteArray().length).isLessThan(png.length);

        MockHttpServletResponse original = fetch(photo.path("url").asText());
        assertThat(original.getContentType()).isEqualTo("image/png");
        assertThat(original.getContentLengthLong()).isEqualTo(png.length);
        assertThat(original.getContentAsByteArray()).isEqualTo(png);

        JsonNode activity = data(multipart("/api/v1/activities/{id}/cover", activityId)
                .file(new MockMultipartFile("file", "cover.png", "image/png", png)).header("Authorization", "Bearer " + token));
        MockHttpServletResponse cover = fetch(activity.path("coverThumbnailUrl").asText());
        assertThat(cover.getContentType()).isEqualTo("image/jpeg");
        assertThat(cover.getHeader("Cache-Control")).isEqualTo("max-age=604800, public");

        JsonNode undecodable = data(multipart("/api/v1/activities/{id}/photos", activityId)
                .file(new MockMultipartFile("file", "tiny.png", "image/png", new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1}))
                .header("Authorization", "Bearer " + token));
        assertThat(undecodable.path("thumbnailUrl").asText()).as("falls back to the original").isEqualTo(undecodable.path("url").asText());
    }

    private MockHttpServletResponse fetch(String absoluteUrl) throws Exception {
        URI uri = URI.create(absoluteUrl);
        var request = get(uri.getPath());
        UriComponentsBuilder.fromUri(uri).build().getQueryParams()
                .forEach((name, values) -> request.param(name, values.stream().map(v -> java.net.URLDecoder.decode(v, java.nio.charset.StandardCharsets.UTF_8)).toArray(String[]::new)));
        return mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse();
    }

    private JsonNode data(org.springframework.test.web.servlet.RequestBuilder request) throws Exception {
        return mapper.readTree(mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray()).path("data");
    }

    private static byte[] png(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x += 50) for (int y = 0; y < height; y++) image.setRGB(x, y, 0x3366cc + x);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
