/*******************************************************************************
 * Copyright (c) 2016-2019 Embedded Systems and Applications Group
 * Department of Computer Science, Technische Universitaet Darmstadt,
 * Hochschulstr. 10, 64289 Darmstadt, Germany.
 * <p>
 * All rights reserved.
 * <p>
 * This software is provided free for educational use only.
 * It may not be used for commercial purposes without the
 * prior written permission of the authors.
 ******************************************************************************/
package mavlc.context_analysis;

import mavlc.syntax.statement.Declaration;

/* TODO enter group information
 *
 * EiCB group number: ...
 * Names and matriculation numbers of all group members:
 * ...
 */

/**
 * A table for identifiers used inside a function.
 */
public class IdentificationTable {

	// The current scope in which identifiers are declared (task 2.1)
	// we use a stack of scopes to represent the scoping of identifiers
	// @author adham-elaraby
	private Scope currentScope = null;

	/**
	 * Declares the given identifier in the current scope.
	 *
	 * @param name the identifier to declare
	 * @param declaration the reference to the identifier's declaration site
	 */
	public void addIdentifier(String name, Declaration declaration) {
		// Task 2.1: @author adham-elaraby
		// Ensure there is an open scope before adding the identifier
		if (currentScope!=null){
			currentScope.addIdentifier(name, declaration);
		}
		// One idea is to use InternalCompilerError to signal an internal error in the compiler implementation.
		// but this adds an import which I think is not allowed
		// TODO: remove or change this error
		else throw new IllegalStateException("no open scope");
	}
	
	/**
	 * Looks up the innermost declaration of the given identifier.
	 *
	 * @param name the identifier to look up
	 * @return the identifier's innermost declaration site
	 */
	public Declaration getDeclaration(String name) {
		// Task 2.1: @author adham-elaraby
		// Ensure there is an open scope before looking up the identifier
		if (currentScope!=null) {
			return currentScope.getDeclaration(name);
		}

		// TODO: remove or change this error
		else throw new IllegalStateException("no open scope");
	}
	
	/**
	 * Opens a new scope.
	 */
	public void openNewScope() {
		// Task 2.1: @author adham-elaraby
		// Create a new scope and set it as the current scope, with the current scope as its parent

		// check added to add logic to handle the case of the first scope
		if (currentScope==null){
			currentScope=new Scope(null);
		} else {
			currentScope = new Scope(currentScope);
		}
	}
	
	/**
	 * Closes the current scope.
	 */
	public void closeCurrentScope() {
		// Task 2.1: @author adham-elaraby
		// Ensure there is an open scope before closing it
		if (currentScope!=null){
			// Move to the parent scope
			currentScope=currentScope.parentScope;
		}
		// TODO: remove or change this error
		else throw new IllegalStateException("No scope to close");
	}
}
