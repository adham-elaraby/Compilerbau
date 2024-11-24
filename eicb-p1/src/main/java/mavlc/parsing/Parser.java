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

	/**
	 * Task 1.7:
	 * Grammar rule:
	 * ’record’ ID ’{’ recordElemDecl+ ’}’
	 *
	 * @author adham-elaraby, onlyvalli (col)
	 */
	private RecordTypeDeclaration parseRecordTypeDeclaration() throws SyntaxError {
		SourceLocation location = currentToken.sourceLocation;

		accept(RECORD);
		String name = accept(ID);
		accept(LBRACE);

		List<RecordElementDeclaration> recordElementDeclarations = new ArrayList<>();

		// Ensure at least one RecordElementDeclaration is parsed before entering the loop.
		if (currentToken.type == RBRACE) {
			throw new SyntaxError(currentToken, VAR, VAL);
		}

		// we do this here to ensure that at least one RecordElementDeclaration is parsed before entering the loop.
		// This is necessary because the grammar rule for a record type declaration requires at least one recordElemDecl.
		// just for safety, we could also check if the current token is a right brace, and throw an error if it is.
		recordElementDeclarations.add(parseRecordElementDeclaration());
		while(currentToken.type != RBRACE) {
			recordElementDeclarations.add(parseRecordElementDeclaration());
		}

		accept(RBRACE);

		return new RecordTypeDeclaration(location, name, recordElementDeclarations);
	}

	/**
	 * Task 1.7:
	 * Grammar rule:
	 * ( ’var’ | ’val’ ) type ID ’;’
	 *
	 * @author adham-elaraby, onlyvalli (col)
	 */

	private RecordElementDeclaration parseRecordElementDeclaration() {
		// RecordElementDeclaration takes in the following parameters:
		// sourceLocation, isVariable, typeSpecifier, name

		SourceLocation location = currentToken.sourceLocation;

		// RecordElementDeclaration can start with either 'var' or 'val'.
		// We need to check which one it is, and then deal with it accordingly.

		// enhanced switch statement recommended by IntelliJ IDEA.
		boolean isVariable = switch (currentToken.type) {
            case VAR -> {
                acceptIt();
                yield true;
            }
            case VAL -> {
                acceptIt();
                yield false;
            }
			// if it's neither 'var' nor 'val', there's something wrong with the syntax.
            default -> throw new SyntaxError(currentToken, VAL, VAR);
        };

        TypeSpecifier<?> typeSpecifier = parseTypeSpecifier();
		String name = accept(ID);
		accept(SEMICOLON);

		return new RecordElementDeclaration(location, isVariable, typeSpecifier, name);
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
	 * @author adham-elaraby, onlyvalli (col)
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
	 * @author adham-elaraby, onlyvalli (col)
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

	/**
	 * Task 1.6: Parses a return statement.
	 * Grammar rule:
	 * ’return’ expr ’;’
	 *
	 * @author adham-elaraby, sebherz (col)
	 */
	private ReturnStatement parseReturn() {
		SourceLocation location = currentToken.sourceLocation;

		// we look for the 'return' keyword.
		accept(RETURN);

		// we parse the expression that follows the 'return' keyword.
		Expression expr = parseExpr();

		// we look for the semicolon that ends the return statement.
		accept(SEMICOLON);

		return new ReturnStatement(location, expr);
	}

	/**
	 * Task 1.6: Parses an assignment or a function call.
	 * Grammar rule:
	 * ID ( assign | call) ’;’
	 *
	 * @author adham-elaraby, sebherz (col)
	 */
	private Statement parseAssignOrCall() {
		SourceLocation location = currentToken.sourceLocation;

		String name = accept(ID);

		Statement s;
		if(currentToken.type != LPAREN) {
			s = parseAssign(name, location);
		}
		else {
			// if the current token is a left parenthesis, it means we have a function call.
			// .. so we need to parse the function call, and get the statement object.
			s = new CallStatement(location, parseCall(name, location));
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
	 * @author adham-elaraby, onlyvalli (col)
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

	/**
	 * Task 1.6: Parses a function call.
	 * Grammar rule:
	 * ’(’ ( expr ( ’,’ expr )* )? ’)’
	 *
	 * @param name name of the function to call
	 * @param location The source location of the assignment in the code for error reporting.
	 * @return a `CallExpression` object representing the parsed function call.
	 *
	 * @author adham-elaraby, sebherz, onlyvalli (col)
	 */
	private CallExpression parseCall(String name, SourceLocation location) {
		// TODO: test for correctness @adham-elaraby
		// we look for the opening parenthesis of the function call.
		accept(LPAREN);

		// and we parse all the parameters within the parentheses, if any, and store them in a list.
		List<Expression> actualParameters = new ArrayList<>();
		if (currentToken.type != RPAREN) {
			actualParameters.add(parseExpr());
			while (currentToken.type != RPAREN) {
				// TEST
				// case found through private testing, if the current token is a semicolon, it means that the function call is missing a closing parenthesis.
				// without this check we would complain that we are expecting a comma and not a RPAREN.
				if (currentToken.type == SEMICOLON) {
					throw new SyntaxError(currentToken, RPAREN);
				}
				accept(COMMA);
				actualParameters.add(parseExpr());
			}
		}
		accept(RPAREN);

		return new CallExpression(location, name, actualParameters);
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
	 * @author adham-elaraby, onlyvalli (col)
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

	/**
	 * Task 1.5: select (ternary operator)
	 * Grammar rule:
	 * or (’?’ or ’:’ or)?
	 *
	 * @author adham-elaraby, sebherz, onlyvalli (col)
	 */
	private Expression parseSelect() {
		SourceLocation location = currentToken.sourceLocation;
		
		Expression cond = parseOr();

		// from Token.TokenType: ternary op is defined as: QMARK("?"),

		if (currentToken.type == QMARK) {
			// SelectExpression takes in the following parameters:
			// sourceLocation, condition, trueCase, falseCase
			// from mavlc.syntax.expression.SelectExpression

			acceptIt();
			Expression trueCase = parseOr();
			accept(COLON);
			Expression falseCase = parseOr();

			return new SelectExpression(location, cond, trueCase, falseCase);
		}

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

	/**
	 * Task 1.2
	 * Grammar: ’!’ ? compare
	 *
	 * @author adham-elaraby, onlyvalli (col)
	 */
	private Expression parseNot() {

		SourceLocation location = currentToken.sourceLocation;

		// create a negation if there is a NOT operator
		if (currentToken.type == NOT) {
			acceptIt();
			return new Not(location, parseCompare());
		}

		return parseCompare();
	}

	/**
	 * Helper method to check if the current token is a comparison operator, used by parseCompare()
	 * @param type The type of the token to check and see if it is a comparison operator.
	 * @return True if the token is a comparison operator, false otherwise.
	 *
	 * @author adham-elaraby
	 */
	private boolean isComparisonOperator(Token.TokenType type) {
		return type == Token.TokenType.RANGLE ||
				type == Token.TokenType.LANGLE ||
				type == Token.TokenType.CMPLE ||
				type == Token.TokenType.CMPGE ||
				type == Token.TokenType.CMPEQ ||
				type == Token.TokenType.CMPNE;
	}

	/**
	 * Task 1.2
	 * Grammar rule:
	 * addSub ( ( ’>’ | ’<’ | ’<=’ | ’>=’ | ’==’ | ’!=’ ) addSub )*
	 *
	 * @author adham-elaraby
	 */
	private Expression parseCompare() {
		SourceLocation location = currentToken.sourceLocation;

		// again we parse the left op of the comp. op which can be an addSub expression.
		Expression x = parseAddSub();

		// TODO: implement private tests fot this @adham-elaraby

		// and then we parse all the comparison operators we can find, and create a new node for each operation.
		while (isComparisonOperator(currentToken.type)) {
			// Determine the type of comparison
			Compare.Comparison type = switch (currentToken.type) {
                case RANGLE -> Compare.Comparison.GREATER;
                case LANGLE -> Compare.Comparison.LESS;
                case CMPLE -> Compare.Comparison.LESS_EQUAL;
                case CMPGE -> Compare.Comparison.GREATER_EQUAL;
                case CMPEQ -> Compare.Comparison.EQUAL;
                case CMPNE -> Compare.Comparison.NOT_EQUAL;
				// I don't think this should ever be reached, but we'll keep it here just in case, and because some default is required.
                default -> throw new IllegalStateException("Unexpected token type: " + currentToken.type);
            };

            // Consume the comparison operator token and parse the next operand
			acceptIt();
			x = new Compare(location, x, parseAddSub(), type);
		}


		return x;
	}

	/**
	 * Task 1.2
	 * Grammar rule:
	 * mulDiv ( ( ’+’ | ’-’ ) mulDiv )*
	 *
	 * @author adham-elaraby
	 */
	private Expression parseAddSub() {
		SourceLocation location = currentToken.sourceLocation;

		// we get the left operand of the addition or subtraction operator, which can be a multiplication or division expression.
		Expression x = parseMulDiv();

		// we parse all the addition and subtraction operators we can find, and create a new node for each operation.
		// and set the left operand to the previous result.
		while (currentToken.type == ADD || currentToken.type == SUB) {
			if (currentToken.type == ADD) {
				acceptIt();
				x = new Addition(location, x, parseMulDiv());
			} else if (currentToken.type == SUB) {
				acceptIt();
				x = new Subtraction(location, x, parseMulDiv());
			}
		}

		return x;
	}

	/**
	 * Task 1.2
	 * Grammar rule:
	 * unaryMinus ( ( ’*’ | ’/’ ) unaryMinus )*
	 *
	 * @author adham-elaraby
	 */
	private Expression parseMulDiv() {
		SourceLocation location = currentToken.sourceLocation;

		Expression x = parseUnaryMinus();

		// TODO: implement private tests fot this @adham-elaraby

		// we parse as many times as we can, as long as the current token is a multiplication or division operator.
		// we create a new node for each operation, and set the left operand to the previous result.
		while (currentToken.type == Token.TokenType.MULT || currentToken.type == Token.TokenType.DIV) {
			switch (currentToken.type) {
				case MULT:
					acceptIt();
					x = new Multiplication(location, x, parseUnaryMinus());
					break;
				case DIV:
					acceptIt();
					x = new Division(location, x, parseUnaryMinus());
					break;
				default:
					// This should never actually be reached because we checked in the while statement, but nevertheless we will keep it here.
					throw new IllegalStateException("Unexpected token type: " + currentToken.type);
			}
		}

		return x;
	}

	/**
	 * Task 1.2
	 * Grammar rule:
	 * ’-’ ? exponentation
	 *
	 * @author adham-elaraby
	 */
	private Expression parseUnaryMinus() {
		// TODO: implement private tests fot this @adham-elaraby
		SourceLocation location = currentToken.sourceLocation;

		// Check if the current token is the '-' operator, and if so, parse the next operand and create a UnaryMinus node.
		if (currentToken.type != SUB) {
			return parseExponentiation();
		}

		acceptIt();
		return new UnaryMinus(location, parseExponentiation());
	}

	/**
	 * Task 1.2
	 * Grammar rule:
	 * dotProd ( ’^’ dotProd )*
	 *
	 * @author adham-elaraby
	 */
	private Expression parseExponentiation() {
		SourceLocation location = currentToken.sourceLocation;

		// Parse the left operand of the exponentiation operator, which is a dot product, as per the grammar rule.
		Expression left = parseDotProd();

		// Check if the current token is the exponentiation operator '^', and if so, parse the right operand and create an Exponentiation node.
		if (currentToken.type != EXP) {
			return left;
		}
		else {
			acceptIt();
			return new Exponentiation(location, left, parseExponentiation());
		}
//		return left; // this was the original return statement before implementing the exponentiation operator.
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

	/**
	 * Task 1.2
	 * Grammar rule:
	 * ’~’ ? dim
	 *
	 * @author adham-elaraby
	 */
	private Expression parseTranspose() {
		// TODO: implement private tests for this @adham-elaraby
		SourceLocation location = currentToken.sourceLocation;

		// Check if the current token is the transpose operator '~'.
		if (currentToken.type == TRANSPOSE) {
			acceptIt();
			// parse the following dim expression and wrap it in a matrix transpose node.
			return new MatrixTranspose(location, parseDim());
		}

		// if there ends up being no '~', we directly parse and return next dim expression.
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

	/**
	 * Task 1.7:
	 * Grammar rule:
	 * atom ( ’@’ ID )?
	 *
	 * @return an expression
	 *
	 * @auther adham-elaraby
	 */
	private Expression parseRecordElementSelect() {
		SourceLocation location = currentToken.sourceLocation;
		
		Expression x = parseAtom();

		if(currentToken.type == AT) {
			acceptIt();
			String name = accept(ID);
			// RecordElementSelect takes in the following parameters:
			// sourceLocation, record (i.e. the atom), name (i.e. the ID)
			return new RecordElementSelect(location, x, name);
		}

		return x;
	}

	/**
	 * Task 1.4:
	 * Grammar rule:
	 * INT | FLOAT | BOOL | STRING
	 * | ID ( call )?
	 * | ’(’ expr ’)’
	 * | (’@’ ID)? initializerList
	 *
	 * an atom here refers to the most basic unit of an expression that cannot be broken down further. It includes literals (like integers, floats, booleans, and strings), identifiers, parenthesized expressions, and initializer lists.
	 *
	 * @author adham-elaraby, ...onlyvalli, sebherz (col)
	 */
	private Expression parseAtom() {
		SourceLocation location = currentToken.sourceLocation;
		
		switch(currentToken.type) {
			case INTLIT:
				return new IntValue(location, parseIntLit());
			case FLOATLIT:
				return new FloatValue(location, parseFloatLit());
			case BOOLLIT:
				// Task 1.4: parse a boolean literal and return a boolean value. @author adham-elaraby
				return new BoolValue(location, parseBoolLit());
			case STRINGLIT:
				return new StringValue(location, accept(STRINGLIT));
			default: /* check other cases below */
		}

		if(currentToken.type == ID) {
			String name = accept(ID);
			if(currentToken.type != LPAREN) {
				return new IdentifierReference(location, name);
			}
			else {
				// Task 1.6: function call.
				// if the current token is a left parenthesis, it means we have a function call, so we should call the method we implemented previously
				return parseCall(name, location);
			}
		}

		if(currentToken.type == LPAREN) {
			acceptIt();
			Expression x = parseExpr();
			accept(RPAREN);
			return x;
		}

		// Task 1.7: record element select
		// handle the case where the current token is an '@' symbol, which means we have a record element select.
		// (’@’ ID)?
		if(currentToken.type == AT) {
			acceptIt();
			String name = accept(ID);
			// we return a new record node with the above.
			return new RecordInit(location, name, parseInitializerList());
		}

		if(currentToken.type == LBRACKET) {
			return new StructureInit(location, parseInitializerList());
		}
		
		throw new SyntaxError(currentToken, INTLIT, FLOATLIT, BOOLLIT, STRINGLIT, ID, LPAREN, LBRACKET, AT);
	}

	/**
	 * Grammar rule:
	 * ’[’ expr ( ’,’ expr )* ’]’
	 *
	 * @return a list of expressions
	 *
	 * @author eicb-team
	 */
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

	/**
	 * Task 1.4: takes a boolean literal and returns a boolean value
	 *
	 * @author adham-elaraby, onlyvalli, sebherz (col)
	 */
	private boolean parseBoolLit() {
		// we are using the parseBoolLit basically to convert the string to a bool, see the commented example below

		// String boolString = "true";
		// boolean boolValue = Boolean.parseBoolean(boolString);
		// System.out.println(boolValue); // Output: true
		// reference: https://www.geeksforgeeks.org/boolean-parseboolean-method-in-java-with-examples/

		return Boolean.parseBoolean(accept(BOOLLIT));
	}
}
