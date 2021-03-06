/**
 * PackageGenerator class that includes methods for
 * listing files in folder and reading XML documents, and
 * processing of their contents.
 * 
 */

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

public class PackageGenerator {
	
	// XPath expressions for retrieving newsItem elements

	private final static String GUID_XPATH = "/newsItem/@guid"; 
	private final static String VERSION_XPATH = "/newsItem/@version"; 
	private final static String PROVIDER_XPATH = "/newsItem/itemMeta/provider/@literal";
	private final static String VERSION_CREATED_XPATH = "/newsItem/itemMeta/versionCreated";
	private final static String PUBSTATUS_XPATH = "/newsItem/itemMeta/pubStatus/@qcode";
	private final static String TYPE_ROLE_XPATH = "/newsItem/itemMeta/role/name";
	private final static String URGENCY_XPATH = "/newsItem/contentMeta/urgency";
	private final static String DEPARTMENT_XPATH = "/newsItem/contentMeta/subject[@type='cpnat:department']/name";
	private final static String CATEGORIES_XPATH = "/newsItem/contentMeta/subject[@type='cpnat:category']/name";
	private final static String TOPICS_XPATH = "/newsItem/contentMeta/subject[@type='cpnat:topic']/name";
	private final static String TOPICS_CODE_XPATH = "/newsItem/contentMeta/subject[@type='cpnat:topic']/@qcode";
	private final static String SERVICE_NAME_XPATH = "/newsItem/itemMeta/service/name";
	private final static String LOCATION_XPATH = "/newsItem/contentMeta/located/name";
	private final static String CLASS_XPATH = "/newsItem/itemMeta/itemClass/@qcode";
	private final static String HEADLINE_XPATH = "/newsItem/contentMeta/headline";
	private final static String DESCRIPTION_XPATH = "/newsItem/contentMeta/description";
	private final static String DESCRIPTION_ROLE_XPATH = "/newsItem/contentMeta/description/@role";
	
	//Id of the root group
	private final static String ROOT_GROUP = "root";
	
	//Type of groupItem
	private final static String ROLE_ROOT = "group:root";
	private final static String ROLE_CLASSIC = "group:package";
	
	private int type_attribute;
	private String value_attribute;
	
	
	private String newsItemFolder;
	private ArrayList<NewsItem> newsItems;
	private Document xmlPackageFile;
	private PackageItem packageItem = new PackageItem();
	private String outputfilePath;
	
	public PackageGenerator(String newsItemFolder) {
		this.newsItemFolder = newsItemFolder;
		getUserInput();
		listItems();
		generatePackage();
		writePackageToFile();
	}
	
	private void getUserInput() {
	    Scanner scanner = new Scanner(System.in);
	    System.out.println("Which type of a package would you like to create (1, 2, 3 or 4)?");
        int type_attribute_idx;
        while(true) {
            try {
                type_attribute_idx = scanner.nextInt();
                if(type_attribute_idx > 0 && type_attribute_idx < 5) 
                {
                    this.type_attribute = type_attribute_idx;
                    break;
                } 
                else 
                {
                    System.out.println("This option doesn't exist");
                    scanner.nextLine();
                }
            } catch(Exception e) {
                System.out.println("This option doesn't exist");
                scanner.nextLine();
            }
        }
        
        scanner.nextLine();
        
        switch(type_attribute_idx) {
            case 1:
                System.out.println("What is the name/qcode of the topic (e.g. \"stttopic:75182\" or \"Saksalaisten niukka enemmist� haluaisi eroon eurosta\")?");
                break;
                
            case 2:
                System.out.println("What is the name of the department (e.g. \"Talous\")?");
                break;
                
            case 3:
                System.out.println("What is the name of the category (e.g. \"Politiikka\")?");
                break;
                
            case 4:
                System.out.println("What is the name of the category (e.g. \"Politiikka\")?");
                break;
                
            default:
                System.out.println("What is the name or the qcode of the topic (e.g. \"stttopic:75182\" or \"Saksalaisten niukka enemmist� haluaisi eroon eurosta\")?");
                break;
        }
        this.value_attribute = scanner.nextLine();
        
        System.out.println("Write a file name for your package XML file (e.g. myNewsItemPackage)");
        this.outputfilePath = "generated/"+scanner.nextLine()+".xml";
        System.out.println("Your package XML file has been created to "+this.outputfilePath);
        System.out.println("\nThe application will now output the XML file of your package...");
	}
	
