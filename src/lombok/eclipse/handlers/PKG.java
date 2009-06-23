package lombok.eclipse.handlers;

import java.lang.reflect.Modifier;

import lombok.AccessLevel;
import lombok.core.AST.Kind;
import lombok.eclipse.EclipseAST;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ConstructorDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;

class PKG {
	private PKG() {}
	
	static int toModifier(AccessLevel value) {
		switch ( value ) {
		case MODULE:
		case PACKAGE:
			return 0;
		default:
		case PUBLIC:
			return Modifier.PUBLIC;
		case PROTECTED:
			return Modifier.PROTECTED;
		case PRIVATE:
			return Modifier.PRIVATE;
		}
	}
	
	static boolean nameEquals(char[][] typeName, String string) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for ( char[] elem : typeName ) {
			if ( first ) first = false;
			else sb.append('.');
			sb.append(elem);
		}
		
		return string.contentEquals(sb);
	}
	
	enum MethodExistsResult {
		NOT_EXISTS, EXISTS_BY_USER, EXISTS_BY_LOMBOK;
	}
	
	static MethodExistsResult methodExists(String methodName, EclipseAST.Node node) {
		while ( node != null && !(node.get() instanceof TypeDeclaration) ) {
			node = node.up();
		}
		
		if ( node.get() instanceof TypeDeclaration ) {
			TypeDeclaration typeDecl = (TypeDeclaration)node.get();
			if ( typeDecl.methods != null ) for ( AbstractMethodDeclaration def : typeDecl.methods ) {
				char[] mName = ((AbstractMethodDeclaration)def).selector;
				if ( mName == null ) continue;
				if ( methodName.equals(new String(mName)) ) {
					EclipseAST.Node existing = node.getNodeFor(def);
					if ( existing == null || !existing.isHandled() ) return MethodExistsResult.EXISTS_BY_USER;
					return MethodExistsResult.EXISTS_BY_LOMBOK;
				}
			}
		}
		
		return MethodExistsResult.NOT_EXISTS;
	}
	
	static MethodExistsResult constructorExists(EclipseAST.Node node) {
		while ( node != null && !(node.get() instanceof TypeDeclaration) ) {
			node = node.up();
		}
		
		if ( node.get() instanceof TypeDeclaration ) {
			TypeDeclaration typeDecl = (TypeDeclaration)node.get();
			if ( typeDecl.methods != null ) for ( AbstractMethodDeclaration def : typeDecl.methods ) {
				if ( def instanceof ConstructorDeclaration ) {
					if ( (def.bits & ASTNode.IsDefaultConstructor) != 0 ) continue;
					EclipseAST.Node existing = node.getNodeFor(def);
					if ( existing == null || !existing.isHandled() ) return MethodExistsResult.EXISTS_BY_USER;
					return MethodExistsResult.EXISTS_BY_LOMBOK;
				}
			}
		}
		
		return MethodExistsResult.NOT_EXISTS;
	}
	
	static EclipseAST.Node getExistingLombokConstructor(EclipseAST.Node node) {
		while ( node != null && !(node.get() instanceof TypeDeclaration) ) {
			node = node.up();
		}
		
		if ( node.get() instanceof TypeDeclaration ) {
			for ( AbstractMethodDeclaration def : ((TypeDeclaration)node.get()).methods ) {
				if ( def instanceof ConstructorDeclaration ) {
					if ( (def.bits & ASTNode.IsDefaultConstructor) != 0 ) continue;
					EclipseAST.Node existing = node.getNodeFor(def);
					if ( existing.isHandled() ) return existing;
				}
			}
		}
		
		return null;
	}
	
	static EclipseAST.Node getExistingLombokMethod(String methodName, EclipseAST.Node node) {
		while ( node != null && !(node.get() instanceof TypeDeclaration) ) {
			node = node.up();
		}
		
		if ( node.get() instanceof TypeDeclaration ) {
			for ( AbstractMethodDeclaration def : ((TypeDeclaration)node.get()).methods ) {
				char[] mName = ((AbstractMethodDeclaration)def).selector;
				if ( mName == null ) continue;
				if ( methodName.equals(new String(mName)) ) {
					EclipseAST.Node existing = node.getNodeFor(def);
					if ( existing.isHandled() ) return existing;
				}
			}
		}
		
		return null;
	}
	
	static void injectMethod(EclipseAST.Node type, AbstractMethodDeclaration method) {
		TypeDeclaration parent = (TypeDeclaration) type.get();
		
		if ( parent.methods == null ) {
			parent.methods = new AbstractMethodDeclaration[1];
			parent.methods[0] = method;
		} else {
			boolean injectionComplete = false;
			if ( method instanceof ConstructorDeclaration ) {
				for ( int i = 0 ; i < parent.methods.length ; i++ ) {
					if ( parent.methods[i] instanceof ConstructorDeclaration &&
							(parent.methods[i].bits & ASTNode.IsDefaultConstructor) != 0 ) {
						EclipseAST.Node tossMe = type.getNodeFor(parent.methods[i]);
						parent.methods[i] = method;
						if ( tossMe != null ) tossMe.up().removeChild(tossMe);
						injectionComplete = true;
						break;
					}
				}
			}
			if ( !injectionComplete ) {
				AbstractMethodDeclaration[] newArray = new AbstractMethodDeclaration[parent.methods.length + 1];
				System.arraycopy(parent.methods, 0, newArray, 0, parent.methods.length);
				newArray[parent.methods.length] = method;
				parent.methods = newArray;
			}
		}
		
		type.add(method, Kind.METHOD).recursiveSetHandled();
	}
}
