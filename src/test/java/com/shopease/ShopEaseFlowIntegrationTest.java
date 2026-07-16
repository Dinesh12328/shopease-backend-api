package com.shopease;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShopEaseFlowIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void frontendAndDocumentationEndpointsArePublic() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));

        mvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ShopEase")));

        mvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/api/auth/login")));

        mvc.perform(get("/styles.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("--primary")));

        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post").exists());

        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void completeShoppingFlowWorksThroughRestEndpoints() throws Exception {
        String adminToken = login("admin@test.com", "Admin@123");

        Long categoryId = postJson("/api/admin/categories", Map.of(
                "name", "Electronics " + UUID.randomUUID(),
                "description", "Electronic products and gadgets"
        ), adminToken)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andReturn()
                .getResponseJson()
                .at("/data/id")
                .asLong();

        Long productId = postJson("/api/admin/products", Map.of(
                "name", "Test Phone",
                "description", "A phone created by integration test",
                "price", new BigDecimal("999.99"),
                "stock", 5,
                "brand", "ShopEase",
                "categoryId", categoryId,
                "imageUrl", "https://example.com/test-phone.jpg"
        ), adminToken)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.stock").value(5))
                .andReturn()
                .getResponseJson()
                .at("/data/id")
                .asLong();

        mvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Test Phone"));

        mvc.perform(get("/api/products").param("brand", "ShopEase"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].brand").value("ShopEase"));

        String customerEmail = "shopper-" + UUID.randomUUID() + "@example.com";
        String userToken = postJson("/api/auth/register", Map.of(
                "name", "Flow Shopper",
                "email", customerEmail,
                "password", "Password1"
        ), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.user.role").value("USER"))
                .andReturn()
                .getResponseJson()
                .at("/data/token")
                .asText();

        postJson("/api/admin/categories", Map.of(
                "name", "Forbidden Category",
                "description", "A user should not create this"
        ), userToken).andExpect(status().isForbidden());

        JsonNode cartAfterAdd = postJson("/api/cart/add", Map.of(
                "productId", productId,
                "quantity", 2
        ), userToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andReturn()
                .getResponseJson();

        Long cartItemId = cartAfterAdd.at("/data/items/0/id").asLong();

        putJson("/api/cart/items/" + cartItemId, Map.of("quantity", 3), userToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].quantity").value(3));

        Long orderId = postJson("/api/orders/place", Map.of(
                "shippingAddress", "123 Integration Test Street",
                "paymentMethod", "COD"
        ), userToken)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.payment.status").value("PENDING"))
                .andReturn()
                .getResponseJson()
                .at("/data/id")
                .asLong();

        mvc.perform(get("/api/cart").header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items", hasSize(0)));

        mvc.perform(get("/api/orders/user").header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(orderId));

        patchJson("/api/admin/orders/" + orderId + "/status", Map.of("status", "CONFIRMED"), adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        mvc.perform(get("/api/admin/orders").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(orderId));

        mvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stock").value(2));
    }

    @Test
    void productImageLinksAcceptLongBrowserUrls() throws Exception {
        String adminToken = login("admin@test.com", "Admin@123");

        Long categoryId = postJson("/api/admin/categories", Map.of(
                "name", "Image Link Test " + UUID.randomUUID(),
                "description", "Products with browser image links"
        ), adminToken)
                .andExpect(status().isCreated())
                .andReturn()
                .getResponseJson()
                .at("/data/id")
                .asLong();

        String longImageUrl = "www.images.example.com/phones/galaxy-s24.png?tracking="
                + "a".repeat(1200)
                + "&size=large&format=webp";

        postJson("/api/admin/products", Map.of(
                "name", "Long Image Link Phone",
                "description", "Product with a long copied browser image URL",
                "price", new BigDecimal("45999.00"),
                "stock", 12,
                "brand", "Samsung",
                "categoryId", categoryId,
                "imageUrl", longImageUrl
        ), adminToken)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.imageUrl").value("https://" + longImageUrl));
    }

    private String login(String email, String password) throws Exception {
        return postJson("/api/auth/login", Map.of("email", email, "password", password), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andReturn()
                .getResponseJson()
                .at("/data/token")
                .asText();
    }

    private ResultActionsWithJson postJson(String url, Object body, String token) throws Exception {
        return jsonRequest("POST", url, body, token);
    }

    private ResultActionsWithJson putJson(String url, Object body, String token) throws Exception {
        return jsonRequest("PUT", url, body, token);
    }

    private ResultActionsWithJson patchJson(String url, Object body, String token) throws Exception {
        return jsonRequest("PATCH", url, body, token);
    }

    private ResultActionsWithJson jsonRequest(String method, String url, Object body, String token) throws Exception {
        var builder = switch (method) {
            case "PUT" -> put(url);
            case "PATCH" -> patch(url);
            default -> post(url);
        };

        builder.contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body));

        if (token != null) {
            builder.header("Authorization", bearer(token));
        }

        return new ResultActionsWithJson(mvc.perform(builder), json);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record ResultActionsWithJson(
            org.springframework.test.web.servlet.ResultActions delegate,
            ObjectMapper json
    ) {
        ResultActionsWithJson andExpect(org.springframework.test.web.servlet.ResultMatcher matcher) throws Exception {
            delegate.andExpect(matcher);
            return this;
        }

        ResultActionsWithJson andReturn() {
            return this;
        }

        JsonNode getResponseJson() throws Exception {
            MvcResult result = delegate.andReturn();
            return json.readTree(result.getResponse().getContentAsString());
        }
    }
}
