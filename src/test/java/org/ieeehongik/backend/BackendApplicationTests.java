package org.ieeehongik.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:backend_smoke_test;MODE=MySQL",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.sql.init.mode=always"
})
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