	private void listItems() {
		
		this.newsItems = new ArrayList<NewsItem>();
		System.out.println("The application is reading the news items from '"+this.newsItemFolder+"'...");
		// List all the files that end with '.xml' in the given folder
		File[] allNewsItems = new File(this.newsItemFolder).listFiles(new FileFilter() {
			
			@Override
			public boolean accept(File file) {
				if (file.isFile() && file.getName().endsWith(".xml")) return true;
				return false;
			}
		});
		
		// Process all newsItem XML documents using Java DOM (Document Object Model)
		DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = null;
		try {
			documentBuilder = documentFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}

		// Reads all the XML documents listed
		Document xmlDocument = null;
		XPath xpath = XPathFactory.newInstance().newXPath();
		for (File newsItemFile : allNewsItems) {
			
			try {
				xmlDocument = documentBuilder.parse(newsItemFile);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (SAXException e) {
                e.printStackTrace();
            }
		
			XPathExpression expr;
			NodeList nodes;
			
			NewsItem newsItem = new NewsItem();
			try {
				//Set guid of the NewsItem
				expr = xpath.compile(GUID_XPATH);
				nodes = (NodeList)expr.evaluate(xmlDocument, XPathConstants.NODESET);
				String guid = nodes.item(0).getTextContent();
				newsItem.setGuid(guid);
				
				//Set version of the NewsItem
				expr = xpath.compile(VERSION_XPATH);
				nodes = (NodeList)expr.evaluate(xmlDocument, XPathConstants.NODESET);
				String version = nodes.item(0).getTextContent();
				newsItem.setVersion(version);
				
				//Set date and time when current version of the NewsItem was sent
                expr = xpath.compile(PROVIDER_XPATH);
                nodes = (NodeList)expr.evaluate(xmlDocument, XPathConstants.NODESET);
                String provider = nodes.item(0).getTextContent();
                newsItem.getItemMeta().setProvider(provider);
				
				//Set date and time when current version of the NewsItem was sent
				expr = xpath.compile(VERSION_CREATED_XPATH);
				nodes = (NodeList)expr.evaluate(xmlDocument, XPathConstants.NODESET);
				String version_created = nodes.item(0).getTextContent();
				newsItem.getItemMeta().setVersionCreated(version_created);
				
				//Set date and time when current version of the NewsItem was sent
                expr = xpath.compile(PUBSTATUS_XPATH);
                nodes = (NodeList)expr.evaluate(xmlDocument, XPathConstants.NODESET);
                String pub_status = nodes.item(0).getTextContent();
                newsItem.getItemMeta().setPubStatus(pub_status);
				
				//Set type of news item article
				expr = xpath.compile(TYPE_ROLE_XPATH);
				nodes = (NodeList)expr.evaluate(xmlDocument, XPathConstants.NODESET);
				String role = nodes.item(0).getTextContent();
				newsItem.getItemMeta().setRole(role);
				
				//Set headline the news item
				expr = xpath.compile(DEPARTMENT_XPATH);
				nodes = (NodeList)expr.evaluate(xmlDocument, XPathConstants.NODESET);
				String department = nodes.item(0).getTextContent();
				newsItem.getContentMeta().getSubject().setDepartment(department);
				
                //Set NewsItem urgency
                expr = xpath.compile(URGENCY_XPATH);
                nodes = (NodeList)expr.evaluate(xmlDocument, XPathConstants.NODESET);
                String urgency = nodes.item(0).getTextContent();
                newsItem.getContentMeta().setUrgency(urgency);
				
				//Set NewsItem categories
				expr = xpath.compile(CATEGORIES_XPATH);
				nodes = (NodeList)expr.evaluate(xmlDocument, XPathConstants.NODESET);
				ArrayList<String> categories = new ArrayList<String>();
				for (int i = 0; i < nodes.getLength(); i++) {
					categories.add(nodes.item(i).getTextContent());
				}
				newsItem.getContentMeta().getSubject().setCategories(categories);
				
				//Set NewsItem topic
				expr = xpath.compile(TOPICS_XPATH);
				nodes = (NodeList)expr.evaluate(xmlDocument, XPathConstants.NODESET);
				if(nodes.item(0) != null) {
					String topic = nodes.item(0).getTextContent();
					newsItem.getContentMeta().getSubject().setTopic(topic);
				} else {
					newsItem.getContentMeta().getSubject().setTopic("");
				}
				
				//Set NewsItem topic_code
				expr = xpath.compile(TOPICS_CODE_XPATH);
				nodes = (NodeList)expr.evaluate(xmlDocument, XPathConstants.NODESET);
				if(nodes.item(0) != null) {
					String topic_code = nodes.item(0).getTextContent();
					newsItem.getContentMeta().getSubject().setTopicCode(topic_code);
				} else {
					newsItem.getContentMeta().getSubject().setTopicCode("");
				}
				//Set name of news item article
				expr = xpath.compile(SERVICE_NAME_XPATH);
				nodes =(NodeList)expr.evaluate(xmlDocument, XPathConstants.NODESET);
				String service_name = nodes.item(0).getTextContent();
				newsItem.getItemMeta().setServiceName(service_name);
				
				//Set location of news item article
				expr = xpath.compile(LOCATION_XPATH);
				nodes =(NodeList)expr.evaluate(xmlDocument, XPathConstants.NODESET);
				String location = nodes.item(0).getTextContent();
				newsItem.getContentMeta().setLocation(location);
				
				//Set the class of news item
				expr = xpath.compile(CLASS_XPATH);
				nodes =(NodeList)expr.evaluate(xmlDocument, XPathConstants.NODESET);
				String item_class = nodes.item(0).getTextContent();
				newsItem.getItemMeta().setItemClass(item_class);
				
				//Set headline of news item
				expr = xpath.compile(HEADLINE_XPATH);
				nodes =(NodeList)expr.evaluate(xmlDocument, XPathConstants.NODESET);
				String headline = nodes.item(0).getTextContent();
				newsItem.getContentMeta().setHeadline(headline);
				
				//Set description of news item
                expr = xpath.compile(DESCRIPTION_XPATH);
                nodes =(NodeList)expr.evaluate(xmlDocument, XPathConstants.NODESET);
                String description = nodes.item(0).getTextContent();
                newsItem.getContentMeta().setDescription(description);
				
                //Set description of news item
                expr = xpath.compile(DESCRIPTION_ROLE_XPATH);
                nodes =(NodeList)expr.evaluate(xmlDocument, XPathConstants.NODESET);
                String description_role = nodes.item(0).getTextContent();
                newsItem.getContentMeta().setDescriptionRole(description_role);
				
				
				newsItem.setSize(newsItemFile.getTotalSpace());
				
				// Adds current news item to newsItems-list
				newsItems.add(newsItem);
				
			} catch (XPathExpressionException e) {
				e.printStackTrace();
			}
		}
	}
	
	/*
	 * Method for generating packageItem from newsItem list.
	 */
	
	private void generatePackage() {
		System.out.println("The application is starting the generation of your package...");
		ArrayList<NewsItem> newsItems;
		int items = 0;
		// Finds all items from specific department
		switch(this.type_attribute) {
			case 1:
				newsItems = getNewsItemsByTopic(this.value_attribute);
				break;
				
			case 2:
				newsItems = getNewsItemsByDepartment(this.value_attribute);
				break;
				
			case 3:
				newsItems = getNewsItemsByCategories(this.value_attribute);
				break;
				
			case 4:
				newsItems = getNewsItemsByCategories(this.value_attribute);
				break;
				
			default:
				newsItems = getNewsItemsByTopic(this.value_attribute);
				break;
		}
	    
		
		//Collections.sort(packageItems, new NewsItemComparator());
		Collections.sort(newsItems, new NewsItemComparator());
		
		//If we want to generate a package of last news
		if(this.type_attribute == 2 || this.type_attribute == 3)  {
			items = 10;
			if (newsItems.size() < 10) items = newsItems.size();
		} else {
			items = newsItems.size();
		}
		
		
		//Initialize ItemMeta of the PackageItem
		this.getPackage().getItemMeta().setItem_class("ninat:composite");
		this.getPackage().getItemMeta().setProvider("Aalto University Group Media");
		this.getPackage().getItemMeta().setVersion_created(new Date());
		this.getPackage().getItemMeta().setFirst_created(new Date());
		this.getPackage().getItemMeta().setPub_status("stat:usable");
		this.getPackage().getItemMeta().setGenerator_version("1.0");
		this.getPackage().getItemMeta().setGenerator_text("Package Generator v1");
		this.getPackage().getItemMeta().setProfile_version("1.0");
		this.getPackage().getItemMeta().setProfile_text("ranked_idref_list");
		this.getPackage().getItemMeta().setService_code("svc:aaltotop");
		this.getPackage().getItemMeta().setService_name("Aalto University");
		this.getPackage().getItemMeta().setTitle("MY PACKAGE");
		this.getPackage().getItemMeta().setEd_note("DEFINE A NOTE");
		this.getPackage().getItemMeta().setSignal_code("act:replacePrev");
		this.getPackage().getItemMeta().setLing_residref("irel:previousVersion");
		this.getPackage().getItemMeta().setLink_rel("NO TAG");
		this.getPackage().getItemMeta().setLink_version("1");
		
		//Initialize ContentMeta of the PackageItem
		this.getPackage().getContentMeta().getContributor().setJobtitle("staffjobs:cpe");
		this.getPackage().getContentMeta().getContributor().setJob_name("Chief Packaging Editor");
		this.getPackage().getContentMeta().getContributor().setName("Maxime Andre");
		this.getPackage().getContentMeta().getContributor().setQcode("mystaff:MAndre");
		this.getPackage().getContentMeta().getContributor().setNote_text("Available everyday");
		this.getPackage().getContentMeta().getContributor().setNote_validto("2013-12-31T17:30:00Z");
		this.getPackage().getContentMeta().getContributor().setDef_validto("2013-12-31T17:30:00Z");
		this.getPackage().getContentMeta().getContributor().setDef_text("Duty Packaging Editor");
		
		this.getPackage().addNewGroup(ROOT_GROUP, ROLE_ROOT, "");
		
		if(items > 0) {
			String groupName = this.getPackage().addNewGroup("", ROLE_CLASSIC, ROOT_GROUP);

			for (int i = 0; i < items; i++) {
				System.out.println("Adding news item " + newsItems.get(i).getGuid() + " (" + newsItems.get(i).getItemMeta().getVersionCreated() + ")");
				this.getPackage().addNewsItem(newsItems.get(i), groupName);
			}
		} else {
			System.out.println("No matches were found.");
		}
	}
	
	public PackageItem getPackage() {
	    return this.packageItem;
	}
	
	public ArrayList<NewsItem> getNewsItemsByDepartment(String department) {
		System.out.println("The application is selecting news item where the department is: "+department);
	    ArrayList<NewsItem> newsItems = new ArrayList<NewsItem>();
        for (int i = 0; i < this.newsItems.size(); i++) {
            NewsItem item = this.newsItems.get(i);
            if (item.getContentMeta().getSubject().getDepartment().equals(department)) {
                newsItems.add(item);
            }
        }
        return newsItems;
	}
	
	public ArrayList<NewsItem> getNewsItemsByTopic(String topic) {
		System.out.println("The application is selecting news item where the topic is: "+topic);
	    ArrayList<NewsItem> newsItems = new ArrayList<NewsItem>();
        for (int i = 0; i < this.newsItems.size(); i++) {
            NewsItem item = this.newsItems.get(i);
            if (item.getContentMeta().getSubject().getTopic().equals(topic)) {
                newsItems.add(item);
            } else if(item.getContentMeta().getSubject().getTopicCode().equals(topic)) {
            	 newsItems.add(item);
            }
        }
        return newsItems;
	}
	
	public ArrayList<NewsItem> getNewsItemsByCategories(String category) {
		System.out.println("The application is selecting news item where the category is: "+category+"\n");
		ArrayList<NewsItem> newsItems_temp = new ArrayList<NewsItem>();
        for (int i = 0; i < this.newsItems.size(); i++) {
            NewsItem item = this.newsItems.get(i);
            for(String cat : item.getContentMeta().getSubject().getCategories()) {
	            if(IsNotOnTheListAlready(item, newsItems_temp) && cat.equals(category)) {
	                newsItems_temp.add(item);
	            }
            }
        }
        return newsItems_temp;
	}
	
	public boolean IsNotOnTheListAlready(NewsItem item, ArrayList<NewsItem> list) {
	    for(NewsItem ni : list)
	    {
	        if(item.getGuid().equals(ni.getGuid()))
	        {
	            return false;
	        }
	    }
	    return true;
	}
	
	/*
	 * Method for storing packageItem as a XML document.
	 */
	
	private void writePackageToFile() {
		try {
            /////////////////////////////
            //Creating an empty XML Document

            //We need a Document
            DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
            this.xmlPackageFile = docBuilder.newDocument();

            ////////////////////////
            //Creating the XML tree

            //create the root element (packageItem in our case)
            Element root = this.xmlPackageFile.createElement("packageItem");
            
            //add all attributes
            root.setAttribute("standard", this.getPackage().getStandard());
            root.setAttribute("standardversion", this.getPackage().getStandardVersion());
            root.setAttribute("conformance", this.getPackage().getConformance());
            root.setAttribute("xmlns", this.getPackage().getXlmns());
            root.setAttribute("xmlns:xsi", this.getPackage().getXlmnsXsi());
            root.setAttribute("xsi:schemaLocation", this.getPackage().getXsiSchemaLocation());
            root.setAttribute("guid", this.getPackage().getGuid());
            this.xmlPackageFile.appendChild(root);


            //create child element, add an attribute, and add to root
            Element itemMeta = generateItemMetaXml();
            Element contentMeta = generateContentMetaXml();
            Element groupSet = generateGroupSetXml();
            
            root.appendChild(itemMeta);
            root.appendChild(contentMeta);
            root.appendChild(groupSet);            
            
            /////////////////
            //Output the XML

            //set up a transformer
            TransformerFactory transfac = TransformerFactory.newInstance();
            Transformer trans = transfac.newTransformer();
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            trans.setOutputProperty(OutputKeys.INDENT, "yes");

            //create string from xml tree
            StringWriter sw = new StringWriter();
            StreamResult result = new StreamResult(sw);
            DOMSource source = new DOMSource(this.xmlPackageFile);
            trans.transform(source, result);
            String xmlString = sw.toString();

            //print xml
            try{
                PrintWriter out  = new PrintWriter(new FileWriter(this.outputfilePath));
                out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + xmlString);
                out.close();
            } catch(Exception e){
                e.printStackTrace();
            }

        } catch (Exception e) {
            System.out.println(e);
        }
		System.out.println("Done!");
	}
	
