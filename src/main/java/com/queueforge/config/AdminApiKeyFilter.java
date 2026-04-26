package com.queueforge.config;

import com.queueforge.config.QueueForgeProperties.Security;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AdminApiKeyFilter implements Filter {
    private static final String HEADER = "X-QueueForge-Admin-Key";

    private final String adminApiKey;

    public AdminApiKeyFilter(QueueForgeProperties properties) {
        this.adminApiKey = properties.getSecurity().getAdminApiKey();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        String path = req.getRequestURI();

        boolean isAdmin = path.startsWith("/api/v1/admin/") || path.startsWith("/api/v1/workers/");
        if (!isAdmin || adminApiKey == null || adminApiKey.isBlank()) {
            chain.doFilter(request, response);
            return;
        }

        String providedKey = req.getHeader(HEADER);
        if (adminApiKey.equals(providedKey)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletResponse res = (HttpServletResponse) response;
        res.setStatus(401);
        res.setContentType("application/json");
        res.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Invalid or missing admin API key\"}");
    }
}
