package org.repositoryminer.metrics.codemetric;

import java.util.HashSet;
import java.util.Set;

import org.repositoryminer.metrics.ast.AST;
import org.repositoryminer.metrics.ast.AbstractFieldAccess;
import org.repositoryminer.metrics.ast.AbstractMethod;
import org.repositoryminer.metrics.ast.AbstractMethodInvocation;
import org.repositoryminer.metrics.ast.AbstractStatement;
import org.repositoryminer.metrics.ast.AbstractType;
import org.repositoryminer.metrics.ast.NodeType;
import org.repositoryminer.metrics.report.ClassReport;
import org.repositoryminer.metrics.report.FileReport;
import org.repositoryminer.metrics.report.MethodReport;
import org.repositoryminer.metrics.report.ProjectReport;

public class FDP extends CodeMetric {

	public FDP() {
		super.id = CodeMetricId.FDP;
	}
	
	@Override
	public void calculate(AST ast, FileReport fileReport, ProjectReport projectReport) {
		for (AbstractType type : ast.getTypes()) {
			ClassReport cr = fileReport.getClass(type.getName());
			for (AbstractMethod method : type.getMethods()) {
				MethodReport mr = cr.getMethodBySignature(method.getName());
				mr.getMetricsReport().setCodeMetric(CodeMetricId.FDP, calculate(type, method));
			}
		}
	}
	
	public int calculate(AbstractType currType, AbstractMethod method) {
		Set<String> accessedClasses = new HashSet<String>();
		for (AbstractStatement stmt : method.getStatements()) {
			String declarringClass = null;

			if (stmt.getNodeType() == NodeType.FIELD_ACCESS) {
				AbstractFieldAccess fieldAccess = (AbstractFieldAccess) stmt;
				declarringClass = fieldAccess.getDeclaringClass();
			} else if (stmt.getNodeType() == NodeType.METHOD_INVOCATION) {
				AbstractMethodInvocation methodInvocation = (AbstractMethodInvocation) stmt;
				if (!methodInvocation.isAccessor()) {
					continue;
				}
				declarringClass = methodInvocation.getDeclaringClass();
			} else {
				continue;
			}

			if (!currType.getName().equals(declarringClass)) {
				accessedClasses.add(declarringClass);
			}
		}

		return accessedClasses.size();
	}

	@Override
	public void clean(ProjectReport projectReport) {}

}