	//Generate the Xml for the ItemMeta element
	private Element generateItemMetaXml() {
		Element itemMeta = this.xmlPackageFile.createElement("itemMeta");
		
		Element itemClass = this.xmlPackageFile.createElement("itemClass");
        itemClass.setAttribute("qcode", this.getPackage().getItemMeta().getItem_class());
        
        Element provider = this.xmlPackageFile.createElement("provider");
        provider.setAttribute("literal", this.getPackage().getItemMeta().getProvider());
        
        Element versionCreated = this.xmlPackageFile.createElement("versionCreated");
        versionCreated.setTextContent(this.getPackage().getItemMeta().getVersion_created().toString());
        
        Element firstCreated = this.xmlPackageFile.createElement("firstCreated");
        firstCreated.setTextContent(this.getPackage().getItemMeta().getFirst_created().toString());
        
        Element pubStatus = this.xmlPackageFile.createElement("pubStatus");
        pubStatus.setAttribute("qcode",this.getPackage().getItemMeta().getPub_status());
        
        Element generator = this.xmlPackageFile.createElement("generator");
        generator.setAttribute("versioninfo", this.getPackage().getItemMeta().getGenerator_version());
        generator.setTextContent(this.getPackage().getItemMeta().getGenerator_text());
        
        Element profile = this.xmlPackageFile.createElement("profile");
        profile.setAttribute("versioninfo", this.getPackage().getItemMeta().getProfile_version());
        profile.setTextContent(this.getPackage().getItemMeta().getProfile_text());
        
        Element service = this.xmlPackageFile.createElement("service");
        Element serviceName = this.xmlPackageFile.createElement("name");
        service.setAttribute("qcode", this.getPackage().getItemMeta().getService_code());
        serviceName.setTextContent(this.getPackage().getItemMeta().getService_name());
        service.appendChild(serviceName);
        
        Element title = this.xmlPackageFile.createElement("title");
        title.setTextContent(this.getPackage().getItemMeta().getTitle());
        
        Element edNote = this.xmlPackageFile.createElement("edNote");
        edNote.setTextContent(this.getPackage().getItemMeta().getEd_note());
        
        Element signal = this.xmlPackageFile.createElement("signal");
        signal.setAttribute("qcode", this.getPackage().getItemMeta().getSignal_code());
        
        Element link = this.xmlPackageFile.createElement("link");
        link.setAttribute("rel", this.getPackage().getItemMeta().getLink_rel());
        link.setAttribute("residref", this.getPackage().getItemMeta().getLing_residref());
        link.setAttribute("version", this.getPackage().getItemMeta().getLink_version());
            		
        
        itemMeta.appendChild(itemClass);
        itemMeta.appendChild(provider);
        itemMeta.appendChild(versionCreated);
        itemMeta.appendChild(firstCreated);
        itemMeta.appendChild(pubStatus);
        itemMeta.appendChild(generator);
        itemMeta.appendChild(profile);
        itemMeta.appendChild(service);
        itemMeta.appendChild(title);
        itemMeta.appendChild(edNote);
        itemMeta.appendChild(signal);
        itemMeta.appendChild(link);
        
        return itemMeta;
	}
	
