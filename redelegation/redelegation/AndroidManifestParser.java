package redelegation;

import java.io.IOException;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class AndroidManifestParser extends DefaultHandler {
    HashMultimap<String, Component> mComponents;
    private String mManifestFileName;
    private Component mTmpComponent;
    static final ImmutableSet<String> COMPONENTS = 
    		ImmutableSet.of(
    		"activity", 
    		"service",
    		"receiver"
    		);
    
    public AndroidManifestParser(String manifestFileName) {
        mManifestFileName = manifestFileName;
        mComponents = HashMultimap.create();
        parseDocument();
    }
    
    private void parseDocument() {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            SAXParser parser = factory.newSAXParser();
            parser.parse(mManifestFileName, this);
        } catch (ParserConfigurationException e) {
            System.out.println("ParserConfig error");
        } catch (SAXException e) {
            System.out.println("SAXException : xml not well formed");
        } catch (IOException e) {
            System.out.println("IO error");
        }
    }

    @Override
    public void startElement(
    		String uri,
    		String localName,
    		String elementName,
    		Attributes attributes) throws SAXException {
        if ( COMPONENTS.contains(elementName)) {
            mTmpComponent = new Component(attributes.getValue("android:name").replace(".", "/"));
            mComponents.put(elementName, mTmpComponent);
            mTmpComponent.mPermissions.add(attributes.getValue("android:permission"));
        }
        if (elementName.equals("action")) {
            mTmpComponent.mActions.add(attributes.getValue("android:name"));
            mTmpComponent.mPublic = true;
        }
        if (attributes.getValue("android:exported") != null &&
        		attributes.getValue("android:exported").equals("true")) {
        	mTmpComponent.mPublic = true;
        }
    }

    public class Component {
        String mName;
        Set<String> mPermissions;
        Set<String> mActions;
        boolean mPublic;
        
        public Component(String name){
        	mName = name;
            mActions = Sets.newHashSet();
            mPermissions = Sets.newHashSet();
        }        
        
        public boolean isPublic() {
        	return mPublic;
        }
    }
}
