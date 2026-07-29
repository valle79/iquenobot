package com.iquenobot.contact.domain.repository;

import com.iquenobot.contact.domain.entity.Contact;
import com.iquenobot.shared.enums.ContactStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContactRepository extends JpaRepository<Contact, UUID> {

    Optional<Contact> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    Optional<Contact> findByPhoneAndTenantIdAndDeletedFalse(String phone, UUID tenantId);

    Optional<Contact> findByWhatsappPhoneAndTenantIdAndDeletedFalse(String whatsappPhone, UUID tenantId);

    Optional<Contact> findByNormalizedPhoneAndTenantIdAndDeletedFalse(String normalizedPhone, UUID tenantId);

    Optional<Contact> findByEmailAndTenantIdAndDeletedFalse(String email, UUID tenantId);

    Page<Contact> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);

    Page<Contact> findByTenantIdAndStatusAndDeletedFalse(UUID tenantId, ContactStatus status, Pageable pageable);

    @Query("SELECT c FROM Contact c WHERE c.tenantId = :tenantId AND c.deleted = false AND " +
           "(LOWER(c.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.phone) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.company) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Contact> searchContacts(@Param("tenantId") UUID tenantId, 
                                 @Param("search") String search, 
                                 Pageable pageable);

    long countByTenantIdAndDeletedFalse(UUID tenantId);

    long countByTenantIdAndStatusAndDeletedFalse(UUID tenantId, ContactStatus status);

    @Query("SELECT c FROM Contact c WHERE c.tenantId = :tenantId AND c.status = 'ACTIVE' AND c.deleted = false")
    List<Contact> findActiveContacts(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(c) FROM Contact c WHERE c.tenantId = :tenantId AND c.status = 'ACTIVE' AND c.deleted = false")
    long countActiveContacts(@Param("tenantId") UUID tenantId);

    @Query("SELECT COUNT(c) FROM Contact c WHERE c.tenantId = :tenantId AND c.status = 'BLOCKED' AND c.deleted = false")
    long countBlockedContacts(@Param("tenantId") UUID tenantId);

    boolean existsByPhoneAndTenantIdAndDeletedFalse(String phone, UUID tenantId);

    boolean existsByEmailAndTenantIdAndDeletedFalse(String email, UUID tenantId);

    @Query("SELECT c.phone FROM Contact c WHERE c.tenantId = :tenantId AND c.deleted = false AND c.phone IN :phones")
    List<String> findExistingPhones(@Param("tenantId") UUID tenantId, @Param("phones") List<String> phones);

    @Query("SELECT c.normalizedPhone FROM Contact c WHERE c.tenantId = :tenantId AND c.deleted = false AND c.normalizedPhone IN :phones")
    List<String> findExistingNormalizedPhones(@Param("tenantId") UUID tenantId, @Param("phones") List<String> phones);

    @Query("SELECT c.email FROM Contact c WHERE c.tenantId = :tenantId AND c.deleted = false AND c.email IN :emails")
    List<String> findExistingEmails(@Param("tenantId") UUID tenantId, @Param("emails") List<String> emails);

    @Modifying
    @Query("UPDATE Contact c SET c.messageCount = c.messageCount + 1 WHERE c.id = :id")
    int incrementMessageCount(@Param("id") UUID id);
}