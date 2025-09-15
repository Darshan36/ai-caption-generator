package com.darshan.caption.aicaptiongenerator.repository;

import com.darshan.caption.aicaptiongenerator.model.Caption;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CaptionRepository extends JpaRepository<Caption, Long> {
    List<Caption> findByUser_UsernameOrderByCreatedAtDesc(String username);
}