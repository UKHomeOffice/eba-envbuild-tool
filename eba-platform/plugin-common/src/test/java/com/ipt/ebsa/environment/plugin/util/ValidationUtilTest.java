package com.ipt.ebsa.environment.plugin.util;


import hudson.util.FormValidation;

import org.junit.Assert;
import org.junit.Test;

import com.ipt.ebsa.util.collection.ParamFactory;

public class ValidationUtilTest {
	
	@Test
	public void validateMandatoryPresent() {
		Assert.assertEquals(FormValidation.ok(), ValidationUtil.validateMandatory("fieldName", "fieldValue"));
	}
	
	@Test
	public void validateMandatoryNull() {
		FormValidation validation = ValidationUtil.validateMandatory("fieldName", null);
		Assert.assertNotEquals(FormValidation.ok(), validation);
		Assert.assertEquals("fieldName is mandatory", validation.getMessage());
	}
	
	@Test
	public void validateMandatoryEmpty() {
		FormValidation validation = ValidationUtil.validateMandatory("fieldName", "");
		Assert.assertNotEquals(FormValidation.ok(), validation);
		Assert.assertEquals("fieldName is mandatory", validation.getMessage());
	}

	@Test
	public void validateMandatoryBlank() {
		FormValidation validation = ValidationUtil.validateMandatory("fieldName", "   ");
		Assert.assertNotEquals(FormValidation.ok(), validation);
		Assert.assertEquals("fieldName is mandatory", validation.getMessage());
	}
	
	@Test
	public void validateMandatoryParamsPresent() {
		ParamFactory params = ParamFactory.with("field1", "value1").and("field2", "value2").and("field3", "value3").and("field4", 4);
		Assert.assertEquals(FormValidation.ok(), ValidationUtil.validateMandatory(params));
	}
	
	@Test
	public void validateMandatoryParamsNull() {
		ParamFactory params = ParamFactory.with("field1", null).and("field2", "value2").and("field3", "value3").and("field4", 4);
		FormValidation validation = ValidationUtil.validateMandatory(params);
		Assert.assertNotEquals(FormValidation.ok(), validation);
		Assert.assertEquals("Missing mandatory value(s) for: field1. ", validation.getMessage());
	}
	
	@Test
	public void validateMandatoryParamsEmpty() {
		ParamFactory params = ParamFactory.with("field1", "value1").and("field2", "").and("field3", "").and("field4", 4);
		FormValidation validation = ValidationUtil.validateMandatory(params);
		Assert.assertNotEquals(FormValidation.ok(), validation);
		Assert.assertEquals("Missing mandatory value(s) for: field2, field3. ", validation.getMessage());
	}
	
	@Test
	public void validateMandatoryParamsBlank() {
		ParamFactory params = ParamFactory.with("field1", "  ").and("field2", "value2").and("field3", " ").and("field4", 4);
		FormValidation validation = ValidationUtil.validateMandatory(params);
		Assert.assertNotEquals(FormValidation.ok(), validation);
		Assert.assertEquals("Missing mandatory value(s) for: field1, field3. ", validation.getMessage());
	}
	
	@Test
	public void validateMandatoryParamsEmptyBlankNull() {
		ParamFactory params = ParamFactory.with("field1", "").and("field2", "    ").and("field3", "value3").and("field4", null);
		FormValidation validation = ValidationUtil.validateMandatory(params);
		Assert.assertNotEquals(FormValidation.ok(), validation);
		Assert.assertEquals("Missing mandatory value(s) for: field1, field2, field4. ", validation.getMessage());
	}
	
	@Test
	public void validateMandatoryParamsNonePresent() {
		ParamFactory params = ParamFactory.with("field1", "").and("field2", "    ").and("field3", null).and("field4", null);
		FormValidation validation = ValidationUtil.validateMandatory(params);
		Assert.assertNotEquals(FormValidation.ok(), validation);
		Assert.assertEquals("Missing mandatory value(s) for: field1, field2, field3, field4. ", validation.getMessage());
	}
	
	@Test
	public void validateMandatoryParamsUnresolved() {
		ParamFactory params = ParamFactory.with("field1", "value1").and("field2", "${value2}").and("field3", "value3").and("field4", 4);
		FormValidation validation = ValidationUtil.validateMandatory(params);
		Assert.assertNotEquals(FormValidation.ok(), ValidationUtil.validateMandatory(params));
		Assert.assertEquals("Unresolved mandatory value(s) for: field2.", validation.getMessage());
	}
	
	@Test
	public void validateMandatoryParamsMissingAndUnresolved() {
		ParamFactory params = ParamFactory.with("field1", "${field1}").and("field2", null).and("field3", "${value3}").and("field4", 4).and("field5", "  ");
		FormValidation validation = ValidationUtil.validateMandatory(params);
		Assert.assertNotEquals(FormValidation.ok(), ValidationUtil.validateMandatory(params));
		Assert.assertEquals("Missing mandatory value(s) for: field2, field5. Unresolved mandatory value(s) for: field1, field3.", validation.getMessage());
	}
	
	@Test
	public void validateWithRegex() {
		FormValidation validation = ValidationUtil.validateWithRegex("field1", "value1", "^[a-z]+[0-9]+$");
		Assert.assertEquals(FormValidation.ok(), validation);
	}
	
	@Test
	public void validateWithRegexBlankValueBlankRegex() {
		FormValidation validation = ValidationUtil.validateWithRegex("field1", "", "");
		Assert.assertEquals(FormValidation.ok(), validation);
	}
	
	@Test
	public void validateWithRegexFail() {
		FormValidation validation = ValidationUtil.validateWithRegex("field1", "value", "^[a-z]+[0-9]+$");
		Assert.assertNotEquals(FormValidation.ok(), validation);
		Assert.assertEquals("field1 is not in the correct format", validation.getMessage());
	}
}
