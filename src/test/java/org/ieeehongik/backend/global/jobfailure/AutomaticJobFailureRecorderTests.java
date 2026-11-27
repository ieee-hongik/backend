package org.ieeehongik.backend.global.jobfailure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:job_failure_test;MODE=MySQL",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.sql.init.mode=always",
		"spring.sql.init.schema-locations=classpath:schema.sql"
})
@ExtendWith(OutputCaptureExtension.class)
class AutomaticJobFailureRecorderTests {

	@Autowired
	private AutomaticJobFailureRecorder recorder;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private JdbcTemplate jdbc;

	@MockitoSpyBean
	private AutomaticJobFailureRepository repository;

	@BeforeEach
	void resetTables() {
		jdbc.execute("CREATE TABLE IF NOT EXISTS job_failure_test_work (id BIGINT PRIMARY KEY)");
		jdbc.update("DELETE FROM job_failure_test_work");
		jdbc.update("DELETE FROM automatic_job_failure");
	}

	@Test
	void storesSafeFailureDataWithAnOptionalTarget(CapturedOutput output) {
		LocalDateTime before = LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1);
		var failure = new IllegalStateException("private application data");
		recorder.recordFailure("TARGETED_JOB", new AutomaticJobFailureRecorder.Target("APPLICATION", 42), failure);
		recorder.recordFailure("WHOLE_JOB", null, failure);

		var targeted = jdbc.queryForMap("SELECT * FROM automatic_job_failure WHERE job_type = 'TARGETED_JOB'");
		String id = (String) targeted.get("id");
		assertThat(UUID.fromString(id).toString()).isEqualTo(id);
		assertThat(targeted.get("target_type")).isEqualTo("APPLICATION");
		assertThat(targeted.get("target_id")).isEqualTo(42L);
		assertThat(targeted.get("error_type")).isEqualTo(IllegalStateException.class.getName());
		assertThat(targeted.get("error_summary")).isEqualTo("자동 작업 실행 중 오류가 발생했습니다.");
		LocalDateTime failedAt = jdbc.queryForObject(
				"SELECT failed_at_utc FROM automatic_job_failure WHERE id = ?", LocalDateTime.class, id);
		assertThat(failedAt).isBetween(before, LocalDateTime.now(ZoneOffset.UTC).plusSeconds(1));
		var wholeJob = jdbc.queryForMap("SELECT * FROM automatic_job_failure WHERE job_type = 'WHOLE_JOB'");
		assertThat(wholeJob.get("target_type")).isNull();
		assertThat(wholeJob.get("target_id")).isNull();
		assertThat(output.getOut()).contains(id).doesNotContain("private application data");
	}

	@Test
	void keepsFailureRecordWhenBusinessTransactionRollsBack() {
		var originalFailure = new IllegalStateException("work failed");

		assertThat(runFailingWork("ROLLBACK_JOB", originalFailure)).isSameAs(originalFailure);

		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM job_failure_test_work", Integer.class)).isZero();
		assertThat(jdbc.queryForObject("SELECT job_type FROM automatic_job_failure", String.class))
				.isEqualTo("ROLLBACK_JOB");
	}

	@Test
	void logsRecordingFailureWithoutReplacingOriginalException(CapturedOutput output) {
		doThrow(new DataAccessResourceFailureException("private database detail"))
				.when(repository).insert(any(), eq("UNAVAILABLE_DB"), any(), any(), any(), any());
		var originalFailure = new IllegalStateException("private work detail");

		assertThat(runFailingWork("UNAVAILABLE_DB", originalFailure)).isSameAs(originalFailure);

		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM automatic_job_failure", Integer.class)).isZero();
		assertThat(output.getOut()).contains("Could not record automatic job failure", "failureId=",
				DataAccessResourceFailureException.class.getName(), IllegalStateException.class.getName())
				.doesNotContain("private database detail", "private work detail");
	}

	private Throwable runFailingWork(String jobType, IllegalStateException originalFailure) {
		return catchThrowable(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
			jdbc.update("INSERT INTO job_failure_test_work (id) VALUES (1)");
			recorder.recordFailure(jobType, null, originalFailure);
			throw originalFailure;
		}));
	}
}
