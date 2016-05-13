package org.fogbowcloud.scheduler.infrastructure.fogbow;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.fogbowcloud.scheduler.core.model.Resource;

import condor.classad.AttrRef;
import condor.classad.ClassAdParser;
import condor.classad.Env;
import condor.classad.Expr;
import condor.classad.Op;
import condor.classad.RecordExpr;

public class FogbowRequirementsHelper {

	private static final Logger LOGGER = Logger.getLogger(FogbowRequirementsHelper.class);
	
	// ------------------ CONSTANTS ------------------//
	protected static final String ZERO = "0";

	public static final String METADATA_FOGBOW_RESOURCE_KIND = "compute";
	public static final String METADATA_FOGBOW_REQUIREMENTS = "FogbowRequirements";
	public static final String METADATA_FOGBOW_REQUIREMENTS_Glue2vCPU = "Glue2vCPU";
	public static final String METADATA_FOGBOW_REQUIREMENTS_Glue2RAM = "Glue2RAM";
	public static final String METADATA_FOGBOW_REQUIREMENTS_Glue2disk = "Glue2disk";
	public static final String METADATA_FOGBOW_REQUIREMENTS_1Glue2CloudComputeManagerID = "Glue2CloudComputeManagerID";
	public static final String METADATA_FOGBOW_REQUEST_TYPE = "RequestType";
	
	public static boolean validateFogbowRequirementsSyntax(String requirementsString) {
		
		LOGGER.debug("Validating Fogbow Requirements ["+requirementsString+"]");
		
		if (requirementsString == null || requirementsString.isEmpty()) {
			
			LOGGER.debug("Fogbow Requirements ["+requirementsString+"] Validate with sucess.");
			return true;
		}
		try {
			ClassAdParser adParser = new ClassAdParser(requirementsString);
			if (adParser.parse() != null) {
			
				LOGGER.debug("Fogbow Requirements ["+requirementsString+"] Validate with sucess.");
				return true;
			}
			LOGGER.info("Fogbow Requirements ["+requirementsString+"] Invalid - Expression not found.");
			return false;
		} catch (Exception e) {
			LOGGER.error("Fogbow Requirements ["+requirementsString+"] Invalid", e);
			return false;
		}
	}

	public static boolean matches(Resource resource, String requirementsStr) {
		
		LOGGER.debug("Matching Fogbow Requirements [" + requirementsStr + "] with Resource [id: "
				+ resource.getId() + "]");
		
		List<String> listAttrSearched = new ArrayList<String>();
		List<String> listAttrProvided = new ArrayList<String>();
		
		try {
			if (requirementsStr == null  || requirementsStr.trim().isEmpty()) {
				return true;
			}
			
			ClassAdParser classAdParser = new ClassAdParser(requirementsStr);		
			Op expr = (Op) classAdParser.parse();
			
			listAttrProvided.add(METADATA_FOGBOW_REQUIREMENTS_Glue2vCPU);
			listAttrProvided.add(METADATA_FOGBOW_REQUIREMENTS_Glue2RAM);
			listAttrProvided.add(METADATA_FOGBOW_REQUIREMENTS_Glue2disk);  
			listAttrProvided.add(METADATA_FOGBOW_REQUIREMENTS_1Glue2CloudComputeManagerID); 
			
			Env env = new Env();
			String value = null;
			for (String attr : listAttrProvided) {
				
				List<ValueAndOperator> findValuesInRequiremets = findValuesInRequiremets(expr, attr);
				
				if (findValuesInRequiremets.size() > 0) {
					
					if (attr.equals(METADATA_FOGBOW_REQUIREMENTS_Glue2vCPU)) {
						listAttrSearched.add(attr);
						value = resource.getMetadataValue(resource.METADATA_VCPU);
					} 
					else if (attr.equals(METADATA_FOGBOW_REQUIREMENTS_Glue2RAM)) {
						listAttrSearched.add(attr);
						value = resource.getMetadataValue(resource.METADATA_MEN_SIZE);
					} 
					else if (attr.equals(METADATA_FOGBOW_REQUIREMENTS_Glue2disk)) {
						value = resource.getMetadataValue(resource.METADATA_DISK_SIZE);
						if (value != null && !value.equals(ZERO) ) {
							listAttrSearched.add(attr);							
						}
					} 
					else if (attr.equals(METADATA_FOGBOW_REQUIREMENTS_1Glue2CloudComputeManagerID)) {
						listAttrSearched.add(attr);
						value = resource.getMetadataValue(resource.METADATA_LOCATION);
					}
					
					env.push((RecordExpr) new ClassAdParser("[" + attr + " = " + value + "]").parse());
					LOGGER.debug("Matching Requirement [" + attr + " = " + value + "]");
				}
			}					
			
			if (listAttrSearched.isEmpty()) {
				return true;
			}
			expr = extractVariablesExpression(expr, listAttrSearched);
			
			return expr.eval(env).isTrue();
		} catch (Exception e) {
			LOGGER.error("Matching Fogbow Requirements ["+requirementsStr+"] with Resource [id: "+resource.getId()+"] FAILED", e);
			return false;
		}finally {
			listAttrSearched.clear();
			listAttrProvided.clear();
		}
	}
	
	private static Op extractVariablesExpression(Op expr, List<String> listAttName) {
		if (expr.arg1 instanceof AttrRef) {
			AttrRef attr = (AttrRef) expr.arg1;
			boolean thereIs = false;
			for (String attName : listAttName) {
				if (attr.name.rawString().equals(attName)) {
					thereIs = true;
				}
			}
			if (thereIs) {
				return expr;				
			}
			return null;
		}
		Expr left = expr.arg1;
		if (left instanceof Op) {
			left = extractVariablesExpression((Op) expr.arg1, listAttName);
		}
		Expr right = expr.arg2;
		if (right instanceof Op) {
			right = extractVariablesExpression((Op) expr.arg2, listAttName);
		}
		try {
			if (left == null) {
				return (Op) right;
			} else if (right == null) {
				return (Op) left;
			}			
		} catch (Exception e) {
			return null;
		}
		return new Op(expr.op, left, right);
	}

	private static List<ValueAndOperator> findValuesInRequiremets(Op expr, String attName) {
		List<ValueAndOperator> valuesAndOperator = new ArrayList<ValueAndOperator>();
		if (expr.arg1 instanceof AttrRef) {
			AttrRef attr = (AttrRef) expr.arg1;
			if (attr.name.rawString().equals(attName)) {
				valuesAndOperator.add(new ValueAndOperator(expr.arg2.toString(), expr.op));
			}
			return valuesAndOperator;
		}
		if (expr.arg1 instanceof Op) {
			List<ValueAndOperator> findValuesInRequiremets = findValuesInRequiremets(
					(Op) expr.arg1, attName);
			if (findValuesInRequiremets != null) {
				valuesAndOperator.addAll(findValuesInRequiremets);
			}
		}
		if (expr.arg2 instanceof Op) {
			List<ValueAndOperator> findValuesInRequiremets = findValuesInRequiremets(
					(Op) expr.arg2, attName);
			if (findValuesInRequiremets != null) {
				valuesAndOperator.addAll(findValuesInRequiremets);
			}
		}
		return valuesAndOperator;
	}


	protected static class ValueAndOperator {
		private String value;
		private int operator;

		public ValueAndOperator(String value, int operator) {
			this.value = value;
			this.operator = operator;
		}

		public int getOperator() {
			return operator;
		}

		public String getValue() {
			return value;
		}
	}
	
}
