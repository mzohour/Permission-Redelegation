/**
 * 
 */
package redelegation;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.AbstractInsnNode;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.ast.Node;
import lombok.ast.grammar.Source;

/**
 * @author mzohour
 *
 */
public class CommandLine {

	/**
	 * @param args
	 * @throws IOException 
	 */
	private static final File ROOT_FILE = new File("");
	private static final String API_DIR = "permission-maps/api-map";
	private static final String INTENT_RECEIVE_DIR = "permission-maps/intent-receive-map";
	private static final String INTENT_SEND_DIR = "permission-maps/intent-send-map";
	private static final String CONTENT_PROVIDER_DIR = "permission-maps/content-provider";
	private static final String INTENT_STR_DIR = "android-sources/intent-mapping";
	

	
	public static void main(String[] args) throws IOException {

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
		HashMultimap<String,String> allApiPermissionMap = HashMultimap.create();
		HashMultimap<String,String> allIntentsSentPermissionMap = HashMultimap.create();
		HashMultimap<String,String> allIntentsReceivedPermissionMap = HashMultimap.create();
		HashMultimap<String,String> allIntentsNameStringMap = HashMultimap.create();
		HashMultimap<String,String> allProviderPermissionMap = HashMultimap.create();

		
		createMap(new File(ROOT_FILE.getAbsolutePath(), API_DIR), allApiPermissionMap);
		createMap(new File(ROOT_FILE.getAbsolutePath(), INTENT_SEND_DIR), allIntentsSentPermissionMap);
		createMap(new File(ROOT_FILE.getAbsolutePath(), INTENT_RECEIVE_DIR), allIntentsReceivedPermissionMap);
		createMap(new File(ROOT_FILE.getAbsolutePath(), INTENT_STR_DIR), allIntentsNameStringMap);
		createMap(new File(ROOT_FILE.getAbsolutePath(), CONTENT_PROVIDER_DIR), allProviderPermissionMap);

		
		AndroidManifestParser manifestParser =
				new AndroidManifestParser(manifestFile.getAbsolutePath());

		Set<File> classFiles = Sets.newHashSet();
		Set<File> javaFiles = Sets.newHashSet();
		
		findFiles(srcDir, ".java", javaFiles);
		findFiles(binDir, ".class", classFiles);
		Map<String,ClassNode> classNodes = Maps.newHashMap();
		Map<String,Node> javaNodes = Maps.newHashMap();		
		
		for (File file : classFiles) {
	        InputStream inputStream = new FileInputStream(file);
	        ClassReader reader = new ClassReader(inputStream);
	        ClassNode node = new ClassNode();
	        reader.accept(node, 0);
	        String [] arr = node.name.split("/");
	        classNodes.put(arr[arr.length-1], node);
			//System.err.println("@@@" + arr[arr.length-1]);

		}
		
		for (File file : javaFiles) {
	        //System.err.println(file.getName());
			String content = readFile(file.getAbsolutePath(), StandardCharsets.UTF_8);
	        Source s = new Source(content, file.getName());
	        List<Node> nodes = s.getNodes();
	        Node lombokNode = nodes.get(0);
	        javaNodes.put(file.getName(), lombokNode);
		}

		
		for ( AndroidManifestParser.Component component : manifestParser.mComponents.values()) {
			if (!component.isPublic()) {
				continue;
			}
			//System.err.println("$$$$" + component.mName);

			for (String action : component.mActions) {
				if (allIntentsReceivedPermissionMap.containsKey(action)) {
					for (String permission : allIntentsReceivedPermissionMap.get(action)){
						RedelegationIssue issue = new RedelegationIssue(permission, component.mName, component.mName, action);
						issue.raiseIssue();
					}
				}
			}

			String [] arr = component.mName.split("/");
			ClassNode classNode = classNodes.get(arr[arr.length-1]);

			if (classNode == null) {
				continue;
			}
			//System.err.println("***********" + classNode.name);

			
			Set<String> visited = Sets.newHashSet();
			Set<String> lombokVisited = Sets.newHashSet();
			Set<String> methodNames = Sets.newHashSet();
			Iterator<MethodNode> methodIterator = classNode.methods.iterator();
			while (methodIterator.hasNext()) {
				methodNames.add(methodIterator.next().name);
			}
			rec(
				classNode,
				methodNames,
				visited,
				lombokVisited,
				classNodes, 
				javaNodes,
				allApiPermissionMap,
				allIntentsSentPermissionMap,
				allIntentsReceivedPermissionMap,
				allIntentsNameStringMap,
				allProviderPermissionMap,
				component.mName, 
				component.mPermissions
			);
			
		}	
	}

