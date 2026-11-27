package org.ieeehongik.backend.global.jobfailure;

import java.time.LocalDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class AutomaticJobFailureRepository {

	private final JdbcTemplate jdbcTemplate;

	AutomaticJobFailureRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	void insert(String id, String jobType, AutomaticJobFailureRecorder.Target target,
			LocalDateTime failedAtUtc, String errorType, String errorSummary) {
		jdbcTemplate.update("""
				INSERT INTO automatic_job_failure
				    (id, job_type, target_type, target_id, failed_at_utc, error_type, error_summary)
				VALUES (?, ?, ?, ?, ?, ?, ?)
				""", id, jobType, target == null ? null : target.type(), target == null ? null : target.id(),
				failedAtUtc, errorType, errorSummary);
	}
}
