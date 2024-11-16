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
package mavlc.parsing;

import mavlc.errors.SyntaxError;
import mavlc.syntax.SourceLocation;
import mavlc.syntax.expression.*;
import mavlc.syntax.function.FormalParameter;
import mavlc.syntax.function.Function;
import mavlc.syntax.module.Module;
import mavlc.syntax.record.RecordElementDeclaration;
import mavlc.syntax.record.RecordTypeDeclaration;
import mavlc.syntax.statement.*;
import mavlc.syntax.type.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import static mavlc.parsing.Token.TokenType.*;
import static mavlc.syntax.expression.Compare.Comparison.*;

/* TODO enter group information
 *
 * EiCB group number: ...
 * Names and matriculation numbers of all group members:
 * ...
 */

/**
 * A recursive-descent parser for MAVL.
 */
public final class Parser {
	
	private final Deque<Token> tokens;
	private Token currentToken;
	
	/**
	 * @param tokens A token stream that was produced by the {@link Scanner}.
	 */
	public Parser(Deque<Token> tokens) {
		this.tokens = tokens;
		currentToken = tokens.poll();
	}
	
	/**
	 * Parses the MAVL grammar's start symbol, Module.
	 *
	 * @return A {@link Module} node that is the root of the AST representing the tokenized input program.
	 * @throws SyntaxError to indicate that an unexpected token was encountered.
	 */
	public Module parse() {
		SourceLocation location = currentToken.sourceLocation;
		
		List<Function> functions = new ArrayList<>();
		List<RecordTypeDeclaration> records = new ArrayList<>();
		while(currentToken.type != EOF) {
			switch(currentToken.type) {
				case FUNCTION:
					functions.add(parseFunction());
					break;
				case RECORD:
					records.add(parseRecordTypeDeclaration());
					break;
				default:
					throw new SyntaxError(currentToken, FUNCTION, RECORD);
			}
		}
		return new Module(location, functions, records);
	}
	
	private String accept(Token.TokenType type) {
		Token t = currentToken;
		if(t.type != type)
			throw new SyntaxError(t, type);
		acceptIt();
		return t.spelling;
	}
	
	private void acceptIt() {
		currentToken = tokens.poll();
		if(currentToken == null || currentToken.type == ERROR)
			throw new SyntaxError(currentToken != null ? currentToken : new Token(EOF, null, -1, -1));
	}
	
	private Function parseFunction() {
		SourceLocation location = currentToken.sourceLocation;

		accept(FUNCTION);
		TypeSpecifier<?> typeSpecifier = parseTypeSpecifier();
		String name = accept(ID);
		
		List<FormalParameter> parameters = new ArrayList<>();
		List<Statement> body = new ArrayList<>();
		
		accept(LPAREN);
		if(currentToken.type != RPAREN) {
			parameters.add(parseFormalParameter());
			while(currentToken.type != RPAREN) {
				accept(COMMA);
				parameters.add(parseFormalParameter());
			}
		}
		accept(RPAREN);
		
		accept(LBRACE);
		while(currentToken.type != RBRACE)
			body.add(parseStatement());
		accept(RBRACE);
		
		return new Function(location, name, typeSpecifier, parameters, body);
	}
	
	private FormalParameter parseFormalParameter() {
		SourceLocation location = currentToken.sourceLocation;
		
		TypeSpecifier<?> typeSpecifier = parseTypeSpecifier();
		String name = accept(ID);
		
		return new FormalParameter(location, name, typeSpecifier);
	}
	
	private RecordTypeDeclaration parseRecordTypeDeclaration() {
		// TODO implement method (task 1.7)
		throw new UnsupportedOperationException();
	}
	
	private RecordElementDeclaration parseRecordElementDeclaration() {
		// TODO implement method (task 1.7)
		throw new UnsupportedOperationException();
	}
	
