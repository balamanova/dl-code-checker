package com.kz.iitu.codeparser.service;

import com.kz.iitu.codeparser.model.PointResult;
import com.kz.iitu.codeparser.model.ProjectResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class CodeParserService {

    private static final Logger logger = LoggerFactory.getLogger(CodeParserService.class);

    public static String PATH_TO_ZIP = "src";
    public static String MAIN_CLASS_NAME = "Test";
    public static String JAVA_RUN_COMMAND = "java -cp " +PATH_TO_ZIP+ " " + MAIN_CLASS_NAME;
    public static String JAVAC_RUN_COMMAND = "javac -cp src "+PATH_TO_ZIP + "/" +MAIN_CLASS_NAME+".java";

    public void putToSource(MultipartFile file) throws IOException {
        String filePath = new File(PATH_TO_ZIP + "/" + file.getOriginalFilename()).getAbsolutePath();
        File f1 = new File(filePath);
        file.transferTo(f1);
    }

    public ProjectResult checkCodeResult(String filePath, MultipartFile resultFile) {
        ProjectResult projectResult = new ProjectResult();
        try {
            List<PointResult> result = getArrayFromFile(resultFile);
            unzip(filePath);
            runProcess(JAVAC_RUN_COMMAND);
            projectResult = runCode(JAVA_RUN_COMMAND, result);
            deleteFilesFromFolder();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return projectResult;
    }

    public static void deleteFilesFromFolder() {
        File[] files = (new File(PATH_TO_ZIP)).listFiles();
        int i=0;
        while (i<files.length){
            if(!files[i].getName().contains(".class") && !files[i].getName().contains(".java"))  {
                i++;
                continue;
            }
            if (!files[i].delete())
            {
                logger.info("Failed to delete "+files[i]);
            }
            i++;
        }
    }

    public static void unzip(String filePath) throws IOException {
        File destDir = new File(PATH_TO_ZIP);
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(filePath));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            File newFile = newFile(destDir, zipEntry);
            FileOutputStream fos = new FileOutputStream(newFile);
            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    }

    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    public static List<PointResult> getArrayFromFile(MultipartFile resultFile) {
        List<PointResult> pointResultList = new ArrayList<>();
        if (!resultFile.isEmpty()) {
            try {
                byte[] bytes = resultFile.getBytes();
                String completeData = new String(bytes);
                String[] result = completeData.split("\n");
                for (String str: result) {
                    String[] splitted = str.split("-point-");
                    pointResultList.add(new PointResult(Double.valueOf(splitted[1]), splitted[0]));
                }
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
        return pointResultList;
    }

    private static ProjectResult printLines(InputStream ins, List<PointResult> result) throws Exception {
        String line = null, resultConsole = "";
        BufferedReader in = new BufferedReader(
                new InputStreamReader(ins));
        int i = 0;
        double point = 0.0;

        while ((line = in.readLine()) != null && i<result.size()) {
            PointResult pointResult = result.get(i);
            if(pointResult.getResultLine().equals(line)) {
                point+=pointResult.getPoint();
            };
            resultConsole+=line+" \n";
            i++;
        }
        ProjectResult projectResult = new ProjectResult();
        projectResult.setTotalResult(100*point);
        projectResult.setResultConsole(resultConsole);
        return projectResult;
    }

    private static int printSimpleLines(String cmd, InputStream ins) throws Exception {
        String line = null;
        BufferedReader in = new BufferedReader(
                new InputStreamReader(ins));
        int point = 0;

        while ((line = in.readLine()) != null) {
            logger.info(cmd + " " +line);
        }
        return point;
    }


    private static ProjectResult runCode(String command, List<PointResult> result) throws Exception {
        logger.info(command + " command is runned!");
        Process pro = Runtime.getRuntime().exec(command);

        ProjectResult projectResult = printLines(pro.getInputStream(), result);
        printSimpleLines(command + " stderr:", pro.getErrorStream());
        pro.waitFor();
        logger.info(command + " exitValue() " + pro.exitValue());
        return projectResult;
    }


    private static void runProcess(String command) throws Exception {
        Process pro = Runtime.getRuntime().exec(command);
        printSimpleLines(command, pro.getInputStream());
        printSimpleLines(command, pro.getErrorStream());
        pro.waitFor();
        logger.info(command + " exitValue() " + pro.exitValue());
    }
}
