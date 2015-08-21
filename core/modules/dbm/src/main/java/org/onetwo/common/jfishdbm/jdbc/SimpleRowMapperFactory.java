package org.onetwo.common.jfishdbm.jdbc;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.onetwo.common.utils.LangUtils;
import org.onetwo.common.utils.map.CamelMap;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;

public class SimpleRowMapperFactory implements RowMapperFactory {

	
	public SimpleRowMapperFactory() {
		super();
	}
	
	public boolean isSimpleType(Class<?> type){
		return LangUtils.isSimpleType(type);
	}

	@Override
	public RowMapper<?> createDefaultRowMapper(Class<?> type) {
		RowMapper<?> rowMapper = null;
		if(type==null || type==Object.class){
//			rowMapper = new SingleColumnRowMapper(Object.class);
			rowMapper = new UnknowTypeRowMapper();
		}else if(isSimpleType(type) || LangUtils.isTimeClass(type)){
			//唯一，而且返回类型是简单类型，则返回单列的RowMapper
			rowMapper = new SingleColumnRowMapper<>(type);
		}else if(LangUtils.isMapClass(type)){
//			rowMapper = new ColumnMapRowMapper();
			rowMapper = new CamelNameRowMapper();
			
		}else if(Object[].class==type){
			rowMapper = new ObjectArrayRowMapper();
		}else if(List.class.isAssignableFrom(type)){
			rowMapper = new ListRowMapper();
		}else if(Collection.class.isAssignableFrom(type)){
			rowMapper = new HashsetRowMapper();
		}else{
			rowMapper = getBeanPropertyRowMapper(type);
		}
		return rowMapper;
	}

	protected RowMapper<?> getBeanPropertyRowMapper(Class<?> entityClass) {
		return new BeanPropertyRowMapper<>(entityClass);
	}
	
	static class CamelNameRowMapper extends ColumnMapRowMapper {

		protected Map<String, Object> createColumnMap(int columnCount) {
			return new CamelMap<Object>(columnCount);
		}
	}
	

}