	private IteratorDeclaration parseIteratorDeclaration() {
		SourceLocation location = currentToken.sourceLocation;
		
		boolean isVariable;
		switch(currentToken.type) {
			case VAL:
				accept(VAL);
				isVariable = false;
				break;
			case VAR:
				accept(VAR);
				isVariable = true;
				break;
			default:
				throw new SyntaxError(currentToken, VAL, VAR);
		}
		TypeSpecifier<?> typeSpecifier = parseTypeSpecifier();
		String name = accept(ID);
		return new IteratorDeclaration(location, name, typeSpecifier, isVariable);
	}
	
	private TypeSpecifier<?> parseTypeSpecifier() {
		SourceLocation location = currentToken.sourceLocation;
		
		boolean vector = false;
		switch(currentToken.type) {
			case INT:
				acceptIt();
				return new IntTypeSpecifier(location);
			case FLOAT:
				acceptIt();
				return new FloatTypeSpecifier(location);
			case BOOL:
				acceptIt();
				return new BoolTypeSpecifier(location);
			case VOID:
				acceptIt();
				return new VoidTypeSpecifier(location);
			case STRING:
				acceptIt();
				return new StringTypeSpecifier(location);
			case VECTOR:
				accept(VECTOR);
				vector = true;
				break;
			case MATRIX:
				accept(MATRIX);
				break;
			case ID:
				String name = accept(ID);
				return new RecordTypeSpecifier(location, name);
			default:
				throw new SyntaxError(currentToken, INT, FLOAT, BOOL, VOID, STRING, VECTOR, MATRIX, ID);
		}
		
		accept(LANGLE);
		TypeSpecifier<?> subtype;
		switch(currentToken.type) {
			case INT:
				subtype = new IntTypeSpecifier(currentToken.sourceLocation);
				break;
			case FLOAT:
				subtype = new FloatTypeSpecifier(currentToken.sourceLocation);
				break;
			default:
				throw new SyntaxError(currentToken, INT, FLOAT);
		}
		acceptIt();
		accept(RANGLE);
		accept(LBRACKET);
		Expression x = parseExpr();
		accept(RBRACKET);
		
		if(vector)
			return new VectorTypeSpecifier(location, subtype, x);
		
		accept(LBRACKET);
		Expression y = parseExpr();
		accept(RBRACKET);
		
		return new MatrixTypeSpecifier(location, subtype, x, y);
	}
	
	private Statement parseStatement() {
		switch(currentToken.type) {
			case VAL:
				return parseValueDef();
			case VAR:
				return parseVarDecl();
			case RETURN:
				return parseReturn();
			case ID:
				return parseAssignOrCall();
			case FOR:
				return parseFor();
			case FOREACH:
				return parseForEach();
			case IF:
				return parseIf();
			case SWITCH:
				return parseSwitch();
			case LBRACE:
				return parseCompound();
			default:
				throw new SyntaxError(currentToken, VAL, VAR, RETURN, ID, FOR, FOREACH, IF, SWITCH, LBRACE);
		}
	}

	/**
	 * Task 1.1: Parses a value definition in the form `val` type ID '=' expr ';'.
	 * Example: `val int x = 42;`
	 * @return A `ValueDefinition` object representing the parsed value definition.
	 *
	 * @author adham-elaraby
	 */
	private ValueDefinition parseValueDef() {
		SourceLocation location = currentToken.sourceLocation;

		accept(VAL);
		TypeSpecifier<?> typeSpecifier = parseTypeSpecifier();
		String name = accept(ID);
		accept(ASSIGN);
		// Parse the expression that provides the value for the definition (i.e. after '=')
		Expression expression = parseExpr();
		accept(SEMICOLON);

		return new ValueDefinition(location,typeSpecifier, name, expression);
	}

	/**
	 * Task 1.1: Parses a variable declaration in the form `var` type ID ';'.
	 * Example: `var int x;`
	 *
	 * @return A `VariableDeclaration` object representing the parsed variable declaration.
	 *
	 * @author adham-elaraby
	 */
	private VariableDeclaration parseVarDecl() {
		SourceLocation location = currentToken.sourceLocation;

		accept(VAR);
		// Parse the type specifier (e.g., `int`, `float`, or any other supported type).
		TypeSpecifier<?> typeSpecifier = parseTypeSpecifier();
		String name = accept(ID);
		accept(SEMICOLON);

		return new VariableDeclaration(location, typeSpecifier, name);
	}
	
