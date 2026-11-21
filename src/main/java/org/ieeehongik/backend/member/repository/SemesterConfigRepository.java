package org.ieeehongik.backend.member.repository;

import org.ieeehongik.backend.member.domain.SemesterConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SemesterConfigRepository
        extends JpaRepository<SemesterConfig, Long> {
}
