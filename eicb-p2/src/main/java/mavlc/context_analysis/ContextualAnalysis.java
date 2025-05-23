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

import mavlc.errors.*;
import mavlc.syntax.AstNode;
import mavlc.syntax.AstNodeBaseVisitor;
import mavlc.syntax.expression.*;
import mavlc.syntax.function.FormalParameter;
import mavlc.syntax.function.Function;
import mavlc.syntax.module.Module;
import mavlc.syntax.record.RecordElementDeclaration;
import mavlc.syntax.record.RecordTypeDeclaration;
import mavlc.syntax.statement.*;
import mavlc.syntax.type.*;
import mavlc.type.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/* TODO enter group information
 *
 * EiCB group number: ...
 * Names and matriculation numbers of all group members:
 * ...
 */

/** A combined identification and type checking visitor. */
public class ContextualAnalysis extends AstNodeBaseVisitor<Type, Void> {
	
	protected final ModuleEnvironment env;
	
	protected final IdentificationTable table;
	
	protected Function currentFunction;
	
	/** @param moduleEnvironment an identification table containing the module's functions. */
	public ContextualAnalysis(ModuleEnvironment moduleEnvironment) {
		env = moduleEnvironment;
		table = new IdentificationTable();
	}
	
	private void checkType(AstNode node, Type t1, Type t2) {
		if(!t1.equals(t2)) throw new TypeError(node, t1, t2);
	}
	
	private int evalConstExpr(Expression expr) {
		expr.setType(expr.accept(this));
		return expr.accept(new ConstantExpressionEvaluator(), null);
	}
	
	@Override
	public Type visitTypeSpecifier(TypeSpecifier<?> typeSpecifier, Void __) {
		// no need to set the type for simple type specifiers
		if(typeSpecifier instanceof IntTypeSpecifier) return IntType.instance;
		if(typeSpecifier instanceof VoidTypeSpecifier) return VoidType.instance;
		if(typeSpecifier instanceof BoolTypeSpecifier) return BoolType.instance;
		if(typeSpecifier instanceof FloatTypeSpecifier) return FloatType.instance;
		if(typeSpecifier instanceof StringTypeSpecifier) return StringType.instance;
		throw new InternalCompilerError("visitTypeSpecifier should only be called for simple types");
	}
	
	@Override
	public Type visitRecordTypeSpecifier(RecordTypeSpecifier recordTypeSpecifier, Void __) {
		RecordType type = new RecordType(recordTypeSpecifier.recordTypeName, env.getRecordTypeDeclaration(recordTypeSpecifier.recordTypeName));
		recordTypeSpecifier.setType(type);
		return type;
	}
	
	@Override
	public Type visitVectorTypeSpecifier(VectorTypeSpecifier vectorTypeSpecifier, Void __) {
		Type elementType = vectorTypeSpecifier.elementTypeSpecifier.accept(this);
		if(!elementType.isNumericType()) {
			throw new InapplicableOperationError(vectorTypeSpecifier, elementType, NumericType.class);
		}
		int dim = evalConstExpr(vectorTypeSpecifier.dimensionExpression);
		if(dim <= 0)
			throw new StructureDimensionError(vectorTypeSpecifier, "Vector dimension must be strictly positive");
		
		VectorType type = new VectorType((NumericType) elementType, dim);
		vectorTypeSpecifier.setType(type);
		return type;
	}
	
	@Override
	public Type visitMatrixTypeSpecifier(MatrixTypeSpecifier matrixTypeSpecifier, Void __) {
		Type elementType = matrixTypeSpecifier.elementTypeSpecifier.accept(this);
		if(!elementType.isNumericType()) {
			throw new InapplicableOperationError(matrixTypeSpecifier, elementType, NumericType.class);
		}
		int rows = evalConstExpr(matrixTypeSpecifier.rowsExpression);
		int cols = evalConstExpr(matrixTypeSpecifier.colsExpression);
		if(rows <= 0 || cols <= 0)
			throw new StructureDimensionError(matrixTypeSpecifier, "Matrix dimensions must be strictly positive");
		
		MatrixType type = new MatrixType((NumericType) elementType, rows, cols);
		matrixTypeSpecifier.setType(type);
		return type;
	}
	
	@Override
	public Type visitModule(Module module, Void __) {
		boolean hasMain = false;
		for(RecordTypeDeclaration record : module.records) {
			env.addRecordTypeDeclaration(record);
			record.accept(this);
		}
		for(Function function : module.functions) {
			env.addFunction(function);
		}
		for(Function function : module.functions) {
			currentFunction = function;
			function.accept(this);
			if(isMainFunction(function)) hasMain = true;
		}
		if(!hasMain) {
			throw new MissingMainFunctionError();
		}
		
		return null;
	}
	
	private boolean isMainFunction(Function func) {
		// signature of the main method must be "void main()"
		return func.name.equals("main")
				&& func.parameters.isEmpty()
				&& func.getReturnType() == VoidType.instance;
	}
	