	private ReturnStatement parseReturn() {
		// TODO implement method (task 1.6)
		throw new UnsupportedOperationException();
	}
	
	private Statement parseAssignOrCall() {
		SourceLocation location = currentToken.sourceLocation;

		String name = accept(ID);

		Statement s;
		if(currentToken.type != LPAREN) {
			s = parseAssign(name, location);
		}
		else {
			// TODO extend method (task 1.6)
			throw new UnsupportedOperationException();
		}


		accept(SEMICOLON);

		return s;
	}

	/**
	 * Task 1.1: Parses the right-hand side (RHS) of an assignment to a variable.
	 * Grammar rule:
	 * ID ('[' expr ']' ('[' expr ']')? | '@' ID)? '=' expr ';'
	 * This handles assignments to variables, vectors, matrices, and record fields.
	 *
	 * @param name The name of the variable being assigned.
	 * @param location The source location of the assignment in the code for error reporting.
	 * @return A `VariableAssignment` object representing the parsed assignment.
	 *
	 * @author adham-elaraby
	 */
	private VariableAssignment parseAssign(String name, SourceLocation location) {
		LeftHandIdentifier lhs;

		// Determine the structure
		switch (currentToken.type) {
			// Case for indexed access, e.g., vector[0] or matrix[0][0].
			case LBRACKET:
				// vector[0] = 0
				// we consume the '[' token, parse the index expression inside the brackets.
				acceptIt();
				Expression x = parseExpr();
				accept(RBRACKET); // Ensure the closing ']' is present.

				if (currentToken.type == LBRACKET) {
					// Case for matrix access, e.g.,
					// matrix[0][0] = 0
					acceptIt();

					lhs = new MatrixLhsIdentifier(location, name, x, parseSelect());
					accept(RBRACKET);
				} else {
					// Case for vector access...
					lhs = new VectorLhsIdentifier(location, name, x);
				}
				break;

			// Case for record field access, e.g., struct@num = 0
			case AT:
				acceptIt();
				lhs = new RecordLhsIdentifier(location, name, accept(ID));
				break;

			// Default case for simple variable assignment, e.g., variable = value
			default:
				// variable = 0
				lhs = new LeftHandIdentifier(location, name);
				break;
		}

		accept(ASSIGN);
		Expression expr = parseExpr(); // Parse the RHS expression.


		return new VariableAssignment(location, lhs, expr);
	}
	
	private CallExpression parseCall(String name, SourceLocation location) {
		// TODO implement method (task 1.6)
		throw new UnsupportedOperationException();
	}
	
	private ForLoop parseFor() {
		SourceLocation location = currentToken.sourceLocation;
		
		accept(FOR);
		accept(LPAREN);
		String name = accept(ID);
		accept(ASSIGN);
		Expression a = parseExpr();
		accept(SEMICOLON);
		Expression b = parseExpr();
		accept(SEMICOLON);
		String inc = accept(ID);
		accept(ASSIGN);
		Expression c = parseExpr();
		accept(RPAREN);
		return new ForLoop(location, name, a, b, inc, c, parseStatement());
	}
	
	private ForEachLoop parseForEach() {
		SourceLocation location = currentToken.sourceLocation;
		
		accept(FOREACH);
		accept(LPAREN);
		IteratorDeclaration param = parseIteratorDeclaration();
		accept(COLON);
		Expression struct = parseExpr();
		accept(RPAREN);
		return new ForEachLoop(location, param, struct, parseStatement());
	}

	/**
	 * Task 1.3: if-statements
	 * Grammar rule:
	 * ’if’ ’(’ expr ’)’ statement ( ’else’ statement )?
	 *
	 * @return An `IfStatement` object.
	 *
	 * @author adham-elaraby
	 */
	private IfStatement parseIf() {
		SourceLocation location = currentToken.sourceLocation;

		accept(IF);
		accept(LPAREN);
		Expression condition = parseExpr();
		accept(RPAREN);
		Statement thenStatement = parseStatement();

		// if there is no else statement, we're done here.
		if (currentToken.type != ELSE) {
			return new IfStatement(location, condition, thenStatement, null);
		}
		else {
			// parse else statement
			acceptIt();
			Statement elseStatement = parseStatement();
			return new IfStatement(location, condition, thenStatement, elseStatement);
		}
	}
	
