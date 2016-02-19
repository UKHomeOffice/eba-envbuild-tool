package com.ipt.ebsa.environment.hiera.msoffice;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 * Some helpers to parse excel spreadsheets.
 * @author James Shepherd
 */
public class MsOffice {
	
	public interface RowParser {
		/**
		 * @return Headings to look for in each sheet (should be all lowercase)
		 */
		public Set<String> getRowHeadings();
		
		/**
		 * Each sheet that contains the {@link #getRowHeadings()} headings has
		 * each row under the headings passed to this method
		 * @param row
		 */
		public void parse(Map<String, String> row);
	}

	private static final Logger LOG = Logger.getLogger(MsOffice.class);
	
	public Workbook getWorkbook(File xl) {
		try {
			LOG.debug(String.format("Opening workbook at [%s]", xl.getAbsolutePath()));
			return WorkbookFactory.create(xl);
		} catch (InvalidFormatException e) {
			throw new RuntimeException("Failed to open Sheet", e);
		} catch (IOException e) {
			throw new RuntimeException("Failed to open Sheet", e);
		}
	}
	
	public static String getCellAsString(Cell c) {
		if (null != c) {
			switch (c.getCellType()) {
				case Cell.CELL_TYPE_STRING:
					return c.getStringCellValue().trim();
				case Cell.CELL_TYPE_BOOLEAN:
					return String.valueOf(c.getBooleanCellValue());
				case Cell.CELL_TYPE_NUMERIC:
					return String.valueOf(new Double(c.getNumericCellValue()).intValue());
				default:
					return "";
			}
		}
		return null;
	}
	
	/**
	 * @param workbook to be parsed
	 * @param parser implement this to get passed relevant parsed rows
	 */
	public static void parseAllSheets(Workbook workbook, RowParser parser) {
		for (int sheetNum = 0; sheetNum < workbook.getNumberOfSheets(); sheetNum++) {
			Sheet sheet = workbook.getSheetAt(sheetNum);
			LOG.debug(String.format("Reading sheet [%s]", sheet.getSheetName()));
			TreeMap<Integer, String> cellToHeadingMap = new TreeMap<>();
			
			for (int rowNum = sheet.getFirstRowNum(); rowNum <= sheet.getLastRowNum(); rowNum++ ) {
				LOG.debug(String.format("Reading row [%s]", rowNum));
				Row row = sheet.getRow(rowNum);
				if (null != row) {
					if (cellToHeadingMap.size() < parser.getRowHeadings().size()) {
						// we haven't found a row with enough headings yet
						cellToHeadingMap = new TreeMap<>();
						for (int cellNum = row.getFirstCellNum(); cellNum < row.getLastCellNum(); cellNum++) {
							String heading = getCellAsString(row.getCell(cellNum));
							
							if (null != heading) {
								heading = heading.trim().toLowerCase();
							
								if (parser.getRowHeadings().contains(heading)) {
									if (cellToHeadingMap.containsValue(heading)) {
										throw new RuntimeException(String.format("Repeated header [%s] in sheet [%s]",
												heading, sheet.getSheetName()));
									} else {
										LOG.debug(String.format("Found heading [%s]", heading));
										cellToHeadingMap.put(cellNum, heading);
									}
									
									if (cellToHeadingMap.size() >= parser.getRowHeadings().size()) {
										LOG.debug("Found all headings");
										break;
									}
								}
							}
						}
					} else {
						// we already have headings, so let's make a map of the values
						TreeMap<String, String> output = new TreeMap<>();
						
						for (int cellNum = row.getFirstCellNum(); cellNum <= row.getLastCellNum(); cellNum++) {
							if (cellToHeadingMap.containsKey(cellNum)) {
								output.put(cellToHeadingMap.get(cellNum), getCellAsString(row.getCell(cellNum)));
							}
						}
						parser.parse(output);
					}
				}
			}
		}
	}
}
