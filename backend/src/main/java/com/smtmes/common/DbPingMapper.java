package com.smtmes.common;

import org.apache.ibatis.annotations.Mapper;

/** MyBatis 연결 확인용 매퍼 (SQL은 resources/mapper/common/DbPingMapper.xml). 실제 도메인 매퍼가 생기면 지워도 된다. */
@Mapper
public interface DbPingMapper {

	int ping();
}