	@Override
	public Type visitFunction(Function function, Void __) {
		table.openNewScope();
		
		if(!function.isReturnTypeSet()) {
			function.setReturnType(function.returnTypeSpecifier.accept(this));
		}
		
		for(FormalParameter param : function.parameters) {
			param.accept(this);
		}
		
		if(function.body.isEmpty() && function.getReturnType().isValueType()) {
			throw new MissingReturnError(function);
		}
		
		for(int i = 0; i < function.body.size(); i++) {
			Statement statement = function.body.get(i);
			if(i == function.body.size() - 1 && function.getReturnType().isValueType()) {
				if(!(statement instanceof ReturnStatement)) {
					throw new MissingReturnError(function);
				}
				ReturnStatement returnStatement = (ReturnStatement) statement;
				Type retVal = returnStatement.returnValue.accept(this);
				checkType(returnStatement, retVal, currentFunction.getReturnType());
				table.closeCurrentScope();
				return retVal;
			} else {
				statement.accept(this);
			}
		}
		
		table.closeCurrentScope();
		return null;
	}
	
	@Override
	public Type visitRecordTypeDeclaration(RecordTypeDeclaration recordTypeDeclaration, Void __) {
		Set<String> elementNames = new HashSet<>();

		// Task 2.2: @author adham-elaraby
		// Iterate over each element declaration in the record type
		for (RecordElementDeclaration elementDeclaration : recordTypeDeclaration.elements){

			// Visit the element declaration
			elementDeclaration.accept(this);

			// Check for duplicate element names
			if (!elementNames.add(elementDeclaration.name)) {
				throw new RecordElementError(recordTypeDeclaration, recordTypeDeclaration.name, elementDeclaration.name);
			}

			// Ensure the element type is a valid member type
			if (!elementDeclaration.getType().isMemberType()){
				throw new RecordElementError(recordTypeDeclaration, recordTypeDeclaration.name, elementDeclaration.name);
			}
		}

		// Return a new RecordType with the name and declaration of the record type
		return new RecordType(recordTypeDeclaration.name, recordTypeDeclaration);

		// TODO: check lecture slides, and see what thy meant by adding it to the table and retuning null
		 // causes errors in the tests although it should be correct according to Foliensatz 03 Seite 67
		// fix @author sebherz
		// Add the record type declaration to the symbol table
		// Return null as required
		// return null;
	}
	
	@Override
	public Type visitRecordElementDeclaration(RecordElementDeclaration recordElementDeclaration, Void __) {
		// Task 2.2: @author adham-elaraby

		// Visit the type specifier of the record element declaration
		Type type = recordElementDeclaration.typeSpecifier.accept(this);

		// Set the type of the record element declaration
		recordElementDeclaration.setType(type);
		return type;
	}
	
	@Override
	public Type visitDeclaration(Declaration declaration, Void __) {
		declaration.setType(declaration.typeSpecifier.accept(this));
		table.addIdentifier(declaration.name, declaration);
		return declaration.getType();
	}
	
	@Override
	public Type visitValueDefinition(ValueDefinition valueDefinition, Void __) {
		Type rhs = valueDefinition.value.accept(this);
		visitDeclaration(valueDefinition, null);
		Type lhs = valueDefinition.getType();
		checkType(valueDefinition, lhs, rhs);
		return null;
	}
	
	@Override
	public Type visitVariableAssignment(VariableAssignment variableAssignment, Void __) {
		// Task 2.4: @author adham-elaraby, sebherz

		// Ensure the declaration of the identifier is set
		Type typeOfVariable = variableAssignment.identifier.accept(this);

		String name = variableAssignment.identifier.name;
		Declaration declaration = table.getDeclaration(name);
		if(declaration==null){
			throw new UndeclaredReferenceError(name);
		}

		// we make sure that the declaration is a variable, as we cannot assign to constants
		if (!(variableAssignment.identifier.getDeclaration().isVariable())) {
			throw new ConstantAssignmentError(variableAssignment, variableAssignment.identifier.getDeclaration());
		}

		// Evaluate the type of value expression, so we can compare it with the type of the variable
		Type typeOfValue = variableAssignment.value.accept(this);
		// we then make sure that they are the same
		checkType(variableAssignment, typeOfVariable, typeOfValue);

		// Return null as the type, because variable assignments do not have a type
		return null;
	}
	
	@Override
	public Type visitLeftHandIdentifier(LeftHandIdentifier leftHandIdentifier, Void __) {
		// Task 2.4: @author adham-elaraby

		// get the declaration linked to the identifier from the table
		Declaration declaration = table.getDeclaration(leftHandIdentifier.name);

		// we make sure that the declaration is a variable, as we cannot assign to constants
		if (!declaration.isVariable()){
			throw new ConstantAssignmentError(leftHandIdentifier, declaration);
		}

		// since the declaration is a variable, we set it to the identifier, and return the type of the declaration
		leftHandIdentifier.setDeclaration(declaration);
		return declaration.getType();

	}
	
