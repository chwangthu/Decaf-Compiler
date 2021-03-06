package decaf.translate;

import java.util.Stack;

import decaf.tree.Tree;
import decaf.backend.OffsetCounter;
import decaf.machdesc.Intrinsic;
import decaf.symbol.Variable;
import decaf.tac.Label;
import decaf.tac.Temp;
import decaf.type.*;

public class TransPass2 extends Tree.Visitor {

	private Translater tr;

	private Temp currentThis;

	private Stack<Label> loopExits;

	public TransPass2(Translater tr) {
		this.tr = tr;
		loopExits = new Stack<Label>();
	}

	@Override
	public void visitClassDef(Tree.ClassDef classDef) {
		for (Tree f : classDef.fields) {
			f.accept(this);
		}
	}

	@Override
	public void visitMethodDef(Tree.MethodDef funcDefn) {
		if (!funcDefn.statik) {
			currentThis = ((Variable) funcDefn.symbol.getAssociatedScope().lookup("this")).getTemp();
		}
		tr.beginFunc(funcDefn.symbol);
		funcDefn.body.accept(this);
		tr.endFunc();
		currentThis = null;
	}

	@Override
	public void visitTopLevel(Tree.TopLevel program) {
		for (Tree.ClassDef cd : program.classes) {
			cd.accept(this);
		}
	}

	@Override
	public void visitVarDef(Tree.VarDef varDef) {
		if (varDef.symbol.isLocalVar()) {
			Temp t = Temp.createTempI4();
			t.sym = varDef.symbol;
			varDef.symbol.setTemp(t);
		}
	}

	@Override
	public void visitBinary(Tree.Binary expr) {
		expr.left.accept(this);
		expr.right.accept(this);
		switch (expr.tag) {
		case Tree.PLUS:
			expr.val = tr.genAdd(expr.left.val, expr.right.val);
			break;
		case Tree.MINUS:
			expr.val = tr.genSub(expr.left.val, expr.right.val);
			break;
		case Tree.MUL:
			expr.val = tr.genMul(expr.left.val, expr.right.val);
			break;
		case Tree.DIV:
			expr.val = tr.genDiv(expr.left.val, expr.right.val);
			break;
		case Tree.MOD:
			expr.val = tr.genMod(expr.left.val, expr.right.val);
			break;
		case Tree.AND:
			expr.val = tr.genLAnd(expr.left.val, expr.right.val);
			break;
		case Tree.OR:
			expr.val = tr.genLOr(expr.left.val, expr.right.val);
			break;
		case Tree.LT:
			expr.val = tr.genLes(expr.left.val, expr.right.val);
			break;
		case Tree.LE:
			expr.val = tr.genLeq(expr.left.val, expr.right.val);
			break;
		case Tree.GT:
			expr.val = tr.genGtr(expr.left.val, expr.right.val);
			break;
		case Tree.GE:
			expr.val = tr.genGeq(expr.left.val, expr.right.val);
			break;
		case Tree.EQ:
		case Tree.NE:
			genEquNeq(expr);
			break;
		}
	}

	private void genEquNeq(Tree.Binary expr) {
		if (expr.left.type.equal(BaseType.STRING) || expr.right.type.equal(BaseType.STRING)) {
			tr.genParm(expr.left.val);
			tr.genParm(expr.right.val);
			expr.val = tr.genDirectCall(Intrinsic.STRING_EQUAL.label, BaseType.BOOL);
			if (expr.tag == Tree.NE) {
				expr.val = tr.genLNot(expr.val);
			}
		} else {
			if (expr.tag == Tree.EQ)
				expr.val = tr.genEqu(expr.left.val, expr.right.val);
			else
				expr.val = tr.genNeq(expr.left.val, expr.right.val);
		}
	}

