package com.tracenet.alert.repository;

import com.tracenet.alert.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID> {

    List<Alert> findByOrgIdOrderByCreatedAtDesc(String orgId);

    List<Alert> findByOrgIdAndStatusOrderByCreatedAtDesc(String orgId, String status);
}