	@Override
	public Type visitMatrixLhsIdentifier(MatrixLhsIdentifier matrixLhsIdentifier, Void __) {
		// Task 2.4: @author adham-elaraby

		// Evaluate and check the type of the column, and row index expression.
		// This ensures that the index is an integer, which is necessary for matrix indexing.
		Type colType = matrixLhsIdentifier.colIndexExpression.accept(this);
		Type rowType = matrixLhsIdentifier.rowIndexExpression.accept(this);
		checkType(matrixLhsIdentifier, colType, IntType.instance);
		checkType (matrixLhsIdentifier, rowType, IntType.instance);

		// get the declaration linked to the identifier from the table
		// this step is necessary to get the details of the variable, such as its type and whether it is a constant or variable.
		Declaration declaration = table.getDeclaration(matrixLhsIdentifier.name);

		// we make sure that the declaration is a variable, as we cannot assign to constants
		// to prevent illegal assignments that could lead to runtime errors or unexpected behavior.
		if(!declaration.isVariable()) {
			throw new ConstantAssignmentError(matrixLhsIdentifier, declaration);
		}

		// Ensure that the type of the declaration is a MatrixType.
		// This check is important because the operations we are performing are specific to matrices.
		// If the type is not a MatrixType, it indicates a misuse of the identifier, and we throw an error.
		if (!(declaration.getType() instanceof MatrixType)){
			throw new InapplicableOperationError(matrixLhsIdentifier, declaration.getType(), MatrixType.class);
		}

		// TODO: do we need to actually check the dimensions of the matrix? or should we remove this?
		// we retrieve the number of columns and rows in the matrix from the declaration
		int declaredCols = ((MatrixType) declaration.getType()).cols;
		int declaredRows = ((MatrixType) declaration.getType()).rows;

		// we then evaluate rows and cols expressions to get the actual indices to
		// ensure that the indices are within the bounds of the matrix dimensions.
		// prevents out-of-bounds errors
		int actualCols = evalConstExpr(matrixLhsIdentifier.colIndexExpression);
		int actualRows = evalConstExpr(matrixLhsIdentifier.rowIndexExpression);

		if (actualCols < 0 || actualCols >= declaredCols)
			throw new StructureDimensionError(matrixLhsIdentifier, actualCols, declaredCols);
		if (actualRows < 0 || actualRows >= declaredRows)
			throw new StructureDimensionError(matrixLhsIdentifier, actualRows, declaredRows);


		// link the identifier to its declaration
		matrixLhsIdentifier.setDeclaration(declaration);
		return ((MatrixType) declaration.getType()).elementType;
	}
	
	@Override
	public Type visitVectorLhsIdentifier(VectorLhsIdentifier vectorLhsIdentifier, Void __) {
		// Task 2.4: @author adham-elaraby
		// eg : a[12] = 2 // b[1] = 2.5

		// ensure that the index is an integer, which is necessary for vector indexing
		Type index = vectorLhsIdentifier.indexExpression.accept(this);
		checkType(vectorLhsIdentifier, index, IntType.instance);

		Declaration declaration = table.getDeclaration(vectorLhsIdentifier.name);

		if (!declaration.isVariable()) {
			throw new ConstantAssignmentError(vectorLhsIdentifier, declaration);
		}
		if (!(declaration.getType() instanceof VectorType)) {
			throw new InapplicableOperationError(vectorLhsIdentifier, declaration.getType(), VectorType.class);
		}

		// TODO: do we need this vector index check? tbh I don't think so
		// Retrieve the dimension of the vector from the declaration
		// to check if the index is within bounds
		int declaredIndex = ((VectorType) declaration.getType()).dimension;
		int passedIndex = evalConstExpr(vectorLhsIdentifier.indexExpression);

		if (passedIndex < 0){
			throw new StructureDimensionError(vectorLhsIdentifier, passedIndex, 0);
		}
		if (passedIndex >= declaredIndex) {
			throw new StructureDimensionError(vectorLhsIdentifier, passedIndex, declaredIndex);
		}

		vectorLhsIdentifier.setDeclaration(declaration);
		return ((VectorType) declaration.getType()).elementType;
	}
	
	@Override
	public Type visitRecordLhsIdentifier(RecordLhsIdentifier recordLhsIdentifier, Void __) {
		// Task 2.4: @author adham-elaraby
		// eg: x@r = 4 // x@r = 5.2

		Declaration declaration = table.getDeclaration(recordLhsIdentifier.name);
		if (!declaration.isVariable()){
			throw new ConstantAssignmentError(recordLhsIdentifier, declaration);
		}

		if (!(declaration.getType() instanceof RecordType)){
			throw new InapplicableOperationError(recordLhsIdentifier, declaration.getType(), RecordType.class);
		}

		// Retrieve the element declaration associated with the record element name.
		// This step is necessary to get the details of the element within the record,
		// such as its type and whether it is a constant or variable.
		RecordElementDeclaration recordElement =
				((RecordType) declaration.getType())
						.typeDeclaration
						.getElement(recordLhsIdentifier.elementName);

		// make sure that the record element exists within the record type
		if (recordElement==null){
			throw new RecordElementError(recordLhsIdentifier, ((RecordType) declaration.getType()).typeDeclaration.name, recordLhsIdentifier.elementName);
		}
		// check that the record element is a variable, as we cannot assign to constants
		if (!recordElement.isVariable()){
			throw new ConstantAssignmentError(recordLhsIdentifier, recordElement);
		}

		recordLhsIdentifier.setDeclaration(declaration);
		return recordElement.getType();
	}
	
