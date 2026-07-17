package com.shopease;

import org.junit.jupiter.api.Test;

import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class FrontendStaticIntegrityTest {

    private static final Path STATIC = Paths.get("src", "main", "resources", "static");

    @Test
    void staticIdsReferencedByJavascriptExistInHtml() throws Exception {
        String html = Files.readString(STATIC.resolve("index.html"));
        String js = Files.readString(STATIC.resolve("app.js"));

        Set<String> htmlIds = matches(html, "id=\"([^\"]+)\"");
        Set<String> referencedIds = matches(js, "\\$\\(\"([^\"`$]+)\"\\)");

        assertThat(referencedIds)
                .as("Every static $(\"id\") lookup in app.js must exist in index.html")
                .isSubsetOf(htmlIds);
    }

    @Test
    void importantButtonsAndFormsStayPresent() throws Exception {
        String html = Files.readString(STATIC.resolve("index.html"));

        assertThat(html).contains(
                "id=\"fillAdminLogin\"",
                "id=\"loginForm\"",
                "id=\"logoutButton\"",
                "id=\"fillSampleRegister\"",
                "id=\"registerForm\"",
                "id=\"clearFilters\"",
                "id=\"refreshProducts\"",
                "id=\"productSearchForm\"",
                "id=\"categoryForm\"",
                "id=\"productForm\"",
                "id=\"fillSampleImage\"",
                "id=\"refreshAdminOrders\"",
                "id=\"openCartDrawer\"",
                "id=\"closeCartDrawer\"",
                "id=\"refreshCart\"",
                "id=\"orderForm\""
        );
    }

    @Test
    void dynamicButtonHandlersRemainWired() throws Exception {
        String js = Files.readString(STATIC.resolve("app.js"));

        assertThat(js).contains(
                ".add-to-cart",
                ".delete-product",
                ".update-cart-item",
                ".remove-cart-item",
                ".update-order-status",
                "fillSampleImage",
                "runButtonAction"
        );
    }

    private Set<String> matches(String text, String regex) {
        var matcher = Pattern.compile(regex).matcher(text);
        Set<String> values = new TreeSet<>();
        while (matcher.find()) {
            values.add(matcher.group(1));
        }
        return values;
    }
}