	@Override
	public void visitAssign(Tree.Assign assign) {
		assign.left.accept(this);
		assign.expr.accept(this);
		switch (assign.left.lvKind) {
		case ARRAY_ELEMENT:
			Tree.Indexed arrayRef = (Tree.Indexed) assign.left;
			Temp esz = tr.genLoadImm4(OffsetCounter.WORD_SIZE);
			Temp t = tr.genMul(arrayRef.index.val, esz);
			Temp base = tr.genAdd(arrayRef.array.val, t);
			tr.genStore(assign.expr.val, base, 0);
			break;
		case MEMBER_VAR:
			Tree.Ident varRef = (Tree.Ident) assign.left;
			tr.genStore(assign.expr.val, varRef.owner.val, varRef.symbol.getOffset());
			break;
		case PARAM_VAR:
		case LOCAL_VAR:
			tr.genAssign(((Tree.Ident) assign.left).symbol.getTemp(), assign.expr.val);
			break;
		case AUTO_VAR:
			// System.out.println(assign.expr.val);
			Temp varTemp = Temp.createTempI4();
			Tree.Ident ident = (Tree.Ident)assign.left;
			varTemp.sym = ident.symbol;
			ident.symbol.setTemp(varTemp);
			// System.out.println(((Tree.Ident)assign.left).symbol.getTemp());
			tr.genAssign(ident.symbol.getTemp(), assign.expr.val);
		}
	}

	@Override
	public void visitLiteral(Tree.Literal literal) {
		switch (literal.typeTag) {
		case Tree.INT:
			literal.val = tr.genLoadImm4(((Integer) literal.value).intValue());
			break;
		case Tree.BOOL:
			literal.val = tr.genLoadImm4((Boolean) (literal.value) ? 1 : 0);
			break;
		default:
			literal.val = tr.genLoadStrConst((String) literal.value);
		}
	}

	@Override
	public void visitExec(Tree.Exec exec) {
		exec.expr.accept(this);
	}

	@Override
	public void visitUnary(Tree.Unary expr) {
		expr.expr.accept(this);
		switch (expr.tag) {
		case Tree.NEG:
			expr.val = tr.genNeg(expr.expr.val);
			break;
		default:
			expr.val = tr.genLNot(expr.expr.val);
		}
	}

	@Override
	public void visitNull(Tree.Null nullExpr) {
		nullExpr.val = tr.genLoadImm4(0);
	}

	@Override
	public void visitBlock(Tree.Block block) {
		for (Tree s : block.block) {
			s.accept(this);
		}
	}

	@Override
	public void visitThisExpr(Tree.ThisExpr thisExpr) {
		thisExpr.val = currentThis;
	}

	@Override
	public void visitReadIntExpr(Tree.ReadIntExpr readIntExpr) {
		readIntExpr.val = tr.genIntrinsicCall(Intrinsic.READ_INT);
	}

	@Override
	public void visitReadLineExpr(Tree.ReadLineExpr readStringExpr) {
		readStringExpr.val = tr.genIntrinsicCall(Intrinsic.READ_LINE);
	}

	@Override
	public void visitReturn(Tree.Return returnStmt) {
		if (returnStmt.expr != null) {
			returnStmt.expr.accept(this);
			tr.genReturn(returnStmt.expr.val);
		} else {
			tr.genReturn(null);
		}

	}

	@Override
	public void visitPrint(Tree.Print printStmt) {
		for (Tree.Expr r : printStmt.exprs) {
			r.accept(this);
			tr.genParm(r.val);
			if (r.type.equal(BaseType.BOOL)) {
				tr.genIntrinsicCall(Intrinsic.PRINT_BOOL);
			} else if (r.type.equal(BaseType.INT)) {
				tr.genIntrinsicCall(Intrinsic.PRINT_INT);
			} else if (r.type.equal(BaseType.STRING)) {
				tr.genIntrinsicCall(Intrinsic.PRINT_STRING);
			}
		}
	}

	@Override
	public void visitIndexed(Tree.Indexed indexed) {
		indexed.array.accept(this);
		indexed.index.accept(this);
		tr.genCheckArrayIndex(indexed.array.val, indexed.index.val);

		Temp esz = tr.genLoadImm4(OffsetCounter.WORD_SIZE);
		Temp t = tr.genMul(indexed.index.val, esz);
		Temp base = tr.genAdd(indexed.array.val, t);
		indexed.val = tr.genLoad(base, 0);
	}

	@Override
	public void visitIdent(Tree.Ident ident) {
		if (ident.lvKind == Tree.LValue.Kind.MEMBER_VAR) {
			ident.owner.accept(this);
		}

		switch (ident.lvKind) {
		case MEMBER_VAR:
			ident.val = tr.genLoad(ident.owner.val, ident.symbol.getOffset());
			break;
		default:
			ident.val = ident.symbol.getTemp();
			break;
		}
	}

	@Override
	public void visitBreak(Tree.Break breakStmt) {
		tr.genBranch(loopExits.peek());
	}