	@Override
	public Type visitForLoop(ForLoop forLoop, Void __) {
		// check for equal type on both sides of the initializer
		Declaration initVarDecl = table.getDeclaration(forLoop.initVarName);
		if(!initVarDecl.isVariable())
			throw new ConstantAssignmentError(forLoop, initVarDecl);
		forLoop.setInitVarDeclaration(initVarDecl);
		Type initVarType = initVarDecl.getType();
		Type initValType = forLoop.initExpression.accept(this);
		checkType(forLoop, initVarType, initValType);
		
		// check that the loop condition has type boolean
		Type testType = forLoop.loopCondition.accept(this);
		checkType(forLoop, testType, BoolType.instance);
		
		// check for equal type on both sides of the increment
		Declaration incrVarDecl = table.getDeclaration(forLoop.incrVarName);
		if(!incrVarDecl.isVariable())
			throw new ConstantAssignmentError(forLoop, incrVarDecl);
		forLoop.setIncrVarDeclaration(incrVarDecl);
		Type incrVarType = incrVarDecl.getType();
		Type incrValType = forLoop.incrExpression.accept(this);
		checkType(forLoop, incrVarType, incrValType);
		
		// process loop body
		table.openNewScope();
		forLoop.body.accept(this);
		table.closeCurrentScope();
		return null;
	}
	
	@Override
	public Type visitForEachLoop(ForEachLoop forEachLoop, Void __) {
		// check for equal type on both sides of the initializer
		IteratorDeclaration iterator = forEachLoop.iteratorDeclaration;
		table.openNewScope();
		// iterator needs to be in an extra scope
		iterator.accept(this);
		
		// check for correct type on both sides of the colon
		Expression struct = forEachLoop.structExpression;
		Type structType = struct.accept(this);
		if(iterator.isVariable()) {
			// struct must be a variable as well
			if(!(struct instanceof IdentifierReference)) {
				// no declaration to pass here
				throw new ConstantAssignmentError(forEachLoop, null);
			} else if(!((IdentifierReference) struct).getDeclaration().isVariable()) {
				throw new ConstantAssignmentError(forEachLoop, ((IdentifierReference) struct).getDeclaration());
			}
		}
		
		if(structType instanceof StructType) {
			checkType(forEachLoop, ((StructType) structType).elementType, iterator.getType());
		} else {
			throw new InapplicableOperationError(forEachLoop, structType, MatrixType.class, VectorType.class);
		}
		
		// process loop body
		table.openNewScope();
		forEachLoop.body.accept(this);
		table.closeCurrentScope();
		table.closeCurrentScope();
		return null;
	}
	
	@Override
	public Type visitIfStatement(IfStatement ifStatement, Void __) {
		Type testType = ifStatement.condition.accept(this);
		checkType(ifStatement, testType, BoolType.instance);
		
		table.openNewScope();
		ifStatement.thenStatement.accept(this);
		table.closeCurrentScope();
		
		if(ifStatement.hasElseStatement()) {
			assert ifStatement.elseStatement != null;
			table.openNewScope();
			ifStatement.elseStatement.accept(this);
			table.closeCurrentScope();
		}
		return null;
	}
	
	@Override
	public Type visitCallStatement(CallStatement callStatement, Void __) {
		// Task 2.6: adham-elaraby

		Type returnType = callStatement.callExpression.accept(this);
		if (!(returnType instanceof VoidType)){
			throw new TypeError(callStatement, returnType, VoidType.instance);
		}

		return null;
	}
	
	@Override
	public Type visitReturnStatement(ReturnStatement returnStatement, Void __) {
		throw new MisplacedReturnError(returnStatement);
	}
	
	@Override
	public Type visitCompoundStatement(CompoundStatement compoundStatement, Void __) {
		// Task 2.1: @author adham-elaraby

		// Open a new scope for the compound statement
		table.openNewScope();

		// Visit each statement within the compound statement
		for(Statement stmt : compoundStatement.statements) {
			stmt.accept(this);
		}

		// Close the scope after processing all statements
		table.closeCurrentScope();

		// Return null as the type, because compound statements do not have a type
		return null;
	}
	
