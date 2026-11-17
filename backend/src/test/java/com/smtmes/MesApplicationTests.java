package com.smtmes;

import static org.assertj.core.api.Assertions.assertThat;

import com.smtmes.common.DbPingMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;

/** 기동 + Flyway + MyBatis 가 일회용 Oracle 컨테이너에서 도는지 확인하는 스모크 테스트. (Docker 필요, 첫 실행은 1~2분) */
@SpringBootTest
@Testcontainers
class MesApplicationTests {

	@Container
	@ServiceConnection
	static OracleContainer oracle = new OracleContainer("gvenzl/oracle-free:23-slim-faststart");

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	DbPingMapper pingMapper;

	@Test
	void Flyway_V1이_적용된다() {
		Integer applied = jdbc.queryForObject(
				"select count(*) from \"flyway_schema_history\" where \"success\" = 1", Integer.class);
		assertThat(applied).isGreaterThanOrEqualTo(1);
	}

	@Test
	void MyBatis_XML_매퍼가_동작한다() {
		assertThat(pingMapper.ping()).isEqualTo(1);
	}
}
