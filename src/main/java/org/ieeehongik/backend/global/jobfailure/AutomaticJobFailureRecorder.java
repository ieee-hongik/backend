package org.ieeehongik.backend.global.jobfailure;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AutomaticJobFailureRecorder {

	private static final Logger log = LoggerFactory.getLogger(AutomaticJobFailureRecorder.class);
	private static final Pattern TYPE_CODE = Pattern.compile("[A-Z][A-Z0-9_]{0,79}");
	private static final int ERROR_TYPE_LENGTH = 255;
	private static final String ERROR_SUMMARY = "자동 작업 실행 중 오류가 발생했습니다.";

	private final AutomaticJobFailureRepository repository;
	private final TransactionTemplate failureTransaction;

	AutomaticJobFailureRecorder(AutomaticJobFailureRepository repository, PlatformTransactionManager transactionManager) {
		this.repository = repository;
		this.failureTransaction = new TransactionTemplate(transactionManager);
		this.failureTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	/** Records a caught failure on a best-effort basis. The caller remains responsible for rethrowing it. */
	public void recordFailure(String jobType, Target target, Exception failure) {
		String failureId = UUID.randomUUID().toString();
		LocalDateTime failedAtUtc = LocalDateTime.now(ZoneOffset.UTC);
		String errorType = failure.getClass().getName();
		String storedErrorType = errorType.substring(0, Math.min(errorType.length(), ERROR_TYPE_LENGTH));
		String validatedJobType = "UNKNOWN";
		try {
			validateType(jobType);
			validatedJobType = jobType;
			if (target != null) {
				validateType(target.type());
			}
			failureTransaction.executeWithoutResult(status ->
					repository.insert(failureId, jobType, target, failedAtUtc, storedErrorType, ERROR_SUMMARY));
		} catch (RuntimeException recordingFailure) {
			log.error("Could not record automatic job failure: failureId={}, jobType={}, jobErrorType={}, recordingErrorType={}",
					failureId, validatedJobType, errorType, recordingFailure.getClass().getName());
			return;
		}
		log.warn("Recorded automatic job failure: failureId={}, jobType={}, errorType={}", failureId, jobType, errorType);
	}

	private static void validateType(String type) {
		if (type == null || !TYPE_CODE.matcher(type).matches()) {
			throw new IllegalArgumentException("Expected an uppercase automatic job or target type code");
		}
	}

	public record Target(String type, long id) {
	}
}