	@Override
	public Type visitSwitchStatement(SwitchStatement switchStatement, Void __) {
		Type testType = switchStatement.condition.accept(this);
		checkType(switchStatement, testType, IntType.instance);
		
		for(Case theCase : switchStatement.cases) {
			theCase.setCondition(evalConstExpr(theCase.conditionExpression));
		}
		
		List<Case> lSC = switchStatement.cases;
		for(int i = 0; i < lSC.size() - 1; i++) {
			for(int j = i + 1; j < lSC.size(); j++) {
				if(lSC.get(i).getCondition() == lSC.get(j).getCondition()) {
					throw new DuplicateCaseError(switchStatement, false, lSC.get(i), lSC.get(j));
				}
			}
		}
		
		List<Default> defaults = switchStatement.defaults;
		
		if(defaults.size() > 1) {
			throw new DuplicateCaseError(switchStatement, true, defaults.get(0), defaults.get(1));
		}
		
		for(SwitchSection curCase : switchStatement.cases) {
			table.openNewScope();
			curCase.accept(this);
			table.closeCurrentScope();
		}
		
		if(defaults.size() == 1) {
			table.openNewScope();
			switchStatement.defaults.get(0).accept(this);
			table.closeCurrentScope();
		}
		
		return null;
	}
	
	@Override
	public Type visitSwitchSection(SwitchSection switchSection, Void __) {
		switchSection.body.accept(this);
		return null;
	}
	
	@Override
	public Type visitMatrixMultiplication(MatrixMultiplication matrixMultiplication, Void __) {
		Type lType = matrixMultiplication.leftOperand.accept(this);
		Type rType = matrixMultiplication.rightOperand.accept(this);
		
		if(!(lType instanceof MatrixType))
			throw new InapplicableOperationError(matrixMultiplication, lType, MatrixType.class);
		if(!(rType instanceof MatrixType))
			throw new InapplicableOperationError(matrixMultiplication, rType, MatrixType.class);
		
		MatrixType lMat = (MatrixType) lType;
		MatrixType rMat = (MatrixType) rType;
		
		// make sure element types match
		checkType(matrixMultiplication, lMat.elementType, rMat.elementType);
		NumericType eType = lMat.elementType;
		
		// make sure dimensions are compatible
		if(lMat.cols != rMat.rows) throw new StructureDimensionError(matrixMultiplication, lMat.cols, rMat.rows);
		
		MatrixType resultType = new MatrixType(eType, lMat.rows, rMat.cols);
		matrixMultiplication.setType(resultType);
		return resultType;
	}
	
	@Override
	public Type visitDotProduct(DotProduct dotProduct, Void __) {
		Type leftOp = dotProduct.leftOperand.accept(this);
		Type rightOp = dotProduct.rightOperand.accept(this);
		
		if(!(leftOp instanceof VectorType))
			throw new InapplicableOperationError(dotProduct, leftOp, VectorType.class);
		if(!(rightOp instanceof VectorType))
			throw new InapplicableOperationError(dotProduct, rightOp, VectorType.class);
		
		VectorType lVec = (VectorType) leftOp;
		VectorType rVec = (VectorType) rightOp;
		
		// make sure element types match
		checkType(dotProduct, lVec.elementType, rVec.elementType);
		NumericType eType = lVec.elementType;
		
		// make sure dimensions are compatible
		if(lVec.dimension != rVec.dimension)
			throw new StructureDimensionError(dotProduct, lVec.dimension, rVec.dimension);
		
		dotProduct.setType(eType);
		return eType;
	}
	
	private Type visitArithmeticOperator(BinaryExpression node, boolean allowLeftStruct, boolean allowRightStruct, boolean allowBothStruct) {
		Type lType = node.leftOperand.accept(this);
		Type rType = node.rightOperand.accept(this);
		
		if(lType.isNumericType() && rType.isNumericType()) {
			checkType(node, lType, rType);
			node.setType(lType);
			return lType;
		}
		
		if(lType.isStructType() && rType.isNumericType()) {
			if(!allowLeftStruct)
				throw new InapplicableOperationError(node, lType, IntType.class, FloatType.class);
			checkType(node, ((StructType) lType).elementType, rType);
			node.setType(lType);
			return lType;
		}
		
		if(lType.isNumericType() && rType.isStructType()) {
			if(!allowRightStruct)
				throw new InapplicableOperationError(node, lType, IntType.class, FloatType.class);
			checkType(node, lType, ((StructType) rType).elementType);
			node.setType(rType);
			return rType;
		}
		
		if(lType.isStructType() && rType.isStructType()) {
			if(!allowBothStruct)
				throw new InapplicableOperationError(node, allowLeftStruct ? rType : lType, IntType.class, FloatType.class);
			checkType(node, lType, rType);
			node.setType(lType);
			return lType;
		}
		
		// if we got here, at least one operand is neither a number nor a structure
		if(!lType.isNumericType() && !lType.isStructType()) {
			//noinspection unchecked
			throw new InapplicableOperationError(node, lType, allowLeftStruct
					? new Class[]{IntType.class, FloatType.class, VectorType.class, MatrixType.class}
					: new Class[]{IntType.class, FloatType.class});
		} else {
			//noinspection unchecked
			throw new InapplicableOperationError(node, rType, allowRightStruct
					? new Class[]{IntType.class, FloatType.class, VectorType.class, MatrixType.class}
					: new Class[]{IntType.class, FloatType.class});
		}
	}
	
