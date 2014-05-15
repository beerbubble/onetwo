package org.onetwo.common.spring.sql;

import java.util.Map;
import java.util.Map.Entry;

import org.onetwo.common.db.ExtQueryUtils;
import org.onetwo.common.db.FileNamedSqlGenerator;
import org.onetwo.common.db.SqlAndValues;
import org.onetwo.common.db.sql.DynamicQuery;
import org.onetwo.common.db.sql.DynamicQueryFactory;
import org.onetwo.common.log.MyLoggerFactory;
import org.slf4j.Logger;

public class DefaultFileNamedSqlGenerator<T extends JFishNamedFileQueryInfo> implements FileNamedSqlGenerator<T> {
	
	private static final Logger logger = MyLoggerFactory.getLogger(DefaultFileNamedSqlGenerator.class);
	protected T info;
	protected boolean countQuery;
	private FileSqlParser<T> parser;
	private ParserContext parserContext;
	private Class<?> resultClass;
	
	private String[] ascFields;
	private String[] desFields;

	private Map<Object, Object> params;
	
	
	
	public DefaultFileNamedSqlGenerator(T info, boolean countQuery,
			FileSqlParser<T> parser) {
		super();
		this.info = info;
		this.countQuery = countQuery;
		this.parser = parser;
	}

	public DefaultFileNamedSqlGenerator(T info, boolean countQuery,
			FileSqlParser<T> parser, ParserContext parserContext,
			Class<?> resultClass, String[] ascFields, String[] desFields,
			Map<Object, Object> params) {
		super();
		this.info = info;
		this.countQuery = countQuery;
		this.parser = parser;
		this.parserContext = parserContext;
		this.resultClass = resultClass;
		this.ascFields = ascFields;
		this.desFields = desFields;
		this.params = params;
	}

	@Override
	public SqlAndValues generatSql(){
		String parsedSql = null;
		SqlAndValues sv = null;
		if(info.getFileSqlParserType()==FileSqlParserType.IGNORENULL){
			String sql = countQuery?info.getCountSql():info.getSql();
			DynamicQuery query = DynamicQueryFactory.createJFishDynamicQuery(sql, resultClass);
			for(Entry<Object, Object> entry : this.params.entrySet()){
				query.setParameter(entry.getKey().toString(), entry.getValue());
			}
			query.asc(ascFields);
			query.desc(desFields);
			query.compile();
			parsedSql = query.getTransitionSql();
			sv = new SqlAndValues(false, sql, query.getValues());
			
		}else if(info.getFileSqlParserType()==FileSqlParserType.TEMPLATE){
			if(this.parserContext==null)
				this.parserContext = ParserContext.create();
			
			this.parserContext.put(SqlFunctionFactory.CONTEXT_KEY, SqlFunctionFactory.getSqlFunctionDialet(info.getDataBaseType()));
			this.parserContext.putAll(params);
			TemplateInNamedQueryParser attrParser = new TemplateInNamedQueryParser(parser, parserContext, info);
			this.parserContext.put(JFishNamedFileQueryInfo.TEMPLATE_KEY, attrParser);
			
			if(countQuery){
				parsedSql = this.parser.parse(info.isAutoGeneratedCountSql()?info.getFullName():info.getCountName(), parserContext);
				if(info.isAutoGeneratedCountSql()){
					parsedSql = ExtQueryUtils.buildCountSql(parsedSql, "");
				}
			}else{
				parsedSql = this.parser.parse(info.getFullName(), parserContext);
			}
			sv = new SqlAndValues(true, parsedSql, params);
			
		}else{
			parsedSql = countQuery?info.getCountSql():info.getSql();
			sv = new SqlAndValues(true, parsedSql, params);
		}
		logger.info("parsed sql : {}", parsedSql);

		return sv;
	}
	
}
