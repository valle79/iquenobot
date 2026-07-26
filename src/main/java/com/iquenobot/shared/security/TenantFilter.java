package com.iquenobot.shared.security;

import com.iquenobot.shared.domain.util.TenantContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.IOException;

@Component
@Order(1)
@Slf4j
public class TenantFilter implements Filter {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        try {
            String tenantId = TenantContext.getTenantId();
            if (tenantId != null) {
                // Enable Hibernate filter for tenant isolation
                Session session = entityManager.unwrap(Session.class);
                org.hibernate.Filter filter = session.enableFilter("tenantFilter");
                filter.setParameter("tenantId", tenantId);
                
                log.debug("Enabled tenant filter for tenant: {}", tenantId);
            }

            chain.doFilter(request, response);
        } finally {
            // Clear tenant context after request
            TenantContext.clear();
            
            // Disable the filter
            if (entityManager != null) {
                try {
                    Session session = entityManager.unwrap(Session.class);
                    session.disableFilter("tenantFilter");
                } catch (Exception e) {
                    log.debug("Error disabling tenant filter: {}", e.getMessage());
                }
            }
        }
    }
}