	@Override
	public Type visitAddition(Addition addition, Void __) {
		return visitArithmeticOperator(addition, false, false, true);
	}
	
	@Override
	public Type visitSubtraction(Subtraction subtraction, Void __) {
		return visitArithmeticOperator(subtraction, false, false, true);
	}
	
	@Override
	public Type visitMultiplication(Multiplication multiplication, Void __) {
		return visitArithmeticOperator(multiplication, true, true, true);
	}
	
	@Override
	public Type visitDivision(Division division, Void __) {
		return visitArithmeticOperator(division, false, false, false);
	}
	
	@Override
	public Type visitExponentiation(Exponentiation exponentiation, Void __) {
		return visitArithmeticOperator(exponentiation, false, false, false);
	}
	
	@Override
	public Type visitCompare(Compare compare, Void __) {
		Type leftOp = compare.leftOperand.accept(this);
		Type rightOp = compare.rightOperand.accept(this);
		
		if(!leftOp.isNumericType())
			throw new InapplicableOperationError(compare, leftOp, IntType.class, FloatType.class);
		if(!rightOp.isNumericType())
			throw new InapplicableOperationError(compare, rightOp, IntType.class, FloatType.class);
		
		checkType(compare, leftOp, rightOp);
		compare.setType(BoolType.instance);
		return BoolType.instance;
	}
	
	@Override
	public Type visitAnd(And and, Void __) {
		return visitBooleanExpression(and);
	}
	
	@Override
	public Type visitOr(Or or, Void __) {
		return visitBooleanExpression(or);
	}
	
	private Type visitBooleanExpression(BinaryExpression exp) {
		Type leftOp = exp.leftOperand.accept(this);
		Type rightOp = exp.rightOperand.accept(this);
		
		if(!(leftOp instanceof BoolType))
			throw new InapplicableOperationError(exp, leftOp, BoolType.class);
		if(!(rightOp instanceof BoolType))
			throw new InapplicableOperationError(exp, rightOp, BoolType.class);
		
		exp.setType(BoolType.instance);
		return BoolType.instance;
	}
	
	@Override
	public Type visitMatrixTranspose(MatrixTranspose matrixTranspose, Void __) {
		Type opType = matrixTranspose.operand.accept(this);
		if(!(opType instanceof MatrixType))
			throw new InapplicableOperationError(matrixTranspose, opType, MatrixType.class);
		MatrixType matType = (MatrixType) opType;
		MatrixType resType = new MatrixType(matType.elementType, matType.cols, matType.rows);
		matrixTranspose.setType(resType);
		return resType;
	}
	
	@Override
	public Type visitMatrixRows(MatrixRows rows, Void __) {
		// Task 2.5: adham-elaraby
		// we evaluate the type of the operand to ensure it is a matrix
		Type operandType = rows.operand.accept(this);

		if(!(operandType instanceof MatrixType)) {
			throw new InapplicableOperationError(rows, operandType, MatrixType.class);
		}

		// we set the type of the rows expression to an integer, as the number of rows is an integer
		rows.setType(IntType.instance);
		return IntType.instance;
	}
	
	@Override
	public Type visitMatrixCols(MatrixCols cols, Void __) {
		Type opType = cols.operand.accept(this);
		if(!(opType instanceof MatrixType))
			throw new InapplicableOperationError(cols, opType, MatrixType.class);
		cols.setType(IntType.instance);
		return IntType.instance;
	}
	
	@Override
	public Type visitVectorDimension(VectorDimension vectorDimension, Void __) {
		Type opType = vectorDimension.operand.accept(this);
		if(!(opType instanceof VectorType))
			throw new InapplicableOperationError(vectorDimension, opType, VectorType.class);
		vectorDimension.setType(IntType.instance);
		return IntType.instance;
	}
	
	@Override
	public Type visitUnaryMinus(UnaryMinus unaryMinus, Void __) {
		// check the type of the operand to make sure it is a some numeric type (int or float)
		// to ensure it can be negated
		Type operandType= unaryMinus.operand.accept(this);
		if (!operandType.isNumericType()){
			throw new InapplicableOperationError(unaryMinus, operandType, IntType.class, FloatType.class);
		}

		// Set the type of the unary minus expression to the type of the operand
		unaryMinus.setType(operandType);
		return operandType;
	}
	
	@Override
	public Type visitNot(Not not, Void __) {
		Type opType = not.operand.accept(this);
		checkType(not, opType, BoolType.instance);
		not.setType(BoolType.instance);
		return BoolType.instance;
	}
	
