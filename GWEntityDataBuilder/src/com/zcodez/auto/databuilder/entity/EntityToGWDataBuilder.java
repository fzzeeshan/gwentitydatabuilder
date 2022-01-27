package com.zcodez.auto.databuilder.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.zcodez.auto.databuilder.handling.EntityException;
import com.zcodez.auto.databuilder.util.APIConstants;
import com.zcodez.auto.databuilder.util.EntityBuilderConstants;
import com.zcodez.auto.databuilder.util.ExceptionHandlingUtils;
import com.zcodez.auto.databuilder.vo.Entity;
import com.zcodez.auto.databuilder.vo.ExceptionInfoVO;
import com.zcodez.auto.databuilder.vo.Field;
import com.zcodez.auto.databuilder.vo.OutputDataVO;
import groovy.ui.SystemOutputInterceptor;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: zacky
 * Date: 30/8/21
 * Time: 9:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class EntityToGWDataBuilder {


    private static final String $CONFIG_FILE_NAME = "config.json";
    private static String $JSON_FILE = "";
    private static String $OUTPUT_DIR = "";
    private static String $CONFIG_FILE_DIR = "";
    private static String $PACKAGE_NAME = "";
    public static  String $JIRA_REFERENCE = "";
    public static  String $JIRA_DESCRIPTION = "";
    public static String $AUTHOR = "author";

    public JSONParser jsonParser = new JSONParser();

    //private static final String $ETI_FILE = "resources/templates/MOJDetail_Ins.eti";
    private static final String $CLASS_TEMPLATE = "resources/templates/BuilderClassModel.gtf";
    private static final String $CLASS_CONSTRUCT_TEMPLATE = "resources/templates/ConstructModel.gtf";
    private static final String $METHOD_WITH_TEMPLATE = "resources/templates/WithModel.gtf";

    private static  String $AUTHOR_NAME = "";

    //private static final String $OUTPUT_FILE = "D:/.MyProjectz/tmp/MOJDetailBuilder_Ins.gs";
    private static ArrayList<ExceptionInfoVO> _exceptionsList = new ArrayList<ExceptionInfoVO>();
    private ExceptionHandlingUtils _exceptionHandlingUtil = new ExceptionHandlingUtils();

    private ArrayList _fieldList = new ArrayList<Field>();
    private StringBuilder _outputFileData = null;
    private static String _entityName = "MOJDetail_Ins";
    private static String _builderName = "MOJDetailBuilder_Ins";
    HashSet<String> _dataTypeSet = new HashSet<String>();
    ArrayList<String> usesList = new ArrayList<String>();

    public boolean $DATE_PRESENT, $INTEGER_PRESENT = false;

    public static void main(String[] args) {
        System.out.println("Print here");
        EntityToGWDataBuilder _currentObject = new EntityToGWDataBuilder();
        //_currentObject.processEntityFile(null);

    }



    public boolean processEntityFile(String _entityFile, Project p ) {


        boolean _processSuccessful = true;

        $JSON_FILE = _entityFile;
        $OUTPUT_DIR = _entityFile.substring(0, _entityFile.lastIndexOf("/")+1);
        $CONFIG_FILE_DIR = $OUTPUT_DIR;
        $PACKAGE_NAME = extractPackageName(_entityFile);
        //Messages.showMessageDialog(p, "File: " +$JSON_FILE + "CONFIG::: " +$CONFIG_FILE_DIR+ "CONFIG_FILE::" +$CONFIG_FILE_NAME, "GW-Entity Plugin", Messages.getInformationIcon());

        boolean isConfigLoaded = loadConfigurationFile();

        if(!isConfigLoaded){
            OutputDataVO _outputObject = new OutputDataVO(_exceptionsList);
            writeOutputFile($OUTPUT_DIR,_outputObject);
            return false;
        }

        //Messages.showMessageDialog(p, "Config loaded properly...", "GW-Entity Plugin", Messages.getInformationIcon());


        // Instantiate the Factory
        DocumentBuilderFactory _documentBuilderFactory = DocumentBuilderFactory.newInstance();
        NodeList _entityNodes = null;
        Entity _entity = null;
        Element _rootElement;

        try {
            // process XML securely, avoid attacks like XML External Entities (XXE)
            _documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            // parse XML file
            DocumentBuilder db = _documentBuilderFactory.newDocumentBuilder();
            //ath _filePath = Paths.get("resources/templates/","WithModel.gtf");
            Document _document = db.parse (loadFile(_entityFile));
            _document.getDocumentElement().normalize();

            _rootElement = _document.getDocumentElement();

            System.out.println("Root Element :" + _rootElement.getNodeName());
            System.out.println("------");

            if (isEntityFile(_document)) {
                try {
                    _entity = createRootNodeObject(_rootElement);
                    _entityNodes = getNodeList(_document, EntityBuilderConstants.$NODE_ENTITY);
                    System.out.println("Entity name in POJO: " +_entity.getEntity());
                } catch (EntityException ee) {
                    System.out.println("Error Block: " +ee.getMessage());
                    return false;
                }
            } else if (isExtensionFile(_document)) {
                try {
                    _entity = createRootNodeObject(_rootElement);
                    _entityNodes = getNodeList(_document, EntityBuilderConstants.$NODE_EXTENSION);
                } catch (EntityException ee) {
                    System.out.println(ee.getMessage());
                    return false;
                }
            }

            //Messages.showMessageDialog(p, "Root Entity done....", "GW-Entity Plugin", Messages.getInformationIcon());


            createEntityChildList(_rootElement);
            //generateFileData();
            StringBuilder _currentRowData, _usesBuilder = new StringBuilder();
            StringBuilder _finalOutputBuilder = new StringBuilder();
            for(Object _currentObject :  _fieldList){
                Field _field = (Field) _currentObject;
                if(EntityBuilderConstants.$NODE_COLUMN.equalsIgnoreCase(_field.getNodeType())){

                    if(!$DATE_PRESENT &&
                        _field.getFieldType().equalsIgnoreCase(EntityBuilderConstants.$DATE_FIELD)){
                        $DATE_PRESENT = true;
                    }

                    if(!$INTEGER_PRESENT &&
                        _field.getFieldType().equalsIgnoreCase(EntityBuilderConstants.$INTEGER_FIELD)){
                        $INTEGER_PRESENT = true;
                    }

                    _currentRowData = generateMethods(_entity, _field);
                    if(_currentRowData!=null){
                        _finalOutputBuilder.append(_currentRowData);
                    }
                }
            }

            //Messages.showMessageDialog(p, "Child Entity done....", "GW-Entity Plugin", Messages.getInformationIcon());

            _usesBuilder.append(EntityBuilderConstants.$USES_DATA_BUILDER);
            _usesBuilder.append("\n");
            if($INTEGER_PRESENT){
                _usesBuilder.append(EntityBuilderConstants.$USES_INTEGER);
                _usesBuilder.append("\n");
            }
            if($DATE_PRESENT){
                _usesBuilder.append(EntityBuilderConstants.$USES_DATE);
                _usesBuilder.append("\n");
            }

           // Messages.showMessageDialog(p, "Uses statement done....", "GW-Entity Plugin", Messages.getInformationIcon());

            _finalOutputBuilder = wrapClassStructure(_entity, _finalOutputBuilder, _usesBuilder);

           // Messages.showMessageDialog(p, "Clazz structure ready....", "GW-Entity Plugin", Messages.getInformationIcon());

            generateGosuDataBuilderClass($OUTPUT_DIR, _finalOutputBuilder, _builderName);

            //Messages.showMessageDialog(p, "Clazz should have been generated!!!", "GW-Entity Plugin", Messages.getInformationIcon());

            /*BufferedWriter bwr = new BufferedWriter(new FileWriter(loadFile($OUTPUT_FILE)));

            //write contents of StringBuffer to a file
            bwr.write(_finalOutputBuilder.toString());

            //flush the stream
            bwr.flush();

            //close the stream
            bwr.close();
*/


            resetCommonObjects();

        } catch (ParserConfigurationException pe) {
            pe.printStackTrace();
            return false;
        } catch (SAXException se) {
            se.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }finally{
            OutputDataVO _outputObject = new OutputDataVO(_exceptionsList);
            writeOutputFile($OUTPUT_DIR,_outputObject);
        }
        return _processSuccessful;
    }

    /**
     * Validates whether the file is an entity (eti) file
     *
     * @param _document
     * @return boolean
     * @since Automated Data Builder v1.0 Implementation
     */
    public boolean isEntityFile(Document _document) {
        return (_document != null && _document.getElementsByTagName(EntityBuilderConstants.$NODE_ENTITY).getLength() > 0) ? true : false;
    }

    /**
     * Validates whether the file is an extension (etx) file     *
     *
     * @param _document
     * @return boolean
     * @since Automated Data Builder v1.0 Implementation
     */
    public boolean isExtensionFile(Document _document) {
        return (_document != null && _document.getElementsByTagName(EntityBuilderConstants.$NODE_EXTENSION).getLength() > 0) ? true : false;
    }

    /**
     * Returns list of nodes under Document object
     *
     * @param _document
     * @param _typeName
     * @return NodeList
     * @since Automated Data Builder v1.0 Implementation
     */
    public NodeList getNodeList(Document _document, String _typeName) {
        return _document.getElementsByTagName(_typeName);
    }

    /**
     * Create Entity object for the given Element
     *
     * @param _rootElement
     * @return Entity
     * @throws EntityException
     * @since Automated Data Builder v1.0 Implementation
     */
    public Entity createRootNodeObject(Element _rootElement) throws EntityException {
        String _entityName, _tableName, _description, _type , _builderName = null;

        _entityName = _rootElement.getAttribute(EntityBuilderConstants.$NODE_ENTITY);
        _tableName = _rootElement.getAttribute(EntityBuilderConstants.$ATTR_TABLE);
        _description = _rootElement.getAttribute(EntityBuilderConstants.$ATTR_DESCRIPTION);
        _type = _rootElement.getAttribute(EntityBuilderConstants.$ATTR_TYPE);
        _builderName = createBuilderName(_entityName);
        if (_entityName != null) {
            return new Entity(_builderName, _description, _entityName, true, false, _tableName, _type);
        } else {
            throw new EntityException("Attribute " + EntityBuilderConstants.$NODE_ENTITY + " not found in eti file.");
        }
    }

    /**
     * Converts Guidewire ETI (Entity) file to POJO file
     *
     * @param _rootElement
     */
    public void createEntityChildList(Element _rootElement) {
        NodeList _childNodes = _rootElement.getChildNodes();
        Node _currentNode;
        String _nodeName, _description, _fieldName, _fieldType, _dataType = null;
        Boolean _isNullOk;

        HashMap<String, String> _dataTypeMap = loadDataTypes();
        for (int _nodeRef = 0; _nodeRef < _childNodes.getLength(); _nodeRef++) {
            _currentNode = _childNodes.item(_nodeRef);
            if (_currentNode != null) {
                _nodeName = _currentNode.getNodeName();
                if (_currentNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element _element = (Element) _currentNode;
                    _description = _element.getAttribute(EntityBuilderConstants.$ATTR_DESCRIPTION);
                    _fieldName = _element.getAttribute(EntityBuilderConstants.$ATTR_NAME);
                    _fieldType = _element.getAttribute(EntityBuilderConstants.$ATTR_TYPE);
                    _isNullOk = Boolean.getBoolean(_element.getAttribute(EntityBuilderConstants.$ATTR_NULLOK));
                    if (!EntityBuilderConstants.$NODE_INDEX.equalsIgnoreCase(_nodeName)
                            && !EntityBuilderConstants.$NODE_ARRAY.equalsIgnoreCase(_nodeName)
                            && !EntityBuilderConstants.$NODE_IMPLEMENTS_ENTITY.equalsIgnoreCase(_nodeName)) {
                        _dataType = _dataTypeMap.get(_fieldType);
                        //System.out.println("Node: " +_nodeName+ "Field Name:" +_fieldName+ ". Field Type: " +_fieldType + " Data Type: " +_dataType);
                        _fieldList.add(new Field(_nodeName, _description, _fieldName, _isNullOk, _dataType));
                    }
                }
            }
        }
    }

    /**
     * To auto-generate method codes for the given Entity and its
     * attributes
     *
     * @param _entity
     * @param _field
     * @return
     */
    public StringBuilder generateMethods(Entity _entity, Field _field){
        _outputFileData = _outputFileData==null ? new StringBuilder(): _outputFileData;
        //StringBuilder _localBuilder = new StringBuilder();

        //File file = loadFile($METHOD_WITH_TEMPLATE);

        StringBuilder _localBuilder = new StringBuilder();
        InputStream _inputStream = loadTemplateFileAsStream($METHOD_WITH_TEMPLATE);
        Scanner _scannerInstance = new Scanner(_inputStream).useDelimiter("\\A");
        String _withMethodTemplate = _scannerInstance.hasNext() ? _scannerInstance.next() : "";

        //File is found
        //System.out.println("File Found : " + file.exists());
        try{
            //Read File Content
            //String data = new String(Files.readAllBytes(file.toPath()));
            _withMethodTemplate = _withMethodTemplate.replaceAll("<fieldName>", _field.getFieldName());
            _withMethodTemplate = _withMethodTemplate.replaceAll("<entityName>", _entity.getEntity());
            _withMethodTemplate = _withMethodTemplate.replaceAll("<builderName>", _entity.getBuilder());
            _withMethodTemplate = _withMethodTemplate.replaceAll("<dataType>", _field.getFieldType());
            _localBuilder.append(_withMethodTemplate);
            _localBuilder.append("\n\n");

            //System.out.println(data);
        }catch(Exception e){
            e.printStackTrace();
        }

        return _localBuilder;

    }


    /**
     * Load the file for the given path in parameter
     *
     * @param _filePath
     * @return File
     */
    public InputStream loadTemplateFileAsStream(String _filePath){

        // The class loader that loaded the class
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(_filePath);

        // the stream holding the file content
        if (inputStream == null) {
            throw new IllegalArgumentException("file not found! " + _filePath);
        } else {
            return inputStream;
        }
    }

    /**
     * Load the file for the given path in parameter
     *
     * @param _filePath
     * @return File
     */
    private File loadFile(String _filePath){
        URL _fileURL = this.getClass().getClassLoader().getResource(_filePath);
        File _fileObject = null;
        if(_fileURL !=null){
            _fileObject =  new File(_fileURL.getFile());
        } else{
            if(_fileObject==null){
                System.out.println("New File: " +_filePath);
                _fileObject = new File(_filePath);
            }
        }
        return _fileObject;
    }

    /**
     * Wraps the auto-generated method code with Class structure
     *
     * @param _entity
     * @param _methodBuilder
     * @return StringBuilder
     */
    public StringBuilder wrapClassStructure(Entity _entity, StringBuilder _methodBuilder, StringBuilder _usesBuilder){

        //StringBuilder _localBuilder = new StringBuilder();
        //File file = loadFile($CLASS_TEMPLATE);
        //File is found
        //System.out.println("File Found : " + file.exists());

        StringBuilder _localBuilder = new StringBuilder();
        InputStream _inputStream = loadTemplateFileAsStream($CLASS_TEMPLATE);
        Scanner _scannerInstance = new Scanner(_inputStream).useDelimiter("\\A");
        String _classTemplate = _scannerInstance.hasNext() ? _scannerInstance.next() : "";

        try{
            //Read File Content
            //String data = new String(Files.readAllBytes(file.toPath()));
            _classTemplate = _classTemplate.replaceAll("<entityName>", _entity.getEntity());
            _classTemplate = _classTemplate.replaceAll("<builderName>", _entity.getBuilder());
            _classTemplate = _classTemplate.replaceAll("<authorName>", $AUTHOR_NAME);
            _classTemplate = _classTemplate.replaceAll("<jiraReference>", $JIRA_REFERENCE);
            _classTemplate = _classTemplate.replaceAll("<jiraDescription>", $JIRA_DESCRIPTION);


            Date date = Calendar.getInstance().getTime();
            DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
            String _currentDate = formatter.format(date);
            formatter = new SimpleDateFormat("hh:mm:ss.SSS a");
            String _currentTime = formatter.format(date);


            _classTemplate = _classTemplate.replaceAll("<today>", _currentDate);
            _classTemplate = _classTemplate.replaceAll("<time>", _currentTime);

            _classTemplate = _classTemplate.replaceAll("<<withModel>>", _methodBuilder.toString());
            _classTemplate = _classTemplate.replaceAll("<<usesStatement>>", _usesBuilder.toString());
            _classTemplate = _classTemplate.replaceAll("<<packageName>>", $PACKAGE_NAME);
            _localBuilder.append(_classTemplate);
            _localBuilder.append("\n\n");
            System.out.println(_classTemplate);

        }catch(Exception e){
            e.printStackTrace();
        }

        //_localBuilder.append(_methodBuilder);
        //_localBuilder.append("\n\n");
        //_localBuilder.append(EntityBuilderConstants.$CLOSE_CURLY_BRACE);

        return _localBuilder;

    }

    /**
     * Returning the name for auto-generated class
     * @param _entityName
     * @return String
     * @since Automated Data Builder v1.0 Implementation
     */
    public String createBuilderName(String _entityName){
        if (_entityName.endsWith(EntityBuilderConstants.$ENTITY_ENDS_WITH_INS)) {
            return _entityName.replaceAll(EntityBuilderConstants.$ENTITY_ENDS_WITH_INS,"Builder_Ins");
        } else if (_entityName.endsWith(EntityBuilderConstants.$ENTITY_ENDS_WITH_EXT)) {
            return _entityName.replaceAll(EntityBuilderConstants.$ENTITY_ENDS_WITH_EXT,"Builder_Ext");
        } else {
            return _entityName.concat("Builder");
        }
    }

    public HashMap<String, String> loadDataTypes() {

        HashMap<String, String> _dataTypeMap = new HashMap<String, String>();
        _dataTypeMap.put("varchar", "String");
        _dataTypeMap.put("shorttext", "String");
        _dataTypeMap.put("text", "String");
        _dataTypeMap.put("mediumtext", "String");

        _dataTypeMap.put("integer", "int");
        _dataTypeMap.put("percentage", "int");
        _dataTypeMap.put("currencyamount", "int");

        _dataTypeMap.put("dateonly", "Date");
        _dataTypeMap.put("datetime", "Date");
        _dataTypeMap.put("bit", "boolean");

        return _dataTypeMap;

    }

    public void resetCommonObjects(){
        _dataTypeSet = null;
        _fieldList = null;
    }


    /**
     *
     * @param _sourceFile
     * @return
     */
    private String extractPackageName(String _sourceFile){

        int _srcIndex = _sourceFile.indexOf("src");
        String _packageName =   _srcIndex>=0 ?
                _sourceFile.substring(_srcIndex + 4, _sourceFile.lastIndexOf("/")) :
                _sourceFile.substring(0, _sourceFile.lastIndexOf("/"));
        _packageName = _packageName.replaceAll("/", ".");
        return _packageName;
    }


    /**
     *
     * @return
     */
    private boolean loadConfigurationFile() {
        FileReader reader = null;

        try{


            reader = loadConfigFile($CONFIG_FILE_DIR + $CONFIG_FILE_NAME);
            if(reader != null){
                JSONObject _jsonObj = (JSONObject) jsonParser.parse(reader);
                if(_jsonObj!=null){

                    Object _value = _jsonObj.get(APIConstants.$JIRA_REFERENCE);
                    if(_value!=null){
                        $JIRA_REFERENCE = _value.toString();
                        _value = null;
                    }else{
                        _exceptionsList.add(
                                new ExceptionInfoVO(APIConstants.$EXCEPTION_CODE_INFO,
                                        APIConstants.$100_EXCEPTION_INFO_VALUE,
                                        _exceptionHandlingUtil.getExceptionShortName(APIConstants.$100_SHORT_NAME,"JIRA Reference"),
                                        _exceptionHandlingUtil.getExceptionDescription(APIConstants.$100_SHORT_NAME, "jiraReference", "JIRA Reference")));
                    }

                    _value = _jsonObj.get(APIConstants.$JIRA_DESCRIPTION);
                    if(_value!=null){
                        $JIRA_DESCRIPTION = _value.toString();
                        _value = null;
                    }else{
                        _exceptionsList.add(
                                new ExceptionInfoVO(APIConstants.$EXCEPTION_CODE_INFO,
                                        APIConstants.$100_EXCEPTION_INFO_VALUE,
                                        _exceptionHandlingUtil.getExceptionShortName(APIConstants.$100_SHORT_NAME, "JIRA Reference"),
                                        _exceptionHandlingUtil.getExceptionDescription(APIConstants.$100_SHORT_NAME,"jiraDescription", "JIRA Description")));
                    }

                    _value = _jsonObj.get(APIConstants.$AUTHOR);
                    if(_value!=null){
                        $AUTHOR_NAME = _value.toString();
                        _value = null;
                    }else{
                        _exceptionsList.add(
                                new ExceptionInfoVO(APIConstants.$EXCEPTION_CODE_INFO,
                                        APIConstants.$100_EXCEPTION_INFO_VALUE,
                                        _exceptionHandlingUtil.getExceptionShortName(APIConstants.$100_SHORT_NAME,"Author"),
                                        _exceptionHandlingUtil.getExceptionDescription(APIConstants.$100_SHORT_NAME,"author", "Author")));
                    }


                    System.out.println("Author: " +$AUTHOR_NAME);


                    return true;
                }
            }
        } catch (FileNotFoundException e) {
            _exceptionsList.add(
                    new ExceptionInfoVO(APIConstants.$EXCEPTION_CODE_ERROR,
                            APIConstants.$400_EXCEPTION_ERROR_VALUE,
                            APIConstants.$400_SHORT_NAME,
                            APIConstants.$400_DESCRIPTION));
            return false;
        } catch (ParseException e) {
            _exceptionsList.add(
                    new ExceptionInfoVO(APIConstants.$EXCEPTION_CODE_ERROR,
                            APIConstants.$401_EXCEPTION_ERROR_VALUE,
                            APIConstants.$401_SHORT_NAME,
                            APIConstants.$401_DESCRIPTION));
            return false;
        }catch (IOException e) {
            _exceptionsList.add(
                    new ExceptionInfoVO(APIConstants.$EXCEPTION_CODE_ALERT,
                            APIConstants.$501_EXCEPTION_ALERT_VALUE,
                            APIConstants.$501_SHORT_NAME,
                            APIConstants.$501_DESCRIPTION));
            return false;
        }  catch (Exception e) {
            _exceptionsList.add(
                    new ExceptionInfoVO(APIConstants.$EXCEPTION_CODE_ALERT,
                            APIConstants.$502_EXCEPTION_ALERT_VALUE,
                            APIConstants.$502_SHORT_NAME,
                            APIConstants.$502_DESCRIPTION));
            return false;
        }
        return false;
    }



    /**
     * Load the file for the given path in parameter
     *
     * @param _filePath
     * @return File
     */
    public FileReader loadConfigFile(String _filePath) throws FileNotFoundException {
        FileReader _reader = null;
        System.out.println(_filePath);
        URL _fileURL = this.getClass().getClassLoader().getResource(_filePath);
        if(_fileURL !=null){
            _reader =  new FileReader(_fileURL.getFile());
        }else{
            _reader = new FileReader(new File(_filePath));
        }
        return _reader;
    }

    public void writeOutputFile(String _path, OutputDataVO _outputVO){
        try{
            if(_outputVO!=null){
                File _outputFile = new File(_path + APIConstants.$EXCEPTION_FILE);
                ObjectMapper mapper = new ObjectMapper();
                mapper.writerWithDefaultPrettyPrinter().writeValue(_outputFile,  _outputVO );
            }
        }catch (IOException e){
            e.printStackTrace();;
        }
    }


    public void generateGosuDataBuilderClass(String _path, StringBuilder _content, String _clazzName){
        try{
            File _file = new File(_path);
            _file.mkdir();
            FileWriter fw = new FileWriter( _path +_clazzName+ ".gs");
            fw.write(_content.toString());
            fw.close();
            //Files.write(path, _content.toString().getBytes(StandardCharsets.UTF_8));
        }catch (IOException e){
            e.printStackTrace();;
        }
    }

}
