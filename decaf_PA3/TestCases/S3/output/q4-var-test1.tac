Exception in thread "main" java.lang.NullPointerException
	at decaf.translate.TransPass2.visitIdent(TransPass2.java:253)
	at decaf.tree.Tree$Ident.accept(Tree.java:1521)
	at decaf.translate.TransPass2.visitAssign(TransPass2.java:126)
	at decaf.tree.Tree$Assign.accept(Tree.java:1045)
	at decaf.translate.TransPass2.visitBlock(TransPass2.java:188)
	at decaf.tree.Tree$Block.accept(Tree.java:518)
	at decaf.translate.TransPass2.visitMethodDef(TransPass2.java:40)
	at decaf.tree.Tree$MethodDef.accept(Tree.java:438)
	at decaf.translate.TransPass2.visitClassDef(TransPass2.java:29)
	at decaf.tree.Tree$ClassDef.accept(Tree.java:403)
	at decaf.translate.TransPass2.visitTopLevel(TransPass2.java:48)
	at decaf.translate.Translater.translate(Translater.java:42)
	at decaf.Driver.compile(Driver.java:104)
	at decaf.Driver.main(Driver.java:117)