	private SwitchStatement parseSwitch() {
		SourceLocation location = currentToken.sourceLocation;
		accept(SWITCH);
		accept(LPAREN);
		Expression condition = parseExpr();
		accept(RPAREN);
		accept(LBRACE);
		
		List<Case> cases = new ArrayList<>();
		List<Default> defaults = new ArrayList<>();
		while(currentToken.type != RBRACE) {
			if(currentToken.type == CASE)
				cases.add(parseCase());
			else if(currentToken.type == DEFAULT)
				defaults.add(parseDefault());
			else
				throw new SyntaxError(currentToken, CASE, DEFAULT);
		}
		
		accept(RBRACE);
		return new SwitchStatement(location, condition, cases, defaults);
	}
	
	private Case parseCase() {
		SourceLocation location = currentToken.sourceLocation;
		
		accept(CASE);
		Expression caseCond = parseExpr();
		accept(COLON);
		Statement stmt = parseStatement();
		return new Case(location, caseCond, stmt);
	}
	
	private Default parseDefault() {
		SourceLocation location = currentToken.sourceLocation;
		
		accept(DEFAULT);
		accept(COLON);
		return new Default(location, parseStatement());
	}
	
	private CompoundStatement parseCompound() {
		SourceLocation location = currentToken.sourceLocation;
		
		List<Statement> statements = new ArrayList<>();
		accept(LBRACE);
		while(currentToken.type != RBRACE)
			statements.add(parseStatement());
		accept(RBRACE);
		
		return new CompoundStatement(location, statements);
	}
	
	private Expression parseExpr() {
		return parseSelect();
	}
	
	private Expression parseSelect() {
		SourceLocation location = currentToken.sourceLocation;
		
		Expression cond = parseOr();
		// TODO extend method (task 1.5)
		return cond;
	}
	
	private Expression parseOr() {
		SourceLocation location = currentToken.sourceLocation;
		
		Expression x = parseAnd();
		while(currentToken.type == OR) {
			acceptIt();
			x = new Or(location, x, parseAnd());
		}
		return x;
	}
	
	private Expression parseAnd() {
		SourceLocation location = currentToken.sourceLocation;
		
		Expression x = parseNot();
		while(currentToken.type == AND) {
			acceptIt();
			x = new And(location, x, parseNot());
		}
		return x;
	}
	
	private Expression parseNot() {
		// TODO extend method (task 1.2)
		
		return parseCompare();
	}
	
	private Expression parseCompare() {
		SourceLocation location = currentToken.sourceLocation;
		
		Expression x = parseAddSub();

		// TODO extend method (task 1.2)
		return x;
	}
	
	private Expression parseAddSub() {
		SourceLocation location = currentToken.sourceLocation;

		Expression x = parseMulDiv();
		// TODO extend method (task 1.2)
		return x;
	}
	
	private Expression parseMulDiv() {
		SourceLocation location = currentToken.sourceLocation;

		Expression x = parseUnaryMinus();
		// TODO extend method (task 1.2)
		return x;
	}
	
	private Expression parseUnaryMinus() {
		// TODO extend method (task 1.2)
		return parseExponentiation();
	}
	
	private Expression parseExponentiation() {
		SourceLocation location = currentToken.sourceLocation;

		Expression left = parseDotProd();
		// TODO extend method (task 1.2)
		return left;
	}
	
	private Expression parseDotProd() {
		SourceLocation location = currentToken.sourceLocation;
		
		Expression x = parseMatrixMul();
		while(currentToken.type == DOTPROD) {
			acceptIt();
			x = new DotProduct(location, x, parseMatrixMul());
		}
		return x;
	}
	
