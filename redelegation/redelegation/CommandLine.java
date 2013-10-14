/**
 * 
 */
package redelegation;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.libs.org.objectweb.asm.ClassReader;
import lombok.libs.org.objectweb.asm.tree.ClassNode;
import lombok.libs.org.objectweb.asm.tree.MethodInsnNode;
import lombok.libs.org.objectweb.asm.tree.MethodNode;
import lombok.libs.org.objectweb.asm.tree.AbstractInsnNode;









import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * @author mzohour
 *
 */
public class CommandLine {

	/**
	 * @param args
	 * @throws IOException 
	 */

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		if (args.length != 1) {
			System.err.println("Error! You should specify absolute path to your project dir.");
			return;
		}
		File path = new File(args[0]);
		
		if (!path.isAbsolute()) {
			System.err.println("Error! Path should be Absolute!");			
			return;
		}
		File manifestFile = new File(path, "AndroidManifest.xml");
		if (!manifestFile.exists()) {
			System.err.println("Error! Can't find AndroidManifest.xml!");	
			return;
		}
		
		File srcDir = new File(path, "src");
		if (!srcDir.exists()) {
			System.err.println("Error! Can't find bin directory!");	
			return;
		}
		
		File binDir = new File(path, "bin");
		if (!binDir.exists()) {
			System.err.println("Error! Can't find bin directory!");	
			return;
		}
		
		AndroidManifestParser manifestParser =
				new AndroidManifestParser(manifestFile.getAbsolutePath());
		for (AndroidManifestParser.Component i : manifestParser.mComponents.values()) {
			System.out.println("name:  " + i.mName +  "  actions:  "  + i.mActions  + "  permissions:  " + i.mPermissions);			
		}
		Set<File> classFiles = Sets.newHashSet();
		Set<File> javaFiles = Sets.newHashSet();
		
		findFiles(srcDir, ".java", javaFiles);
		findFiles(binDir, ".class", classFiles);
		System.out.println(javaFiles + "\n" + classFiles);
		Map<String,ClassNode> classNodes = Maps.newHashMap();
		for (File file : classFiles) {
	        InputStream inputStream = new FileInputStream(file);
	        ClassReader reader = new ClassReader(inputStream);
	        ClassNode node = new ClassNode();
	        reader.accept(node, 0);
	        classNodes.put(node.name, node);			
		}
		
		for ( AndroidManifestParser.Component i : manifestParser.mComponents.values()) {
			String className = i.mName;
			ClassNode classNode = classNodes.get(className);
			if (classNode == null) {
				continue;
			}
			Set<String> visited = Sets.newHashSet();
			Set<String> methodNames = Sets.newHashSet();
			Iterator<MethodNode> methodIterator = classNode.methods.iterator();
			while (methodIterator.hasNext()) {
				methodNames.add(methodIterator.next().name);
			}
			rec(classNode, methodNames, visited, classNodes);
		}
		
		

		
	}

	private static void rec(
			ClassNode classNode,
			Set<String> methodNames,
			Set<String> visited, 
			Map<String, ClassNode> classNodes) {
		System.err.println("&&&&&&" + classNode.name + methodNames);
		Set methods = Sets.newHashSet();
		Iterator<MethodNode> methodIterator = classNode.methods.iterator();
		while (methodIterator.hasNext()) {
			MethodNode methodNode = methodIterator.next();
			if (!methodNames.contains(methodNode.name)) {
				continue;
			}
			visited.add(classNode.name + methodNode.name);
			Iterator<AbstractInsnNode> instructionIterator = methodNode.instructions.iterator();
			while(instructionIterator.hasNext()) {
				AbstractInsnNode instruction = instructionIterator.next();
				if (instruction instanceof MethodInsnNode) {
					MethodInsnNode callee = (MethodInsnNode) instruction;
					String hashVal = callee.owner + callee.name;
					if (visited.contains(hashVal)) {
						continue;
					}
					ClassNode nextNode = classNodes.get(callee.owner);
					if (nextNode == null) {
						continue;
					}
					HashSet <String> methodSet = Sets.newHashSet();
					methodSet.add(callee.name);
					rec(nextNode, methodSet, visited, classNodes);
					System.err.println(hashVal + " $$$$$$ "+ classNode.name + methodNode.name);
				}
			}
		}
	}
	
	public static void findFiles(File root, String type, Set <File> allFiles)
	{
	    File[] files = root.listFiles(); 
	    for (File file : files) {
	        if (file.isFile()) {
	        	if (file.getName().endsWith(type)) {
	        		allFiles.add(file);
	        	}
	        } 
	        else if (file.isDirectory()) {
	            findFiles(file, type, allFiles);
	        }
	    }
	}

}
