package org.example;

import myclass.Field;
import myclass.Schema;

import javax.tools.*;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String schemaPath = "C:\\Users\\Lyner Wong\\Desktop\\New folder\\schema.txt";
        String sampleInputPath = "C:\\Users\\Lyner Wong\\Desktop\\New folder\\sample_input.txt";

        Main main1 = new Main();
        try {
            if(!main1.doValid(schemaPath)) return;
            if(!main1.doValid(sampleInputPath)) return;

            String result = main1.readFile(schemaPath);

            String[] results = result.split("\n");

            Schema schema = new Schema();
            schema.name = results[0].split(",")[0];
            schema.description = results[0].split(",")[1];

            String classNamePath = "src/main/java/" + schema.name;
            String classNameDeserialiserPath = "src/main/java/" + schema.name +"Deserialiser";
            String extJava =".java";
            String extClass =".class";

            for(int i=1; i<results.length; i++){
                Field fd = new Field();
                schema.fieldList.add(fd);
                fd.name = results[i].split(",")[0];
                fd.start = Integer.parseInt(results[i].split(",")[1]);
                fd.end = Integer.parseInt(results[i].split(",")[2]);
            }

            StringBuilder classInfo= new StringBuilder();
            classInfo.append("public class " + schema.name + "{");
            classInfo.append(System.lineSeparator());
            for (Field fd1 : schema.fieldList) {
                classInfo.append("public String " + fd1.name + ";");
                classInfo.append(System.lineSeparator());
            }
            classInfo.append("}");


            StringBuilder classInfoDeserialiser = new StringBuilder();
            String record = "record";
            classInfoDeserialiser.append("public class " + schema.name + "Deserialiser" + "{");
            classInfoDeserialiser.append(System.lineSeparator());
            classInfoDeserialiser.append("public " + schema.name + " parse" + "(String lineFeed){");
            classInfoDeserialiser.append(System.lineSeparator());
            classInfoDeserialiser.append(schema.name + " " + record + " = new " + schema.name + "();");
            classInfoDeserialiser.append(System.lineSeparator());
            for (Field fd1 : schema.fieldList) {
                classInfoDeserialiser.append(record + "." + fd1.name);
                classInfoDeserialiser.append(" = lineFeed.substring(" + String.valueOf(fd1.start) + "," + String.valueOf(fd1.end) + ")");
                classInfoDeserialiser.append(".trim();");
                classInfoDeserialiser.append(System.lineSeparator());
            }
            classInfoDeserialiser.append("return " + record + ";");
            classInfoDeserialiser.append("}");
            classInfoDeserialiser.append(System.lineSeparator());
            classInfoDeserialiser.append("}");


            //Creating the class File
            main1.createClass(classNamePath+extJava, classInfo);
            main1.createClass(classNameDeserialiserPath+extJava, classInfoDeserialiser);

            //After create the class then compile
            List<File> classNames = new ArrayList<>();
            classNames.add(new File(classNamePath+extJava));
            classNames.add(new File(classNameDeserialiserPath+extJava));
            main1.compileClass(classNames);

            // Then call the file
            main1.displayStudentInfo(sampleInputPath, schema.name, classNamePath);

            //Before exit then remove the class file & java file
            classNames.add(new File(classNamePath+extClass));
            classNames.add(new File(classNameDeserialiserPath+extClass));
            main1.deleteFile(classNames);

        }catch(NoSuchFileException nsfe) {
            System.out.println("File not found : " + nsfe.getFile());
        }catch(Exception e){
            e.printStackTrace();
        }

    }

    private void createClass(String pathName, StringBuilder sb)throws IOException {

        File filename = new File(pathName);
        if(filename.exists()) filename.delete();
        FileOutputStream fo = new FileOutputStream(pathName);
        PrintStream ps = new PrintStream(fo);
        ps.println(sb);
        ps.close();
        fo.close();
    }

    public void compileClass(List<File> classFileNames) throws IOException{
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        File parentDirectory = classFileNames.get(0).getParentFile();
        fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(parentDirectory));
        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(classFileNames);
        compiler.getTask(null, fileManager, null, null, null, compilationUnits).call();
        fileManager.close();
    }

    private String readFile(String path) throws IOException {
        return Files.readString(Paths.get(path));
    }

    private void deleteFile(List<File> filenames){

        for(File filename: filenames){
            if(filename.exists()) filename.delete();
        }
    }

    private void displayStudentInfo(String sampleInput, String schemaName, String pathName) throws Exception{

        File studentFile = new File(pathName);
        File sourceFile = studentFile.getParentFile();
        URLClassLoader classLoader = URLClassLoader.newInstance(new URL[] { sourceFile.toURI().toURL() });
        Class myclass = classLoader.loadClass(schemaName+"Deserialiser");
        String[] inputResults = readFile(sampleInput).split("\n");

        for(int i=0; i<inputResults.length; i++){

            System.out.println(schemaName+" Result : " + (i+1));
            Method parseMethod = myclass.getMethod("parse", String.class);
            Object studentInfo = parseMethod.invoke(myclass.newInstance(), inputResults[i]);

            Class<?> studentClass = classLoader.loadClass(schemaName);
            Object student = studentClass.cast(studentInfo);
            java.lang.reflect.Field[] fields = studentClass.getDeclaredFields();

            for (java.lang.reflect.Field field : fields) {
                //Access the fields of the Student object using reflection
                java.lang.reflect.Field nameField = studentClass.getDeclaredField(field.getName());
                nameField.setAccessible(true);
                System.out.println(schemaName + " " + field.getName() + " : " + nameField.get(student));
            }
            System.out.print(System.lineSeparator());

        }

    }

    private boolean doValid(String pathName){
        File file = new File(pathName);

        if(!file.exists()){
            System.out.println("File not found : " + pathName);
            return false;
        }

        if(file.length()==0){
            System.out.println("The file content is empty..The file path : " + pathName);
            return false;
        }
        return true;
    }

}