	//Generate the Xml for the ContentMeta element
	private Element generateContentMetaXml() {
		
		Element contentMeta = this.xmlPackageFile.createElement("contentMeta");
		
		Element contributor = this.xmlPackageFile.createElement("contributor");
		Element headline = this.xmlPackageFile.createElement("headline");
		
		//Define the contributor element
		Element name = this.xmlPackageFile.createElement("name");
		Element nameJob = this.xmlPackageFile.createElement("name");
		name.setTextContent(this.getPackage().getContentMeta().getContributor().getName());
		nameJob.setTextContent(this.getPackage().getContentMeta().getContributor().getJob_name());
		Element definition = this.xmlPackageFile.createElement("definition");
		definition.setAttribute("validto", this.getPackage().getContentMeta().getContributor().getDef_validto());
		definition.setTextContent(this.getPackage().getContentMeta().getContributor().getDef_text());
		Element note = this.xmlPackageFile.createElement("note");
		note.setAttribute("validto", this.getPackage().getContentMeta().getContributor().getNote_validto());
		note.setTextContent(this.getPackage().getContentMeta().getContributor().getNote_text());
		
		contributor.setAttribute("jobtitle", this.getPackage().getContentMeta().getContributor().getJobtitle());
		contributor.setAttribute("qcode", this.getPackage().getContentMeta().getContributor().getQcode());
		contributor.appendChild(name);
		contributor.appendChild(nameJob);
		contributor.appendChild(definition);
		contributor.appendChild(note);
		
		//Define the headline element
		headline.setAttribute("xml:lang", this.getPackage().getContentMeta().getHeadline().getHeadline_lang());
		headline.setTextContent(this.getPackage().getContentMeta().getHeadline().getHeadline_text());
		
		
		contentMeta.appendChild(contributor);
		contentMeta.appendChild(headline);
		
		return contentMeta;
	}
	
