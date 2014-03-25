package org.onetwo.common.excel;

import java.io.FileOutputStream;
import java.io.OutputStream;

import org.apache.poi.ss.usermodel.Workbook;
import org.onetwo.common.exception.ServiceException;
import org.onetwo.common.interfaces.TemplateGenerator;
import org.onetwo.common.utils.LangUtils;

abstract public class AbstractWorkbookExcelGenerator implements TemplateGenerator {

//	protected Workbook workbook;
	
	
	public void write(OutputStream out) {
		try {
			this.getWorkbook().write(out);
		} catch (Exception e) {
			throw new ServiceException(e);
		} finally {
			//LangUtils.closeIO(out);
		}
	}

	@Override
	public void generateTo(String path) {
		this.generateIt();
		this.write(path);
	}

	public void write(String path) {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(path);
			this.getWorkbook().write(fos);
		} catch (Exception e) {
			throw new ServiceException(e);
		} finally {
			LangUtils.closeIO(fos);
		}
	}

	abstract public Workbook getWorkbook();


}