	private Expression parseMatrixMul() {
		SourceLocation location = currentToken.sourceLocation;
		
		Expression x = parseTranspose();
		while(currentToken.type == MATMULT) {
			acceptIt();
			x = new MatrixMultiplication(location, x, parseTranspose());
		}
		return x;
	}
	
	private Expression parseTranspose() {
		// TODO extend method (task 1.2)
		return parseDim();
	}
	
	private Expression parseDim() {
		SourceLocation location = currentToken.sourceLocation;
		
		Expression x = parseSubRange();
		switch(currentToken.type) {
			case ROWS:
				acceptIt();
				return new MatrixRows(location, x);
			case COLS:
				acceptIt();
				return new MatrixCols(location, x);
			case DIM:
				acceptIt();
				return new VectorDimension(location, x);
			default:
				return x;
		}
	}
	
	private Expression parseSubRange() {
		SourceLocation location = currentToken.sourceLocation;
		
		Expression x = parseElementSelect();
		
		if(currentToken.type == LBRACE) {
			acceptIt();
			Expression xStartIndex = parseExpr();
			accept(COLON);
			Expression xBaseIndex = parseExpr();
			accept(COLON);
			Expression xEndIndex = parseExpr();
			accept(RBRACE);
			if(currentToken.type != LBRACE)
				return new SubVector(location, x, xBaseIndex, xStartIndex, xEndIndex);
			
			accept(LBRACE);
			Expression yStartIndex = parseExpr();
			accept(COLON);
			Expression yBaseIndex = parseExpr();
			accept(COLON);
			Expression yEndIndex = parseExpr();
			accept(RBRACE);
			return new SubMatrix(location, x, xBaseIndex, xStartIndex, xEndIndex, yBaseIndex, yStartIndex, yEndIndex);
		}
		
		return x;
	}
	
	private Expression parseElementSelect() {
		SourceLocation location = currentToken.sourceLocation;
		
		Expression x = parseRecordElementSelect();
		
		while(currentToken.type == LBRACKET) {
			acceptIt();
			Expression idx = parseExpr();
			accept(RBRACKET);
			x = new ElementSelect(location, x, idx);
		}
		
		return x;
	}
	
	private Expression parseRecordElementSelect() {
		SourceLocation location = currentToken.sourceLocation;
		
		Expression x = parseAtom();

		// TODO extend method (task 1.7)

		return x;
	}
	
	private Expression parseAtom() {
		SourceLocation location = currentToken.sourceLocation;
		
		switch(currentToken.type) {
			case INTLIT:
				return new IntValue(location, parseIntLit());
			case FLOATLIT:
				return new FloatValue(location, parseFloatLit());
			// TODO extend method (task 1.4)
			case STRINGLIT:
				return new StringValue(location, accept(STRINGLIT));
			default: /* check other cases below */
		}

		if(currentToken.type == ID) {
			String name = accept(ID);
			if(currentToken.type != LPAREN) {
				return new IdentifierReference(location, name);
			}
			// TODO extend method (task 1.6)
		}

		if(currentToken.type == LPAREN) {
			acceptIt();
			Expression x = parseExpr();
			accept(RPAREN);
			return x;
		}

		// TODO extend method (task 1.7)
		
		if(currentToken.type == LBRACKET) {
			return new StructureInit(location, parseInitializerList());
		}
		
		throw new SyntaxError(currentToken, INTLIT, FLOATLIT, BOOLLIT, STRINGLIT, ID, LPAREN, LBRACKET, AT);
	}
	
	private List<Expression> parseInitializerList() {
		List<Expression> elements = new ArrayList<>();
		
		accept(LBRACKET);
		elements.add(parseExpr());
		while(currentToken.type == COMMA) {
			accept(COMMA);
			elements.add(parseExpr());
		}
		accept(RBRACKET);
		
		return elements;
	}
	
	private int parseIntLit() {
		return Integer.parseInt(accept(INTLIT));
	}
	
	private float parseFloatLit() {
		return Float.parseFloat(accept(FLOATLIT));
	}
	
	private boolean parseBoolLit() {
		// TODO implement method (task 1.4)
		throw new UnsupportedOperationException();
	}
}
