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

import mavlc.errors.NonConstantExpressionError;
import mavlc.syntax.AstNode;
import mavlc.syntax.AstNodeBaseVisitor;
import mavlc.syntax.expression.*;

/* TODO enter group information
 *
 * EiCB group number: ...
 * Names and matriculation numbers of all group members:
 * ...
 */

public class ConstantExpressionEvaluator extends AstNodeBaseVisitor<Integer, Void> {
	@Override
	protected Integer defaultOperation(AstNode node, Void obj) {
		if(node instanceof Expression) {
			throw new NonConstantExpressionError((Expression) node);
		} else {
			throw new RuntimeException("Internal compiler error: should not try to constant-evaluate non-expressions");
		}
	}
	
	@Override
	public Integer visitIntValue(IntValue intValue, Void __) {
		return intValue.value;
	}

	// Task 2.3: @author adham-elaraby (all visit{..} methods)
	// implNote: we use Void __ to comply with the method signature defined by the AstNodeBaseVisitor class.
	// The Void type is used to indicate that no additional data is passed to the visitor methods,
	// and the __ is just a placeholder name for the parameter, which is not used in the method body.

	@Override
	public Integer visitAddition(Addition addition, Void __){
		// Evaluate the left and right operands and return their sum
		return addition.leftOperand.accept(this) + addition.rightOperand.accept(this);
	}

	@Override
	public Integer visitSubtraction(Subtraction subtraction, Void __){
		// Evaluate the left and right operands and return their difference
		return subtraction.leftOperand.accept(this) - subtraction.rightOperand.accept(this);
	}

	@Override
	public Integer visitMultiplication(Multiplication multiplication, Void __){
		// Evaluate the left and right operands and return their product
		return multiplication.leftOperand.accept(this) * multiplication.rightOperand.accept(this);
	}

	@Override
	public Integer visitDivision(Division division, Void __){
		// Evaluate the left and right operands and return their quotient
		// note: float values will be implicitly cast to integers
		return division.leftOperand.accept(this) / division.rightOperand.accept(this);
	}

	@Override
	public Integer visitExponentiation(Exponentiation exponentiation, Void __){
		// Evaluate the base and exponent operands
		int base = exponentiation.leftOperand.accept(this);
		int exponent = exponentiation.rightOperand.accept(this);

		// If the exponent is negative, throw an arithmetic exception, as we do not support negative exponents
		if (exponent < 0){
			throw new ArithmeticException();
		}

		// Calculate the power by multiplying the base with itself exponent times
		int power=1;
		for (int i=0; i < exponent; i++){
			power = power*base;
		}
		return power;
	}
	@Override
	public Integer visitUnaryMinus(UnaryMinus unaryMinus, Void __){
		// Evaluate the operand and return its negation
		int operand = unaryMinus.operand.accept(this);
		return -operand;
	}
}