	@Override
	public Type visitCallExpression(CallExpression callExpression, Void __) {
		// Task 2.6: adham-elaraby

		// Retrieve the function declaration for the function being called
		Function calledFunction = env.getFunctionDeclaration(callExpression.functionName);

		// Get the list of formal parameters and actual parameters
		List<FormalParameter> formalParameters = calledFunction.parameters;
		List<Expression> actualParameters = callExpression.actualParameters;
		int formalCount = calledFunction.parameters.size();
		int actualCount = callExpression.actualParameters.size();

		// Check if the number of actual parameters matches the number of formal parameters
		if (formalCount != actualCount) {
			throw new ArgumentCountError(callExpression, calledFunction, formalCount, actualCount);
		}

		// For each actual parameter, check if its type matches the corresponding formal parameter's type
		for (int i = 0; i < actualCount; i++) {
			FormalParameter formalParameter = formalParameters.get(i);
			Expression parameter = actualParameters.get(i);

			// There was an error message indicating that the type of an expression has not been set,
			// which is causing a NullPointerException when trying to retrieve the type.
			// This issue occured here.
			// To resolve this, we needed to ensure that the type of each actual parameter is set
			// before checking their types against the formal parameters.
			// hence:
			// Visit the actual parameter to set its type
			Type actualType = parameter.accept(this);

			// If the formal parameter's type is not set, set it by visiting the type specifier
			if (!formalParameter.isTypeSet()) {
				formalParameter.setType(formalParameter.typeSpecifier.accept(this));
			}

			// Check if the type of the actual parameter matches the type of the formal parameter
			checkType(callExpression, formalParameter.getType(), actualType);
		}

		// If the return type of the called function is not set, set it by visiting the return type specifier
		if (!calledFunction.isReturnTypeSet()) {
			calledFunction.setReturnType(calledFunction.returnTypeSpecifier.accept(this));
		}

		// Set the callee definition and type of the call expression
		callExpression.setCalleeDefinition(calledFunction);
		callExpression.setType(calledFunction.getReturnType());

		return callExpression.getType();
	}
	
	@Override
	public Type visitElementSelect(ElementSelect elementSelect, Void __) {
		Type baseType = elementSelect.structExpression.accept(this);
		if(!(baseType instanceof StructType))
			throw new InapplicableOperationError(elementSelect, baseType, MatrixType.class, VectorType.class);
		
		Type indexType = elementSelect.indexExpression.accept(this);
		if(!indexType.equals(IntType.instance))
			throw new TypeError(elementSelect, indexType, IntType.instance);
		
		if(baseType instanceof VectorType) {
			Type resultType = ((VectorType) baseType).elementType;
			elementSelect.setType(resultType);
			return resultType;
		} else if(baseType instanceof MatrixType) {
			NumericType elementType = ((MatrixType) baseType).elementType;
			int size = ((MatrixType) baseType).cols;
			Type resultType = new VectorType(elementType, size);
			elementSelect.setType(resultType);
			return resultType;
		}
		return null;
	}
	
	@Override
	public Type visitRecordElementSelect(RecordElementSelect recordElementSelect, Void __) {
		Type baseType = recordElementSelect.recordExpression.accept(this);
		if(!(baseType instanceof RecordType)) {
			throw new InapplicableOperationError(recordElementSelect, baseType, RecordType.class);
		}
		String elementName = recordElementSelect.elementName;
		RecordElementDeclaration element =
				(((RecordType) baseType).typeDeclaration.getElement(elementName));
		if(element == null) {
			throw new RecordElementError(recordElementSelect, ((RecordType) baseType).name, elementName);
		}
		recordElementSelect.setType(element.getType());
		return element.getType();
	}
	
	@Override
	public Type visitSubMatrix(SubMatrix subMatrix, Void __) {
		int rso = evalConstExpr(subMatrix.rowStartOffsetExpression);
		int reo = evalConstExpr(subMatrix.rowEndOffsetExpression);
		int cso = evalConstExpr(subMatrix.colStartOffsetExpression);
		int ceo = evalConstExpr(subMatrix.colEndOffsetExpression);
		int rows = reo - rso + 1;
		int cols = ceo - cso + 1;
		
		subMatrix.setRowStartOffset(rso);
		subMatrix.setRowEndOffset(reo);
		subMatrix.setColStartOffset(cso);
		subMatrix.setColEndOffset(ceo);
		
		Type rowBaseType = subMatrix.rowBaseIndexExpression.accept(this);
		checkType(subMatrix, rowBaseType, IntType.instance);
		Type colBaseType = subMatrix.colBaseIndexExpression.accept(this);
		checkType(subMatrix, colBaseType, IntType.instance);
		
		Type baseType = subMatrix.structExpression.accept(this);
		if(!(baseType instanceof MatrixType))
			throw new InapplicableOperationError(subMatrix, baseType, MatrixType.class);
		MatrixType matrix = (MatrixType) baseType;
		
		if(reo < rso) throw new StructureDimensionError(subMatrix, reo, rso);
		if(ceo < cso) throw new StructureDimensionError(subMatrix, ceo, cso);
		if(matrix.rows < rows) throw new StructureDimensionError(subMatrix, matrix.rows, rows);
		if(matrix.cols < cols) throw new StructureDimensionError(subMatrix, matrix.cols, cols);
		
		Type resultType = new MatrixType(((MatrixType) baseType).elementType, rows, cols);
		subMatrix.setType(resultType);
		return resultType;
	}
	
