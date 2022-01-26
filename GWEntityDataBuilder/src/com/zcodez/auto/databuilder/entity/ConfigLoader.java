package com.zcodez.auto.databuilder.entity;


import com.zcodez.auto.databuilder.util.APIConstants;
import com.zcodez.auto.databuilder.util.ExceptionHandlingUtils;
import com.zcodez.auto.databuilder.vo.ExceptionInfoVO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: zacky
 * Date: 6/1/22
 * Time: 8:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConfigLoader {


    private static final String $CONFIG_FILE_NAME = "config.json";
    private static String $JSON_FILE = "";
    private static String $OUTPUT_DIR = "";
    private static String $CONFIG_FILE_DIR = "";
    private static String $PACKAGE_NAME = "";

    private static final String $CLASS_TEMPLATE = "resources/templates/ClassModel.gtf";
    private static String $DECLARATIONS_TEMPLATE = "resources/templates/Declarations.gtf";
    private static final String $PACKAGE_TEMPLATE = "resources/templates/PackageModel.gtf";
    private static final String $HEADER_TEMPLATE = "resources/templates/HeaderModel.gtf";
    private static final String $GET_TEMPLATE = "resources/templates/GetModel.gtf";
    private static final String $SET_TEMPLATE = "resources/templates/SetModel.gtf";

    private static String $JIRA_REFERENCE = "JIRA Reference";
    private static String $JIRA_DESCRIPTION = "Place description here";
    private static String $AUTHOR_NAME = "AUTHOR";
    private static String $RELEASE_VERSION = "1.0";

    private static ArrayList<String> _generatedClazzezList = new ArrayList<String>();
    private static ArrayList<String> _erroredClazzezList = new ArrayList<String>();


    public JSONParser jsonParser = new JSONParser();
    private StringBuilder _outputFileData, _propzMethodBuilder = null;
    private static ArrayList<ExceptionInfoVO> _exceptionsList = new ArrayList<ExceptionInfoVO>();
    private ExceptionHandlingUtils _exceptionHandlingUtil = new ExceptionHandlingUtils();

    /**
     *
     * @return
     */
    public boolean loadConfigurationFile() {
        FileReader reader = null;

        try{

            reader = loadFile($CONFIG_FILE_DIR + $CONFIG_FILE_NAME);
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
    public FileReader loadFile(String _filePath) throws FileNotFoundException {
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

}