	//Generate the Xml for the GroupSet element
	private Element generateGroupSetXml() {
		Element groupSet = this.xmlPackageFile.createElement("groupSet");
		
		groupSet.setAttribute("root", ROOT_GROUP);
		
		for(PackageItem.GroupItem group : packageItem.getGroupItems()) {
			Element groupElement = this.xmlPackageFile.createElement("group");
			groupElement.setAttribute("id", group.getId());
			groupElement.setAttribute("role", group.getRole());
			
			for(PackageItem.GroupRef group_ref : group.getGroupRef()) {
				Element groupRefElement = this.xmlPackageFile.createElement("groupRef");
				groupRefElement.setAttribute("idref", group_ref.getId());
				
				groupElement.appendChild(groupRefElement);
			}
			
			for(PackageItem.ItemRef item_ref : group.getItemRef()) {
				Element itemRefElement = this.xmlPackageFile.createElement("itemRef");
				
				itemRefElement.setAttribute("residref",item_ref.getResidref());
				itemRefElement.setAttribute("contenttype",item_ref.getContentType());
				itemRefElement.setAttribute("size",item_ref.getSize());
				
				Element itemClass = this.xmlPackageFile.createElement("itemClass");
				itemClass.setAttribute("qcode", item_ref.getItemClass());
				
				Element provider = this.xmlPackageFile.createElement("provider");
				provider.setAttribute("literal", item_ref.getProvider());
				
				Element versionCreated = this.xmlPackageFile.createElement("versionCreated");
				versionCreated.setTextContent(item_ref.getVersion_created());
				
				Element pubStatus = this.xmlPackageFile.createElement("pubStatus");
				pubStatus.setAttribute("qcode", item_ref.getPubStatus());
				
				Element headline = this.xmlPackageFile.createElement("headline");
				headline.setTextContent(item_ref.getHeadline());
				
				Element description = this.xmlPackageFile.createElement("description");
				description.setAttribute("role", item_ref.getDescriptionRole());
				description.setTextContent(item_ref.getDescription());
				
				itemRefElement.appendChild(itemClass);
				itemRefElement.appendChild(provider);
				itemRefElement.appendChild(versionCreated);
				itemRefElement.appendChild(pubStatus);
				itemRefElement.appendChild(headline);
				itemRefElement.appendChild(description);
				
				groupElement.appendChild(itemRefElement);
			}
			
			groupSet.appendChild(groupElement);
		} 
		
		return groupSet;
	}
	
	/**
	 * Comparator class for sorting NewsItems by date they were sent to customers.
	 */
	
	private class NewsItemComparator implements Comparator<NewsItem>{
		@Override
		public int compare(NewsItem item1, NewsItem item2) {
			return item1.getItemMeta().getVersionCreatedDate().compareTo(item2.getItemMeta().getVersionCreatedDate());
		}
	}

	
	/*
     * Main method
     */
	public static void main(String[] args) {
		ArrayList<String> options = new ArrayList<String>();
		
		options.add("All news items related to a specific topic");
		options.add("Most recent news items from a specific department");
		options.add("Most recent news items related to a specific category"); 
		options.add("All news items related to a specific category"); 
		
		System.out.println("<--- NEWSML-G2 news package generator --->\n");
		System.out.println("Please choose which type of a package you'd like to create.\n");
		System.out.println("Available types are:");
		for(int i = 0; i < options.size(); i++)
		{
		    System.out.println(" - "+options.get(i)+" "+(i+1));
		}
		System.out.println("");
		
		
        PackageGenerator packageGenerator = new PackageGenerator("./stt_lehtikuva_newsItems");
	}
}