	@Override
	public Type visitSubVector(SubVector subVector, Void __) {
		int so = evalConstExpr(subVector.startOffsetExpression);
		int eo = evalConstExpr(subVector.endOffsetExpression);
		int size = eo - so + 1;
		
		subVector.setStartOffset(so);
		subVector.setEndOffset(eo);
		
		Type indexType = subVector.baseIndexExpression.accept(this);
		checkType(subVector, indexType, IntType.instance);
		Type baseType = subVector.structExpression.accept(this);
		if(!(baseType instanceof VectorType)) {
			throw new InapplicableOperationError(subVector, baseType, VectorType.class);
		}
		VectorType vector = (VectorType) baseType;
		if(eo < so) {
			throw new StructureDimensionError(subVector, eo, so);
		}
		if(vector.dimension < size) {
			throw new StructureDimensionError(subVector, vector.dimension, size);
		}
		
		Type resultType = new VectorType(((VectorType) baseType).elementType, size);
		subVector.setType(resultType);
		return resultType;
	}
	
	@Override
	public Type visitStructureInit(StructureInit structureInit, Void __) {
		// The type of the first element determines the structure
		Type firstElem = structureInit.elements.get(0).accept(this);
		if(firstElem instanceof VectorType) {
			// Matrix init
			NumericType elemType = ((VectorType) firstElem).elementType;
			int size = ((VectorType) firstElem).dimension;
			int x = 0;
			for(Expression element : structureInit.elements) {
				Type t = element.accept(this);
				checkType(structureInit, firstElem, t);
				++x;
			}
			MatrixType resultType = new MatrixType(elemType, x, size);
			structureInit.setType(resultType);
			return resultType;
		} else {
			// Vector init
			// Task 2.5: @adham-elaraby

			if(!firstElem.isNumericType()) {
				throw new InapplicableOperationError(structureInit, firstElem, IntType.class, FloatType.class);
			}

			// Cast the type of the first element to NumericType.
			// This is safe because we have already checked that the element is numeric.
			// the cast to NumericType is necessary because firstElem is of type Type,
			// and we need to access methods specific to NumericType
			NumericType elementType = (NumericType) firstElem;

			// variable to keep track of the size of the vector
			int size = 0;
			for(Expression element : structureInit.elements) {
				// we evaluate the type of each element in the vector, making sure they are all numeric, i.e. matching the first element
				Type currentElementType = element.accept(this);
				checkType(structureInit, elementType, currentElementType);
				size++;
			}

			// we create a new VectorType with the determined element type and size from above.
			VectorType resultType = new VectorType(elementType, size);
			structureInit.setType(resultType);
			return resultType;
		}
	}
	
	@Override
	public Type visitRecordInit(RecordInit recordInit, Void __) {
		RecordTypeDeclaration decl = env.getRecordTypeDeclaration(recordInit.typeName);
		recordInit.setType(decl.accept(this));
		if(recordInit.elements.size() != decl.elements.size()) {
			throw new StructureDimensionError(recordInit, recordInit.elements.size(), decl.elements.size());
		}
		for(int i = 0; i < recordInit.elements.size(); i++) {
			Type elemType = recordInit.elements.get(i).accept(this);
			Type declType = decl.elements.get(i).getType();
			checkType(recordInit, elemType, declType);
		}
		return recordInit.getType();
	}
	
	@Override
	public Type visitStringValue(StringValue stringValue, Void __) {
		return StringType.instance;
	}
	
	@Override
	public Type visitBoolValue(BoolValue boolValue, Void __) {
		// Task 2.5: @adham-elaraby
		return BoolType.instance;
	}
	
	@Override
	public Type visitIntValue(IntValue intValue, Void __) {
		return IntType.instance;
	}
	
	@Override
	public Type visitFloatValue(FloatValue floatValue, Void __) {
		return FloatType.instance;
	}
	
	@Override
	public Type visitIdentifierReference(IdentifierReference identifierReference, Void __) {
		Declaration decl = table.getDeclaration(identifierReference.name);
		identifierReference.setDeclaration(decl);
		identifierReference.setType(decl.getType());
		return decl.getType();
	}
	
	@Override
	public Type visitSelectExpression(SelectExpression exp, Void __) {
		// Task 2.5: @adham-elaraby
		Type conditionType = exp.condition.accept(this);
		// make sure the condition is a boolean
		checkType(exp, conditionType, BoolType.instance);

		// get the types of the true and false cases and make sure they are the same
		Type trueCaseType = exp.trueCase.accept(this);
		Type falseCaseType = exp.falseCase.accept(this);
		checkType(exp, trueCaseType, falseCaseType);

		// we set the type (both true and false cases have the same type so its not so important which one we use)
		exp.setType(trueCaseType);
		return trueCaseType;
	}
}
