/**
 * 
 */
package redelegation;
import java.util.HashSet;
import java.util.Set;

import lombok.ast.ForwardingAstVisitor;
import lombok.ast.Node;
import lombok.ast.MethodDeclaration;
import lombok.ast.MethodInvocation;
import lombok.ast.ConstructorInvocation;
import lombok.ast.StringLiteral;

public class IntentVisitor extends ForwardingAstVisitor {
	Set<String> mIntentsSent;
	Set<String> mIntentsReceived;
	Set<String> mAllStrings;

	public IntentVisitor(
			Set<String> intentsSent,
			Set<String> intentsReceived, 
			Set<String> allStrings) {
		this.mIntentsSent = intentsSent;
		this.mIntentsReceived = intentsReceived;
		this.mAllStrings = allStrings;
	}
	
	@Override
	public void endVisit(Node node) {
	}
	
	@Override 
	public boolean visitStringLiteral(StringLiteral node) {
		mAllStrings.add(node.astValue().toString());
		return false;
	}

	@Override
	public boolean visitMethodInvocation(MethodInvocation methodInvoc) {

		if (methodInvoc.astName().toString().equals("setAction")) {
			if (methodInvoc.astArguments().size() > 0) {
				String className = methodInvoc.astArguments().first().getClass().getName();
				String intentName = methodInvoc.astArguments().first().toString();
				
				if (className.equals("lombok.ast.VariableReference")) {
					mIntentsSent.add(intentName);
				}
				else if (className.equals("lombok.ast.Select") && intentName.startsWith("Intent.")) {
					mIntentsSent.add(intentName.replace("Intent.", ""));
				}
			}
		}
		else if (methodInvoc.astName().toString().equals("addAction")) {
			if (methodInvoc.astArguments().size() > 0) {
				String className = methodInvoc.astArguments().first().getClass().getName();
				String intentName = methodInvoc.astArguments().first().toString();
				
				if (className.equals("lombok.ast.VariableReference")) {
					mIntentsReceived.add(intentName);
				}
				else if (className.equals("lombok.ast.Select") && intentName.startsWith("Intent.")) {
					mIntentsReceived.add(intentName.replace("Intent.", ""));
				}
			}
		}
		return false;
	}
	
	@Override
	public boolean visitConstructorInvocation(ConstructorInvocation constructorInvocation) {
		if (constructorInvocation.getDescription().toString().equals("Intent")) {
			if (constructorInvocation.astArguments().size() > 0) {
				String className = constructorInvocation.astArguments().first().getClass().getName();
				String intentName = constructorInvocation.astArguments().first().toString();
				
				if (className.equals("lombok.ast.VariableReference")) {
					mIntentsSent.add(intentName);
				}
				else if (className.equals("lombok.ast.Select") && intentName.startsWith("Intent.")) {
					mIntentsSent.add(intentName.replace("Intent.", ""));
				}
			}
		}	
		else if (constructorInvocation.getDescription().toString().equals("IntentFilter")) {
			if (constructorInvocation.astArguments().size() > 0) {
				String className = constructorInvocation.astArguments().first().getClass().getName();
				String intentName = constructorInvocation.astArguments().first().toString();
				if (className.equals("lombok.ast.VariableReference")) {
					mIntentsReceived.add(intentName);
				}
				else if (className.equals("lombok.ast.Select") && intentName.startsWith("Intent.")) {
					mIntentsReceived.add(intentName.replace("Intent.", ""));
				}
			}
		}
		
		return false;
	}
}