	@Override
	public void visitCallExpr(Tree.CallExpr callExpr) {
		if (callExpr.isArrayLength) {
			callExpr.receiver.accept(this);
			callExpr.val = tr.genLoad(callExpr.receiver.val, -OffsetCounter.WORD_SIZE);
		} else {
			if (callExpr.receiver != null) {
				callExpr.receiver.accept(this);
			}
			for (Tree.Expr expr : callExpr.actuals) {
				expr.accept(this);
			}
			if (callExpr.receiver != null) {
				tr.genParm(callExpr.receiver.val);
			}
			for (Tree.Expr expr : callExpr.actuals) {
				tr.genParm(expr.val);
			}
			if (callExpr.receiver == null) {
				callExpr.val = tr.genDirectCall(callExpr.symbol.getFuncty().label, callExpr.symbol.getReturnType());
			} else {
				Temp vt = tr.genLoad(callExpr.receiver.val, 0);
				Temp func = tr.genLoad(vt, callExpr.symbol.getOffset());
				callExpr.val = tr.genIndirectCall(func, callExpr.symbol.getReturnType());
			}
		}

	}

	@Override
	public void visitForLoop(Tree.ForLoop forLoop) {
		if (forLoop.init != null) {
			forLoop.init.accept(this);
		}
		Label cond = Label.createLabel();
		Label loop = Label.createLabel();
		tr.genBranch(cond);
		tr.genMark(loop);
		if (forLoop.update != null) {
			forLoop.update.accept(this);
		}
		tr.genMark(cond);
		forLoop.condition.accept(this);
		Label exit = Label.createLabel();
		tr.genBeqz(forLoop.condition.val, exit);
		loopExits.push(exit);
		if (forLoop.loopBody != null) {
			forLoop.loopBody.accept(this);
		}
		tr.genBranch(loop);
		loopExits.pop();
		tr.genMark(exit);
	}

	@Override
	public void visitIf(Tree.If ifStmt) {
		ifStmt.condition.accept(this);
		if (ifStmt.falseBranch != null) {
			Label falseLabel = Label.createLabel();
			tr.genBeqz(ifStmt.condition.val, falseLabel);
			ifStmt.trueBranch.accept(this);
			Label exit = Label.createLabel();
			tr.genBranch(exit);
			tr.genMark(falseLabel);
			ifStmt.falseBranch.accept(this);
			tr.genMark(exit);
		} else if (ifStmt.trueBranch != null) {
			Label exit = Label.createLabel();
			tr.genBeqz(ifStmt.condition.val, exit);
			if (ifStmt.trueBranch != null) {
				ifStmt.trueBranch.accept(this);
			}
			tr.genMark(exit);
		}
	}

	@Override
	public void visitNewArray(Tree.NewArray newArray) {
		newArray.length.accept(this);
		newArray.val = tr.genNewArray(newArray.length.val, tr.genLoadImm4(0), 0);
	}

	@Override
	public void visitNewClass(Tree.NewClass newClass) {
		newClass.val = tr.genDirectCall(newClass.symbol.getNewFuncLabel(), BaseType.INT);
	}

	@Override
	public void visitWhileLoop(Tree.WhileLoop whileLoop) {
		Label loop = Label.createLabel();
		tr.genMark(loop);
		whileLoop.condition.accept(this);
		Label exit = Label.createLabel();
		tr.genBeqz(whileLoop.condition.val, exit);
		loopExits.push(exit);
		if (whileLoop.loopBody != null) {
			whileLoop.loopBody.accept(this);
		}
		tr.genBranch(loop);
		loopExits.pop();
		tr.genMark(exit);
	}

	@Override
	public void visitTypeTest(Tree.TypeTest typeTest) {
		typeTest.instance.accept(this);
		typeTest.val = tr.genInstanceof(typeTest.instance.val, typeTest.symbol);
	}

	@Override
	public void visitTypeCast(Tree.TypeCast typeCast) {
		typeCast.expr.accept(this);
		if (!typeCast.expr.type.compatible(typeCast.symbol.getType())) {
			tr.genClassCast(typeCast.expr.val, typeCast.symbol);
		}
		typeCast.val = typeCast.expr.val;
	}

	// wc add
	@Override
	public void visitScopy(Tree.Scopy scopy) {
		scopy.ident.accept(this);
		scopy.expr.accept(this);
		int size = ((ClassType)scopy.ident.type).getSymbol().getSize();

		Temp tSize = tr.genLoadImm4(size);
		tr.genParm(tSize);
		Temp obj = tr.genIntrinsicCall(Intrinsic.ALLOCATE);
		tr.genAssign(scopy.ident.val, obj);
		int offset = 0;
		while(offset < size) {
			Temp tep = tr.genLoad(scopy.expr.val, offset);
			tr.genStore(tep, obj, offset);
			offset += OffsetCounter.WORD_SIZE;
		}
	}