	private static void rec(
			ClassNode classNode,
			Set<String> methodNames,
			Set<String> visited,
			Set<String> lombokVisited,
			Map<String, ClassNode> classNodes,
			Map<String,Node> javaNodes,
			HashMultimap<String,String> allApiPermissionMap,
			HashMultimap<String,String> allIntentsSentPermissionMap,
			HashMultimap<String,String> allIntentsReceivedPermissionMap,
			HashMultimap<String,String> allIntentsNameStringMap,
			HashMultimap<String,String> allProviderPermissionMap,
			String componentName,
			Set<String> componentPermission) {
		
		//System.err.println("!!!" + classNode.name);

		Set methods = Sets.newHashSet();
		Iterator<MethodNode> methodIterator = classNode.methods.iterator();
		Set<String> intentsSent = Sets.newHashSet();
		Set<String> intentsReceived = Sets.newHashSet();
		Set<String> allStrings = Sets.newHashSet();
		//System.err.println("@@@@@" + classNode.name + methodNames);
		String [] stringList = classNode.name.split("/");

		String lombokName = stringList[stringList.length-1] + ".java";
		if (!lombokVisited.contains(lombokName)) {
			lombokVisited.add(lombokName);
			Node lombokNode = javaNodes.get(lombokName);
			if (lombokNode != null) {
				lombokNode.accept(new IntentVisitor(intentsSent, intentsReceived, allStrings));				
			}

			for (String intent : intentsSent) {
				if (allIntentsNameStringMap.containsKey(intent)) {
					for (String intentString : allIntentsNameStringMap.get(intent)){
						if (allIntentsSentPermissionMap.containsKey(intentString)) {
							for (String permission : allIntentsSentPermissionMap.get(intentString)){
								if (!componentPermission.contains(permission)) {
									RedelegationIssue issue = new RedelegationIssue(permission, componentName, classNode.name, intentString);
									issue.raiseIssue();
								}
							}
						}
					}
				}
			}
			for (String intent : intentsReceived) {
				if (allIntentsNameStringMap.containsKey(intent)) {
					for (String intentString : allIntentsNameStringMap.get(intent)){
						if (allIntentsReceivedPermissionMap.containsKey(intentString)) {
							for (String permission : allIntentsReceivedPermissionMap.get(intentString)){
								if (!componentPermission.contains(permission)) {
									RedelegationIssue issue = new RedelegationIssue(permission, componentName, classNode.name, intentString);
									issue.raiseIssue();
								}
							}
						}
					}
				}
			}
			for (String provider : allStrings) {
				if (allProviderPermissionMap.containsKey(provider)) {
					for (String permission : allProviderPermissionMap.get(provider)) {
						if (!componentPermission.contains(permission)) {
							RedelegationIssue issue = new RedelegationIssue(permission, componentName, classNode.name, provider);
							issue.raiseIssue();
						}
					}
				}
			}
		}
		while (methodIterator.hasNext()) {
			MethodNode methodNode = methodIterator.next();
			if (!methodNames.contains(methodNode.name)) {
				continue;
			}
			visited.add(classNode.name + methodNode.name);
			//System.err.println("####" + classNode.name + methodNode.name);
			Iterator<AbstractInsnNode> instructionIterator = methodNode.instructions.iterator();
			while(instructionIterator.hasNext()) {
				AbstractInsnNode instruction = instructionIterator.next();
				if (!(instruction instanceof MethodInsnNode)) {
					continue;
				}
				MethodInsnNode callee = (MethodInsnNode) instruction;
				String hashVal = callee.owner + callee.name;
				String apiName = (callee.owner + "." + callee.name).replace("/", ".");
				if (allApiPermissionMap.containsKey(apiName)) {
					for (String permission : allApiPermissionMap.get(apiName)) {
						if (!componentPermission.contains(permission)) {
							RedelegationIssue issue = new RedelegationIssue(permission, componentName, classNode.name, apiName);
							issue.raiseIssue();
						}
					}
				}

				if (visited.contains(hashVal)) {
					continue;
				}
				String [] arr = callee.owner.split("/");
				ClassNode nextNode = classNodes.get(arr[arr.length-1]);

				if (nextNode == null || !nextNode.name.equals(callee.owner)) {
					continue;
				}
				HashSet <String> methodSet = Sets.newHashSet();
				methodSet.add(callee.name);
				rec(
					nextNode,
					methodSet,
					visited, 
					lombokVisited,
					classNodes,
					javaNodes,
					allApiPermissionMap,
					allIntentsSentPermissionMap,
					allIntentsReceivedPermissionMap,
					allIntentsNameStringMap,
					allProviderPermissionMap,
					componentName,
					componentPermission
				);	
			}
		}
	}
	
  public static void createMap(File csvFile, HashMultimap<String, String> permissionMap) {  
	BufferedReader br = null;
	String line = "";
	String cvsSplitBy = ",";
	try {
		br = new BufferedReader(new FileReader(csvFile));
		while ((line = br.readLine()) != null) {
			String[] country = line.split(cvsSplitBy);
			permissionMap.put(country[0], country[1]);
		}
	} catch (FileNotFoundException e) {
		e.printStackTrace();
	} catch (IOException e) {
		e.printStackTrace();
	} finally {
		if (br != null) {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
 }
  
  public static String readFile(String path, Charset encoding) 
		  throws IOException {
		  byte[] encoded = Files.readAllBytes(Paths.get(path));
		  return encoding.decode(ByteBuffer.wrap(encoded)).toString();
  }
	
  public static void findFiles(File root, String type, Set <File> allFiles) {
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