	@Override
	public void visitGuarded(Tree.Guarded guarded) {
		for (Tree condition : guarded.conditions) {
			condition.accept(this);
		}
	}

	@Override
	public void visitIfSub(Tree.IfSub ifSub) {
		ifSub.condition.accept(this);
		Label label = Label.createLabel();
		tr.genBeqz(ifSub.condition.val, label);
		ifSub.branch.accept(this);
		tr.genMark(label);
	}

	@Override
	public void visitArrayRepeat(Tree.ArrayRepeat arrRepeat) {
		arrRepeat.expr1.accept(this);
		arrRepeat.expr2.accept(this);
		Type t = ((ArrayType) arrRepeat.type).getElementType();
		if(t.isClassType()) {
			int classSize = ((ClassType)arrRepeat.expr1.type).getSymbol().getSize();
			arrRepeat.val = tr.genNewClassArray(arrRepeat.expr2.val, arrRepeat.expr1.val, classSize);
		} else {
			arrRepeat.val = tr.genNewArray(arrRepeat.expr2.val, arrRepeat.expr1.val, 1);
		}
	}

	@Override
	public void visitDefault(Tree.Default arrDefault) {
		arrDefault.expr1.accept(this);
		arrDefault.expr2.accept(this);
		arrDefault.expr3.accept(this);

		Temp defaultTemp = Temp.createTempI4();
		Temp length = tr.genLoad(arrDefault.expr1.val, -OffsetCounter.WORD_SIZE);
		Temp index = arrDefault.expr2.val;
		Label outOfBound = Label.createLabel();
		Temp cond = tr.genLes(index, tr.genLoadImm4(0));//判断小于0
		Label inBound = Label.createLabel();
		Label exit = Label.createLabel();
		tr.genBnez(cond, outOfBound); //跳到越界的情况
		cond = tr.genLes(index, length); //判断是否小于length
		tr.genBnez(cond, inBound);

		//越界情况
		tr.genMark(outOfBound);
		tr.genAssign(defaultTemp, arrDefault.expr3.val);
		tr.genBranch(exit);

		tr.genMark(inBound); //没越界的情况
		Temp offset = tr.genMul(tr.genLoadImm4(OffsetCounter.WORD_SIZE), arrDefault.expr2.val);
		Temp addr = tr.genAdd(arrDefault.expr1.val, offset);
		tr.genAssign(defaultTemp, tr.genLoad(addr, 0)); 
		tr.genBranch(exit);

		tr.genMark(exit);
		arrDefault.val = defaultTemp;
	}

	@Override
	public void visitForeach(Tree.Foreach foreach) {
		foreach.stmt1.accept(this);
		foreach.expr1.accept(this);

		Tree.BoundedVariable bvar = (Tree.BoundedVariable)foreach.stmt1;
		Temp bvarTemp = Temp.createTempI4();
		bvarTemp.sym = bvar.symbol;
		bvar.symbol.setTemp(bvarTemp);

		
		Temp length = tr.genLoad(foreach.expr1.val, -OffsetCounter.WORD_SIZE);
		Temp iter = tr.genLoadImm4(0);
		Temp cond = tr.genEqu(iter, length);
		Label loop = Label.createLabel();
		Label exit = Label.createLabel();

		tr.genMark(loop);
		cond = tr.genEqu(iter, length);
		tr.genBnez(cond, exit); //不为0即iter=length，结束
		Temp offset = tr.genMul(tr.genLoadImm4(OffsetCounter.WORD_SIZE), iter);
		Temp addr = tr.genAdd(foreach.expr1.val, offset);
		tr.genAssign(bvarTemp, tr.genLoad(addr, 0));
		tr.genAssign(iter, tr.genAdd(iter, tr.genLoadImm4(1))); //加1
		
		if(foreach.expr2 != null) { //有while
			foreach.expr2.accept(this); //while部分
			tr.genBeqz(foreach.expr2.val, exit); //为假跳出循环
		}
		loopExits.push(exit);
		foreach.stmt2.accept(this); //为真执行语句块
		loopExits.push(exit);
		tr.genBranch(loop);

		tr.genMark(exit);
	}
	// wc add ended
}
